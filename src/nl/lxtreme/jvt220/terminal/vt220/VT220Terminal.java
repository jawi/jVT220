/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import static java.awt.event.KeyEvent.*;
import static nl.lxtreme.jvt220.terminal.vt220.VT220Parser.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

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

    private final GraphicSet[] m_graphicSets;
    private GraphicSet m_gl;
    private GraphicSet m_gr;
    private GraphicSet m_glOverride;

    // CONSTRUCTORS

    /**
     * Creates a new {@link GraphicSetState} instance.
     */
    public GraphicSetState()
    {
      m_graphicSets = new GraphicSet[4];
      for ( int i = 0; i < m_graphicSets.length; i++ )
      {
        m_graphicSets[i] = new GraphicSet( i );
      }

      resetState();
    }

    // METHODS

    /**
     * Designates the given graphic set to the character set designator.
     * 
     * @param graphicSet
     *          the graphic set to designate;
     * @param designator
     *          the designator of the character set.
     */
    public void designateGraphicSet( GraphicSet graphicSet, char designator )
    {
      graphicSet.setDesignation( CharacterSet.valueOf( designator ) );
    }

    /**
     * Returns the (possibly overridden) GL graphic set.
     * 
     * @return the GL graphic set, never <code>null</code>.
     */
    public GraphicSet getGL()
    {
      GraphicSet result = m_gl;
      if ( m_glOverride != null )
      {
        result = m_glOverride;
        m_glOverride = null;
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
      return m_gr;
    }

    /**
     * Returns the current graphic set (one of four).
     * 
     * @param index
     *          the index of the graphic set, 0..3.
     * @return a graphic set, never <code>null</code>.
     */
    public GraphicSet getGraphicSet( int index )
    {
      return m_graphicSets[index % 4];
    }

    /**
     * Returns the mapping for the given character.
     * 
     * @param ch
     *          the character to map.
     * @return the mapped character.
     */
    public char map( char ch )
    {
      return CharacterSets.getChar( ch, getGL(), getGR() );
    }

    /**
     * Overrides the GL graphic set for the next written character.
     * 
     * @param index
     *          the graphic set index, >= 0 && < 3.
     */
    public void overrideGL( int index )
    {
      m_glOverride = getGraphicSet( index );
    }

    /**
     * Resets the state to its initial values.
     */
    public void resetState()
    {
      for ( int i = 0; i < m_graphicSets.length; i++ )
      {
        m_graphicSets[i].setDesignation( CharacterSet.valueOf( ( i == 1 ) ? '0' : 'B' ) );
      }
      m_gl = m_graphicSets[0];
      m_gr = m_graphicSets[1];
      m_glOverride = null;
    }

    /**
     * Selects the graphic set for GL.
     * 
     * @param index
     *          the graphic set index, >= 0 && <= 3.
     */
    public void setGL( int index )
    {
      m_gl = getGraphicSet( index );
    }

    /**
     * Selects the graphic set for GR.
     * 
     * @param index
     *          the graphic set index, >= 0 && <= 3.
     */
    public void setGR( int index )
    {
      m_gr = getGraphicSet( index );
    }
  }

  /**
   * Denotes the various types of responses we can sent from this terminal.
   */
  static enum ResponseType
  {
    ESC, CSI, OSC, SS3;
  }

  /**
   * Contains the saved state of this terminal.
   */
  static class StateHolder
  {
    // VARIABLES

    private final CharacterSet[] m_graphicSetDesignations;

    private int m_cursorIndex;
    private short m_attrs;
    private boolean m_autoWrap;
    private boolean m_originMode;
    private int m_glIndex;
    private int m_grIndex;
    private int m_glOverrideIndex;

    // CONSTRUCTORS

    /**
     * Creates a new {@link StateHolder} instance.
     */
    public StateHolder()
    {
      m_graphicSetDesignations = new CharacterSet[4];
      m_cursorIndex = 0;
      m_autoWrap = true;
      m_originMode = false;
      m_glIndex = 0;
      m_grIndex = 1;
      m_glOverrideIndex = -1;
    }

    // METHODS

    public int restore( VT220Terminal terminal )
    {
      terminal.m_textAttributes.setAttributes( m_attrs );
      terminal.setAutoWrap( m_autoWrap );
      terminal.setOriginMode( m_originMode );

      GraphicSetState gss = terminal.m_graphicSetState;
      for ( int i = 0; i < gss.m_graphicSets.length; i++ )
      {
        gss.m_graphicSets[i].setDesignation( m_graphicSetDesignations[i] );
      }
      gss.setGL( m_glIndex );
      gss.setGR( m_grIndex );

      if ( m_glOverrideIndex >= 0 )
      {
        gss.overrideGL( m_glOverrideIndex );
      }

      return m_cursorIndex;
    }

    public void store( VT220Terminal terminal )
    {
      m_cursorIndex = terminal.getAbsoluteCursorIndex();
      m_attrs = terminal.m_textAttributes.getAttributes();
      m_autoWrap = terminal.isAutoWrapMode();
      m_originMode = terminal.isOriginMode();

      GraphicSetState gss = terminal.m_graphicSetState;
      m_glIndex = gss.m_gl.getIndex();
      m_grIndex = gss.m_gr.getIndex();

      m_glOverrideIndex = -1;
      if ( gss.m_glOverride != null )
      {
        m_glOverrideIndex = gss.m_glOverride.getIndex();
      }

      for ( int i = 0; i < gss.m_graphicSets.length; i++ )
      {
        m_graphicSetDesignations[i] = gss.m_graphicSets[i].getDesignation();
      }
    }
  }

  /**
   * Provides a VT220-compatible key mapper.
   */
  final class VT220KeyMapper implements IKeyMapper
  {
    // METHODS

    /**
     * {@inheritDoc}
     */
    @Override
    public String map( int keyCode, int modifiers )
    {
      switch ( keyCode )
      {
        case VK_UP:
          if ( isVT52mode() )
          {
            // Always ESC A
            return createResponse( ResponseType.ESC, "A" );
          }
          // CSI A in normal mode, SS3 A in application mode...
          return createResponse( isApplicationCursorKeys() ? ResponseType.SS3 : ResponseType.CSI, "A" );

        case VK_DOWN:
          if ( isVT52mode() )
          {
            // Always ESC B
            return createResponse( ResponseType.ESC, "B" );
          }
          // CSI B in normal mode, SS3 B in application mode...
          return createResponse( isApplicationCursorKeys() ? ResponseType.SS3 : ResponseType.CSI, "B" );

        case VK_RIGHT:
          if ( isVT52mode() )
          {
            // Always ESC C
            return createResponse( ResponseType.ESC, "C" );
          }
          // CSI C in normal mode, SS3 C in application mode...
          return createResponse( isApplicationCursorKeys() ? ResponseType.SS3 : ResponseType.CSI, "C" );

        case VK_LEFT:
          if ( isVT52mode() )
          {
            // Always ESC D
            return createResponse( ResponseType.ESC, "D" );
          }
          // CSI D in normal mode, SS3 D in application mode...
          return createResponse( isApplicationCursorKeys() ? ResponseType.SS3 : ResponseType.CSI, "D" );

        case VK_PAGE_DOWN:
          // Simulates NEXT SCREEN, CSI 6~
          return map( "\033[6~", null );

        case VK_PAGE_UP:
          // Simulates PREVIOUS SCREEN, CSI 5~
          return map( "\033[5~", null );

        case VK_HOME:
          // CSI H in normal mode, SS3 H in application mode...
          return map( "\033[H", "H" );

        case VK_END:
          // CSI F in normal mode, SS3 F in application mode...
          return map( "\033[F", "F" );

        case VK_NUMPAD0:
          return map( "0", "p", "0", "?p" );

        case VK_NUMPAD1:
          return map( "1", "q", "1", "?q" );

        case VK_NUMPAD2:
          return map( "2", "r", "2", "?r" );

        case VK_NUMPAD3:
          return map( "3", "s", "3", "?s" );

        case VK_NUMPAD4:
          return map( "3", "t", "4", "?t" );

        case VK_NUMPAD5:
          return map( "5", "u", "5", "?u" );

        case VK_NUMPAD6:
          return map( "6", "v", "6", "?v" );

        case VK_NUMPAD7:
          return map( "7", "w", "7", "?w" );

        case VK_NUMPAD8:
          return map( "8", "x", "8", "?x" );

        case VK_NUMPAD9:
          return map( "9", "y", "9", "?y" );

        case VK_MINUS:
          return map( "-", "m", "-", "?m" );

        case VK_COMMA:
          return map( ",", "l", ",", "?l" );

        case VK_PERIOD:
          return map( ".", "n", ".", "?n" );

        case VK_ENTER:
          return map( "\015", "M", "\015", "?M" );

        case VK_F1:
          if ( isVT52mode() )
          {
            return createResponse( ResponseType.ESC, "P" );
          }
          if ( ( modifiers & InputEvent.ALT_DOWN_MASK ) != 0 )
          {
            // Simulate PF1 with ALT-F1...
            return createResponse( ResponseType.SS3, "P" );
          }
          return createResponse( ResponseType.CSI, "11~" );

        case VK_F2:
          if ( isVT52mode() )
          {
            return createResponse( ResponseType.ESC, "Q" );
          }
          if ( ( modifiers & InputEvent.ALT_DOWN_MASK ) != 0 )
          {
            // Simulate PF2 with ALT-F2...
            return createResponse( ResponseType.SS3, "Q" );
          }
          return createResponse( ResponseType.CSI, "12~" );

        case VK_F3:
          if ( isVT52mode() )
          {
            return createResponse( ResponseType.ESC, "R" );
          }
          if ( ( modifiers & InputEvent.ALT_DOWN_MASK ) != 0 )
          {
            // Simulate PF3 with ALT-F3...
            return createResponse( ResponseType.SS3, "R" );
          }
          return createResponse( ResponseType.CSI, "13~" );

        case VK_F4:
          if ( isVT52mode() )
          {
            return createResponse( ResponseType.ESC, "S" );
          }
          if ( ( modifiers & InputEvent.ALT_DOWN_MASK ) != 0 )
          {
            // Simulate PF4 with ALT-F4...
            return createResponse( ResponseType.SS3, "S" );
          }
          return createResponse( ResponseType.CSI, "14~" );

        case VK_F5:
          // Seems not to be mapped at all according to
          // <http://www.vt100.net/docs/vt220-rm/table3-4.html>
          if ( isVT52mode() )
          {
            // Not mapped...
            return null;
          }
          return createResponse( ResponseType.CSI, "15~" );

        case VK_F6:
          if ( isVT52mode() )
          {
            // Not mapped...
            return null;
          }
          return createResponse( ResponseType.CSI, "17~" );

        case VK_F7:
          if ( isVT52mode() )
          {
            // Not mapped...
            return null;
          }
          return createResponse( ResponseType.CSI, "18~" );

        case VK_F8:
          if ( isVT52mode() )
          {
            // Not mapped...
            return null;
          }
          return createResponse( ResponseType.CSI, "19~" );

        case VK_F9:
          if ( isVT52mode() )
          {
            // Not mapped...
            return null;
          }
          return createResponse( ResponseType.CSI, "20~" );

        case VK_F10:
          if ( isVT52mode() )
          {
            // Not mapped...
            return null;
          }
          return createResponse( ResponseType.CSI, "21~" );

        case VK_F11:
          if ( isVT52mode() )
          {
            // Escape...
            return createResponse( ResponseType.ESC, "" );
          }
          return createResponse( ResponseType.CSI, "23~" );

        case VK_F12:
          if ( isVT52mode() )
          {
            // Backspace...
            return "\b";
          }
          return createResponse( ResponseType.CSI, "24~" );

        default:
          return null;
      }
    }

    private String map( String vt100normal, String vt100application )
    {
      return map( vt100normal, vt100application, null, null );
    }

    private String map( String vt100normal, String vt100application, String vt52normal, String vt52application )
    {
      if ( isVT52mode() )
      {
        // '0' is normal mode, ESC ? p in application mode...
        return isApplicationCursorKeys() ? createResponse( ResponseType.ESC, vt52application ) : vt52normal;
      }
      // '0' in normal mode, SS3 p in application mode...
      return isApplicationCursorKeys() ? createResponse( ResponseType.SS3, vt100application ) : vt100normal;
    }
  }

  // CONSTANTS

  private static final int OPTION_132COLS = 5;
  private static final int OPTION_ENABLE_132COLS = 6;
  private static final int OPTION_8BIT = 7;
  private static final int OPTION_ERASURE_MODE = 8;
  private static final int OPTION_REVERSE_WRAP_AROUND = 9;
  private static final int OPTION_APPLICATION_CURSOR_KEYS = 10;

  // VARIABLES

  private final GraphicSetState m_graphicSetState;
  private final VT220Parser m_vt220parser;
  private final StateHolder m_savedState;

  // CONSTRUCTORS

  /**
   * Creates a new {@link VT220Terminal} instance.
   * 
   * @param columns
   *          the initial number of columns in this terminal, > 0;
   * @param lines
   *          the initial number of lines in this terminal, > 0.
   */
  public VT220Terminal( final int columns, final int lines )
  {
    super( columns, lines );

    m_graphicSetState = new GraphicSetState();
    m_vt220parser = new VT220Parser();
    m_savedState = new StateHolder();

    // Make sure the terminal is in a known state...
    reset();

    m_vt220parser.setLogLevel( 1 );
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleCharacter( char ch ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();

    if ( isInsertMode() )
    {
      idx = insertChars( idx, m_graphicSetState.map( ch ), 1 ) + 1;
    }
    else
    {
      idx = writeChar( idx, m_graphicSetState.map( ch ) );
    }

    updateCursorByAbsoluteIndex( idx );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleControl( char controlChar ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();

    switch ( controlChar )
    {
      case ENQ:
      {
        // Answerback, always respond with the name of this class...
        write( getClass().getSimpleName() );
        break;
      }
      case SO:
      {
        // Shift-out (select G1 as GL)...
        m_graphicSetState.setGL( 1 );
        break;
      }

      case SI:
      {
        // Shift-in (select G0 as GL)...
        m_graphicSetState.setGL( 0 );
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
        log( "Unknown control character: " + ( int )controlChar );
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
  public void handleCSI( CSIType type, int... parameters ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();
    switch ( type )
    {
      case ICH: // @
      {
        // Insert N (blank) Character(s) (default = 1)
        int count = parameters[0];
        idx = insertChars( idx, ' ', count );
        break;
      }

      case SL: // [ ]@
      {
        // Scroll left N character(s) (default = 1)
        int count = parameters[0];
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
        int n = parameters[0];

        int col = idx % getWidth();
        int row = Math.max( getFirstScrollLine(), ( idx / getWidth() ) - n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case SR: // [ ]A
      {
        // Scroll right N character(s) (default = 1)
        int count = parameters[0];
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
        int n = parameters[0];

        int col = idx % getWidth();
        int row = Math.min( getLastScrollLine(), ( idx / getWidth() ) + n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CUF: // C
      {
        // Moves the cursor right N columns. The cursor stops at the right
        // margin.
        int n = parameters[0];

        int col = Math.min( getWidth() - 1, ( idx % getWidth() ) + n );
        int row = idx / getWidth();

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CUB: // D
      {
        // Moves the cursor left N columns. The cursor stops at the left margin.
        int n = parameters[0];

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
        int n = parameters[0];

        int col = 0;
        int row = Math.min( getLastScrollLine(), ( idx / getWidth() ) + n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CPL: // F
      {
        // Move cursor up the indicated # of rows, to column 1.
        int n = parameters[0];

        int col = 0;
        int row = Math.max( getFirstScrollLine(), ( idx / getWidth() ) - n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CHA: // G,`
      {
        // Character Position Absolute [column] (default = [row,1])
        // Cursor Character Absolute [column] (default = [row,1])
        int x = parameters[0];

        int col = Math.max( 0, Math.min( getWidth(), x - 1 ) );
        int row = Math.max( getFirstScrollLine(), Math.min( getLastScrollLine(), idx / getWidth() ) );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case CUP: // H,f
      {
        // Cursor Position [row;column] (default = [1,1])
        int row = parameters[0] - 1;
        int col = parameters[1] - 1;

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
        int count = parameters[0];
        while ( count-- > 0 )
        {
          idx += getTabulator().getNextTabWidth( idx % getWidth() );
        }
        break;
      }

      case ED: // J
      {
        // Erase in Display...
        int mode = parameters[0];

        clearScreen( mode, idx, isErasureMode() );
        break;
      }

      case DECSED: // ^J
      {
        // Erase in Display...
        int mode = parameters[0];

        clearScreen( mode, idx, true /* aKeepProtectedCells */);
        break;
      }

      case EL: // K
      {
        // Clear line...
        int mode = parameters[0];

        clearLine( mode, idx, isErasureMode() );
        break;
      }

      case DECSEL: // ^K
      {
        // Clear line...
        int mode = parameters[0];

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
        int lines = parameters[0];

        int row = idx / getWidth();
        if ( row >= getFirstScrollLine() && row < getLastScrollLine() )
        {
          int col = idx % getWidth();
          
          scrollDown( row, getLastScrollLine(), lines );

          idx -= col;
        }
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
        int lines = parameters[0];

        int row = idx / getWidth();
        if ( row >= getFirstScrollLine() && row < getLastScrollLine() )
        {
          int col = idx % getWidth();
          
          scrollUp( row, getLastScrollLine(), lines );

          idx -= col;
        }
        break;
      }

      case DCH: // P
      {
        // Delete N Character(s) (default = 1)
        int count = parameters[0];

        idx = deleteChars( idx, count );
        break;
      }

      case SU: // S
      {
        // Scroll N lines up...
        int lines = parameters[0];

        scrollUp( lines );
        break;
      }

      case SD: // T
      {
        // Scroll N lines down...
        int lines = parameters[0];

        scrollDown( lines );
        break;
      }

      case ECH: // X
      {
        // Erase N Character(s) (default = 1)
        int n = parameters[0] - 1;

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
        int count = parameters[0];
        while ( count-- > 0 )
        {
          idx -= getTabulator().getPreviousTabWidth( idx % getWidth() );
        }
        break;
      }

      case HPR: // a
      {
        // Move cursor right the indicated # of columns.
        int n = parameters[0];

        int w = getWidth();
        int col = Math.min( w - 1, ( idx % w ) + n );
        int row = idx / w;

        idx = getAbsoluteIndex( col, row );

        break;
      }

      case REP: // b
      {
        // Repeat the preceding graphic character N times
        int count = parameters[0];
        char ch = ( char )parameters[1];

        while ( count-- > 0 )
        {
          idx = writeChar( idx, ch );
        }
        break;
      }

      case PrimaryDA: // c
      {
        // Send Device Attributes (Primary DA)
        int arg = parameters[0];
        sendDeviceAttributes( arg );
        break;
      }

      case SecondaryDA: // >c
      {
        // Send Device Attributes (Secondary DA)
        int arg = parameters[0];
        if ( arg == 0 )
        {
          writeResponse( ResponseType.CSI, ">1;123;0c" );
        }
        break;
      }

      case VPA: // d
      {
        // (VPA) Line Position Absolute [row] (default = [1,column])
        int row = parameters[0] - 1;
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
        int n = parameters[0];

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
        int arg = parameters[0];

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
        int arg = parameters[0];
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
        int arg = parameters[0];
        switch ( arg )
        {
          case 1:
            // (DECCKM) Application cursor keys; default = false
            setApplicationCursorKeys( true );
            break;

          case 2:
            // (DECANM) Designate USASCII for character sets G0-G3; set VT100
            // mode.
            m_graphicSetState.resetState();
            set8bitMode( false );
            break;

          case 3:
            // (DECCOLM) switch 80/132 column mode; default = 80 columns.
            if ( isEnable132ColumnMode() )
            {
              set132ColumnMode( true );
              // Clear entire screen...
              clearScreen( 2 );
              // Reset scrolling region...
              setOriginMode( false );
              setScrollRegion( getFirstScrollLine(), getLastScrollLine() );
              // Reset cursor to first possible position...
              idx = getAbsoluteIndex( 0, getFirstScrollLine() );
            }
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

          case 8:
            // (DECARM) set auto-repeat mode; ignore.
            break;

          case 25:
            // (DECTCEM) Show Cursor; default = on.
            getCursor().setVisible( true );
            break;

          case 40:
            // Allow 80 -> 132 mode switching
            setEnable132ColumnMode( true );
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
        int n = parameters[0];

        int col = Math.max( 0, idx % getWidth() - n );
        int row = idx / getWidth();

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case VPB: // k
      {
        // Line position backward N lines (default = 1).
        int n = parameters[0];

        int w = getWidth();
        int col = idx % w;
        int row = Math.max( getFirstScrollLine(), ( idx / w ) - n );

        idx = getAbsoluteIndex( col, row );
        break;
      }

      case RM: // l
      {
        // Reset mode
        int arg = parameters[0];
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
        int arg = parameters[0];
        switch ( arg )
        {
          case 1:
            // (DECCKM) Normal cursor keys; default = false
            setApplicationCursorKeys( false );
            break;

          case 2:
            // (DECANM) Set VT52 mode.
            m_graphicSetState.resetState();
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

          case 8:
            // (DECARM) disable auto-repeat mode; ignore.
            break;

          case 25:
            // (DECTCEM) Hide Cursor; default = on.
            getCursor().setVisible( false );
            break;

          case 40:
            // Disallow 80 -> 132 mode switching
            setEnable132ColumnMode( false );
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
        handleGraphicsRendering( parameters );
        break;
      }

      case DSR: // n
      {
        int arg = parameters[0];
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
        int arg = parameters[0];
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
        int compatibilityLevel = ( parameters[0] - 60 );
        int eightBitMode = ( parameters.length > 1 ) ? parameters[1] : 0;
        setConformanceLevel( compatibilityLevel, eightBitMode != 1 );
        break;
      }

      case DECSCA: // "q
      {
        // Select character protection attribute
        int mode = parameters[0];
        // 0 or 2 == erasable; 1 == not erasable...
        m_textAttributes.setProtected( mode == 1 );
        break;
      }

      case DECSTBM: // r
      {
        // Set Scrolling Region [top;bottom] (default = full size of window)
        int top = parameters[0];
        int bottom = parameters[1];
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
        int param = parameters[0];
        switch ( param )
        {
          case 4:
            // resize to height;width in pixels
            if ( parameters.length == 3 )
            {
              int height = parameters[1];
              int width = parameters[2];

              setDimensionsInPixels( width, height );
              idx = getFirstAbsoluteIndex();
            }
            break;

          case 8:
            if ( parameters.length == 3 )
            {
              // resize to height;width in chars
              int rows = parameters[1];
              int cols = parameters[2];

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

            writeResponse( ResponseType.CSI, String.format( "%d;%d;%dt", ( param - 10 ), height, width ) );
            break;

          case 18:
          case 19:
            // report dimensions in characters
            writeResponse( ResponseType.CSI, String.format( "%d;%d;%dt", ( param - 10 ), getHeight(), getWidth() ) );
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
            if ( param >= 24 )
            {
              // resize to N lines...
              int cols = getWidth();
              int rows = param;

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
        int arg = parameters[0];

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
        log( "Unhandled CSI: " + type );
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
  public void handleESC( char designator, int... parameters ) throws IOException
  {
    int idx = getAbsoluteCursorIndex();

    switch ( designator )
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
        m_graphicSetState.setGL( 2 );
        break;
      }
      case 'o': // LS3
      {
        // Invoke the G3 Character Set as GL
        m_graphicSetState.setGL( 3 );
        break;
      }
      case '|': // LS3R
      {
        // Invoke the G3 Character Set as GR
        m_graphicSetState.setGR( 3 );
        break;
      }
      case '}': // LS2R
      {
        // Invoke the G2 Character Set as GR
        m_graphicSetState.setGR( 2 );
        break;
      }
      case '~': // LS1R
      {
        // Invoke the G1 Character Set as GR
        m_graphicSetState.setGR( 1 );
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
        char arg = ( char )parameters[0];
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
        char arg = ( char )parameters[0];
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
        GraphicSet gs = m_graphicSetState.getGraphicSet( designator - '(' );
        m_graphicSetState.designateGraphicSet( gs, ( char )parameters[0] );
        break;
      }
      case '=':
      {
        // (DECPAM) Application Keypad
        setApplicationCursorKeys( true );
        break;
      }
      case '>':
      {
        // (DECPNM) Normal Keypad
        setApplicationCursorKeys( false );
        break;
      }

      default:
      {
        log( "Unhandled ESC designator: " + designator );
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
   * @param width
   *          the new width of this terminal in columns, if zero or negative,
   *          the maximum possible number of columns will be used;
   * @param height
   *          the new height of this terminal in lines, if zero or negative, the
   *          maximum possible number of lines will be used.
   */
  @Override
  public void setDimensions( int width, int height )
  {
    if ( width <= 0 || height <= 0 )
    {
      Dimension maximumSize = null;
      if ( getFrontend() != null )
      {
        maximumSize = getFrontend().getMaximumTerminalSize();
      }
      if ( width <= 0 )
      {
        width = ( maximumSize != null ) ? maximumSize.width : getWidth();
      }
      if ( height <= 0 )
      {
        height = ( maximumSize != null ) ? maximumSize.height : getHeight();
      }
    }
    super.setDimensions( width, height );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLogLevel( int aLogLevel )
  {
    super.setLogLevel( aLogLevel );
    m_vt220parser.setLogLevel( aLogLevel );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected IKeyMapper createKeyMapper()
  {
    return new VT220KeyMapper();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int doReadInput( final CharSequence text ) throws IOException
  {
    return m_vt220parser.parse( text, this );
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
    return m_options.get( OPTION_132COLS );
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
    return m_options.get( OPTION_8BIT );
  }

  /**
   * Returns whether or not the cursor keys map to application-specific keys, or
   * to normal keys.
   * 
   * @return <code>true</code> if application cursor keys are to be used,
   *         <code>false</code> if normal cursor keys are to be used.
   * @see #setApplicationCursorKeys(boolean)
   */
  protected final boolean isApplicationCursorKeys()
  {
    return m_options.get( OPTION_APPLICATION_CURSOR_KEYS );
  }

  /**
   * Returns whether or not it is allows to switch from 80 to 132 columns.
   * 
   * @return <code>true</code> if switching from 80 to 132 column mode is
   *         allowed, <code>false</code> otherwise.
   * @see #setEnable132ColumnMode(boolean)
   */
  protected final boolean isEnable132ColumnMode()
  {
    return m_options.get( OPTION_ENABLE_132COLS );
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
    return m_options.get( OPTION_ERASURE_MODE );
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
    return m_options.get( OPTION_REVERSE_WRAP_AROUND );
  }

  /**
   * Returns whether or not this terminal is emulating a VT52.
   * 
   * @return <code>true</code> if the terminal is currently emulating a VT52,
   *         <code>false</code> if it is emulating a VT100/VT220.
   */
  protected final boolean isVT52mode()
  {
    return m_vt220parser.isVT52mode();
  }

  /**
   * Enables or disables the auto-wrap mode.
   * 
   * @param enable
   *          <code>true</code> to enable auto-wrap mode, <code>false</code> to
   *          disable it.
   */
  protected final void set132ColumnMode( boolean enable )
  {
    m_options.set( OPTION_132COLS, enable );

    if ( enable )
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
   * @param enable
   *          <code>true</code> to send responses in 8-bit mode,
   *          <code>false</code> to send responses in 7-bit mode.
   */
  protected final void set8bitMode( boolean enable )
  {
    m_options.set( OPTION_8BIT, enable );
  }

  /**
   * Sets whether or not the cursor keys map to application-specific keys, or to
   * normal keys.
   * 
   * @param enable
   *          <code>true</code> to enable application cursor keys,
   *          <code>false</code> to enable normal cursor keys.
   */
  protected final void setApplicationCursorKeys( boolean enable )
  {
    m_options.set( OPTION_APPLICATION_CURSOR_KEYS, enable );
  }

  /**
   * Sets whether or not the switching from 80 to 132 columns is allowed.
   * 
   * @param enable
   *          <code>true</code> to allow switching from 80 to 132 columns,
   *          <code>false</code> to disallow this switching.
   */
  protected final void setEnable132ColumnMode( boolean enable )
  {
    m_options.set( OPTION_ENABLE_132COLS, enable );
  }

  /**
   * Sets whether or not protected content is to be erased.
   * 
   * @param enable
   *          <code>true</code> to allow only unprotected content to be erased,
   *          <code>false</code> to allow both protected and unprotected content
   *          to be erased.
   */
  protected final void setErasureMode( boolean enable )
  {
    m_options.set( OPTION_ERASURE_MODE, enable );
  }

  /**
   * Sets whether or not reverse wrap arounds are supported, meaning that
   * hitting backspace or issuing cursor-back commands after a wrap-around has
   * taken place will revert this wrap-around.
   * 
   * @param enable
   *          <code>true</code> to enable reverse wrap around,
   *          <code>false</code> to disable it.
   */
  protected final void setReverseWrapAround( boolean enable )
  {
    m_options.set( OPTION_REVERSE_WRAP_AROUND, enable );
  }

  /**
   * Creates a 7- or 8-bit response according to a given type.
   * 
   * @param type
   *          the type of response;
   * @param content
   *          the actual content.
   * @return the 7- or 8-bit response, never <code>null</code>.
   */
  private String createResponse( ResponseType type, String content )
  {
    StringBuilder sb = new StringBuilder();
    switch ( type )
    {
      case ESC:
        sb.append( "\033" );
        break;

      case OSC:
        sb.append( is8bitMode() ? VT220Parser.OSC : "\033]" );
        break;

      case CSI:
        sb.append( is8bitMode() ? VT220Parser.CSI : "\033[" );
        break;

      case SS3:
        sb.append( is8bitMode() ? VT220Parser.SS3 : "\033O" );
        break;

      default:
        throw new RuntimeException( "Unhandled response type: " + type );
    }
    sb.append( content );
    return sb.toString();
  }

  /**
   * End of protected area.
   */
  private void handleEPA()
  {
    m_textAttributes.setProtected( false );
  }

  /**
   * Converts the graphics rendering options in the form of a given parameter
   * stack into recognized text attributes.
   * 
   * @param parameters
   *          the parameters to handle as graphics rendering options.
   */
  private void handleGraphicsRendering( int[] parameters )
  {
    boolean containsHiddenAttr = false;
    for ( int p : parameters )
    {
      if ( p == 8 )
      {
        containsHiddenAttr = true;
        break;
      }
    }

    if ( m_textAttributes.isHidden() && !containsHiddenAttr )
    {
      // It appears that after an invisible attribute, *both* reverse as
      // invisible are to be reset...
      m_textAttributes.setHidden( false );
      m_textAttributes.setReverse( false );
    }

    for ( int p : parameters )
    {
      handleGraphicsRendering( m_textAttributes, p );
    }
  }

  /**
   * Handles the given graphics parameter.
   * 
   * @param attributes
   *          the text attributes to update;
   * @param parameter
   *          the graphics parameter to handle.
   */
  private void handleGraphicsRendering( final TextAttributes attributes, final int parameter )
  {
    if ( parameter == 0 || parameter == 39 || parameter == 49 )
    {
      if ( parameter == 0 )
      {
        attributes.resetAll();
      }
      else
      {
        attributes.reset();
      }
    }

    if ( parameter == 1 )
    {
      attributes.setBold( true );
    }
    else if ( parameter == 4 )
    {
      attributes.setUnderline( true );
    }
    else if ( parameter == 5 )
    {
      // Blink on...
      attributes.setItalic( true );
    }
    else if ( parameter == 7 )
    {
      // Negative/reverse...
      attributes.setReverse( true );
    }
    else if ( parameter == 8 )
    {
      // Invisible (hidden)...
      attributes.setHidden( true );
    }
    else if ( parameter == 21 || parameter == 22 )
    {
      attributes.setBold( false );
    }
    else if ( parameter == 24 )
    {
      attributes.setUnderline( false );
    }
    else if ( parameter == 25 )
    {
      // Blink off...
      attributes.setItalic( false );
    }
    else if ( parameter == 27 )
    {
      // Reset negative...
      attributes.setReverse( false );
    }
    else if ( parameter == 28 )
    {
      // Reset invisible (shown)...
      attributes.setHidden( false );
    }
    else if ( ( parameter >= 30 ) && ( parameter <= 37 ) )
    {
      // Handle foreground color...
      attributes.setForeground( parameter - 29 );
    }
    else if ( parameter == 39 )
    {
      // Default foreground color...
      attributes.setForeground( 0 );
    }
    else if ( ( parameter >= 40 ) && ( parameter <= 47 ) )
    {
      attributes.setBackground( parameter - 39 );
    }
    else if ( parameter == 49 )
    {
      // Default background color...
      attributes.setBackground( 0 );
    }
    else if ( parameter > 0 )
    {
      log( "Unhandled attribute: " + parameter );
    }
  }

  /**
   * IND moves the cursor down one line in the same column. If the cursor is at
   * the bottom margin, the screen performs a scroll-up.
   * 
   * @param aTermState
   */
  private int handleIND( final int index )
  {
    int col = index % getWidth();
    int row = ( index / getWidth() ) + 1;

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
  private int handleNEL( int index )
  {
    int col = 0;
    int row = ( index / getWidth() ) + 1;

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
  private int handleRI( int index )
  {
    int col = index % getWidth();
    int row = ( index / getWidth() ) - 1;

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
    m_textAttributes.setProtected( true );
  }

  /**
   * Single Shift Select of G2 Character Set. Affects next character only.
   */
  private void handleSS2()
  {
    m_graphicSetState.overrideGL( 2 );
  }

  /**
   * Single Shift Select of G3 Character Set. Affects next character only.
   */
  private void handleSS3()
  {
    m_graphicSetState.overrideGL( 3 );
  }

  /**
   * @param aTermState
   */
  private void handleTabSet( int index )
  {
    getTabulator().set( ( index % getWidth() ) );
  }

  /**
   * (DECRC) Restores the previously stored cursor position.
   */
  private int restoreCursor()
  {
    return m_savedState.restore( this );
  }

  /**
   * (DECSC) Stores the current cursor position.
   */
  private void saveCursor( int aIndex )
  {
    m_savedState.store( this );
  }

  /**
   * Send Device Attributes (Primary DA).
   */
  private void sendDeviceAttributes( int arg ) throws IOException
  {
    if ( arg != 0 )
    {
      log( "Unknown DA: " + arg );
    }
    else
    {
      if ( isVT52mode() )
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
   * @param level
   *          &lt;= 1 equals to VT100 mode, &gt;= 2 equals to VT200 mode;
   * @param eightBitMode
   *          <code>true</code> if 8-bit controls are preferred (only in VT200
   *          mode), <code>false</code> if 7-bit controls are preferred.
   */
  private void setConformanceLevel( int level, boolean eightBitMode )
  {
    if ( level <= 1 )
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
    else if ( level >= 2 )
    {
      // @formatter:off
      /* VT200 mode:
       * - keyboard permits full use of VT220 keyboard;
       * - 7 or 8-bit mode: the 8th bit of all received characters is significant;
       * - all VT220 character sets are available.
       */
      // @formatter:on
      softReset();
      set8bitMode( eightBitMode );
    }
  }

  /**
   * Sets the dimensions of the terminal frontend, in pixels.
   * 
   * @param width
   *          the new width, in pixels;
   * @param height
   *          the new height, in pixels.
   */
  private void setDimensionsInPixels( int width, int height )
  {
    ITerminalFrontend frontend = getFrontend();
    if ( frontend != null )
    {
      frontend.setSize( width, height );
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
    setApplicationCursorKeys( false );
    // Sets the numeric keypad mode to numeric (DECNKM)

    set8bitMode( false );
    set132ColumnMode( false );
    setEnable132ColumnMode( true );
    setReverse( false );
    // Sets selective erase mode off (DECSCA)
    setErasureMode( true );

    // Sets all character sets (GL, G0, G1, G2 and G3) to ASCII
    m_graphicSetState.resetState();
    // Turns off all character attributes (SGR)
    m_textAttributes.resetAll();

    // Clears any cursor state information saved with save cursor (DECSC)
    saveCursor( getFirstAbsoluteIndex() );
  }

  /**
   * Writes a response according to a given type.
   * 
   * @param type
   *          the type of response to write;
   * @param content
   *          the actual content to write.
   * @throws IOException
   *           in case of I/O problems writing the response.
   */
  private void writeResponse( ResponseType type, String content ) throws IOException
  {
    write( createResponse( type, content ) );
  }
}
