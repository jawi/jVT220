/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import static nl.lxtreme.jvt220.terminal.vt220.VT220Parser.*;

import java.awt.*;
import java.io.*;

import javax.swing.text.*;

import nl.lxtreme.jvt220.terminal.*;
import nl.lxtreme.jvt220.terminal.vt220.CharacterSets.CharacterSet;
import nl.lxtreme.jvt220.terminal.vt220.CharacterSets.GraphicSet;
import nl.lxtreme.jvt220.terminal.vt220.VT220Parser.CSIType;
import nl.lxtreme.jvt220.terminal.vt220.VT220Parser.VT220ParserHandler;


/**
 * Represents a VT100 terminal implementation.
 */
public class VT220Terminal extends AbstractTerminal implements VT220ParserHandler
{
  // INNER TYPES

  /**
   * Provides the current state of the graphic set.
   */
  static class GraphicSetState
  {
    // VARIABLES

    private final GraphicSet[] graphicSets;
    private GraphicSet gl;
    private GraphicSet gr;
    private GraphicSet glOverride;

    // CONSTRUCTORS

    /**
     * Creates a new {@link GraphicSetState} instance.
     */
    public GraphicSetState()
    {
      this.graphicSets = new GraphicSet[4];
      for ( int i = 0; i < this.graphicSets.length; i++ )
      {
        this.graphicSets[i] = new GraphicSet( i );
      }

      resetState();
    }

    // METHODS

    /**
     * Designates the given graphic set to the character set designator.
     * 
     * @param aGraphicSet
     *          the graphic set to designate;
     * @param aDesignator
     *          the designator of the character set.
     */
    public void designateGraphicSet( GraphicSet aGraphicSet, char aDesignator )
    {
      aGraphicSet.setDesignation( CharacterSet.valueOf( aDesignator ) );
    }

    /**
     * Returns the GL graphic set.
     * 
     * @return the GL graphic set (possibly overridden), never <code>null</code>
     *         .
     */
    public GraphicSet getGL()
    {
      GraphicSet result = this.gl;
      if ( this.glOverride != null )
      {
        result = this.glOverride;
        this.glOverride = null;
      }
      return result;
    }

    /**
     * Returns the GR graphic set.
     * 
     * @return the GR graphic set, never <code>null</code>.
     */
    public GraphicSet getGR()
    {
      return this.gr;
    }

    /**
     * Returns the current graphic set (one of four).
     * 
     * @param aIndex
     *          the index of the graphic set, 0..3.
     * @return a graphic set, never <code>null</code>.
     */
    public GraphicSet getGraphicSet( int aIndex )
    {
      return this.graphicSets[aIndex % 4];
    }

    /**
     * Overrides the GL graphic set for the next written character.
     * 
     * @param aIndex
     *          the graphic set index, >= 0 && < 3.
     */
    public void overrideGL( int aIndex )
    {
      this.glOverride = getGraphicSet( aIndex );
    }

    /**
     * Resets the state to its initial values.
     */
    public void resetState()
    {
      for ( int i = 0; i < this.graphicSets.length; i++ )
      {
        this.graphicSets[i].setDesignation( CharacterSet.valueOf( ( i == 1 ) ? '0' : 'B' ) );
      }
      this.gl = this.graphicSets[0];
      this.gr = this.graphicSets[1];
      this.glOverride = null;
    }

    /**
     * Selects the graphic set for GL.
     * 
     * @param aIndex
     *          the graphic set index, >= 0 && <= 3.
     */
    public void setGL( int aIndex )
    {
      this.gl = getGraphicSet( aIndex );
    }

    /**
     * Selects the graphic set for GR.
     * 
     * @param aIndex
     *          the graphic set index, >= 0 && <= 3.
     */
    public void setGR( int aIndex )
    {
      this.gr = getGraphicSet( aIndex );
    }
  }

  static enum ResponseType
  {
    ESC, CSI, OSC; // XXX
  }

  /**
   * Contains the saved state of this terminal.
   */
  static class StateHolder
  {
    // VARIABLES

    private final CharacterSet[] graphicSetDesignations;

    private int cursorIndex;
    private short attrs;
    private boolean autoWrap;
    private boolean originMode;
    private int glIndex;
    private int grIndex;
    private int glOverrideIndex;

    // CONSTRUCTORS

    /**
     * Creates a new {@link StateHolder} instance.
     */
    public StateHolder()
    {
      this.graphicSetDesignations = new CharacterSet[4];
      this.cursorIndex = 0;
      this.autoWrap = true;
      this.originMode = false;
      this.glIndex = 0;
      this.grIndex = 1;
      this.glOverrideIndex = -1;
    }

    // METHODS

    public int restore( VT220Terminal aTerminal )
    {
      aTerminal.textAttributes.setAttributes( this.attrs );
      aTerminal.setAutoWrap( this.autoWrap );
      aTerminal.setOriginMode( this.originMode );

      GraphicSetState gss = aTerminal.graphicSetState;
      for ( int i = 0; i < gss.graphicSets.length; i++ )
      {
        gss.graphicSets[i].setDesignation( this.graphicSetDesignations[i] );
      }
      gss.setGL( this.glIndex );
      gss.setGR( this.grIndex );

      if ( this.glOverrideIndex >= 0 )
      {
        gss.overrideGL( this.glOverrideIndex );
      }

      return this.cursorIndex;
    }

    public void store( VT220Terminal aTerminal )
    {
      this.cursorIndex = aTerminal.getAbsoluteCursorIndex();
      this.attrs = aTerminal.textAttributes.getAttributes();
      this.autoWrap = aTerminal.isAutoWrapMode();
      this.originMode = aTerminal.isOriginMode();

      GraphicSetState gss = aTerminal.graphicSetState;
      this.glIndex = gss.gl.getIndex();
      this.grIndex = gss.gr.getIndex();

      this.glOverrideIndex = -1;
      if ( gss.glOverride != null )
      {
        this.glOverrideIndex = gss.glOverride.getIndex();
      }

      for ( int i = 0; i < gss.graphicSets.length; i++ )
      {
        this.graphicSetDesignations[i] = gss.graphicSets[i].getDesignation();
      }
    }
  }

  // CONSTANTS

  private static final int OPTION_132COLS = 5;
  private static final int OPTION_8BIT = 6;
  private static final int OPTION_ERASURE_MODE = 7;
  private static final int OPTION_REVERSE_WRAP_AROUND = 8;

  // VARIABLES

  private final GraphicSetState graphicSetState;
  private final VT220Parser vt220parser;
  private final StateHolder savedState;

  // CONSTRUCTORS

  /**
   * Creates a new {@link VT220Terminal} instance.
   * 
   * @param aOutputStream
   *          the output stream to write back to, cannot be <code>null</code>;
   * @param aColumns
   *          the initial number of columns in this terminal, > 0;
   * @param aLines
   *          the initial number of lines in this terminal, > 0.
   */
  public VT220Terminal( final OutputStream aOutputStream, final int aColumns, final int aLines )
  {
    super( aOutputStream, aColumns, aLines );

    this.graphicSetState = new GraphicSetState();
    this.vt220parser = new VT220Parser( 4 );
    this.savedState = new StateHolder();

    // Make sure the terminal is in a known state...
    reset();
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleCharacter( char aChar ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();
    char ch = CharacterSets.getChar( aChar, this.graphicSetState.getGL(), this.graphicSetState.getGR() );

    idx = writeChar( idx, ch );

    updateCursorByAbsoluteIndex( idx );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleControl( char aControlChar ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();
    switch ( aControlChar )
    {
      case SO:
      {
        // Shift-out (select G1 as GL)...
        this.graphicSetState.setGL( 1 );
        break;
      }

      case SI:
      {
        // Shift-in (select G0 as GL)...
        this.graphicSetState.setGL( 0 );
        break;
      }

      case BELL:
      {
        // Bell...
        Toolkit.getDefaultToolkit().beep();
        break;
      }

      case BS:
      {
        // Backspace...
        if ( isWrapped() && ( isAutoWrapMode() || isReverseWrapAround() ) )
        {
          /*
           * When reverse-autowrap mode is enabled, and a backspace is received
           * when the cursor is at the left-most column of the page, the cursor
           * is wrapped to the right-most column of the previous line. If the
           * cursor is at the top line of the scrolling region, the cursor is
           * wrapped to the right-most column of the bottom line of the
           * scrolling region. If the cursor is at the top line of terminal
           * window, the cursor is wrapped to the right-most column of the
           * bottom line of the terminal window.
           */
          idx -= isWrapped() ? 2 : 1;
          if ( idx < getAbsoluteIndex( 0, getFirstScrollLine() ) )
          {
            idx = getAbsoluteIndex( getWidth(), getLastScrollLine() );
          }
        }
        else
        {
          /*
           * When reverse-autowrap mode is disabled, and a backspace is received
           * when the cursor is at the left-most column of the page, the cursor
           * remains at that position.
           */
          idx -= ( idx % getWidth() == 0 ? 0 : 1 );
        }
        break;
      }

      case TAB:
      {
        /*
         * (Horizontal) Tab. The cursor moves right to the next tab stop. If
         * there are no further tab stops set to the right of the cursor, the
         * cursor moves to the right-most column of the current line.
         */
        idx += getTabulator().getNextTabWidth( idx % getWidth() );
        break;
      }

      case VT:
      case LF:
      case FF:
      {
        /*
         * Line feed, vertical tab, or form feed. The cursor moves to the same
         * column of the next line. If the cursor is in the bottom-most line of
         * the scrolling region, the scrolling region scrolls up one line. Lines
         * scrolled off the top of the scrolling region are lost. Blank lines
         * with no visible character attributes are added at the bottom of the
         * scrolling region.
         */
        int row = ( idx / getWidth() );
        if ( row >= getLastScrollLine() )
        {
          scrollUp( 1 );
        }
        else
        {
          idx += getWidth();
        }
        if ( !isAutoNewlineMode() )
        {
          break;
        }
        // In case of auto-newline mode: fall through to CR...
      }

      case CR:
      {
        /*
         * Carriage return. The cursor moves to the left-most column of the
         * current line.
         */
        if ( isWrapped() && isAutoWrapMode() )
        {
          // In case we're already wrapped around to the next line we need to
          // undo this and go back to the previous line instead...
          idx--;
        }
        idx -= ( idx % getWidth() );
        break;
      }

      default:
      {
        log( "Unknown control character: " + ( int )aControlChar );
        break;
      }
    }

    updateCursorByAbsoluteIndex( idx );
    resetWrapped();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleCSI( CSIType aType, int... aParameters ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();
    switch ( aType )
    {
      case ICH: // @
      {
        // Insert N (blank) Character(s) (default = 1)
        int count = aParameters[0];
        idx = insertChars( idx, ' ', count );
        break;
      }

      case SL: // [ ]@
      {
        // Scroll left N character(s) (default = 1)
        int count = aParameters[0];
        for ( int r = getFirstScrollLine(), last = getLastScrollLine(); r < last; r++ )
        {
          deleteChars( getAbsoluteIndex( 0, r ), count );
        }
        break;
      }

      case CUU: // A
      {
        // Moves the cursor up N lines in the same column. The cursor stops at
        // the top margin.
        int n = aParameters[0];

        int col = idx % getWidth();
        int row = Math.max( getFirstScrollLine(), ( idx / getWidth() ) - n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case SR: // [ ]A
      {
        // Scroll right N character(s) (default = 1)
        int count = aParameters[0];
        for ( int r = getFirstScrollLine(), last = getLastScrollLine(); r < last; r++ )
        {
          insertChars( getAbsoluteIndex( 0, r ), ' ', count );
        }
        break;
      }

      case CUD: // B
      {
        // Moves the cursor down N lines in the same column. The cursor stops at
        // the bottom margin.
        int n = aParameters[0];

        int col = idx % getWidth();
        int row = Math.min( getLastScrollLine(), ( idx / getWidth() ) + n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CUF: // C
      {
        // Moves the cursor right N columns. The cursor stops at the right
        // margin.
        int n = aParameters[0];

        int col = Math.min( getWidth() - 1, ( idx % getWidth() ) + n );
        int row = idx / getWidth();

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CUB: // D
      {
        // Moves the cursor left N columns. The cursor stops at the left margin.
        int n = aParameters[0];

        if ( isAutoWrapMode() && isWrapped() )
        {
          idx = Math.max( getFirstAbsoluteIndex(), idx - n );
        }
        else
        {
          int col = Math.max( 0, ( idx % getWidth() ) - n );
          int row = idx / getWidth();
          idx = getAbsoluteIndex( col, row );
        }
        break;
      }

      case CNL: // E
      {
        // Move cursor down the indicated # of rows, to column 1.
        int n = aParameters[0];

        int col = 0;
        int row = Math.min( getLastScrollLine(), ( idx / getWidth() ) + n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CPL: // F
      {
        // Move cursor up the indicated # of rows, to column 1.
        int n = aParameters[0];

        int col = 0;
        int row = Math.max( getFirstScrollLine(), ( idx / getWidth() ) - n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CHA: // G,`
      {
        // Character Position Absolute [column] (default = [row,1])
        // Cursor Character Absolute [column] (default = [row,1])
        int x = aParameters[0];

        int col = Math.max( 0, Math.min( getWidth(), x - 1 ) );
        int row = Math.max( getFirstScrollLine(), Math.min( getLastScrollLine(), idx / getWidth() ) );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CUP: // H,f
      {
        // Cursor Position [row;column] (default = [1,1])
        int row = aParameters[0] - 1;
        int col = aParameters[1] - 1;

        // Movement is *relative* to origin, if enabled...
        if ( isOriginMode() )
        {
          row += getFirstScrollLine();
        }
        if ( row > getLastScrollLine() )
        {
          row = getLastScrollLine();
        }
        if ( col >= getWidth() )
        {
          col = getWidth() - 1;
        }

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CHT: // I
      {
        // Cursor Forward Tabulation N tab stops (default = 1)
        int count = aParameters[0];
        while ( count-- > 0 )
        {
          idx += getTabulator().getNextTabWidth( idx % getWidth() );
        }
        break;
      }

      case ED: // J
      {
        // Erase in Display...
        int mode = aParameters[0];

        clearScreen( mode, idx, isErasureMode() );
        break;
      }

      case DECSED: // ^J
      {
        // Erase in Display...
        int mode = aParameters[0];

        clearScreen( mode, idx, true /* aKeepProtectedCells */);
        break;
      }

      case EL: // K
      {
        // Clear line...
        int mode = aParameters[0];

        clearLine( mode, idx, isErasureMode() );
        break;
      }

      case DECSEL: // ^K
      {
        // Clear line...
        int mode = aParameters[0];

        clearLine( mode, idx, true /* aKeepProtectedCells */);
        break;
      }

      case IL: // L
      {
        // (IL) Inserts N lines at the cursor. If fewer than N lines
        // remain from the current line to the end of the scrolling
        // region, the number of lines inserted is the lesser number.
        // Lines within the scrolling region at and below the cursor move
        // down. Lines moved past the bottom margin are lost. The cursor
        // is reset to the first column. This sequence is ignored when the
        // cursor is outside the scrolling region.
        int lines = aParameters[0];

        scrollDown( lines ); // XXX this is not correct!
        break;
      }

      case DL: // M
      {
        // (DL) Deletes N lines starting at the line with the cursor. If
        // fewer than N lines remain from the current line to the end of
        // the scrolling region, the number of lines deleted is the lesser
        // number. As lines are deleted, lines within the scrolling region
        // and below the cursor move up, and blank lines are added at the
        // bottom of the scrolling region. The cursor is reset to the
        // first column. This sequence is ignored when the cursor is
        // outside the scrolling region.
        int lines = aParameters[0];

        scrollUp( lines ); // XXX this is not correct!
        break;
      }

      case DCH: // P
      {
        // Delete N Character(s) (default = 1)
        int count = aParameters[0];

        idx = deleteChars( idx, count );
        break;
      }

      case SU: // S
      {
        // Scroll N lines up...
        int lines = aParameters[0];

        scrollUp( lines );
        break;
      }

      case SD: // T
      {
        // Scroll N lines down...
        int lines = aParameters[0];

        scrollDown( lines );
        break;
      }

      case ECH: // X
      {
        // Erase N Character(s) (default = 1)
        int n = aParameters[0] - 1;

        int tmpIdx = idx;
        if ( tmpIdx + n > getWidth() )
        {
          n = getWidth() - ( tmpIdx % getWidth() ) - 1;
        }

        do
        {
          removeChar( tmpIdx++, isErasureMode() );
        }
        while ( n-- > 0 );
        break;
      }

      case CBT: // Z
      {
        // Cursor Backward Tabulation N tab stops (default = 1)
        int count = aParameters[0];
        while ( count-- > 0 )
        {
          idx -= getTabulator().getPreviousTabWidth( idx % getWidth() );
        }
        break;
      }

      case HPR: // a
      {
        // Move cursor right the indicated # of columns.
        int n = aParameters[0];

        int w = getWidth();
        int col = Math.min( w - 1, ( idx % w ) + n );
        int row = idx / w;

        idx = getAbsoluteIndex( col, row );

        break;
      }

      case REP: // b
      {
        // Repeat the preceding graphic character N times
        int count = aParameters[0];
        char ch = ( char )aParameters[1];

        while ( count-- > 0 )
        {
          idx = writeChar( idx, ch );
        }
        break;
      }

      case PrimaryDA: // c
      {
        // Send Device Attributes (Primary DA)
        int arg = aParameters[0];
        sendDeviceAttributes( arg );
        break;
      }

      case SecondaryDA: // >c
      {
        // Send Device Attributes (Secondary DA)
        int arg = aParameters[0];
        if ( arg == 0 )
        {
          writeResponse( ResponseType.CSI, ">1;123;0c" );
        }
        break;
      }

      case VPA: // d
      {
        // (VPA) Line Position Absolute [row] (default = [1,column])
        int row = aParameters[0] - 1;
        int col = idx % getWidth();

        if ( row < getFirstScrollLine() )
        {
          row = getFirstScrollLine();
        }
        else if ( row > getLastScrollLine() )
        {
          row = getLastScrollLine();
        }

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case VPR: // e
      {
        // Move cursor down the indicated N of columns (default = 1).
        int n = aParameters[0];

        int col = idx % getWidth();
        int row = ( idx / getWidth() ) + n;
        if ( row > getLastScrollLine() )
        {
          row = getLastScrollLine();
        }

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case TBC: // g
      {
        // Tab Clear
        int arg = aParameters[0];

        if ( arg == 0 )
        {
          int col = ( idx % getWidth() );
          getTabulator().clear( col );
        }
        else if ( arg == 3 )
        {
          clearAllTabStops();
        }
        break;
      }

      case SM: // h
      {
        // Set mode
        int arg = aParameters[0];
        switch ( arg )
        {
          case 4:
            // (IRM) Insert/Replace Mode
            setInsertMode( true );
            break;

          case 6:
            // (ERM) Erasure mode
            setErasureMode( false );
            break;

          case 20:
            // (LNM) Automatic Newline
            setAutoNewlineMode( true );
            break;

          default:
            log( "Unknown SET MODE: " + arg );
            break;
        }
        break;
      }

      case DECSET: // ?h
      {
        // DEC Private Mode Set
        int arg = aParameters[0];
        switch ( arg )
        {
          case 2:
            // (DECANM) Designate USASCII for character sets G0-G3; set VT100
            // mode.
            this.graphicSetState.resetState();
            set8bitMode( false );
            break;

          case 3:
            // (DECCOLM) switch 80/132 column mode; default = 80 columns.
            set132ColumnMode( true );
            // Clear entire screen...
            clearScreen( 2 );
            // Reset scrolling region...
            setOriginMode( false );
            setScrollRegion( getFirstScrollLine(), getLastScrollLine() );
            // Reset cursor to first possible position...
            idx = getAbsoluteIndex( 0, getFirstScrollLine() );
            break;

          case 4:
            // (DECSCLM) Smooth (Slow) Scroll; ignored...
            break;

          case 5:
            // (DECSCNM) set reverse-video mode; default = off.
            setReverse( true );
            break;

          case 6:
            // (DECOM) set origin mode; default = off.
            setOriginMode( true );

            // Reset cursor to first possible position...
            idx = getAbsoluteIndex( 0, getFirstScrollLine() );
            break;

          case 7:
            // (DECAWM) set auto-wrap mode; default = on.
            setAutoWrap( true );
            break;

          case 25:
            // (DECTCEM) Show Cursor; default = on.
            getCursor().setVisible( true );
            break;

          case 45:
            // Reverse-wraparound Mode; default = on.
            setReverseWrapAround( true );
            break;

          default:
            log( "Unknown DEC SET PRIVATE MODE: " + arg );
            break;
        }
        break;
      }

      case MC: // i
      case DECSMC: // ?i
      {
        // Media copy; not implemented...
        break;
      }

      case HPB: // j
      {
        // Character position backward N positions (default = 1).
        int n = aParameters[0];

        int col = Math.max( 0, idx % getWidth() - n );
        int row = idx / getWidth();

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case VPB: // k
      {
        // Line position backward N lines (default = 1).
        int n = aParameters[0];

        int w = getWidth();
        int col = idx % w;
        int row = Math.max( getFirstScrollLine(), ( idx / w ) - n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case RM: // l
      {
        // Reset mode
        int arg = aParameters[0];
        switch ( arg )
        {
          case 4:
            // (IRM) Insert/Replace Mode
            setInsertMode( false );
            break;

          case 6:
            // (ERM) Erasure mode
            setErasureMode( true );
            break;

          case 20:
            // (LNM) Automatic Newline
            setAutoNewlineMode( false );
            break;

          default:
            log( "Unknown RESET MODE: " + arg );
            break;
        }
        break;
      }

      case DECRST: // ?l
      {
        // Reset mode / DEC Private Mode Reset...
        int arg = aParameters[0];
        switch ( arg )
        {
          case 2:
            // (DECANM) Set VT52 mode.
            this.graphicSetState.resetState();
            break;

          case 3:
            // (DECCOLM) switch 80/132 column mode; default = 80
            // columns.
            set132ColumnMode( false );
            // Clear entire screen...
            clearScreen( 2 );
            // Reset scrolling region...
            setOriginMode( false );
            setScrollRegion( getFirstScrollLine(), getLastScrollLine() );
            // Reset cursor to first possible position...
            idx = getAbsoluteIndex( 0, getFirstScrollLine() );
            break;

          case 4:
            // (DECSCLM) Smooth (Slow) Scroll; ignored...
            break;

          case 5:
            // (DECSCNM) set reverse-video mode; default = off.
            setReverse( false );
            break;

          case 6:
            // (DECOM) set origin mode; default = off.
            setOriginMode( false );

            // Reset cursor to first possible position...
            idx = getAbsoluteIndex( 0, getFirstScrollLine() );
            break;

          case 7:
            // (DECAWM) set auto-wrap mode; default = on.
            setAutoWrap( false );
            break;

          case 25:
            // (DECTCEM) Hide Cursor; default = on.
            getCursor().setVisible( false );
            break;

          case 45:
            // Reverse-wraparound Mode; default = on.
            setReverseWrapAround( false );
            break;

          default:
            log( "Unknown DEC RESET PRIVATE MODE: " + arg );
            break;
        }
        break;
      }

      case SGR: // m
      {
        // Set Graphics Rendering
        handleGraphicsRendering( aParameters );
        break;
      }

      case DSR: // n
      {
        int arg = aParameters[0];
        if ( arg == 5 )
        {
          // Status report...
          writeResponse( ResponseType.CSI, "0n" );
        }
        else if ( arg == 6 )
        {
          // Report cursor position...
          int col = ( idx % getWidth() ) + 1;
          int row = ( idx / getWidth() ) + 1;
          writeResponse( ResponseType.CSI, String.format( "%d;%dR", row, col ) );
        }
        break;
      }

      case DECSDSR: // ?n
      {
        // Device Status Report (DEC-specific)
        int arg = aParameters[0];
        switch ( arg )
        {
          case 6:
            // Report Cursor Position
            int col = ( idx % getWidth() ) + 1;
            int row = ( idx / getWidth() ) + 1;
            writeResponse( ResponseType.CSI, String.format( "?%d;%dR", row, col ) );
            break;

          case 15:
            // Report Printer Status; always reports it as not ready
            writeResponse( ResponseType.CSI, "?11n" );
            break;

          case 25:
            // Report UDK status; always reports it as locked
            writeResponse( ResponseType.CSI, "?21n" );
            break;

          case 26:
            // Report keyboard status; always reports it as North American
            writeResponse( ResponseType.CSI, "?27;1;0;0n" );
            break;

          default:
            log( "Unknown/unhandled DECSDR argument: " + arg );
            break;
        }
        break;
      }

      case DECSTR: // !p
      {
        // Soft terminal reset
        softReset();
        break;
      }

      case DECSCL: // "p
      {
        // Set conformance level
        int compatibilityLevel = ( aParameters[0] - 60 );
        int eightBitMode = ( aParameters.length > 1 ) ? aParameters[1] : 0;
        setConformanceLevel( compatibilityLevel, eightBitMode != 1 );
        break;
      }

      case DECSCA: // "q
      {
        // Select character protection attribute
        int mode = aParameters[0];
        // 0 or 2 == erasable; 1 == not erasable...
        this.textAttributes.setProtected( mode == 1 );
        break;
      }

      case DECSTBM: // r
      {
        // Set Scrolling Region [top;bottom] (default = full size of window)
        int top = aParameters[0];
        int bottom = aParameters[1];
        if ( bottom == 0 )
        {
          bottom = getHeight();
        }
        if ( bottom > top )
        {
          setScrollRegion( top - 1, bottom - 1 );
          idx = getAbsoluteIndex( 0, getFirstScrollLine() );
        }
        break;
      }

      case RestoreDECPM: // ?r
      {
        // TODO
        break;
      }

      case DECCARA: // $r
      {
        // TODO
        break;
      }

      case SaveDECPM: // ?s
      {
        // TODO
        break;
      }

      case WindowManipulation: // t
      {
        // dtterm window manipulation...
        int type = aParameters[0];
        switch ( type )
        {
          case 4:
            // resize to height;width in pixels
            if ( aParameters.length == 3 )
            {
              int height = aParameters[1];
              int width = aParameters[2];

              setDimensionsInPixels( width, height );
              idx = getFirstAbsoluteIndex();
            }
            break;

          case 8:
            if ( aParameters.length == 3 )
            {
              // resize to height;width in chars
              int rows = aParameters[1];
              int cols = aParameters[2];

              setDimensions( cols, rows );
              idx = getFirstAbsoluteIndex();
            }
            break;

          case 11:
            // report window state (always fixed)
            writeResponse( ResponseType.CSI, "1t" );
            break;

          case 13:
            // report window location (always fixed)
            writeResponse( ResponseType.CSI, "3;0;0t" );
            break;

          case 14:
            // report dimensions in pixels
            int width = 0;
            int height = 0;

            ITerminalFrontend frontend = getFrontend();
            if ( frontend != null )
            {
              Dimension size = frontend.getSize();
              width = size.width;
              height = size.height;
            }

            writeResponse( ResponseType.CSI, String.format( "%d;%d;%dt", ( type - 10 ), height, width ) );
            break;

          case 18:
          case 19:
            // report dimensions in characters
            writeResponse( ResponseType.CSI, String.format( "%d;%d;%dt", ( type - 10 ), getHeight(), getWidth() ) );
            break;

          case 20:
            // report window icon's label: OSC L <text> ST
            writeResponse( ResponseType.OSC, "L\033\\" );
            break;

          case 21:
            // report window title: OSC l <text> ST
            writeResponse( ResponseType.OSC, "l\033\\" );
            break;

          default:
            if ( type >= 24 )
            {
              // resize to N lines...
              int cols = getWidth();
              int rows = type;

              setDimensions( cols, rows );
              idx = getFirstAbsoluteIndex();
            }
            break;
        }
        break;
      }

      case DECRARA: // $t
      {
        // TODO
        break;
      }

      case DECCRA: // $v
      {
        // TODO
        break;
      }

      case DECEFR: // 'w
      {
        // TODO
        break;
      }

      case DECREQTPARM: // x
      {
        // Request Terminal Parameters
        int arg = aParameters[0];

        writeResponse( ResponseType.CSI, String.format( "%d;1;1;112;112;1;0x", arg + 2 ) );
        break;
      }

      case DECFRA: // $x
      {
        // TODO
        break;
      }

      case DECELR: // 'z
      {
        // TODO
        break;
      }

      case DECERA: // $z
      {
        // TODO
        break;
      }

      case DECSLE: // '{
      {
        // TODO
        break;
      }

      case DECSERA: // ${
      {
        // TODO
        break;
      }

      case DECRQLP: // '|
      {
        // TODO
        break;
      }

      default:
        log( "Unhandled CSI: " + aType );
        break;
    }
    // Update the cursor position...
    updateCursorByAbsoluteIndex( idx );
    resetWrapped();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleESC( char aDesignator, int... aParameters ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();
    switch ( aDesignator )
    {
      case 'D': // IND
      {
        idx = handleIND( idx );
        break;
      }
      case 'E': // NEL
      {
        idx = handleNEL( idx );
        break;
      }
      case 'H': // HTS
      {
        handleTabSet( idx );
        break;
      }
      case 'M': // RI
      {
        idx = handleRI( idx );
        break;
      }
      case 'N': // SS2
      {
        handleSS2();
        break;
      }
      case 'O': // SS3
      {
        handleSS3();
        break;
      }
      case 'V': // SPA
      {
        handleSPA();
        break;
      }
      case 'W': // EPA
      {
        handleEPA();
        break;
      }
      case 'Z': // DECID
      {
        sendDeviceAttributes( 0 );
        break;
      }
      case 'c': // RIS
      {
        reset();
        idx = getFirstAbsoluteIndex();
        break;
      }
      case 'n': // LS2
      {
        // Invoke the G2 Character Set as GL
        this.graphicSetState.setGL( 2 );
        break;
      }
      case 'o': // LS3
      {
        // Invoke the G3 Character Set as GL
        this.graphicSetState.setGL( 3 );
        break;
      }
      case '|': // LS3R
      {
        // Invoke the G3 Character Set as GR
        this.graphicSetState.setGR( 3 );
        break;
      }
      case '}': // LS2R
      {
        // Invoke the G2 Character Set as GR
        this.graphicSetState.setGR( 2 );
        break;
      }
      case '~': // LS1R
      {
        // Invoke the G1 Character Set as GR
        this.graphicSetState.setGR( 1 );
        break;
      }
      case '7': // DECSC
      {
        saveCursor( idx );
        break;
      }
      case '8': // DECRC
      {
        idx = restoreCursor();
        break;
      }
      case ' ':
      {
        char arg = ( char )aParameters[0];
        switch ( arg )
        {
          case 'F':
            // 7-bit controls...
            set8bitMode( false );
            break;

          case 'G':
            // 8-bit controls...
            set8bitMode( true );
            break;

          case 'L':
          case 'M':
          case 'N':
            // ANSI conformance level... ignored.
            break;

          default:
            log( "Unhandled argument for ESC sp: " + arg );
            break;
        }
        break;
      }
      case '#':
      {
        char arg = ( char )aParameters[0];
        switch ( arg )
        {
          case '8':
            // DEC Screen Alignment Test (DECALN)
            for ( int j = getFirstAbsoluteIndex(), last = getLastAbsoluteIndex(); j <= last; j++ )
            {
              writeChar( j, 'E' );
            }
            break;

          case '3':
          case '4':
          case '5':
          case '6':
            // Single/double line width/height... ignored.
            break;

          default:
            log( "Unhandled argument for ESC sp: " + arg );
            break;
        }
        break;
      }
      case '(':
      case ')':
      case '*':
      case '+':
      {
        // Designate G0/G1/G2 or G3 Character Set
        GraphicSet gs = this.graphicSetState.getGraphicSet( aDesignator - '(' );
        this.graphicSetState.designateGraphicSet( gs, ( char )aParameters[0] );
        break;
      }
      case '=':
      {
        // (DECPAM) Application Keypad
        log( "TODO: implemenent Application Keypad mode!" );
        break;
      }
      case '>':
      {
        // (DECPNM) Normal Keypad
        log( "TODO: implemenent Normal Keypad mode!" );
        break;
      }

      default:
      {
        log( "Unhandled ESC designator: " + aDesignator );
        break;
      }
    }
    // Update the cursor position...
    updateCursorByAbsoluteIndex( idx );
    resetWrapped();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reset()
  {
    softReset();
    // Clear entire screen...
    clearScreen( 2 );
  }

  /**
   * Sets the dimensions of this terminal in columns and lines. Overridden in
   * order to handle zero and negative values as possible values.
   * 
   * @param aNewWidth
   *          the new width of this terminal in columns, if zero or negative,
   *          the maximum possible number of columns will be used;
   * @param aNewHeight
   *          the new height of this terminal in lines, if zero or negative, the
   *          maximum possible number of lines will be used.
   */
  @Override
  public void setDimensions( int aNewWidth, int aNewHeight )
  {
    if ( aNewWidth <= 0 || aNewHeight <= 0 )
    {
      Dimension maximumSize = null;
      if ( getFrontend() != null )
      {
        maximumSize = getFrontend().getMaximumTerminalSize();
      }
      if ( aNewWidth <= 0 )
      {
        aNewWidth = ( maximumSize != null ) ? maximumSize.width : getWidth();
      }
      if ( aNewHeight <= 0 )
      {
        aNewHeight = ( maximumSize != null ) ? maximumSize.height : getHeight();
      }
    }
    super.setDimensions( aNewWidth, aNewHeight );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int doReadInput( final CharSequence aText ) throws IOException
  {
    return this.vt220parser.parse( aText, this );
  }

  /**
   * Returns whether or not the 132-column mode is enabled.
   * 
   * @return <code>true</code> if 132-column mode is enabled, <code>false</code>
   *         otherwise.
   * @see #set132ColumnMode(boolean)
   */
  protected final boolean is132ColumnMode()
  {
    return this.options.get( OPTION_132COLS );
  }

  /**
   * Returns whether or not responses are send in 8-bit or in 7-bit mode.
   * 
   * @return <code>true</code> if responses are to be sent in 8-bit mode,
   *         <code>false</code> to send responses in 7-bit mode.
   * @see #set8bitMode(boolean)
   */
  protected final boolean is8bitMode()
  {
    return this.options.get( OPTION_8BIT );
  }

  /**
   * Returns whether or not protected content can be erased.
   * 
   * @return <code>true</code> if only unprotected content can be erased,
   *         <code>false</code> if both protected and unprotected content can be
   *         erased.
   * @see #setErasureMode(boolean)
   */
  protected final boolean isErasureMode()
  {
    return this.options.get( OPTION_ERASURE_MODE );
  }

  /**
   * Returns whether or not reverse wrap arounds are supported.
   * 
   * @return <code>true</code> if a "reverse wrap around" is allowed,
   *         <code>false</code> otherwise.
   * @see #setReverseWrapAround(boolean)
   */
  protected final boolean isReverseWrapAround()
  {
    return this.options.get( OPTION_REVERSE_WRAP_AROUND );
  }

  /**
   * Enables or disables the auto-wrap mode.
   * 
   * @param aEnable
   *          <code>true</code> to enable auto-wrap mode, <code>false</code> to
   *          disable it.
   */
  protected final void set132ColumnMode( boolean aEnable )
  {
    this.options.set( OPTION_132COLS, aEnable );

    if ( aEnable )
    {
      setDimensions( 132, getHeight() );
    }
    else
    {
      setDimensions( 80, getHeight() );
    }
  }

  /**
   * Sets whether or not responses send by this terminal are in 7- or in 8-bit
   * mode.
   * 
   * @param aEnable
   *          <code>true</code> to send responses in 8-bit mode,
   *          <code>false</code> to send responses in 7-bit mode.
   */
  protected final void set8bitMode( boolean aEnable )
  {
    this.options.set( OPTION_8BIT, aEnable );
  }

  /**
   * Sets whether or not protected content is to be erased.
   * 
   * @param aEnable
   *          <code>true</code> to allow only unprotected content to be erased,
   *          <code>false</code> to allow both protected and unprotected content
   *          to be erased.
   */
  protected final void setErasureMode( boolean aEnable )
  {
    this.options.set( OPTION_ERASURE_MODE, aEnable );
  }

  /**
   * Sets whether or not reverse wrap arounds are supported, meaning that
   * hitting backspace or issuing cursor-back commands after a wrap-around has
   * taken place will revert this wrap-around.
   * 
   * @param aEnable
   *          <code>true</code> to enable reverse wrap around,
   *          <code>false</code> to disable it.
   */
  protected final void setReverseWrapAround( boolean aEnable )
  {
    this.options.set( OPTION_REVERSE_WRAP_AROUND, aEnable );
  }

  /**
   * End of protected area.
   */
  private void handleEPA()
  {
    this.textAttributes.setProtected( false );
  }

  /**
   * Converts the graphics rendering options in the form of a given parameter
   * stack into a {@link AttributeSet}.
   * 
   * @param aTermState
   *          the current parser state, used to create a {@link AttributeSet}
   *          instance, cannot be <code>null</code>.
   * @return a {@link SimpleAttributeSet} instance, never <code>null</code>.
   */
  private void handleGraphicsRendering( int[] aParameters )
  {
    boolean containsHiddenAttr = false;
    for ( int p : aParameters )
    {
      if ( p == 8 )
      {
        containsHiddenAttr = true;
        break;
      }
    }

    if ( this.textAttributes.isHidden() && !containsHiddenAttr )
    {
      // It appears that after an invisible attribute, *both* reverse as
      // invisible are to be reset...
      this.textAttributes.setHidden( false );
      this.textAttributes.setReverse( false );
    }

    for ( int p : aParameters )
    {
      handleGraphicsRendering( this.textAttributes, p );
    }
  }

  /**
   * Handles the given graphics parameter.
   * 
   * @param aAttributes
   *          the text attributes to update;
   * @param aParameter
   *          the graphics parameter to handle.
   */
  private void handleGraphicsRendering( final TextAttributes aAttributes, final int aParameter )
  {
    if ( aParameter == 0 || aParameter == 39 || aParameter == 49 )
    {
      if ( aParameter == 0 )
      {
        aAttributes.resetAll();
      }
      else
      {
        aAttributes.reset();
      }
    }

    if ( aParameter == 1 )
    {
      aAttributes.setBold( true );
    }
    else if ( aParameter == 4 )
    {
      aAttributes.setUnderline( true );
    }
    else if ( aParameter == 5 )
    {
      // Blink on...
      aAttributes.setItalic( true );
    }
    else if ( aParameter == 7 )
    {
      // Negative/reverse...
      aAttributes.setReverse( true );
    }
    else if ( aParameter == 8 )
    {
      // Invisible (hidden)...
      aAttributes.setHidden( true );
    }
    else if ( aParameter == 21 || aParameter == 22 )
    {
      aAttributes.setBold( false );
    }
    else if ( aParameter == 24 )
    {
      aAttributes.setUnderline( false );
    }
    else if ( aParameter == 25 )
    {
      // Blink off...
      aAttributes.setItalic( false );
    }
    else if ( aParameter == 27 )
    {
      // Reset negative...
      aAttributes.setReverse( false );
    }
    else if ( aParameter == 28 )
    {
      // Reset invisible (shown)...
      aAttributes.setHidden( false );
    }
    else if ( ( aParameter >= 30 ) && ( aParameter <= 37 ) )
    {
      // Handle foreground color...
      aAttributes.setForeground( aParameter - 29 );
    }
    else if ( aParameter == 39 )
    {
      // Default foreground color...
      aAttributes.setForeground( 0 );
    }
    else if ( ( aParameter >= 40 ) && ( aParameter <= 47 ) )
    {
      aAttributes.setBackground( aParameter - 39 );
    }
    else if ( aParameter == 49 )
    {
      // Default background color...
      aAttributes.setBackground( 0 );
    }
    else if ( aParameter > 0 )
    {
      log( "Unhandled attribute: " + aParameter );
    }
  }

  /**
   * IND moves the cursor down one line in the same column. If the cursor is at
   * the bottom margin, the screen performs a scroll-up.
   * 
   * @param aTermState
   */
  private int handleIND( final int aIndex )
  {
    int col = aIndex % getWidth();
    int row = ( aIndex / getWidth() ) + 1;

    if ( row > getLastScrollLine() )
    {
      row = getLastScrollLine();
      scrollUp( 1 );
    }

    return getAbsoluteIndex( col, row );
  }

  /**
   * NEL moves the cursor to the first position on the next line. If the cursor
   * is at the bottom margin, the screen performs a scroll-up.
   * 
   * @param aTermState
   */
  private int handleNEL( int aIndex )
  {
    int col = 0;
    int row = ( aIndex / getWidth() ) + 1;

    if ( row > getLastScrollLine() )
    {
      row = getLastScrollLine();
      scrollUp( 1 );
    }

    return getAbsoluteIndex( col, row );
  }

  /**
   * RI moves the cursor up one line in the same column. If the cursor is at the
   * top margin, the screen performs a scroll-down.
   * 
   * @param aTermState
   */
  private int handleRI( int aIndex )
  {
    int col = aIndex % getWidth();
    int row = ( aIndex / getWidth() ) - 1;

    if ( row < getFirstScrollLine() )
    {
      row = getFirstScrollLine();
      scrollDown( 1 );
    }

    return getAbsoluteIndex( col, row );
  }

  /**
   * Start of protected area.
   */
  private void handleSPA()
  {
    this.textAttributes.setProtected( true );
  }

  /**
   * Single Shift Select of G2 Character Set. Affects next character only.
   */
  private void handleSS2()
  {
    this.graphicSetState.overrideGL( 2 );
  }

  /**
   * Single Shift Select of G3 Character Set. Affects next character only.
   */
  private void handleSS3()
  {
    this.graphicSetState.overrideGL( 3 );
  }

  /**
   * @param aTermState
   */
  private void handleTabSet( int aIndex )
  {
    getTabulator().set( ( aIndex % getWidth() ) );
  }

  /**
   * (DECRC) Restores the previously stored cursor position.
   */
  private int restoreCursor()
  {
    return this.savedState.restore( this );
  }

  /**
   * (DECSC) Stores the current cursor position.
   */
  private void saveCursor( int aIndex )
  {
    this.savedState.store( this );
  }

  /**
   * Send Device Attributes (Primary DA).
   */
  private void sendDeviceAttributes( int aArg ) throws IOException
  {
    if ( aArg != 0 )
    {
      log( "Unknown DA: " + aArg );
    }
    else
    {
      if ( this.vt220parser.isVT52mode() )
      {
        // Send back that we're a VT52...
        writeResponse( ResponseType.ESC, "/Z" );
      }
      else
      {
        // Send back that we're a VT220...
        writeResponse( ResponseType.CSI, "?62;1;2;4;6;8;9;15c" );
      }
    }
  }

  /**
   * Sets the compatibility level for this terminal.
   * 
   * @param aLevel
   *          &lt;= 1 equals to VT100 mode, &gt;= 2 equals to VT200 mode;
   * @param aEightBitMode
   *          <code>true</code> if 8-bit controls are preferred (only in VT200
   *          mode), <code>false</code> if 7-bit controls are preferred.
   */
  private void setConformanceLevel( int aLevel, boolean aEightBitMode )
  {
    if ( aLevel <= 1 )
    {
      // @formatter:off
      /* VT100 mode:
       * - keyboard sends ASCII only;
       * - keystrokes that normally send DEC Supplemental Characters transfer nothing;
       * - user-defined keys are do not operate;
       * - special function keys and six editing keys do not operate (except F11, F12, and F13, which send ESC, BS, and LF, respectively);
       * - always 7-bit mode: the 8th bit of all received characters is set to zero (0);
       * - character sets: Only ASCII, national replacement characters, and DEC special graphics are available;
       * - all transmitted C1 controls are forced to S7C1 state and sent as 7-bit escape sequences.
       */
      // @formatter:on
      softReset();
      set8bitMode( false );
    }
    else if ( aLevel >= 2 )
    {
      // @formatter:off
      /* VT200 mode:
       * - keyboard permits full use of VT220 keyboard;
       * - 7 or 8-bit mode: the 8th bit of all received characters is significant;
       * - all VT220 character sets are available.
       */
      // @formatter:on
      softReset();
      set8bitMode( aEightBitMode );
    }
  }

  /**
   * Sets the dimensions of the terminal frontend, in pixels.
   * 
   * @param aWidth
   *          the new width, in pixels;
   * @param aHeight
   *          the new height, in pixels.
   */
  private void setDimensionsInPixels( int aWidth, int aHeight )
  {
    ITerminalFrontend frontend = getFrontend();
    if ( frontend != null )
    {
      frontend.setSize( aWidth, aHeight );
    }
  }

  /**
   * Performs a soft reset.
   */
  private void softReset()
  {
    // See: <http://www.vt100.net/docs/vt220-rm/table4-10.html> and
    // <http://h30097.www3.hp.com/docs/base_doc/DOCUMENTATION/V50A_HTML/MAN/MAN5/0036____.HTM>

    // Turns on the text cursor (DECTCEM)
    getCursor().setVisible( true );
    // Enables replace mode (IRM)
    setInsertMode( false );
    // Turns off origin mode (DECOM)
    setOriginMode( false );
    // Sets the top and bottom margins to the first and last lines of the window
    // (DECSTBM)
    setScrollRegion( getFirstScrollLine(), getLastScrollLine() );
    // Turns on autowrap (DECAWM)
    setAutoWrap( true );
    // Turns off reverse wrap
    setReverseWrapAround( true );
    // Unlocks the keyboard (KAM)
    // Sets the cursor keypad mode to normal (DECCKM)
    // Sets the numeric keypad mode to numeric (DECNKM)

    set8bitMode( false );
    set132ColumnMode( false );
    // Sets selective erase mode off (DECSCA)
    setErasureMode( true );

    // Sets all character sets (GL, G0, G1, G2 and G3) to ASCII
    this.graphicSetState.resetState();
    // Turns off all character attributes (SGR)
    this.textAttributes.resetAll();

    // Clears any cursor state information saved with save cursor (DECSC)
    saveCursor( getFirstAbsoluteIndex() );
  }

  /**
   * Writes a response according to a given type.
   * 
   * @param aType
   *          the type of response to write;
   * @param aContent
   *          the actual content to write.
   * @throws IOException
   *           in case of I/O problems writing the response.
   */
  private void writeResponse( ResponseType aType, String aContent ) throws IOException
  {
    switch ( aType )
    {
      case ESC:
        writeResponse( "\033" );
        break;

      case OSC:
        writeResponse( is8bitMode() ? Character.toString( VT220Parser.OSC ) : "\033]" );
        break;

      case CSI:
        writeResponse( is8bitMode() ? Character.toString( VT220Parser.CSI ) : "\033[" );
        break;

      default:
        throw new RuntimeException( "Unhandled response type: " + aType );
    }
    writeResponse( aContent );
  }
}
