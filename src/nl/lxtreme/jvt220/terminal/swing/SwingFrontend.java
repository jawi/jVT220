/**
 * jVT220 - Java VT220 terminal emulator.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package nl.lxtreme.jvt220.terminal.swing;


import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import nl.lxtreme.jvt220.terminal.*;
import nl.lxtreme.jvt220.terminal.ITerminal.ITextCell;


/**
 * Provides a Swing frontend for {@link ITerminal}.
 */
public class SwingFrontend extends JComponent implements ITerminalFrontend
{
  // INNER TYPES

  /**
   * Small container for the width, height and line spacing of a single
   * character.
   */
  static final class CharacterDimensions
  {
    final int m_height;
    final int m_width;
    final int m_lineSpacing;

    /**
     * Creates a new {@link CharacterDimensions} instance.
     * 
     * @param width
     *          the width of a single character, in pixels;
     * @param height
     *          the height of a single character, in pixels;
     * @param lineSpacing
     *          the spacing to use between two lines with characters, in pixels.
     */
    public CharacterDimensions( int width, int height, int lineSpacing )
    {
      m_width = width;
      m_height = height;
      m_lineSpacing = lineSpacing;
    }
  }

  /**
   * Asynchronous worker that reads data from an input stream and passes this to
   * the terminal backend.
   */
  final class InputStreamWorker extends SwingWorker<Void, Integer>
  {
    // VARIABLES

    private final InputStreamReader m_reader;

    // CONSTRUCTORS

    /**
     * Creates a new {@link InputStreamWorker} instance.
     * 
     * @param inputStream
     *          the input stream to read from, cannot be <code>null</code>;
     * @param encoding
     *          the character encoding to use for the read input, cannot be
     *          <code>null</code>.
     */
    public InputStreamWorker( final InputStream inputStream, String encoding ) throws IOException
    {
      m_reader = new InputStreamReader( inputStream, encoding );
    }

    // METHODS

    @Override
    protected Void doInBackground() throws Exception
    {
      while ( !isCancelled() && !Thread.currentThread().isInterrupted() )
      {
        int r = m_reader.read();
        if ( r > 0 )
        {
          publish( Integer.valueOf( r ) );
        }
      }
      return null;
    }

    @Override
    protected void process( final List<Integer> readChars )
    {
      Integer[] chars = readChars.toArray( new Integer[readChars.size()] );

      try
      {
        writeCharacters( chars );
      }
      catch ( IOException exception )
      {
        exception.printStackTrace(); // XXX
      }
    }
  }

  // CONSTANTS

  /**
   * The default encoding to use for the I/O with the outer world.
   */
  private static final String ISO8859_1 = "ISO8859-1";

  // VARIABLES

  private final String m_encoding;
  private final CharBuffer m_buffer;

  private ITerminalColorScheme m_colorScheme;
  private ICursor m_oldCursor;
  private volatile CharacterDimensions m_charDims;
  private volatile BufferedImage m_image;
  private volatile boolean m_listening;
  private ITerminal m_terminal;
  private InputStreamWorker m_inputStreamWorker;
  private Writer m_writer;

  // CONSTRUCTORS

  /**
   * Creates a new {@link SwingFrontend} instance using ISO8859-1 encoding.
   */
  public SwingFrontend()
  {
    this( ISO8859_1 );
  }

  /**
   * Creates a new {@link SwingFrontend} instance.
   * 
   * @param encoding
   *          the character encoding to use for the terminal.
   */
  public SwingFrontend( String encoding )
  {
    if ( encoding == null || "".equals( encoding.trim() ) )
    {
      throw new IllegalArgumentException( "Encoding cannot be null or empty!" );
    }

    m_encoding = encoding;
    m_buffer = new CharBuffer();
    m_colorScheme = new XtermColorScheme();

    setFont( Font.decode( "Monospaced-PLAIN-14" ) );

    mapKeyboard();

    setEnabled( false );
    setFocusable( true );
    setFocusTraversalKeysEnabled( false ); // disables TAB handling
    requestFocus();
  }

  // METHODS

  /**
   * Calculates the character dimensions for the given font, which is presumed
   * to be a monospaced font.
   * 
   * @param aFont
   *          the font to get the character dimensions for, cannot be
   *          <code>null</code>.
   * @return an array of length 2, containing the character width and height (in
   *         that order).
   */
  private static CharacterDimensions getCharacterDimensions( Font aFont )
  {
    BufferedImage im = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );

    Graphics2D g2d = im.createGraphics();
    g2d.setFont( aFont );
    FontMetrics fm = g2d.getFontMetrics();
    g2d.dispose();
    im.flush();

    int w = fm.charWidth( '@' );
    int h = fm.getAscent() + fm.getDescent();

    return new CharacterDimensions( w, h, fm.getLeading() + 1 );
  }

  /**
   * Calculates the union of two rectangles, allowing <code>null</code> values
   * to be passed in.
   * 
   * @param rect1
   *          the 1st rectangle to create the union, if <code>null</code>, the
   *          2nd argument will be returned;
   * @param rect2
   *          the 2nd rectangle to create the union, if <code>null</code>, the
   *          1st argument will be returned.
   * @return the union of the two given rectangles.
   */
  private static Rectangle union( Rectangle rect1, Rectangle rect2 )
  {
    if ( rect2 == null )
    {
      return rect1;
    }
    if ( rect1 == null )
    {
      return rect2;
    }

    return rect1.union( rect2 );
  }

  /**
   * Connects this frontend to a given input and output stream.
   * <p>
   * This method will start a background thread to read continuously from the
   * given input stream.
   * </p>
   * 
   * @param inputStream
   *          the input stream to connect to, cannot be <code>null</code>;
   * @param outputStream
   *          the output stream to connect to, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems.
   */
  @Override
  public void connect( InputStream inputStream, OutputStream outputStream ) throws IOException
  {
    if ( inputStream == null )
    {
      throw new IllegalArgumentException( "Input stream cannot be null!" );
    }
    if ( outputStream == null )
    {
      throw new IllegalArgumentException( "Output stream cannot be null!" );
    }

    disconnect();

    m_writer = new OutputStreamWriter( outputStream, m_encoding );

    m_inputStreamWorker = new InputStreamWorker( inputStream, m_encoding );
    m_inputStreamWorker.execute();

    setEnabled( true );
  }

  /**
   * Connects this frontend to a given output stream.
   * <p>
   * NOTE: when using this method, you need to explicitly call
   * {@link #writeCharacters(Integer...)} yourself in order to let anything
   * appear on the terminal.
   * </p>
   * 
   * @param outputStream
   *          the output stream to connect to, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems.
   */
  @Override
  public void connect( OutputStream outputStream ) throws IOException
  {
    if ( outputStream == null )
    {
      throw new IllegalArgumentException( "Output stream cannot be null!" );
    }

    disconnect();

    m_writer = new OutputStreamWriter( outputStream, m_encoding );

    setEnabled( true );
  }

  /**
   * Disconnects this frontend from any input and output stream.
   * 
   * @throws IOException
   *           in case of I/O problems.
   */
  @Override
  public void disconnect() throws IOException
  {
    try
    {
      if ( m_inputStreamWorker != null )
      {
        m_inputStreamWorker.cancel( true /* mayInterruptIfRunning */);
        m_inputStreamWorker = null;
      }
      if ( m_writer != null )
      {
        m_writer.close();
        m_writer = null;
      }
    }
    finally
    {
      setEnabled( false );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Dimension getMaximumTerminalSize()
  {
    Rectangle bounds = getGraphicsConfiguration().getBounds();
    Insets insets = calculateTotalInsets();

    int width = bounds.width - insets.left - insets.right;
    int height = bounds.height - insets.top - insets.bottom;

    CharacterDimensions charDims = m_charDims;

    // Calculate the maximum number of columns & lines...
    int columns = width / charDims.m_width;
    int lines = height / ( charDims.m_height + charDims.m_lineSpacing );

    return new Dimension( columns, lines );
  }

  /**
   * Returns the current terminal.
   * 
   * @return the terminal, can be <code>null</code>.
   */
  public ITerminal getTerminal()
  {
    return m_terminal;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Writer getWriter()
  {
    return m_writer;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isListening()
  {
    return m_listening;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFont( Font font )
  {
    super.setFont( font );

    m_charDims = getCharacterDimensions( font );
  }

  /**
   * @see nl.lxtreme.jvt220.terminal.ITerminalFrontend#setReverse(boolean)
   */
  @Override
  public void setReverse( boolean reverse )
  {
    m_colorScheme.setInverted( reverse );
  }

  /**
   * Sets the size of this component in pixels. Overridden in order to redirect
   * this call to {@link #terminalSizeChanged(int, int)} with the correct number
   * of columns and lines.
   */
  @Override
  public void setSize( int width, int height )
  {
    Rectangle bounds = getGraphicsConfiguration().getBounds();
    Insets insets = calculateTotalInsets();

    if ( width == 0 )
    {
      width = bounds.width - insets.left - insets.right;
    }
    else if ( width < 0 )
    {
      width = getWidth();
    }
    if ( height == 0 )
    {
      height = bounds.height - insets.top - insets.bottom;
    }
    else if ( height < 0 )
    {
      height = getHeight();
    }

    CharacterDimensions charDims = m_charDims;

    int columns = width / charDims.m_width;
    int lines = height / ( charDims.m_height + charDims.m_lineSpacing );

    terminalSizeChanged( columns, lines );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminal( ITerminal terminal )
  {
    if ( terminal == null )
    {
      throw new IllegalArgumentException( "Terminal cannot be null!" );
    }
    m_terminal = terminal;
    m_terminal.setFrontend( this );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void terminalChanged( ITextCell[] cells, BitSet heatMap )
  {
    final int columns = m_terminal.getWidth();
    final int lines = m_terminal.getHeight();

    // Create copies of these data items to ensure they remain constant for
    // the remainer of this method...
    CharacterDimensions charDims = m_charDims;

    int cw = charDims.m_width;
    int ch = charDims.m_height;
    int ls = charDims.m_lineSpacing;

    if ( m_image == null )
    {
      // Ensure there's a valid image to paint on...
      terminalSizeChanged( columns, lines );
    }

    final Graphics2D canvas = m_image.createGraphics();
    canvas.setFont( getFont() );

    final Font font = getFont();
    final FontMetrics fm = canvas.getFontMetrics();
    final FontRenderContext frc = new FontRenderContext( null, true /* aa */, true /* fractionalMetrics */);

    Color cursorColor = null;
    Rectangle repaintArea = null;

    if ( m_oldCursor != null )
    {
      repaintArea = drawCursor( canvas, m_oldCursor, m_colorScheme.getBackgroundColor() );
    }

    for ( int i = 0; i < cells.length; i++ )
    {
      boolean cellChanged = heatMap.get( i );
      if ( cellChanged )
      {
        // Cell is changed...
        final ITextCell cell = cells[i];

        final int x = ( i % columns ) * cw;
        final int y = ( i / columns ) * ( ch + ls );

        final Rectangle rect = new Rectangle( x, y, cw, ch + ls );

        canvas.setColor( convertToColor( cell.getBackground(), m_colorScheme.getBackgroundColor() ) );
        canvas.fillRect( rect.x, rect.y, rect.width, rect.height );

        final String txt = Character.toString( cell.getChar() );

        AttributedString attrStr = new AttributedString( txt );
        cursorColor = applyAttributes( cell, attrStr, font );

        AttributedCharacterIterator characterIterator = attrStr.getIterator();
        LineBreakMeasurer measurer = new LineBreakMeasurer( characterIterator, frc );

        while ( measurer.getPosition() < characterIterator.getEndIndex() )
        {
          TextLayout textLayout = measurer.nextLayout( getWidth() );
          textLayout.draw( canvas, x, y + fm.getAscent() );
        }

        repaintArea = union( repaintArea, rect );
      }
    }

    // Draw the cursor...
    m_oldCursor = m_terminal.getCursor().clone();
    if ( cursorColor == null )
    {
      cursorColor = m_colorScheme.getTextColor();
    }

    repaintArea = union( repaintArea, drawCursor( canvas, m_oldCursor, cursorColor ) );

    // Free the resources...
    canvas.dispose();

    if ( repaintArea != null )
    {
      repaintArea.grow( 5, 3 );
      repaint( repaintArea );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void terminalSizeChanged( int columns, int lines )
  {
    final Dimension dims = calculateSizeInPixels( columns, lines );

    if ( ( m_image == null ) || ( m_image.getWidth() != dims.width ) || ( m_image.getHeight() != dims.height ) )
    {
      if ( m_image != null )
      {
        m_image.flush();
      }
      m_image = getGraphicsConfiguration().createCompatibleImage( dims.width, dims.height );

      Graphics2D canvas = m_image.createGraphics();

      try
      {
        canvas.setBackground( m_colorScheme.getBackgroundColor() );
        canvas.clearRect( 0, 0, m_image.getWidth(), m_image.getHeight() );
      }
      finally
      {
        canvas.dispose();
        canvas = null;
      }

      // Update the size of this component as well...
      Insets insets = getInsets();
      super.setSize( dims.width + insets.left + insets.right, dims.height + insets.top + insets.bottom );

      repaint( 50L );
    }
  }

  /**
   * Writes the given sequence of characters directly to the terminal, similar
   * as writing to the standard output.
   * 
   * @param charSeq
   *          the sequence of characters to write, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems writing to the terminal.
   */
  @Override
  public void writeCharacters( CharSequence charSeq ) throws IOException
  {
    InputStream is = new ByteArrayInputStream( charSeq.toString().getBytes() );
    InputStreamReader isr = new InputStreamReader( is, m_encoding );

    int ch;
    while ( ( ch = isr.read() ) >= 0 )
    {
      writeCharacters( ch );
    }
  }

  /**
   * Writes the given array of characters directly to the terminal, similar as
   * writing to the standard output.
   * 
   * @param chars
   *          the characters to write, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems writing to the terminal.
   */
  @Override
  public void writeCharacters( Integer... chars ) throws IOException
  {
    m_buffer.append( chars );

    int n = m_terminal.read( m_buffer );

    m_buffer.removeUntil( n );
  }

  /**
   * Maps the keyboard to respond to keys like 'up', 'down' and the function
   * keys.
   */
  protected void mapKeyboard()
  {
    mapKeystroke( KeyEvent.VK_UP );
    mapKeystroke( KeyEvent.VK_DOWN );
    mapKeystroke( KeyEvent.VK_RIGHT );
    mapKeystroke( KeyEvent.VK_LEFT );

    mapKeystroke( KeyEvent.VK_PAGE_DOWN );
    mapKeystroke( KeyEvent.VK_PAGE_UP );
    mapKeystroke( KeyEvent.VK_HOME );
    mapKeystroke( KeyEvent.VK_END );

    mapKeystroke( KeyEvent.VK_NUMPAD0 );
    mapKeystroke( KeyEvent.VK_NUMPAD1 );
    mapKeystroke( KeyEvent.VK_NUMPAD2 );
    mapKeystroke( KeyEvent.VK_NUMPAD3 );
    mapKeystroke( KeyEvent.VK_NUMPAD4 );
    mapKeystroke( KeyEvent.VK_NUMPAD5 );
    mapKeystroke( KeyEvent.VK_NUMPAD6 );
    mapKeystroke( KeyEvent.VK_NUMPAD7 );
    mapKeystroke( KeyEvent.VK_NUMPAD8 );
    mapKeystroke( KeyEvent.VK_NUMPAD9 );
    mapKeystroke( KeyEvent.VK_MINUS );
    mapKeystroke( KeyEvent.VK_PLUS );
    mapKeystroke( KeyEvent.VK_COMMA );
    mapKeystroke( KeyEvent.VK_PERIOD );
    mapKeystroke( KeyEvent.VK_ENTER );
    mapKeystroke( KeyEvent.VK_KP_DOWN );
    mapKeystroke( KeyEvent.VK_KP_LEFT );
    mapKeystroke( KeyEvent.VK_KP_RIGHT );
    mapKeystroke( KeyEvent.VK_KP_UP );

    mapKeystroke( KeyEvent.VK_F1 );
    mapKeystroke( KeyEvent.VK_F1, InputEvent.ALT_DOWN_MASK );
    mapKeystroke( KeyEvent.VK_F2 );
    mapKeystroke( KeyEvent.VK_F2, InputEvent.ALT_DOWN_MASK );
    mapKeystroke( KeyEvent.VK_F3 );
    mapKeystroke( KeyEvent.VK_F3, InputEvent.ALT_DOWN_MASK );
    mapKeystroke( KeyEvent.VK_F4 );
    mapKeystroke( KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK );
    mapKeystroke( KeyEvent.VK_F5 );
    mapKeystroke( KeyEvent.VK_F6 );
    mapKeystroke( KeyEvent.VK_F7 );
    mapKeystroke( KeyEvent.VK_F8 );
    mapKeystroke( KeyEvent.VK_F9 );
    mapKeystroke( KeyEvent.VK_F10 );
    mapKeystroke( KeyEvent.VK_F11 );
    mapKeystroke( KeyEvent.VK_F12 );
  }

  /**
   * Maps the given keycode and modifiers to a terminal specific sequence.
   * 
   * @param keycode
   *          the keycode to map;
   * @param modifiers
   *          the modifiers to map.
   * @return a terminal-specific sequence for the given input, or
   *         <code>null</code> if no specific sequence exists for this terminal.
   */
  protected String mapKeyCode( int keycode, int modifiers )
  {
    return getTerminal().getKeyMapper().map( keycode, modifiers );
  }

  /**
   * Creates a key mapping for the given keystroke and the given action which is
   * send as literal text to the terminal.
   * 
   * @param keycode
   *          the keycode to map.
   */
  protected void mapKeystroke( int keycode )
  {
    mapKeystroke( keycode, 0 );
  }

  /**
   * Creates a key mapping for the given keystroke and the given action which is
   * send as literal text to the terminal.
   * 
   * @param keycode
   *          the keycode to map;
   * @param modifiers
   *          the modifiers to map.
   */
  protected void mapKeystroke( int keycode, int modifiers )
  {
    final KeyStroke keystroke = KeyStroke.getKeyStroke( keycode, modifiers );
    final String key = keystroke.toString();

    getInputMap().put( keystroke, key );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void paintComponent( Graphics canvas )
  {
    m_listening = false;

    canvas.setColor( m_colorScheme.getBackgroundColor() );

    Rectangle clip = canvas.getClipBounds();
    canvas.fillRect( clip.x, clip.y, clip.width, clip.height );

    try
    {
      Insets insets = getInsets();

      canvas.drawImage( m_image, insets.left, insets.top, null /* observer */);
    }
    finally
    {
      m_listening = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean processKeyBinding( KeyStroke keystroke, KeyEvent event, int condition, boolean pressed )
  {
    if ( !isEnabled() )
    {
      // Don't bother to process keys for a disabled component...
      return false;
    }

    InputMap inputMap = getInputMap( condition );
    ActionMap actionMap = getActionMap();

    try
    {
      if ( ( inputMap != null ) && ( actionMap != null ) && ( event.getID() == KeyEvent.KEY_PRESSED ) )
      {
        Object binding = inputMap.get( keystroke );
        if ( binding != null )
        {
          Action action = actionMap.get( binding );
          if ( action != null )
          {
            // Normal action; invoke it...
            return SwingUtilities.notifyAction( action, keystroke, event, this, event.getModifiers() );
          }
          else
          {
            // Keystroke we've mapped without an action, means we're going to
            // test whether there's a mapping for it. If so, use that as
            // response, otherwise respond with the 'regular' key...
            String mapping = mapKeyCode( keystroke.getKeyCode(), keystroke.getModifiers() );
            if ( mapping != null )
            {
              respond( mapping );
              return true;
            }
          }
        }

        if ( isRegularKey( keystroke ) )
        {
          respond( event.getKeyChar() );
          return true;
        }
      }
    }
    catch ( IOException exception )
    {
      exception.printStackTrace(); // XXX
    }

    return false;
  }

  /**
   * Writes a given number of characters to the terminal.
   * 
   * @param chars
   *          the characters to write, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems responding.
   */
  protected void respond( char ch ) throws IOException
  {
    if ( m_writer != null )
    {
      m_writer.write( ch );
      m_writer.flush();
    }
  }

  /**
   * Writes a given number of characters to the terminal.
   * 
   * @param chars
   *          the characters to write, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems responding.
   */
  protected void respond( String chars ) throws IOException
  {
    if ( m_writer != null )
    {
      m_writer.write( chars );
      m_writer.flush();
    }
  }

  /**
   * Applies the attributes from the given {@link TextCell} to the given
   * {@link AttributedString}.
   * 
   * @param textCell
   *          the text cell to get the attributes from;
   * @param attributedString
   *          the {@link AttributedString} to apply the attributes to;
   * @param font
   *          the font to use.
   * @return the primary foreground color, never <code>null</code>.
   */
  private Color applyAttributes( ITextCell textCell, AttributedString attributedString, Font font )
  {
    Color fg = convertToColor( textCell.getForeground(), m_colorScheme.getTextColor() );
    Color bg = convertToColor( textCell.getBackground(), m_colorScheme.getBackgroundColor() );

    attributedString.addAttribute( TextAttribute.FAMILY, font.getFamily() );
    attributedString.addAttribute( TextAttribute.SIZE, font.getSize() );
    attributedString.addAttribute( TextAttribute.FOREGROUND, textCell.isReverse() ^ textCell.isHidden() ? bg : fg );
    attributedString.addAttribute( TextAttribute.BACKGROUND, textCell.isReverse() ? fg : bg );

    if ( textCell.isUnderline() )
    {
      attributedString.addAttribute( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON );
    }
    if ( textCell.isBold() )
    {
      attributedString.addAttribute( TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD );
    }
    if ( textCell.isItalic() )
    {
      attributedString.addAttribute( TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE );
    }
    return textCell.isReverse() ^ textCell.isHidden() ? bg : fg;
  }

  /**
   * Calculates the size (in pixels) of the back buffer image.
   * 
   * @param columns
   *          the number of columns, > 0;
   * @param lines
   *          the number of lines, > 0.
   * @return a dimension with the image width and height in pixels.
   */
  private Dimension calculateSizeInPixels( int columns, int lines )
  {
    CharacterDimensions charDims = m_charDims;

    int width = ( columns * charDims.m_width );
    int height = ( lines * ( charDims.m_height + charDims.m_lineSpacing ) );
    return new Dimension( width, height );
  }

  /**
   * Calculates the total insets of this container and all of its parents.
   * 
   * @return the total insets, never <code>null</code>.
   */
  private Insets calculateTotalInsets()
  {
    // Take the screen insets as starting point...
    Insets insets = Toolkit.getDefaultToolkit().getScreenInsets( getGraphicsConfiguration() );

    Container ptr = this;
    do
    {
      Insets compInsets = ptr.getInsets();
      insets.top += compInsets.top;
      insets.bottom += compInsets.bottom;
      insets.left += compInsets.left;
      insets.right += compInsets.right;
      ptr = ptr.getParent();
    }
    while ( ptr != null );
    return insets;
  }

  /**
   * Converts a given color index to a concrete color value.
   * 
   * @param index
   *          the numeric color index, >= 0;
   * @param defaultColor
   *          the default color to use, cannot be <code>null</code>.
   * @return a color value, never <code>null</code>.
   */
  private Color convertToColor( int index, Color defaultColor )
  {
    if ( index < 1 )
    {
      return defaultColor;
    }
    return m_colorScheme.getColorByIndex( index - 1 );
  }

  /**
   * Draws the cursor on screen.
   * 
   * @param canvas
   *          the canvas to paint on;
   * @param cursor
   *          the cursor information;
   * @param color
   *          the color to paint the cursor in.
   */
  private Rectangle drawCursor( final Graphics2D canvas, final ICursor cursor, final Color color )
  {
    if ( !cursor.isVisible() )
    {
      return null;
    }

    CharacterDimensions charDims = m_charDims;

    int cw = charDims.m_width;
    int ch = charDims.m_height;
    int ls = charDims.m_lineSpacing;

    int x = cursor.getX() * cw;
    int y = cursor.getY() * ( ch + ls );

    Rectangle rect = new Rectangle( x, y, cw, ch - 2 * ls );

    canvas.setColor( color );
    canvas.draw( rect );

    return rect;
  }

  /**
   * Returns whether the given keystroke represents a "regular" key, that is, it
   * is defined and not a modifier.
   * 
   * @param keystroke
   *          the keystroke to test, cannot be <code>null</code>.
   * @return <code>false</code> if the given keystroke is either not defined or
   *         represents a modifier key (SHIFT, CTRL, etc.), <code>true</code>
   *         otherwise.
   */
  private boolean isRegularKey( KeyStroke keystroke )
  {
    int c = keystroke.getKeyCode();
    return ( c != KeyEvent.VK_UNDEFINED ) && ( c != KeyEvent.VK_SHIFT ) && ( c != KeyEvent.VK_ALT )
        && ( c != KeyEvent.VK_ALT_GRAPH ) && ( c != KeyEvent.VK_META ) && ( c != KeyEvent.VK_WINDOWS )
        && ( c != KeyEvent.VK_CONTROL );
  }
}
