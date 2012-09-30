/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import java.io.*;
import java.util.*;

import nl.lxtreme.jvt220.terminal.*;


/**
 * Provides an abstract base implementation of {@link ITerminal}.
 */
public abstract class AbstractTerminal implements ITerminal
{
  // INNER TYPES

  /**
   * Provides a default key mapper that does not map anything.
   */
  protected static class DefaultKeyMapper implements IKeyMapper
  {
    // METHODS

    /**
     * {@inheritDoc}
     */
    @Override
    public String map( int keyCode, int modifiers )
    {
      return null;
    }
  }

  /**
   * Provides a default tabulator implementation.
   */
  protected class DefaultTabulator implements ITabulator
  {
    // CONSTANTS

    private static final int DEFAULT_TABSTOP = 8;

    // VARIABLES

    private final SortedSet<Integer> m_tabStops;

    // CONSTRUCTORS

    /**
     * Creates a new {@link DefaultTabulator} instance.
     * 
     * @param columns
     *          the number of colums, for example, 80.
     */
    public DefaultTabulator( int columns )
    {
      this( columns, DEFAULT_TABSTOP );
    }

    /**
     * Creates a new {@link DefaultTabulator} instance.
     * 
     * @param columns
     *          the number of colums, for example, 80;
     * @param tabStop
     *          the default tab stop to use, for example, 8.
     */
    public DefaultTabulator( int columns, int tabStop )
    {
      m_tabStops = new TreeSet<Integer>();
      for ( int i = tabStop; i < columns; i += tabStop )
      {
        m_tabStops.add( Integer.valueOf( i ) );
      }
    }

    // METHODS

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear( int position )
    {
      m_tabStops.remove( Integer.valueOf( position ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAll()
    {
      m_tabStops.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNextTabWidth( int position )
    {
      return nextTab( position ) - position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPreviousTabWidth( int position )
    {
      return position - previousTab( position );
    }

    /**
     * @return the current tab stops, never <code>null</code>.
     */
    public SortedSet<Integer> getTabStops()
    {
      return m_tabStops;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextTab( int position )
    {
      int tabStop = Integer.MAX_VALUE;

      // Search for the first tab stop after the given position...
      SortedSet<Integer> tailSet = m_tabStops.tailSet( Integer.valueOf( position + 1 ) );
      if ( !tailSet.isEmpty() )
      {
        tabStop = tailSet.first();
      }

      // Don't go beyond the end of the line...
      return Math.min( tabStop, ( getWidth() - 1 ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int previousTab( int position )
    {
      int tabStop = 0;

      // Search for the first tab stop before the given position...
      SortedSet<Integer> headSet = m_tabStops.headSet( Integer.valueOf( position ) );
      if ( !headSet.isEmpty() )
      {
        tabStop = headSet.last();
      }

      // Don't go beyond the start of the line...
      return Math.max( 0, tabStop );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set( int position )
    {
      m_tabStops.add( Integer.valueOf( position ) );
    }
  }

  // CONSTANTS

  /**
   * The font name of Java's mono-spaced font.
   */
  protected static final String FONT_MONOSPACED = "Monospaced";

  private static final int OPTION_ORIGIN = 0;
  private static final int OPTION_REVERSE = 1;
  private static final int OPTION_AUTOWRAP = 2;
  private static final int OPTION_NEWLINE = 3;
  private static final int OPTION_INSERT = 4;

  // VARIABLES

  private final CursorImpl m_cursor;
  private final ITabulator m_tabulator;
  private final IKeyMapper m_keymapper;

  protected final BitSet m_options;
  protected final TextAttributes m_textAttributes;

  private volatile ITerminalFrontend m_frontend;
  private volatile boolean[] m_heatMap;
  private volatile TextCell[] m_buffer;
  private volatile int m_width;
  private volatile int m_height;

  private int m_logLevel;

  private int m_firstScrollLine;
  private int m_lastScrollLine;
  /**
   * denotes that the last written character caused a wrap to the next line (if
   * AutoWrap is enabled).
   */
  private boolean m_wrapped;

  // CONSTRUCTORS

  /**
   * Creates a new {@link AbstractTerminal} instance.
   * 
   * @param columns
   *          the initial number of columns in this terminal, > 0;
   * @param lines
   *          the initial number of lines in this terminal, > 0.
   */
  protected AbstractTerminal( final int columns, final int lines )
  {
    m_keymapper = createKeyMapper();
    m_textAttributes = new TextAttributes();
    m_cursor = new CursorImpl();
    m_options = new BitSet();
    m_tabulator = new DefaultTabulator( columns );

    internalSetDimensions( columns, lines );

    m_logLevel = 3;
  }

  // METHODS

  /**
   * Clears the current line.
   * 
   * @param mode
   *          the clear modus: 0 = erase from cursor to right (default), 1 =
   *          erase from cursor to left, 2 = erase entire line.
   */
  public void clearLine( final int mode )
  {
    final int xPos = m_cursor.getX();
    final int yPos = m_cursor.getY();

    int idx;

    switch ( mode )
    {
      case 0:
        // erase from cursor to end of line...
        idx = getAbsoluteIndex( xPos, yPos );
        break;
      case 1:
        // erase from cursor to start of line...
        idx = getAbsoluteIndex( xPos, yPos );
        break;
      case 2:
        // erase entire line...
        idx = getAbsoluteIndex( 0, yPos );
        break;

      default:
        throw new IllegalArgumentException( "Invalid clear line mode!" );
    }

    clearLine( mode, idx, false /* aKeepProtectedCells */);
  }

  /**
   * Clears the screen.
   * 
   * @param mode
   *          the clear modus: 0 = erase from cursor to below (default), 1 =
   *          erase from cursor to top, 2 = erase entire screen.
   */
  public void clearScreen( final int mode )
  {
    final int xPos = m_cursor.getX();
    final int yPos = m_cursor.getY();

    clearScreen( mode, getAbsoluteIndex( xPos, yPos ), false /* aKeepProtectedCells */);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    // TODO Auto-generated method stub

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ICursor getCursor()
  {
    return m_cursor;
  }

  /**
   * Returns the first line that can be scrolled, by default line 0.
   * 
   * @return the index of the first scroll line, >= 0.
   */
  public final int getFirstScrollLine()
  {
    if ( !isOriginMode() )
    {
      // If not relative to the scroll-region origin, then we simply return
      // 0...
      return 0;
    }
    return m_firstScrollLine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ITerminalFrontend getFrontend()
  {
    return m_frontend;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHeight()
  {
    return m_height;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public final IKeyMapper getKeyMapper()
  {
    return m_keymapper;
  }

  /**
   * Returns the last line that can be scrolled, by default the screen height
   * minus 1.
   * 
   * @return the index of the last scroll line, >= 0 && < {@link #getHeight()} .
   */
  public final int getLastScrollLine()
  {
    if ( !isOriginMode() )
    {
      // If not relative to the scroll-region origin, then we simply return
      // the height of the screen minus one...
      return getHeight() - 1;
    }
    return m_lastScrollLine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ITabulator getTabulator()
  {
    return m_tabulator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getWidth()
  {
    return m_width;
  }

  /**
   * Returns whether or not the auto-newline mode is enabled.
   * <p>
   * When auto-newline mode is enabled, a LF, FF or VT will cause the cursor to
   * move to the <em>first</em> column of the next line. When disabled, it will
   * stay in the same column while moving the cursor to the next line.
   * </p>
   * 
   * @return <code>true</code> if auto-wrap mode is enabled, <code>false</code>
   *         otherwise.
   * @see #setAutoNewlineMode(boolean)
   */
  public final boolean isAutoNewlineMode()
  {
    return m_options.get( OPTION_NEWLINE );
  }

  /**
   * Returns whether or not the auto-wrap mode is enabled.
   * 
   * @return <code>true</code> if auto-wrap mode is enabled, <code>false</code>
   *         otherwise.
   * @see #setAutoWrap(boolean)
   */
  public final boolean isAutoWrapMode()
  {
    return m_options.get( OPTION_AUTOWRAP );
  }

  /**
   * Returns whether or not insert mode is enabled.
   * 
   * @return <code>true</code> if insert mode is enabled, <code>false</code>
   *         otherwise.
   * @see #setInsertMode(boolean)
   */
  public final boolean isInsertMode()
  {
    return m_options.get( OPTION_INSERT );
  }

  /**
   * Returns whether or not the origin mode is enabled.
   * 
   * @return <code>true</code> if origin mode is enabled, <code>false</code>
   *         otherwise.
   * @see #setOriginMode(boolean)
   */
  public final boolean isOriginMode()
  {
    return m_options.get( OPTION_ORIGIN );
  }

  /**
   * Returns whether or not the reverse mode is enabled.
   * 
   * @return <code>true</code> if reverse mode is enabled, <code>false</code>
   *         otherwise.
   * @see #setReverse(boolean)
   */
  public final boolean isReverseMode()
  {
    return m_options.get( OPTION_REVERSE );
  }

  /**
   * Moves the cursor to the given X,Y position.
   * 
   * @param x
   *          the absolute X-position, zero-based (zero meaning start of current
   *          line). If -1, then the current X-position is unchanged.
   * @param y
   *          the absolute Y-position, zero-based (zero meaning start of current
   *          screen). If -1, then the current Y-position is unchanged.
   */
  public void moveCursorAbsolute( final int x, final int y )
  {
    int xPos = x;
    if ( xPos < 0 )
    {
      xPos = m_cursor.getX();
    }
    if ( xPos >= m_width )
    {
      xPos = m_width;
    }

    int yPos = y;
    if ( yPos < 0 )
    {
      yPos = m_cursor.getY();
    }
    if ( yPos >= m_height )
    {
      yPos = m_height - 1;
    }

    m_cursor.setPosition( xPos, yPos );
  }

  /**
   * Moves the cursor relatively to the given X,Y position.
   * 
   * @param x
   *          the relative X-position to move. If > 0, then move to the right;
   *          if 0, then the X-position is unchanged; if < 0, then move to the
   *          left;
   * @param y
   *          the relative Y-position to move. If > 0, then move to the bottom;
   *          if 0, then the Y-position is unchanged; if < 0, then move to the
   *          top.
   */
  public void moveCursorRelative( final int x, final int y )
  {
    int xPos = Math.max( 0, m_cursor.getX() + x );
    int yPos = Math.max( 0, m_cursor.getY() + y );

    moveCursorAbsolute( xPos, yPos );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int read( CharSequence chars ) throws IOException
  {
    int r = doReadInput( chars );

    if ( m_frontend != null && m_frontend.isListening() )
    {
      TextCell[] b = m_buffer.clone();
      boolean[] hm = m_heatMap.clone();

      m_frontend.terminalChanged( b, hm );

      Arrays.fill( m_heatMap, false );
    }

    return r;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset()
  {
    // Clear the entire screen...
    clearScreen( 2 );
    // Move cursor to the first (upper left) position...
    updateCursorByAbsoluteIndex( getFirstAbsoluteIndex() );
    // Reset scroll region...
    m_firstScrollLine = 0;
    m_lastScrollLine = getHeight() - 1;
  }

  /**
   * Scrolls a given number of lines down, inserting empty lines at the top of
   * the scrolling region. The contents of the lines scrolled off the screen are
   * lost.
   * 
   * @param lines
   *          the number of lines to scroll down, > 0.
   * @see #setScrollRegion(int, int)
   */
  public void scrollDown( final int lines )
  {
    if ( lines < 1 )
    {
      throw new IllegalArgumentException( "Invalid number of lines!" );
    }

    int region = ( m_lastScrollLine - m_firstScrollLine + 1 );
    int n = Math.min( lines, region );
    int width = getWidth();

    int srcPos = m_firstScrollLine * width;
    int destPos = ( n + m_firstScrollLine ) * width;
    int length = ( m_lastScrollLine + 1 ) * width - destPos;

    if ( length > 0 )
    {
      System.arraycopy( m_buffer, srcPos, m_buffer, destPos, length );
    }

    Arrays.fill( m_buffer, srcPos, destPos, new TextCell( ' ', getAttributes() ) );
    // Update the heat map...
    Arrays.fill( m_heatMap, true );
  }

  /**
   * Scrolls a given number of lines up, inserting empty lines at the bottom of
   * the scrolling region. The contents of the lines scrolled off the screen are
   * lost.
   * 
   * @param lines
   *          the number of lines to scroll up, > 0.
   * @see #setScrollRegion(int, int)
   */
  public void scrollUp( final int lines )
  {
    if ( lines < 1 )
    {
      throw new IllegalArgumentException( "Invalid number of lines!" );
    }

    int region = ( m_lastScrollLine - m_firstScrollLine + 1 );
    int n = Math.min( lines, region );
    int width = getWidth();

    int srcPos = ( n + m_firstScrollLine ) * width;
    int destPos = m_firstScrollLine * width;
    int lastPos = ( m_lastScrollLine + 1 ) * width;
    int length = lastPos - srcPos;

    if ( length > 0 )
    {
      System.arraycopy( m_buffer, srcPos, m_buffer, destPos, length );
    }
    Arrays.fill( m_buffer, destPos + length, srcPos + length, new TextCell( ' ', getAttributes() ) );
    // Update the heat map...
    Arrays.fill( m_heatMap, true );
  }

  /**
   * Enables or disables the auto-newline mode.
   * 
   * @param enable
   *          <code>true</code> to enable auto-newline mode, <code>false</code>
   *          to disable it.
   */
  public void setAutoNewlineMode( boolean enable )
  {
    m_options.set( OPTION_NEWLINE, enable );
  }

  /**
   * Enables or disables the auto-wrap mode.
   * 
   * @param enable
   *          <code>true</code> to enable auto-wrap mode, <code>false</code> to
   *          disable it.
   */
  public void setAutoWrap( boolean enable )
  {
    m_options.set( OPTION_AUTOWRAP, enable );
  }

  /**
   * Sets the dimensions of this terminal to the given width and height.
   * 
   * @param newWidth
   *          the new width of this terminal, in columns. If <= 0, then the
   *          current width will be used;
   * @param newHeight
   *          the new height of this terminal, in lines. If <= 0, then the
   *          current height will be used.
   */
  public void setDimensions( int newWidth, int newHeight )
  {
    if ( newWidth <= 0 )
    {
      newWidth = m_width;
    }
    if ( newHeight <= 0 )
    {
      newHeight = m_height;
    }

    if ( ( newWidth == m_width ) && ( newHeight == m_height ) )
    {
      // Nothing to do...
      return;
    }

    internalSetDimensions( newWidth, newHeight );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFrontend( ITerminalFrontend frontend )
  {
    if ( frontend == null )
    {
      throw new IllegalArgumentException( "Frontend cannot be null!" );
    }
    m_frontend = frontend;
  }

  /**
   * Enables or disables the insert mode.
   * 
   * @param enable
   *          <code>true</code> to enable insert mode, <code>false</code> to
   *          disable it.
   */
  public void setInsertMode( boolean enable )
  {
    m_options.set( OPTION_INSERT, enable );
  }

  /**
   * @param logLevel
   *          the logLevel to set
   */
  public void setLogLevel( int logLevel )
  {
    m_logLevel = logLevel;
  }

  /**
   * Enables or disables the origin mode.
   * <p>
   * When the origin mode is set, cursor addressing is relative to the upper
   * left corner of the scrolling region.
   * </p>
   * 
   * @param enable
   *          <code>true</code> to enable origin mode, <code>false</code> to
   *          disable it.
   * @see #setScrollRegion(int, int)
   */
  public void setOriginMode( boolean enable )
  {
    m_options.set( OPTION_ORIGIN, enable );
  }

  /**
   * Enables or disables the reverse mode.
   * 
   * @param enable
   *          <code>true</code> to enable reverse mode, <code>false</code> to
   *          disable it.
   */
  public void setReverse( boolean enable )
  {
    m_options.set( OPTION_REVERSE, enable );

    if ( m_frontend != null )
    {
      m_frontend.setReverse( enable );
    }
  }

  /**
   * Sets the scrolling region. By default the entire screen is set as scrolling
   * region.
   * 
   * @param topIndex
   *          the top line that will be scrolled, >= 0;
   * @param bottomIndex
   *          the bottom line that will be scrolled, >= aTopIndex.
   */
  public void setScrollRegion( final int topIndex, final int bottomIndex )
  {
    if ( topIndex < 0 )
    {
      throw new IllegalArgumentException( "TopIndex cannot be negative!" );
    }
    if ( bottomIndex <= topIndex )
    {
      throw new IllegalArgumentException( "BottomIndex cannot be equal or less than TopIndex!" );
    }

    m_firstScrollLine = Math.max( 0, topIndex );
    m_lastScrollLine = Math.min( getHeight() - 1, bottomIndex );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for ( int idx = getFirstAbsoluteIndex(); idx <= getLastAbsoluteIndex(); idx++ )
    {
      ITextCell cell = getCellAt( idx );
      if ( ( idx > 0 ) && ( idx % getWidth() ) == 0 )
      {
        sb.append( "\n" );
      }
      sb.append( cell == null ? ' ' : cell.getChar() );
    }
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int write( CharSequence response ) throws IOException
  {
    int length = 0;

    final Writer w = getWriter();
    if ( ( response != null ) && ( w != null ) )
    {
      w.write( response.toString() );
      w.flush();

      length = response.length();
    }

    return length;
  }

  /**
   * Clears all tab stops.
   */
  protected final void clearAllTabStops()
  {
    getTabulator().clearAll();
  }

  /**
   * Clears the line using the given absolute index as cursor position.
   * 
   * @param mode
   *          the clear modus: 0 = erase from cursor to right (default), 1 =
   *          erase from cursor to left, 2 = erase entire line.
   * @param absoluteIndex
   *          the absolute index of the cursor;
   * @param keepProtectedCells
   *          <code>true</code> to honor the 'protected' option in text cells
   *          leaving those cells as-is, <code>false</code> to disregard this
   *          option and clear all text cells.
   */
  protected final void clearLine( final int mode, final int absoluteIndex, final boolean keepProtectedCells )
  {
    int width = getWidth();
    int yPos = ( int )Math.floor( absoluteIndex / width );
    int xPos = absoluteIndex - ( yPos * width );

    int idx;
    int length;

    switch ( mode )
    {
      case 0:
        // erase from cursor to end of line...
        idx = absoluteIndex;
        length = width - xPos;
        break;
      case 1:
        // erase from cursor to start of line...
        idx = absoluteIndex - xPos;
        length = xPos + 1;
        break;
      case 2:
        // erase entire line...
        idx = absoluteIndex - xPos;
        length = width;
        break;

      default:
        throw new IllegalArgumentException( "Invalid clear line mode!" );
    }

    for ( int i = 0; i < length; i++ )
    {
      removeChar( idx++, keepProtectedCells );
    }
  }

  /**
   * Clears the screen using the given absolute index as cursor position.
   * 
   * @param mode
   *          the clear modus: 0 = erase from cursor to below (default), 1 =
   *          erase from cursor to top, 2 = erase entire screen.
   * @param absoluteIndex
   *          the absolute index of the cursor;
   * @param keepProtectedCells
   *          <code>true</code> to honor the 'protected' option in text cells
   *          leaving those cells as-is, <code>false</code> to disregard this
   *          option and clear all text cells.
   */
  protected final void clearScreen( final int mode, final int absoluteIndex, final boolean keepProtectedCells )
  {
    switch ( mode )
    {
      case 0:
        // erase from cursor to end of screen...
        int lastIdx = getLastAbsoluteIndex();
        for ( int i = absoluteIndex; i <= lastIdx; i++ )
        {
          removeChar( i, keepProtectedCells );
        }
        break;
      case 1:
        // erase from cursor to start of screen...
        int firstIdx = getFirstAbsoluteIndex();
        for ( int i = firstIdx; i <= absoluteIndex; i++ )
        {
          removeChar( i, keepProtectedCells );
        }
        break;
      case 2:
        // erase entire screen...
        if ( keepProtectedCells )
        {
          // Be selective in what we remove...
          for ( int i = getFirstAbsoluteIndex(), last = getLastAbsoluteIndex(); i <= last; i++ )
          {
            removeChar( i, keepProtectedCells );
          }
        }
        else
        {
          // Don't be selective in what we remove...
          Arrays.fill( m_buffer, getFirstAbsoluteIndex(), getLastAbsoluteIndex() + 1, new TextCell( ' ',
              getAttributes() ) );
          // Update the heat map...
          Arrays.fill( m_heatMap, true );
        }
        break;

      default:
        throw new IllegalArgumentException( "Invalid clear screen mode!" );
    }
  }

  /**
   * Factory method for creating {@link IKeyMapper} instances.
   * 
   * @return a new {@link IKeyMapper} instance, never <code>null</code>.
   */
  protected IKeyMapper createKeyMapper()
  {
    return new DefaultKeyMapper();
  }

  /**
   * Deletes a given number of characters at the absolute index, first shifting
   * the remaining characters on that line to the left, inserting spaces at the
   * end of the line.
   * 
   * @param absoluteIndex
   *          the absolute index to delete the character at;
   * @param count
   *          the number of times to insert the given character, > 0.
   * @return the next index.
   */
  protected final int deleteChars( final int absoluteIndex, final int count )
  {
    int col = ( absoluteIndex % getWidth() );
    int length = Math.max( 0, getWidth() - col - count );

    // Make room for the new characters at the end...
    System.arraycopy( m_buffer, absoluteIndex + count, m_buffer, absoluteIndex, length );

    // Fill the created room with the character to insert...
    int startIdx = absoluteIndex + length;
    int endIdx = absoluteIndex + getWidth() - col;
    Arrays.fill( m_buffer, startIdx, endIdx, new TextCell( ' ', getAttributes() ) );

    // Update the heat map for the *full* line...
    Arrays.fill( m_heatMap, absoluteIndex, absoluteIndex + getWidth() - col, true );

    return absoluteIndex;
  }

  /**
   * Provides the actual implementation for {@link #read(CharSequence)}.
   * 
   * @see {@link #read(CharSequence)}
   */
  protected abstract int doReadInput( CharSequence chars ) throws IOException;

  /**
   * Returns the absolute index according to the current cursor position.
   * 
   * @return an absolute index of the cursor position, >= 0.
   */
  protected final int getAbsoluteCursorIndex()
  {
    return getAbsoluteIndex( m_cursor.getX(), m_cursor.getY() );
  }

  /**
   * Returns the absolute index according to the given X,Y-position.
   * 
   * @param x
   *          the X-position;
   * @param y
   *          the Y-position.
   * @return an absolute index of the cursor position, >= 0.
   */
  protected final int getAbsoluteIndex( final int x, final int y )
  {
    return ( y * getWidth() ) + x;
  }

  /**
   * @return the (encoded) text attributes.
   */
  protected final short getAttributes()
  {
    return m_textAttributes.getAttributes();
  }

  /**
   * Returns the cell at the given absolute index.
   * 
   * @param absoluteIndex
   *          the absolute of the cell to retrieve.
   * @return the text cell at the given index, can be <code>null</code> if no
   *         cell is defined.
   */
  protected final ITextCell getCellAt( final int absoluteIndex )
  {
    return m_buffer[absoluteIndex];
  }

  /**
   * Returns the cell at the given X,Y-position.
   * 
   * @param x
   *          the X-position of the cell to retrieve;
   * @param y
   *          the Y-position of the cell to retrieve.
   * @return the text cell at the given X,Y-position, or <code>null</code> if
   *         there is no such cell.
   */
  protected final ITextCell getCellAt( final int x, final int y )
  {
    return getCellAt( getAbsoluteIndex( x, y ) );
  }

  /**
   * @return the first absolute index of this screen, >= 0.
   */
  protected final int getFirstAbsoluteIndex()
  {
    return getAbsoluteIndex( 0, 0 );
  }

  /**
   * @return the last absolute index of this screen, >= 0.
   */
  protected final int getLastAbsoluteIndex()
  {
    return getAbsoluteIndex( getWidth() - 1, getHeight() - 1 );
  }

  /**
   * Inserts a given character at the absolute index, first shifting the
   * remaining characters on that line to the right (possibly shifting text of
   * the line).
   * 
   * @param absoluteIndex
   *          the absolute index to insert the character at;
   * @param ch
   *          the character to insert;
   * @param count
   *          the number of times to insert the given character, > 0.
   * @return the next index.
   */
  protected final int insertChars( final int absoluteIndex, final char ch, final int count )
  {
    int col = absoluteIndex % getWidth();
    int length = getWidth() - col - count;

    // Make room for the new characters...
    System.arraycopy( m_buffer, absoluteIndex, m_buffer, absoluteIndex + count, length );

    // Fill the created room with the character to insert...
    Arrays.fill( m_buffer, absoluteIndex, absoluteIndex + count, new TextCell( ch, getAttributes() ) );

    // Update the heat map for the *full* line...
    Arrays.fill( m_heatMap, absoluteIndex, absoluteIndex + getWidth() - col, true );

    return absoluteIndex;
  }

  /**
   * @return <code>true</code> if the last written character caused a wrap to
   *         next line, <code>false</code> otherwise.
   */
  protected final boolean isWrapped()
  {
    return m_wrapped;
  }

  /**
   * Logs the given text verbatimely at loglevel 0 or higher.
   * 
   * @param text
   *          the text to log, cannot be <code>null</code>.
   */
  protected final void log( String text )
  {
    if ( m_logLevel < 0 )
    {
      return;
    }

    System.out.printf( "LOG> %s%n", text );
  }

  /**
   * Removes the character at the absolute index.
   * 
   * @param absoluteIndex
   *          the index on which to remove the character, >= 0;
   * @param keepProtectedCells
   *          <code>true</code> to honor the 'protected' bit of text cells and
   *          leave the text cell unchanged, <code>false</code> to ignore this
   *          bit and clear the text cell anyways.
   * @return the absolute index on which the character was removed.
   */
  protected final int removeChar( final int absoluteIndex, final boolean keepProtectedCells )
  {
    int idx = absoluteIndex;
    int firstIdx = getFirstAbsoluteIndex();
    if ( idx < firstIdx )
    {
      return firstIdx;
    }
    if ( idx > getLastAbsoluteIndex() )
    {
      idx = getLastAbsoluteIndex();
    }

    // Clear the character at the given position, using the most current
    // attributes...
    if ( !( keepProtectedCells && m_buffer[idx].isProtected() ) )
    {
      m_buffer[idx] = new TextCell( ' ', getAttributes() );
      m_heatMap[idx] = true;
    }

    return idx;
  }

  /**
   * Resets the wrapped state.
   */
  protected final void resetWrapped()
  {
    m_wrapped = false;
  }

  /**
   * Updates the cursor according to the given absolute index.
   * 
   * @param absoluteIndex
   *          the absolute index to convert back to a X,Y-position.
   */
  protected final void updateCursorByAbsoluteIndex( final int absoluteIndex )
  {
    int width = getWidth();
    int yPos = ( int )Math.floor( absoluteIndex / width );
    int xPos = absoluteIndex - ( yPos * width );
    m_cursor.setPosition( xPos, yPos );
  }

  /**
   * Writes a given character at the absolute index, scrolling the screen up if
   * beyond the last index is written.
   * 
   * @param absoluteIndex
   *          the index on which to write the given char, >= 0;
   * @param ch
   *          the character to write;
   * @param aAttributes
   *          the attributes to use to write the character.
   * @return the absolute index after which the character was written.
   */
  protected final int writeChar( final int absoluteIndex, final char ch )
  {
    int idx = absoluteIndex;
    int lastIdx = getAbsoluteIndex( getWidth() - 1, getLastScrollLine() );
    int width = getWidth();

    if ( idx > lastIdx )
    {
      idx -= width;
      scrollUp( 1 );
    }

    if ( idx <= lastIdx )
    {
      m_buffer[idx] = new TextCell( ch, getAttributes() );
      m_heatMap[idx] = true;
    }

    // determine new absolute index...
    boolean lastColumn = ( ( idx % width ) == ( width - 1 ) );
    m_wrapped = ( isAutoWrapMode() && lastColumn );
    if ( !( !isAutoWrapMode() && lastColumn ) )
    {
      idx++;
    }

    return idx;
  }

  /**
   * @return the {@link Writer} to write the responses from this terminal to,
   *         can be <code>null</code>.
   */
  private Writer getWriter()
  {
    Writer result = null;
    if ( m_frontend != null )
    {
      result = m_frontend.getWriter();
    }
    return result;
  }

  /**
   * Sets the dimensions of this terminal to the given width and height.
   * 
   * @param width
   *          the new width in columns, > 0;
   * @param height
   *          the new height in lines, > 0.
   */
  private void internalSetDimensions( final int width, final int height )
  {
    TextCell[] newBuffer = new TextCell[width * height];
    Arrays.fill( newBuffer, new TextCell() );
    if ( m_buffer != null )
    {
      int oldWidth = m_width;

      for ( int oldIdx = 0; oldIdx < m_buffer.length; oldIdx++ )
      {
        int oldColumn = ( oldIdx % oldWidth );
        int oldLine = ( oldIdx / oldWidth );
        if ( ( oldColumn >= width ) || ( oldLine >= height ) )
        {
          continue;
        }

        int newIdx = ( oldLine * width ) + oldColumn;
        newBuffer[newIdx] = m_buffer[oldIdx];
      }
    }

    m_width = width;
    m_height = height;

    m_firstScrollLine = 0;
    m_lastScrollLine = height - 1;

    m_buffer = newBuffer;
    m_heatMap = new boolean[newBuffer.length];

    if ( m_frontend != null )
    {
      // Notify the frontend that we've changed...
      m_frontend.terminalSizeChanged( width, height );
    }
  }
}
