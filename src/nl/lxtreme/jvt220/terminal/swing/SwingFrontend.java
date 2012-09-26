/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
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
import nl.lxtreme.jvt220.terminal.vt220.*;


/**
 * Provides a Swing frontend for {@link ITerminal}.
 */
public class SwingFrontend extends JComponent implements ITerminalFrontend
{
  // INNER TYPES

  /**
   * Asynchronous worker that reads data from an input stream and passes this to
   * the terminal backend.
   */
  final class InputStreamWorker extends SwingWorker<Void, Integer>
  {
    // VARIABLES

    private final InputStreamReader reader;
    private final ITerminal terminal;
    private final CharBuffer buffer;

    // CONSTRUCTORS

    /**
     * Creates a new {@link InputStreamWorker} instance.
     */
    public InputStreamWorker( final InputStream aInputStream, ITerminal aTerminal ) throws IOException
    {
      this.reader = new InputStreamReader( aInputStream, "ISO8859-1" );
      this.terminal = aTerminal;
      this.buffer = new CharBuffer();
    }

    // METHODS

    @Override
    protected Void doInBackground() throws Exception
    {
      while ( !isCancelled() && !Thread.currentThread().isInterrupted() )
      {
        int r = this.reader.read();
        if ( r > 0 )
        {
          publish( Integer.valueOf( r ) );
        }
      }
      return null;
    }

    @Override
    protected void process( final List<Integer> aReadChars )
    {
      this.buffer.append( aReadChars );

      try
      {
        int n = this.terminal.readInput( this.buffer );

        this.buffer.removeUntil( n );

        repaint();
      }
      catch ( IOException exception )
      {
        exception.printStackTrace(); // XXX
      }
    }
  }

  /**
   * Provides a synchronous worker that takes data from the terminal backend and
   * writes data to an outputstream.
   */
  final class OutputStreamWorker implements Closeable
  {
    // VARIABLES

    private final OutputStreamWriter writer;

    // CONSTRUCTORS

    /**
     * Creates a new {@link OutputStreamWorker} instance.
     */
    public OutputStreamWorker( final OutputStream aOutputStream ) throws IOException
    {
      this.writer = new OutputStreamWriter( aOutputStream, "ISO8859-1" );
    }

    // METHODS

    @Override
    public void close() throws IOException
    {
      try
      {
        this.writer.flush();
      }
      finally
      {
        this.writer.close();
      }
    }

    public void write( final char... aData ) throws IOException
    {
      char[] data = aData;
      this.writer.write( data );
      this.writer.flush();
    }

    public void write( final String aData ) throws IOException
    {
      write( aData.toCharArray() );
    }
  }

  private class SendLiteralAction extends AbstractAction
  {
    private final char[] chars;

    public SendLiteralAction( char... aChars )
    {
      this.chars = Arrays.copyOf( aChars, aChars.length );
    }

    @Override
    public void actionPerformed( ActionEvent aEvent )
    {
      writeCharacters( this.chars );
    }
  }

  // VARIABLES

  private ITerminalColorScheme colorScheme;

  private int charWidth;
  private int charHeight;
  private int lineSpacing;

  private ICursor oldCursor;

  private volatile BufferedImage image;
  private volatile boolean listening;

  private volatile ITerminal terminal;
  private volatile InputStreamWorker inputStreamWorker;
  private volatile OutputStreamWorker outputStreamWorker;

  // CONSTRUCTORS

  /**
   * Creates a new {@link SwingFrontend} instance.
   * 
   * @param aColumns
   *          the number of initial columns, > 0;
   * @param aLines
   *          the number of initial lines, > 0.
   */
  public SwingFrontend()
  {
    this.colorScheme = new XtermColorScheme();

    setFont( Font.decode( "Monospaced-PLAIN-14" ) );

    enableEvents( AWTEvent.KEY_EVENT_MASK );

    setEnabled( false );
    setFocusable( true );
    setFocusTraversalKeysEnabled( false ); // disables TAB handling
    requestFocus();

    InputMap inputMap = getInputMap();
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP, 0 ), new SendLiteralAction( '\033', 'O', 'A' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN, 0 ), new SendLiteralAction( '\033', 'O', 'B' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_RIGHT, 0 ), new SendLiteralAction( '\033', 'O', 'C' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_LEFT, 0 ), new SendLiteralAction( '\033', 'O', 'D' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F1, 0 ), new SendLiteralAction( '\033', 'O', 'P' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F2, 0 ), new SendLiteralAction( '\033', 'O', 'Q' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ), new SendLiteralAction( '\033', 'O', 'R' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F4, 0 ), new SendLiteralAction( '\033', 'O', 'S' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F5, 0 ), new SendLiteralAction( '\033', 'O', 't' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F6, 0 ), new SendLiteralAction( '\033', 'O', 'u' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F7, 0 ), new SendLiteralAction( '\033', 'O', 'v' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F8, 0 ), new SendLiteralAction( '\033', 'O', 'I' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F9, 0 ), new SendLiteralAction( '\033', 'O', 'w' ) );
    inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_F10, 0 ), new SendLiteralAction( '\033', 'O', 'x' ) );
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
  private static int[] getCharacterDimensions( Font aFont )
  {
    BufferedImage im = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );

    Graphics2D g2d = im.createGraphics();
    g2d.setFont( aFont );
    FontMetrics fm = g2d.getFontMetrics();
    g2d.dispose();
    im.flush();

    int w = fm.charWidth( '@' );
    int h = fm.getAscent() + fm.getDescent();

    return new int[] { w, h, fm.getLeading() + 1 };
  }

  /**
   * Connects this frontend to a given input and output stream.
   * 
   * @param aInputStream
   *          the input stream to connect to;
   * @param aOutputStream
   *          the output stream to connect to.
   * @throws IOException
   *           in case of I/O problems.
   */
  public void connect( InputStream aInputStream, OutputStream aOutputStream ) throws IOException
  {
    disconnect();

    this.inputStreamWorker = new InputStreamWorker( aInputStream, this.terminal );
    this.outputStreamWorker = new OutputStreamWorker( aOutputStream );

    this.inputStreamWorker.execute();

    setEnabled( true );
  }

  /**
   * Disconnects this frontend from any input and output stream.
   * 
   * @throws IOException
   *           in case of I/O problems.
   */
  public void disconnect() throws IOException
  {
    try
    {
      if ( this.inputStreamWorker != null )
      {
        this.inputStreamWorker.cancel( true /* mayInterruptIfRunning */);
        this.inputStreamWorker = null;
      }
      if ( this.outputStreamWorker != null )
      {
        this.outputStreamWorker.close();
        this.outputStreamWorker = null;
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

    // Calculate the maximum number of columns & lines...
    int columns = width / this.charWidth;
    int lines = height / ( this.charHeight + this.lineSpacing );
    
    return new Dimension( columns, lines );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isListening()
  {
    return this.listening;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFont( Font aFont )
  {
    super.setFont( aFont );

    int[] cdims = getCharacterDimensions( aFont );

    this.charWidth = cdims[0];
    this.charHeight = cdims[1];
    this.lineSpacing = cdims[2];
  }

  /**
   * Sets the size of this component in pixels. Overridden in order to redirect
   * this call to {@link #terminalSizeChanged(int, int)} with the correct number
   * of columns and lines.
   */
  @Override
  public void setSize( int aWidth, int aHeight )
  {
    Rectangle bounds = getGraphicsConfiguration().getBounds();
    Insets insets = calculateTotalInsets();

    if ( aWidth == 0 )
    {
      aWidth = bounds.width - insets.left - insets.right;
    }
    else if ( aWidth < 0 )
    {
      aWidth = getWidth();
    }
    if ( aHeight == 0 )
    {
      aHeight = bounds.height - insets.top - insets.bottom;
    }
    else if ( aHeight < 0 )
    {
      aHeight = getHeight();
    }

    int columns = aWidth / this.charWidth;
    int lines = aHeight / ( this.charHeight + this.lineSpacing );

    terminalSizeChanged( columns, lines );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTerminal( ITerminal aTerminal )
  {
    if ( aTerminal == null )
    {
      throw new IllegalArgumentException( "Terminal cannot be null!" );
    }
    this.terminal = aTerminal;
    this.terminal.setFrontend( this );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void terminalChanged( ITextCell[] aCells, boolean[] aHeatMap )
  {
    final int columns = this.terminal.getWidth();
    final int lines = this.terminal.getHeight();

    // Create copies of these data items to ensure they remain constant for
    // the remainer of this method...
    int cw = this.charWidth;
    int ch = this.charHeight;
    int ls = this.lineSpacing;

    if ( this.image == null )
    {
      // Ensure there's a valid image to paint on...
      terminalSizeChanged( columns, lines );
    }

    final Graphics2D canvas = this.image.createGraphics();
    canvas.setFont( getFont() );

    final Font font = getFont();
    final FontMetrics fm = canvas.getFontMetrics();
    final FontRenderContext frc = new FontRenderContext( null, false, true );

    if ( this.oldCursor != null )
    {
      drawCursor( canvas, this.oldCursor, this.colorScheme.getBackgroundColor() );
    }

    Color cursorColor = null;
    Rectangle repaintArea = new Rectangle();

    for ( int i = 0; i < aHeatMap.length; i++ )
    {
      if ( aHeatMap[i] )
      {
        // Cell is changed...
        final ITextCell cell = aCells[i];

        final int x = ( i % columns ) * cw;
        final int y = ( i / columns ) * ( ch + ls );

        final Rectangle rect = new Rectangle( x, y, cw, ch + ls );

        canvas.setColor( convertToColor( cell.getBackground(), this.colorScheme.getBackgroundColor() ) );
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

        repaintArea = rect.intersection( repaintArea );
      }
    }

    // Draw the cursor...
    this.oldCursor = this.terminal.getCursor().clone();
    if ( cursorColor == null )
    {
      cursorColor = this.colorScheme.getPlainTextColor();
    }

    drawCursor( canvas, this.oldCursor, cursorColor );

    // Free the resources...
    canvas.dispose();

    if ( !repaintArea.isEmpty() )
    {
      repaint( repaintArea );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void terminalSizeChanged( int aColumns, int aLines )
  {
    final Dimension dims = calculateSizeInPixels( aColumns, aLines );

    if ( ( this.image == null ) || ( this.image.getWidth() != dims.width ) || ( this.image.getHeight() != dims.height ) )
    {
      if ( this.image != null )
      {
        this.image.flush();
      }
      this.image = getGraphicsConfiguration().createCompatibleImage( dims.width, dims.height );

      Graphics2D canvas = this.image.createGraphics();

      try
      {
        canvas.setBackground( this.colorScheme.getBackgroundColor() );
        canvas.clearRect( 0, 0, this.image.getWidth(), this.image.getHeight() );
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
   * {@inheritDoc}
   */
  @Override
  protected void paintComponent( Graphics aCanvas )
  {
    this.listening = false;

    try
    {
      Insets insets = getInsets();

      aCanvas.drawImage( this.image, insets.left, insets.top, null /* observer */);
    }
    finally
    {
      this.listening = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void processKeyEvent( KeyEvent aEvent )
  {
    int id = aEvent.getID();
    if ( id == KeyEvent.KEY_TYPED )
    {
      writeCharacters( aEvent.getKeyChar() );
      aEvent.consume();
    }

    super.processKeyEvent( aEvent );
  }

  /**
   * @param aChars
   */
  protected void writeCharacters( char... aChars )
  {
    try
    {
      this.outputStreamWorker.write( aChars );
    }
    catch ( IOException e )
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Applies the attributes from the given {@link TextCell} to the given
   * {@link AttributedString}.
   * 
   * @param aTextCell
   *          the text cell to get the attributes from;
   * @param aAttributedString
   *          the {@link AttributedString} to apply the attributes to;
   * @param aFont
   *          the font to use.
   * @return the primary foreground color, never <code>null</code>.
   */
  private Color applyAttributes( ITextCell aTextCell, AttributedString aAttributedString, Font aFont )
  {
    Color fg = convertToColor( aTextCell.getForeground(), this.colorScheme.getPlainTextColor() );
    Color bg = convertToColor( aTextCell.getBackground(), this.colorScheme.getBackgroundColor() );

    aAttributedString.addAttribute( TextAttribute.FAMILY, aFont.getFamily() );
    aAttributedString.addAttribute( TextAttribute.SIZE, aFont.getSize() );
    aAttributedString.addAttribute( TextAttribute.FOREGROUND, aTextCell.isReverse() ^ aTextCell.isHidden() ? bg : fg );
    aAttributedString.addAttribute( TextAttribute.BACKGROUND, aTextCell.isReverse() ? fg : bg );

    if ( aTextCell.isUnderline() )
    {
      aAttributedString.addAttribute( TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON );
    }
    if ( aTextCell.isBold() )
    {
      aAttributedString.addAttribute( TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD );
    }
    if ( aTextCell.isItalic() )
    {
      aAttributedString.addAttribute( TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE );
    }
    return aTextCell.isReverse() ^ aTextCell.isHidden() ? bg : fg;
  }

  /**
   * Calculates the size (in pixels) of the backbuffer image.
   * 
   * @param aColumns
   *          the number of columns, > 0;
   * @param aLines
   *          the number of lines, > 0.
   * @return a dimension with the image width and height in pixels.
   */
  private Dimension calculateSizeInPixels( int aColumns, int aLines )
  {
    int width = ( aColumns * this.charWidth );
    int height = ( aLines * ( this.charHeight + this.lineSpacing ) );
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
      ptr = ( Container )ptr.getParent();
    }
    while ( ptr != null );
    return insets;
  }

  /**
   * @param aIndex
   * @param aDefaultColor
   * @return
   */
  private Color convertToColor( int aIndex, Color aDefaultColor )
  {
    if ( aIndex < 1 )
    {
      return aDefaultColor;
    }
    return this.colorScheme.getColorByIndex( aIndex - 1 );
  }

  /**
   * @param aCanvas
   * @param aCursor
   */
  private void drawCursor( final Graphics2D aCanvas, final ICursor aCursor, final Color aColor )
  {
    if ( !aCursor.isVisible() )
    {
      return;
    }

    int cw = this.charWidth;
    int ch = this.charHeight;
    int ls = this.lineSpacing;

    int x = aCursor.getX() * cw;
    int y = aCursor.getY() * ( ch + ls );

    aCanvas.setColor( aColor );
    aCanvas.drawRect( x, y, cw, ch - 2 * ls );
  }
}
