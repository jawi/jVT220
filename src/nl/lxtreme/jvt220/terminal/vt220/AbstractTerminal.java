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
   * Provides a default tabulator implementation.
   */
  protected class DefaultTabulator implements ITabulator
  {
    // VARIABLES

    private final SortedSet<Integer> tabStops;
    private final boolean useDefaultTabStops;

    private int defaultTabStop;

    // CONSTRUCTORS

    /**
     * Creates a new {@link DefaultTabulator} instance.
     */
    public DefaultTabulator()
    {
      this( false /* aUseDefaultTabStops */);
    }

    /**
     * Creates a new {@link DefaultTabulator} instance.
     */
    public DefaultTabulator( boolean aUseDefaultTabStops )
    {
      this.tabStops = new TreeSet<Integer>();
      this.defaultTabStop = 8;
      this.useDefaultTabStops = aUseDefaultTabStops;
    }

    // METHODS

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear( int aPosition )
    {
      this.tabStops.remove( Integer.valueOf( aPosition ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAll()
    {
      this.tabStops.clear();
    }

    /**
     * @return the current tab stops, never <code>null</code>.
     */
    public SortedSet<Integer> getTabStops()
    {
      return this.tabStops;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTabWidth( int aPosition )
    {
      return nextTab( aPosition ) - aPosition;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int nextTab( int aPosition )
    {
      // Search for the first tab stop at or after the given position...
      int tabStop;

      SortedSet<Integer> tailSet = this.tabStops.tailSet( Integer.valueOf( aPosition ) );
      if ( !tailSet.isEmpty() )
      {
        tabStop = tailSet.first();
      }
      else if ( this.useDefaultTabStops )
      {
        double pos = aPosition + 1.0;
        tabStop = ( int )( Math.ceil( pos / this.defaultTabStop ) * this.defaultTabStop );
      }
      else
      {
        tabStop = Integer.MAX_VALUE;
      }

      // Don't go beyond the end of the line...
      tabStop = Math.min( tabStop, ( getWidth() - 1 ) );

      return tabStop;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set( int aPosition )
    {
      this.tabStops.add( Integer.valueOf( aPosition ) );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDefault( int aDefault )
    {
      this.defaultTabStop = aDefault;
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

  private final OutputStream outputStream;
  private final CursorImpl cursor;
  private final ITabulator tabulator;

  protected final BitSet options;
  protected final TextAttributes textAttributes;

  private volatile ITerminalFrontend frontend;
  private volatile boolean[] heatMap;
  private volatile TextCell[] buffer;
  private volatile int width;
  private volatile int height;

  private int logLevel;

  private int firstScrollLine;
  private int lastScrollLine;
  /**
   * denotes that the last written character caused a wrap to the next line (if
   * AutoWrap is enabled).
   */
  private boolean wrapped;

  // CONSTRUCTORS

  /**
   * Creates a new {@link AbstractTerminal} instance.
   * 
   * @param aOutputStream
   *          the output stream to write back to, cannot be <code>null</code>;
   * @param aColumns
   *          the initial number of columns in this terminal, > 0;
   * @param aLines
   *          the initial number of lines in this terminal, > 0.
   */
  protected AbstractTerminal( final OutputStream aOutputStream, final int aColumns, final int aLines )
  {
    this.outputStream = aOutputStream;

    this.textAttributes = new TextAttributes();
    this.cursor = new CursorImpl();
    this.options = new BitSet();
    this.tabulator = new DefaultTabulator();

    internalSetDimensions( aColumns, aLines );

    this.options.set( OPTION_AUTOWRAP );

    this.logLevel = 3;
  }

  // METHODS

  /**
   * Clears the current line.
   * 
   * @param aMode
   *          the clear modus: 0 = erase from cursor to right (default), 1 =
   *          erase from cursor to left, 2 = erase entire line.
   */
  public void clearLine( final int aMode )
  {
    final int xPos = this.cursor.getX();
    final int yPos = this.cursor.getY();

    int idx;

    switch ( aMode )
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

    clearLine( aMode, idx );
  }

  /**
   * Clears the screen.
   * 
   * @param aMode
   *          the clear modus: 0 = erase from cursor to below (default), 1 =
   *          erase from cursor to top, 2 = erase entire screen.
   */
  public void clearScreen( final int aMode )
  {
    final int xPos = this.cursor.getX();
    final int yPos = this.cursor.getY();

    clearScreen( aMode, getAbsoluteIndex( xPos, yPos ) );
  }

  /**
   * Returns the cell at the given X,Y-position.
   * 
   * @param aXpos
   *          the X-position of the cell to retrieve;
   * @param aYpos
   *          the Y-position of the cell to retrieve.
   * @return the text cell at the given X,Y-position, or <code>null</code> if
   *         there is no such cell.
   */
  public final ITextCell getCellAt( final int aXpos, final int aYpos )
  {
    return getCellAt( getAbsoluteIndex( aXpos, aYpos ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ICursor getCursor()
  {
    return this.cursor;
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
    return this.firstScrollLine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ITerminalFrontend getFrontend()
  {
    return this.frontend;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getHeight()
  {
    return this.height;
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
    return this.lastScrollLine;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ITabulator getTabulator()
  {
    return this.tabulator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getWidth()
  {
    return this.width;
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
    return this.options.get( OPTION_NEWLINE );
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
    return this.options.get( OPTION_AUTOWRAP );
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
    return this.options.get( OPTION_INSERT );
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
    return this.options.get( OPTION_ORIGIN );
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
    return this.options.get( OPTION_REVERSE );
  }

  /**
   * Moves the cursor to the given X,Y position.
   * 
   * @param aXpos
   *          the absolute X-position, zero-based (zero meaning start of current
   *          line). If -1, then the current X-position is unchanged.
   * @param aYpos
   *          the absolute Y-position, zero-based (zero meaning start of current
   *          screen). If -1, then the current Y-position is unchanged.
   */
  public void moveCursorAbsolute( final int aXpos, final int aYpos )
  {
    int xPos = aXpos;
    if ( xPos < 0 )
    {
      xPos = this.cursor.getX();
    }
    if ( xPos >= this.width )
    {
      xPos = this.width;
    }

    int yPos = aYpos;
    if ( yPos < 0 )
    {
      yPos = this.cursor.getY();
    }
    if ( yPos >= this.height )
    {
      yPos = this.height - 1;
    }

    this.cursor.setPosition( xPos, yPos );
  }

  /**
   * Moves the cursor relatively to the given X,Y position.
   * 
   * @param aXpos
   *          the relative X-position to move. If > 0, then move to the right;
   *          if 0, then the X-position is unchanged; if < 0, then move to the
   *          left;
   * @param aYpos
   *          the relative Y-position to move. If > 0, then move to the bottom;
   *          if 0, then the Y-position is unchanged; if < 0, then move to the
   *          top.
   */
  public void moveCursorRelative( final int aXpos, final int aYpos )
  {
    int xPos = Math.max( 0, this.cursor.getX() + aXpos );
    int yPos = Math.max( 0, this.cursor.getY() + aYpos );

    moveCursorAbsolute( xPos, yPos );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int readInput( CharSequence aChars ) throws IOException
  {
    int r = doReadInput( aChars );

    if ( this.frontend != null && this.frontend.isListening() )
    {
      TextCell[] b = this.buffer.clone();
      boolean[] hm = this.heatMap.clone();

      this.frontend.terminalChanged( b, hm );

      Arrays.fill( this.heatMap, false );
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
    this.firstScrollLine = 0;
    this.lastScrollLine = getHeight() - 1;
  }

  /**
   * Scrolls all lines a given number down.
   * 
   * @param aLines
   *          the number of lines to scroll down, > 0.
   */
  public void scrollDown( final int aLines )
  {
    if ( aLines < 1 )
    {
      throw new IllegalArgumentException( "Invalid number of lines!" );
    }

    int region = ( this.lastScrollLine - this.firstScrollLine + 1 );
    int n = Math.min( aLines, region );
    int width = getWidth();

    int srcPos = this.firstScrollLine * width;
    int destPos = ( n + this.firstScrollLine ) * width;
    int length = ( this.lastScrollLine + 1 ) * width - destPos;

    if ( length > 0 )
    {
      System.arraycopy( this.buffer, srcPos, this.buffer, destPos, length );
    }

    Arrays.fill( this.buffer, srcPos, destPos, new TextCell( ' ', getAttributes() ) );
    // Update the heat map...
    Arrays.fill( this.heatMap, true );
  }

  /**
   * Scrolls all lines a given number up.
   * 
   * @param aLines
   *          the number of lines to scroll up, > 0.
   */
  public void scrollUp( final int aLines )
  {
    if ( aLines < 1 )
    {
      throw new IllegalArgumentException( "Invalid number of lines!" );
    }

    int region = ( this.lastScrollLine - this.firstScrollLine + 1 );
    int n = Math.min( aLines, region );
    int width = getWidth();

    int srcPos = ( n + this.firstScrollLine ) * width;
    int destPos = this.firstScrollLine * width;
    int lastPos = ( this.lastScrollLine + 1 ) * width;
    int length = lastPos - srcPos;

    if ( length > 0 )
    {
      System.arraycopy( this.buffer, srcPos, this.buffer, destPos, length );
    }
    Arrays.fill( this.buffer, destPos + length, srcPos + length, new TextCell( ' ', getAttributes() ) );
    // Update the heat map...
    Arrays.fill( this.heatMap, true );
  }

  /**
   * Enables or disables the auto-newline mode.
   * 
   * @param aEnable
   *          <code>true</code> to enable auto-newline mode, <code>false</code>
   *          to disable it.
   */
  public void setAutoNewlineMode( boolean aEnable )
  {
    this.options.set( OPTION_NEWLINE, aEnable );
  }

  /**
   * Enables or disables the auto-wrap mode.
   * 
   * @param aEnable
   *          <code>true</code> to enable auto-wrap mode, <code>false</code> to
   *          disable it.
   */
  public void setAutoWrap( boolean aEnable )
  {
    this.options.set( OPTION_AUTOWRAP, aEnable );
  }

  /**
   * Sets the dimensions of this terminal to the given width and height.
   * 
   * @param aNewWidth
   *          the new width of this terminal, > 0;
   * @param aNewHeight
   *          the new height of this terminal, > 0.
   */
  public void setDimensions( final int aNewWidth, final int aNewHeight )
  {
    if ( aNewWidth <= 0 )
    {
      throw new IllegalArgumentException( "Invalid width!" );
    }
    if ( aNewHeight <= 0 )
    {
      throw new IllegalArgumentException( "Invalid height!" );
    }

    if ( ( aNewWidth == this.width ) && ( aNewHeight == this.height ) )
    {
      // Nothing to do...
      return;
    }

    internalSetDimensions( aNewWidth, aNewHeight );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setFrontend( ITerminalFrontend aFrontend )
  {
    if ( aFrontend == null )
    {
      throw new IllegalArgumentException( "Frontend cannot be null!" );
    }
    this.frontend = aFrontend;
  }

  /**
   * Enables or disables the insert mode.
   * 
   * @param aEnable
   *          <code>true</code> to enable insert mode, <code>false</code> to
   *          disable it.
   */
  public void setInsertMode( boolean aEnable )
  {
    this.options.set( OPTION_INSERT, aEnable );
  }

  /**
   * @param logLevel
   *          the logLevel to set
   */
  public void setLogLevel( int logLevel )
  {
    this.logLevel = logLevel;
  }

  /**
   * Enables or disables the origin mode.
   * <p>
   * When the origin mode is set, cursor addressing is relative to the upper
   * left corner of the scrolling region.
   * </p>
   * 
   * @param aEnable
   *          <code>true</code> to enable origin mode, <code>false</code> to
   *          disable it.
   * @see #setScrollRegion(int, int)
   */
  public void setOriginMode( boolean aEnable )
  {
    this.options.set( OPTION_ORIGIN, aEnable );
  }

  /**
   * Enables or disables the reverse mode.
   * 
   * @param aEnable
   *          <code>true</code> to enable reverse mode, <code>false</code> to
   *          disable it.
   */
  public void setReverse( boolean aEnable )
  {
    this.options.set( OPTION_REVERSE, aEnable );
  }

  /**
   * Sets the scrolling region. By default the entire screen is set as scrolling
   * region.
   * 
   * @param aTopIndex
   *          the top line that will be scrolled, >= 0;
   * @param aBottomIndex
   *          the bottom line that will be scrolled, >= aTopIndex.
   */
  public void setScrollRegion( final int aTopIndex, final int aBottomIndex )
  {
    if ( aTopIndex < 0 )
    {
      throw new IllegalArgumentException( "TopIndex cannot be negative!" );
    }
    if ( aBottomIndex <= aTopIndex )
    {
      throw new IllegalArgumentException( "BottomIndex cannot be equal or less than TopIndex!" );
    }

    this.firstScrollLine = Math.max( 0, aTopIndex );
    this.lastScrollLine = Math.min( getHeight() - 1, aBottomIndex );
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
  public int writeResponse( CharSequence aChars ) throws IOException
  {
    int length = aChars.length();
    for ( int i = 0; i < length; i++ )
    {
      this.outputStream.write( aChars.charAt( i ) );
    }
    this.outputStream.flush();
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
   * @param aMode
   *          the clear modus: 0 = erase from cursor to right (default), 1 =
   *          erase from cursor to left, 2 = erase entire line.
   * @param aAbsoluteIndex
   *          the absolute index of the cursor.
   */
  protected final void clearLine( final int aMode, final int aAbsoluteIndex )
  {
    int width = getWidth();
    int yPos = ( int )Math.floor( aAbsoluteIndex / width );
    int xPos = aAbsoluteIndex - ( yPos * width );

    int idx;
    int length;

    switch ( aMode )
    {
      case 0:
        // erase from cursor to end of line...
        idx = aAbsoluteIndex;
        length = width - xPos;
        break;
      case 1:
        // erase from cursor to start of line...
        idx = aAbsoluteIndex - xPos;
        length = xPos + 1;
        break;
      case 2:
        // erase entire line...
        idx = aAbsoluteIndex - xPos;
        length = width;
        break;

      default:
        throw new IllegalArgumentException( "Invalid clear line mode!" );
    }

    for ( int i = 0; i < length; i++ )
    {
      removeChar( idx++ );
    }
  }

  /**
   * Clears the screen using the given absolute index as cursor position.
   * 
   * @param aMode
   *          the clear modus: 0 = erase from cursor to below (default), 1 =
   *          erase from cursor to top, 2 = erase entire screen.
   * @param aAbsoluteIndex
   *          the absolute index of the cursor.
   */
  protected final void clearScreen( final int aMode, final int aAbsoluteIndex )
  {
    switch ( aMode )
    {
      case 0:
        // erase from cursor to end of screen...
        int lastIdx = getLastAbsoluteIndex();
        for ( int i = aAbsoluteIndex; i <= lastIdx; i++ )
        {
          removeChar( i );
        }
        break;
      case 1:
        // erase from cursor to start of screen...
        int firstIdx = getFirstAbsoluteIndex();
        for ( int i = firstIdx; i <= aAbsoluteIndex; i++ )
        {
          removeChar( i );
        }
        break;
      case 2:
        // erase entire screen...
        Arrays.fill( this.buffer, getFirstAbsoluteIndex(), getLastAbsoluteIndex() + 1, new TextCell( ' ',
            getAttributes() ) );
        // Update the heat map...
        Arrays.fill( this.heatMap, true );
        break;

      default:
        throw new IllegalArgumentException( "Invalid clear screen mode!" );
    }
  }

  /**
   * Deletes a given number of characters at the absolute index, first shifting
   * the remaining characters on that line to the left, inserting spaces at the
   * end of the line.
   * 
   * @param aAbsoluteIndex
   *          the absolute index to delete the character at;
   * @param aCount
   *          the number of times to insert the given character, > 0.
   * @return the next index.
   */
  protected final int deleteChars( final int aAbsoluteIndex, final int aCount )
  {
    int col = ( aAbsoluteIndex % getWidth() );
    int length = Math.max( 0, getWidth() - col - aCount );

    // Make room for the new characters at the end...
    System.arraycopy( this.buffer, aAbsoluteIndex + aCount, this.buffer, aAbsoluteIndex, length );

    // Fill the created room with the character to insert...
    int startIdx = aAbsoluteIndex + length;
    int endIdx = aAbsoluteIndex + getWidth() - col;
    Arrays.fill( this.buffer, startIdx, endIdx, new TextCell( ' ', getAttributes() ) );

    // Update the heat map...
    Arrays.fill( this.heatMap, aAbsoluteIndex, endIdx, true );

    return aAbsoluteIndex;
  }

  /**
   * Provides the actual implementation for {@link #readInput(CharSequence)}.
   * 
   * @see {@link #readInput(CharSequence)}
   */
  protected abstract int doReadInput( CharSequence aChars ) throws IOException;

  /**
   * Returns the absolute index according to the current cursor position.
   * 
   * @return an absolute index of the cursor position, >= 0.
   */
  protected final int getAbsoluteCursorIndex()
  {
    return getAbsoluteIndex( this.cursor.getX(), this.cursor.getY() );
  }

  /**
   * Returns the absolute index according to the given X,Y-position.
   * 
   * @param aXpos
   *          the X-position;
   * @param aYpos
   *          the Y-position.
   * @return an absolute index of the cursor position, >= 0.
   */
  protected final int getAbsoluteIndex( final int aXpos, final int aYpos )
  {
    return ( aYpos * getWidth() ) + aXpos;
  }

  /**
   * @return the (encoded) text attributes.
   */
  protected final short getAttributes()
  {
    return this.textAttributes.getAttributes();
  }

  /**
   * Returns the cell at the given absolute index.
   * 
   * @param aAbsoluteIndex
   *          the absolute of the cell to retrieve.
   * @return the text cell at the given index, can be <code>null</code> if no
   *         cell is defined.
   */
  protected final ITextCell getCellAt( final int aAbsoluteIndex )
  {
    return this.buffer[aAbsoluteIndex];
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
   * @param aAbsoluteIndex
   *          the absolute index to insert the character at;
   * @param aChar
   *          the character to insert;
   * @param aCount
   *          the number of times to insert the given character, > 0.
   * @return the next index.
   */
  protected final int insertChars( final int aAbsoluteIndex, final char aChar, final int aCount )
  {
    int col = aAbsoluteIndex % getWidth();
    int length = getWidth() - col - aCount;

    // Make room for the new characters...
    System.arraycopy( this.buffer, aAbsoluteIndex, this.buffer, aAbsoluteIndex + aCount, length );

    // Fill the created room with the character to insert...
    Arrays.fill( this.buffer, aAbsoluteIndex, aAbsoluteIndex + aCount, new TextCell( aChar, getAttributes() ) );

    // Update the heat map...
    Arrays.fill( this.heatMap, aAbsoluteIndex, aAbsoluteIndex + aCount, true );

    return aAbsoluteIndex;
  }

  /**
   * @return <code>true</code> if the last written character caused a wrap to
   *         next line, <code>false</code> otherwise.
   */
  protected final boolean isWrapped()
  {
    return this.wrapped;
  }

  /**
   * Logs the writing of the given character at the given index, at loglevel 2
   * or higher.
   * 
   * @param aChar
   * @param aIndex
   */
  protected final void log( char aChar, char aMappedChar, int aIndex )
  {
    if ( this.logLevel < 2 )
    {
      return;
    }

    System.out.printf( "LOG> (%4d)", aIndex );
    if ( aChar >= ' ' && aChar <= '~' )
    {
      System.out.printf( "[%c]", aChar );
    }
    else
    {
      System.out.printf( "<%d>", ( int )aChar );
    }
    if ( aChar != aMappedChar )
    {
      System.out.printf( " => [%c]", aMappedChar );
    }
    System.out.printf( "%n" );
  }

  /**
   * Logs the character sequence at the given starting and ending index, at
   * loglevel 1 or higher.
   * 
   * @param aText
   * @param aStart
   * @param aEnd
   * @throws IOException
   */
  protected final void log( CharSequence aText, int aStart, int aEnd )
  {
    if ( this.logLevel < 1 )
    {
      return;
    }

    System.out.printf( "LOG> " );
    for ( int i = aStart; i <= aEnd; i++ )
    {
      char c = aText.charAt( i );
      if ( c >= ' ' && c <= '~' )
      {
        System.out.printf( "%c ", c );
      }
      else
      {
        System.out.printf( "<%d> ", ( int )c );
      }
    }
    System.out.printf( "%n" );
  }

  /**
   * Logs the given text verbatimely at loglevel 0 or higher.
   * 
   * @param aText
   */
  protected final void log( String aText )
  {
    if ( this.logLevel < 0 )
    {
      return;
    }

    System.out.printf( "LOG> %s%n", aText );
  }

  /**
   * Removes the character at the absolute index.
   * 
   * @param aAbsoluteIndex
   *          the index on which to remove the character, >= 0.
   * @return the absolute index on which the character was removed.
   */
  protected final int removeChar( final int aAbsoluteIndex )
  {
    int idx = aAbsoluteIndex;
    int firstIdx = getFirstAbsoluteIndex();
    if ( idx < firstIdx )
    {
      return firstIdx;
    }
    if ( idx > getLastAbsoluteIndex() )
    {
      idx = getLastAbsoluteIndex();
    }

    TextCell cell = this.buffer[idx];
    if ( cell != null )
    {
      cell = new TextCell( ' ', cell.getAttributes() );
    }
    this.buffer[idx] = cell;
    this.heatMap[idx] = true;
    return idx;
  }

  /**
   * Resets the wrapped state.
   */
  protected final void resetWrapped()
  {
    this.wrapped = false;
  }

  /**
   * Updates the cursor according to the given absolute index.
   * 
   * @param aIndex
   *          the absolute index to convert back to a X,Y-position.
   */
  protected final void updateCursorByAbsoluteIndex( final int aIndex )
  {
    int width = getWidth();
    int yPos = ( int )Math.floor( aIndex / width );
    int xPos = aIndex - ( yPos * width );
    this.cursor.setPosition( xPos, yPos );
  }

  /**
   * Writes a given character at the absolute index, scrolling the screen up if
   * beyond the last index is written.
   * 
   * @param aAbsoluteIndex
   *          the index on which to write the given char, >= 0;
   * @param aChar
   *          the character to write;
   * @param aAttributes
   *          the attributes to use to write the character.
   * @return the absolute index after which the character was written.
   */
  protected final int writeChar( final int aAbsoluteIndex, final char aChar )
  {
    int idx = aAbsoluteIndex;
    int lastIdx = getAbsoluteIndex( getWidth() - 1, getLastScrollLine() );
    int width = getWidth();

    if ( idx > lastIdx )
    {
      idx -= width;
      scrollUp( 1 );
    }

    if ( idx <= lastIdx )
    {
      this.buffer[idx] = new TextCell( aChar, getAttributes() );
      this.heatMap[idx] = true;
    }

    // determine new absolute index...
    boolean lastColumn = ( ( idx % width ) == ( width - 1 ) );
    this.wrapped = ( isAutoWrapMode() && lastColumn );
    if ( !( !isAutoWrapMode() && lastColumn ) )
    {
      idx++;
    }

    return idx;
  }

  /**
   * Sets the dimensions of this terminal to the given width and height.
   * 
   * @param aNewWidth
   *          the new width in columns, > 0;
   * @param aNewHeight
   *          the new height in lines, > 0.
   */
  private void internalSetDimensions( final int aNewWidth, final int aNewHeight )
  {
    TextCell[] newBuffer = new TextCell[aNewWidth * aNewHeight];
    Arrays.fill( newBuffer, new TextCell() );
    if ( this.buffer != null )
    {
      int oldWidth = this.width;

      for ( int oldIdx = 0; oldIdx < this.buffer.length; oldIdx++ )
      {
        int oldColumn = ( oldIdx % oldWidth );
        int oldLine = ( oldIdx / oldWidth );
        if ( ( oldColumn >= aNewWidth ) || ( oldLine >= aNewHeight ) )
        {
          continue;
        }

        int newIdx = ( oldLine * aNewWidth ) + oldColumn;
        newBuffer[newIdx] = this.buffer[oldIdx];
      }
    }

    this.width = aNewWidth;
    this.height = aNewHeight;

    this.firstScrollLine = 0;
    this.lastScrollLine = aNewHeight - 1;

    this.buffer = newBuffer;
    this.heatMap = new boolean[newBuffer.length];

    if ( this.frontend != null )
    {
      // Notify the frontend that we've changed...
      this.frontend.terminalSizeChanged( aNewWidth, aNewHeight );
    }
  }
}
