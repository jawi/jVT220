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
package nl.lxtreme.jvt220.terminal.vt220;


import java.io.*;
import java.util.*;


/**
 * Parses VT220-compatible text sequences.
 */
public final class VT220Parser
{
  // INNER TYPES

  /**
   * The various CSIs that are recognized by this parser.
   */
  public static enum CSIType
  {
    ICH, CUU, CUD, CUF, CUB, CNL, CPL, CHA, CUP, CHT, ED, DECSED, EL, DECSEL, IL, DL, DCH, SU, SD, ECH, CBT, //
    HPR, REP, PrimaryDA, SecondaryDA, VPA, VPR, TBC, SM, DECSET, MC, DECSMC, HPB, VPB, RM, DECRST, SGR, DSR, //
    DECSDSR, DECSTR, DECSCL, DECSCA, DECSTBM, RestoreDECPM, DECCARA, SaveDECPM, DECRARA, DECCRA, DECEFR, //
    DECREQTPARM, DECFRA, DECELR, DECERA, DECSLE, DECSERA, DECRQLP, SL, SR, WindowManipulation;
  }

  /**
   * Callback for parser events.
   */
  public static interface VT220ParserHandler
  {
    // METHODS

    /**
     * Called when a plain (non C0/C1) character is found.
     * 
     * @param ch
     *          the found character, >= 32.
     * @throws IOException
     *           in case of I/O problems handling the given character.
     */
    void handleCharacter( char ch ) throws IOException;

    /**
     * Called when a C0 control character is found.
     * 
     * @param controlChar
     *          the C0 control character, >= 0 && < 32.
     * @throws IOException
     *           in case of I/O problems handling the given control character.
     */
    void handleControl( char controlChar ) throws IOException;

    /**
     * Called when a complete CSI is found.
     * 
     * @param type
     *          the type of CSI that was found;
     * @param parameters
     *          the (optional) list of parameters for this CSI.
     * @throws IOException
     *           in case of I/O problems handling the given CSI.
     */
    void handleCSI( CSIType type, int... parameters ) throws IOException;

    /**
     * Called when a non-CSI escape is found.
     * 
     * @param designator
     *          the designator of the escape sequence;
     * @param parameters
     *          the (optional) parameters for the escape sequence.
     * @throws IOException
     *           in case of I/O problems handling the given escape sequence.
     */
    void handleESC( char designator, int... parameters ) throws IOException;
  }

  /**
   * Denotes the various states in which the VT220 parser can be.
   */
  private static enum ParserState
  {
    VT100, VT52, ESC, CSI, OSC, DCS, APC, PM, SOS;
  }

  // CONSTANTS

  public static final char ENQ = 0x05;
  public static final char BELL = 0x07;
  public static final char BS = 0x08;
  public static final char TAB = 0x09;
  public static final char LF = 0x0A;
  public static final char VT = 0x0B;
  public static final char FF = 0x0C;
  public static final char CR = 0x0D;
  public static final char SO = 0x0E;
  public static final char SI = 0x0F;

  public static final char CAN = 0x18;

  public static final char ESCAPE = 0x1b;

  public static final char SPACE = 0x20;

  public static final char IND = 0x84;
  public static final char NEL = 0x85;
  public static final char HTS = 0x88;
  public static final char RI = 0x8d;
  public static final char SS2 = 0x8e;
  public static final char SS3 = 0x8f;
  public static final char DCS = 0x90;
  public static final char SPA = 0x96;
  public static final char EPA = 0x97;
  public static final char SOS = 0x98;
  public static final char DECID = 0x9a;
  public static final char CSI = 0x9b;
  public static final char ST = 0x9c;
  public static final char OSC = 0x9d;
  public static final char PM = 0x9e;
  public static final char APC = 0x9f;

  // VARIABLES

  private final Stack<Object> m_parameters;

  private int m_logLevel;
  private CharSequence m_text;
  private int m_i;
  private int m_lastParsePos;
  private int m_lastWrittenChar;
  private ParserState m_state;
  private boolean m_vt52mode;
  private char m_designator;

  // CONSTRUCTORS

  /**
   * Creates a new {@link VT220Parser} instance.
   */
  public VT220Parser()
  {
    m_parameters = new Stack<Object>();
    m_vt52mode = false;
    m_logLevel = 0;
  }

  // METHODS

  /**
   * Returns whether this parser is currently in VT52 mode.
   * 
   * @return <code>true</code> if this parser is in VT52 compatibility mode,
   *         <code>false</code> if this parser is in VT100 mode.
   */
  public boolean isVT52mode()
  {
    return m_vt52mode;
  }

  /**
   * Parses the contained text and invokes the callback methods on the given
   * handler.
   * 
   * @param text
   *          the text (as {@link CharSequence}) to parse, cannot be
   *          <code>null</code>;
   * @param handler
   *          the handler to use as callback, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems parsing the input.
   */
  public int parse( CharSequence text, VT220ParserHandler handler ) throws IOException
  {
    m_text = text;
    m_i = 0;
    m_lastParsePos = 0;
    m_parameters.clear();
    m_state = m_vt52mode ? ParserState.VT52 : ParserState.VT100;
    m_lastWrittenChar = -1;

    int c;
    while ( ( c = nextChar() ) != -1 )
    {
      switch ( m_state )
      {
        case DCS:
        case OSC:
        case PM:
        case APC:
        case SOS:
          switch ( c )
          {
            case ESCAPE:
            {
              // Escape; check whether the next character is a '\'...
              if ( la() == '\\' )
              {
                eat( 1 );
                escSequenceFound();
              }
              break;
            }

            case ST:
            {
              // 8-bit sequence...
              m_state = m_vt52mode ? ParserState.VT52 : ParserState.VT100;
              eightBitSequenceFound();
              break;
            }

            default:
              // Ignore all others...
              eat( 0 ); // does update last parse position...
              break;
          }
          break;

        case CSI:
          switch ( c )
          {
            case '@':
            {
              if ( lb() == SPACE )
              {
                // (SL) Scroll Left N Character(s) (default = 1)
                int count = getIntegerParameter( 1 );
                handler.handleCSI( CSIType.SL, count );
              }
              else
              {
                // (ICH) Insert N (Blank) Character(s) (default = 1)
                int count = getIntegerParameter( 1 );
                handler.handleCSI( CSIType.ICH, count );
              }
              csiFound();
              break;
            }

            case 'A':
            {
              if ( lb() == SPACE )
              {
                // (SR) Scroll Right N Character(s) (default = 1)
                int count = getIntegerParameter( 1 );
                handler.handleCSI( CSIType.SR, count );
              }
              else
              {
                // (CUU) Moves the cursor up N lines in the same column. The
                // cursor stops at the top margin.
                int n = getIntegerParameter( 1 );
                handler.handleCSI( CSIType.CUU, n );
              }
              csiFound();
              break;
            }

            case 'B':
            {
              // (CUD) Moves the cursor down N lines in the same column. The
              // cursor stops at the bottom margin.
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CUD, n );
              csiFound();
              break;
            }

            case 'C':
            {
              // (CUF) Moves the cursor right N columns. The cursor stops at
              // the right margin.
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CUF, n );
              csiFound();
              break;
            }

            case 'D':
            {
              // (CUB) Moves the cursor left N columns. The cursor stops at the
              // left margin.
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CUB, n );
              csiFound();
              break;
            }

            case 'E':
            {
              // (CNL) Move cursor down the indicated # of rows, to column 1.
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CNL, n );
              csiFound();
              break;
            }

            case 'F':
            {
              // (CPL) Move cursor up the indicated # of rows, to column 1.
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CPL, n );
              csiFound();
              break;
            }

            case '`':
            case 'G':
            {
              // (HPA) Character Position Absolute [column] (default = [row,1])
              // (CHA) Cursor Character Absolute [column] (default = [row,1])
              int x = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CHA, x );
              csiFound();
              break;
            }

            case 'f':
            case 'H':
            {
              // (CUP) Move cursor to [row, column]...
              int row = getIntegerParameter( 1 );
              int col = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CUP, row, col );
              csiFound();
              break;
            }

            case 'I':
            {
              // (CHT) Cursor Forward Tabulation P s tab stops (default = 1)
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CHT, n );
              csiFound();
              break;
            }

            case 'J':
            {
              // (ED/DECSED) Erase in Display...
              int mode = getIntegerParameter( 0 );
              if ( isDecSpecific() )
              {
                handler.handleCSI( CSIType.DECSED, mode );
              }
              else
              {
                handler.handleCSI( CSIType.ED, mode );
              }
              csiFound();
              break;
            }

            case 'K':
            {
              // (EL/DECSEL) Clear line...
              int mode = getIntegerParameter( 0 );
              if ( isDecSpecific() )
              {
                handler.handleCSI( CSIType.DECSEL, mode );
              }
              else
              {
                handler.handleCSI( CSIType.EL, mode );
              }
              csiFound();
              break;
            }

            case 'L':
            {
              // (IL) Inserts N lines at the cursor.
              int lines = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.IL, lines );
              csiFound();
              break;
            }

            case 'M':
            {
              // (DL) Deletes N lines starting at the line with the cursor.
              int lines = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.DL, lines );
              csiFound();
              break;
            }

            case 'P':
            {
              // (DCH) Delete N Character(s) (default = 1)
              int count = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.DCH, count );
              csiFound();
              break;
            }

            case 'S':
            {
              // (SU) Scroll N lines up...
              int lines = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.SU, lines );
              csiFound();
              break;
            }

            case 'T':
            {
              // (SD) Scroll N lines down...
              int lines = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.SD, lines );
              csiFound();
              break;
            }

            case 'X':
            {
              // (ECH) Erase N Character(s) (default = 1)
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.ECH, n );
              csiFound();
              break;
            }

            case 'Z':
            {
              // (CBT) Cursor Backward Tabulation N tab stops (default = 1)
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.CBT, n );
              csiFound();
              break;
            }

            // case '`' (HPA) is handled with case 'G'!

            case 'a':
            {
              // (HPR) Move cursor right the indicated # of columns.
              int count = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.HPR, count );
              csiFound();
              break;
            }

            case 'b':
            {
              // (REP) Repeat the last written character N times
              if ( m_lastWrittenChar >= 0 )
              {
                int count = getIntegerParameter( 1 );

                handler.handleCSI( CSIType.REP, count, m_lastWrittenChar );
              }
              csiFound();
              break;
            }

            case 'c':
            {
              // Send Device Attributes (Primary/secondary DA)
              int option = getIntegerParameter( 0 );
              if ( m_designator == '>' )
              {
                handler.handleCSI( CSIType.SecondaryDA, option );
              }
              else
              {
                handler.handleCSI( CSIType.PrimaryDA, option );
              }
              csiFound();
              break;
            }

            case 'd':
            {
              // (VPA) Line Position Absolute [row] (default = [1,column])
              int row = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.VPA, row );
              csiFound();
              break;
            }

            case 'e':
            {
              // (VPR) Move cursor down the indicated N of columns (default =
              // 1).
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.VPR, n );
              csiFound();
              break;
            }

            // case 'f' (HVP) is handled with case 'H'!

            case 'g':
            {
              // (TBC) Tab clear
              int arg = getIntegerParameter( 0 );
              handler.handleCSI( CSIType.TBC, arg );
              csiFound();
              break;
            }

            case 'h':
            {
              // (SM/DECSET) Set mode / DEC Private Mode Set ...
              int arg = getIntegerParameter( 0 );
              if ( isDecSpecific() )
              {
                handler.handleCSI( CSIType.DECSET, arg );
              }
              else
              {
                handler.handleCSI( CSIType.SM, arg );
              }
              csiFound();
              break;
            }

            case 'i':
            {
              // (MC/DECSMC) Media Copy
              int arg = getIntegerParameter( 0 );
              if ( isDecSpecific() )
              {
                handler.handleCSI( CSIType.DECSMC, arg );
              }
              else
              {
                handler.handleCSI( CSIType.MC, arg );
              }
              csiFound();
              break;
            }

            case 'j':
            {
              // (HPB) Character position backward N positions (default = 1)
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.HPB, n );
              csiFound();
              break;
            }

            case 'k':
            {
              // (VPB) Line position backward N lines (default = 1)
              int n = getIntegerParameter( 1 );
              handler.handleCSI( CSIType.VPB, n );
              csiFound();
              break;
            }

            case 'l':
            {
              // (RM/DECRST) Reset mode / DEC Private Mode Reset...
              int arg = getIntegerParameter( 0 );
              if ( isDecSpecific() )
              {
                handler.handleCSI( CSIType.DECRST, arg );
                if ( arg == 2 )
                {
                  // (DECANM) Set VT52 mode...
                  m_state = ParserState.VT52;
                  m_vt52mode = true;
                }
              }
              else
              {
                handler.handleCSI( CSIType.RM, arg );
              }
              csiFound();
              break;
            }

            case 'm':
            {
              // (SGR) Turn on/off character attributes ...
              int[] args = getIntegerParameters( 0 );
              handler.handleCSI( CSIType.SGR, args );
              csiFound();
              break;
            }

            case 'n':
            {
              // (DSR) Device Status Report
              int arg = getIntegerParameter( 0 );
              if ( isDecSpecific() )
              {
                handler.handleCSI( CSIType.DECSDSR, arg );
              }
              else
              {
                handler.handleCSI( CSIType.DSR, arg );
              }
              csiFound();
              break;
            }

            case 'p':
            {
              if ( lb() == '!' )
              {
                // (DECSTR) Soft terminal reset
                handler.handleCSI( CSIType.DECSTR );
              }
              else if ( lb() == '"' )
              {
                // (DECSCL) Set conformance level
                int arg1 = getIntegerParameter( 0 );
                int arg2 = getIntegerParameter( 0 );
                handler.handleCSI( CSIType.DECSCL, arg1, arg2 );
              }
              csiFound();
              break;
            }

            case 'q':
            {
              if ( lb() == '"' )
              {
                // (DECSCA) Select character protection attribute
                int arg = getIntegerParameter( 0 );
                handler.handleCSI( CSIType.DECSCA, arg );
              }
              csiFound();
              break;
            }

            case 'r':
            {
              if ( lb() == '$' )
              {
                // (DECCARA) Change Attributes in Rectangular Area
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECCARA, args );
              }
              else if ( isDecSpecific() )
              {
                // Restore DEC Private Mode Values. The value of N1..Nn
                // previously saved is restored. N1..Nn values are the same as
                // for DECSET.
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.RestoreDECPM, args );
              }
              else
              {
                // (DECSTBM) Set Scrolling Region [top;bottom] (default = full
                // size of window)
                int top = getIntegerParameter( 1 );
                int bottom = getIntegerParameter( 0 );
                handler.handleCSI( CSIType.DECSTBM, top, bottom );
              }
              csiFound();
              break;
            }

            case 's':
            {
              // Save DEC Private Mode Values. N1..Nn values are the same as for
              // DECSET.
              if ( isDecSpecific() )
              {
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.SaveDECPM, args );
              }
              csiFound();
              break;
            }

            case 't':
            {
              if ( lb() == '$' )
              {
                // (DECRARA) Reverse Attributes in Rectangular Area
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECRARA, args );
              }
              else
              {
                // dtterm window manipulation; ignored...
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.WindowManipulation, args );
              }
              csiFound();
              break;
            }

            case 'v':
            {
              if ( lb() == '$' )
              {
                // (DECCRA) Copy Rectangular Area
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECCRA, args );
              }
              csiFound();
              break;
            }

            case 'w':
            {
              if ( lb() == '\'' )
              {
                // (DECEFR) Enable Filter Rectangle
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECEFR, args );
              }
              csiFound();
              break;
            }

            case 'x':
            {
              if ( lb() == '$' )
              {
                // (DECFRA) Fill Rectangular Area
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECFRA, args );
              }
              else
              {
                // (DECREQTPARM) Request Terminal Parameters
                int arg = getIntegerParameter( 0 );
                handler.handleCSI( CSIType.DECREQTPARM, arg );
              }
              csiFound();
              break;
            }

            case 'z':
            {
              if ( lb() == '\'' )
              {
                // (DECELR) Enable Locator Reporting
                int arg1 = getIntegerParameter( 0 );
                int arg2 = getIntegerParameter( 0 );
                handler.handleCSI( CSIType.DECELR, arg1, arg2 );
              }
              else if ( lb() == '$' )
              {
                // (DECERA) Erase Rectangular Area
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECERA, args );
              }
              csiFound();
              break;
            }

            case '{':
            {
              if ( lb() == '\'' )
              {
                // (DECSLE) Select Locator Events
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECSLE, args );
              }
              else if ( lb() == '$' )
              {
                // (DECSERA) Selective Erase Rectangular Area
                int[] args = getIntegerParameters();
                handler.handleCSI( CSIType.DECSERA, args );
              }
              csiFound();
              break;
            }

            case '|':
            {
              if ( lb() == '\'' )
              {
                // (DECRQLP) Request Locator Position
                int arg = getIntegerParameter( 0 );
                handler.handleCSI( CSIType.DECRQLP, arg );
              }
              csiFound();
              break;
            }

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            {
              int value = c - '0';
              while ( Character.isDigit( ( char )la() ) )
              {
                c = nextChar();
                value = ( value * 10 ) + ( c - '0' );
              }
              pushParameter( value );
              break;
            }

            case ';':
              // Param separator...
              if ( !hasParameters() )
              {
                pushParameter( 0 );
              }
              break;

            case '?':
            case '=':
            case '>':
            case ' ':
            case '\'':
            case '"':
            case '!':
            case '$':
            {
              // Additional selectors; used in some CSIs...
              m_designator = ( char )c;
              break;
            }

            default:
              // Unknown; handle as C0 control character or plain text...
              if ( c < SPACE )
              {
                handler.handleControl( ( char )c );
              }
              else
              {
                m_lastWrittenChar = c;
                handler.handleCharacter( ( char )c );
              }
              break;
          }
          break;

        case ESC:
        {
          if ( m_vt52mode )
          {
            // Handle VT52 sequences...
            switch ( c )
            {
              case CAN:
                // Cancel current operation...
                m_state = ParserState.VT52;
                break;

              case 'A':
                // Cursor UP
                handler.handleCSI( CSIType.CUU, 1 );
                escSequenceFound();
                break;

              case 'B':
                // Cursor down
                handler.handleCSI( CSIType.CUD, 1 );
                escSequenceFound();
                break;

              case 'C':
                // Cursor right
                handler.handleCSI( CSIType.CUF, 1 );
                escSequenceFound();
                break;

              case 'D':
                // Cursor left
                handler.handleCSI( CSIType.CUB, 1 );
                escSequenceFound();
                break;

              case 'F':
                // Enter graphics mode TODO
                escSequenceFound();
                break;

              case 'G':
                // Leave graphics mode TODO
                escSequenceFound();
                break;

              case 'H':
                // Move cursor to the home position
                handler.handleCSI( CSIType.CUP, 1, 1 );
                escSequenceFound();
                break;

              case 'I':
                // Reverse line feed / Reverse index
                handler.handleESC( 'M' );
                escSequenceFound();
                break;

              case 'J':
                // Erase from cursor to end of screen
                handler.handleCSI( CSIType.ED, 0 );
                escSequenceFound();
                break;

              case 'K':
                // Erase from cursor to end of line
                handler.handleCSI( CSIType.EL, 0 );
                escSequenceFound();
                break;

              case 'Y':
                // Move cursor to row;column
                if ( la() != CAN )
                {
                  int row = Math.max( 0, nextChar() - 32 );
                  int col;
                  if ( la() != CAN )
                  {
                    col = Math.max( 0, nextChar() - 32 );
                    handler.handleCSI( CSIType.CUP, row + 1, col + 1 );
                    escSequenceFound();
                  }
                  else
                  {
                    // Move to row only...
                    handler.handleCSI( CSIType.CUP, row + 1, 1 );
                    escSequenceFound();
                  }
                }
                else
                {
                  // Cancel...
                  m_state = ParserState.VT52;
                }
                break;

              case 'Z':
                // Identify
                handler.handleESC( 'Z' );
                escSequenceFound();
                break;

              case '=':
                // Enter alternate keypad mode TODO
                escSequenceFound();
                break;

              case '>':
                // Leave alternate keypad mode TODO
                escSequenceFound();
                break;

              case '<':
                // Exit VT52 mode
                m_state = ParserState.VT100;
                m_vt52mode = false;
                escSequenceFound();
                break;

              default:
                // Not recognized; assume the sequence is finished...
                escSequenceFound();
                break;
            }
          }
          else
          {
            // Handle VT100 sequences...
            switch ( c )
            {
              case '[':
                // 7-bit sequence...
                m_state = ParserState.CSI;
                m_parameters.clear();
                break;

              case '_':
                // 7-bit sequence...
                m_state = ParserState.APC;
                break;

              case '\\':
                // 7-bit sequence. Ignored in this state...
                break;

              case ']':
                // 7-bit sequence...
                m_state = ParserState.OSC;
                break;

              case '^':
                // 7-bit sequence...
                m_state = ParserState.PM;
                break;

              case 'P':
                // 7-bit sequence...
                m_state = ParserState.DCS;
                break;

              case 'X':
                // 7-bit sequence...
                m_state = ParserState.SOS;
                break;

              case 'D': // IND
              case 'E': // NEL
              case 'H': // TabSet
              case 'M': // RI
              case 'N': // SS2
              case 'O': // SS3
              case 'V': // SPA
              case 'W': // EPA
              case 'Z': // DecID
              case '=': // DECPAM
              case '>': // DECPNM
              case 'c': // RIS
              case 'n': // LS2
              case 'o': // LS3
              case '|': // LS3R
              case '}': // LS2R
              case '~': // LS1R
                handler.handleESC( ( char )c );
                escSequenceFound();
                break;

              case ' ':
              {
                int la = la();
                if ( la == 'F' || la == 'G' )
                {
                  // 7- or 8-bit responses...
                  handler.handleESC( ( char )c, la );
                }
                else if ( la == 'L' || la == 'M' || la == 'N' )
                {
                  // set ANSI conformance level...
                  handler.handleESC( ( char )c, la );
                }
                eat( 1 ); // eat the LA...
                escSequenceFound();
                break;
              }

              case '#':
              {
                int la = la();
                if ( la == '3' || la == '4' || la == '5' || la == '6' )
                {
                  // DEC line height settings; currently ignored...
                }
                else if ( la == '8' )
                {
                  // DEC Screen Alignment Test (DECALN)
                  handler.handleESC( ( char )c, la );
                }
                eat( 1 ); // eat the LA...
                escSequenceFound();
                break;
              }

              case '(':
              case ')':
              case '*':
              case '+':
              {
                // Designate G0/G1/G2 or G3 Character Set
                // Ignore any additional designators...
                while ( la() >= ' ' && la() <= '/' )
                {
                  eat( 1 );
                }
                int f = nextChar();
                handler.handleESC( ( char )c, f );
                escSequenceFound();
                break;
              }

              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
              {
                // Ignore optional digits after an escape sequence...
                int value = c - '0';
                while ( Character.isDigit( ( char )la() ) )
                {
                  c = nextChar();
                  value = ( value * 10 ) + ( c - '0' );
                }

                // Handle DECSC & DECRC...
                if ( ( value == 7 || value == 8 ) && lb() == ESCAPE )
                {
                  handler.handleESC( ( char )c );
                  escSequenceFound();
                }
                break;
              }

              default:
                // Not recognized; assume the sequence is finished...
                escSequenceFound();
                break;
            }
          }
          break;
        }

        case VT100:
          switch ( c )
          {
            case ESCAPE:
            {
              m_state = ParserState.ESC;
              break;
            }

            case CSI:
            {
              // 8-bit character...
              m_state = ParserState.CSI;
              m_parameters.clear();
              eightBitSequenceFound();
              break;
            }

            case DCS:
            {
              // 8-bit character...
              m_state = ParserState.DCS;
              eightBitSequenceFound();
              break;
            }

            case ST:
            {
              // 8-bit character. Ignored in this state...
              eightBitSequenceFound();
              break;
            }

            case OSC:
            {
              // 8-bit character...
              m_state = ParserState.OSC;
              eightBitSequenceFound();
              break;
            }

            case PM:
            {
              // 8-bit character...
              m_state = ParserState.PM;
              eightBitSequenceFound();
              break;
            }

            case APC:
            {
              // 8-bit character...
              m_state = ParserState.APC;
              eightBitSequenceFound();
              break;
            }

            case IND: // ESC D
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'D' );
              eightBitSequenceFound();
              break;
            }

            case NEL: // ESC E
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'E' );
              eightBitSequenceFound();
              break;
            }

            case HTS: // ESC H
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'H' );
              eightBitSequenceFound();
              break;
            }

            case RI: // ESC M
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'M' );
              eightBitSequenceFound();
              break;
            }

            case SS2: // ESC N
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'N' );
              eightBitSequenceFound();
              break;
            }

            case SS3: // ESC O
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'O' );
              eightBitSequenceFound();
              break;
            }

            case SPA: // ESC V
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'V' );
              eightBitSequenceFound();
              break;
            }

            case EPA: // ESC W
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'W' );
              eightBitSequenceFound();
              break;
            }

            case SOS: // ESC X
            {
              // 8-bit character...
              m_state = ParserState.SOS;
              eightBitSequenceFound();
              break;
            }

            case DECID: // ESC Z
            {
              // Translate to 7-bit sequence...
              handler.handleESC( 'Z' );
              eightBitSequenceFound();
              break;
            }

            default:
            {
              // Check whether it is a single character function (= everything
              // below ' ')...
              if ( c < SPACE )
              {
                m_lastWrittenChar = -1;
                handler.handleControl( ( char )c );
              }
              else
              {
                m_lastWrittenChar = c;
                handler.handleCharacter( ( char )c );
              }
              eat( 0 ); // update last parsing position...
              break;
            }
          }
          break;

        case VT52:
          switch ( c )
          {
            case ESCAPE:
            {
              m_state = ParserState.ESC;
              break;
            }

            default:
            {
              // Check whether it is a single character function (= everything
              // below ' ')...
              if ( c < SPACE )
              {
                m_lastWrittenChar = -1;
                handler.handleControl( ( char )c );
              }
              else
              {
                m_lastWrittenChar = c;
                handler.handleCharacter( ( char )c );
              }
              eat( 0 ); // update last parsing position...
              break;
            }
          }
          break;

        default:
          throw new RuntimeException( "Unknown/unhandled state: " + m_state );
      }
    }

    return m_lastParsePos;
  }

  /**
   * @param logLevel
   *          the log level to set, >= 0.
   */
  public void setLogLevel( int logLevel )
  {
    m_logLevel = logLevel;
  }

  /**
   * Called when a complete CSI sequence is found, resets the current state to
   * VT100, clears all remaining parameters and updates the parsing position.
   * 
   * @param aText
   *          the text we're currently parsing;
   * @param aIndex
   *          the index of the current parse position.
   * @param aTermState
   *          the current state of the parser;
   */
  private void csiFound()
  {
    log( m_text, m_lastParsePos, m_i );

    m_state = ParserState.VT100;
    m_parameters.clear();
    m_lastWrittenChar = -1;
    m_designator = 0;
    m_lastParsePos = Math.min( m_text.length(), m_i );
  }

  /**
   * "Eats" (ignores) the next number of characters and updates the parsing
   * position.
   * 
   * @param count
   *          the number of characters to eat, > 0.
   */
  private void eat( int count )
  {
    log( m_text, m_lastParsePos, m_i + count );

    m_i += count;
    m_lastParsePos = Math.min( m_text.length(), m_i );
  }

  /**
   * Called when a 8-bit control sequence is found and updates the parsing
   * position.
   */
  private void eightBitSequenceFound()
  {
    log( m_text, m_lastParsePos, m_i );

    m_lastWrittenChar = -1;
    m_designator = 0;
    m_lastParsePos = Math.min( m_text.length(), m_i );
  }

  /**
   * Called when a escape sequence is found and updates the parsing position.
   */
  private void escSequenceFound()
  {
    log( m_text, m_lastParsePos, m_i );

    if ( m_vt52mode )
    {
      m_state = ParserState.VT52;
    }
    else
    {
      m_state = ParserState.VT100;
    }
    m_lastWrittenChar = -1;
    m_designator = 0;
    m_lastParsePos = Math.min( m_text.length(), m_i );
  }

  /**
   * Returns the bottom of the parameter stack as integer value, falling back to
   * the given default if either the parameter stack is empty, the value is not
   * an integer or less than the given default value.
   * <p>
   * In case the bottom of the stack is a non-integer value, this value will be
   * lost and the default value will be returned!
   * </p>
   * 
   * @param defaultValue
   *          the default (minimal) value of the parameter.
   * @return an integer value, >= the given default value.
   */
  private int getIntegerParameter( final int defaultValue )
  {
    int result = Integer.MIN_VALUE;
    if ( hasParameters() )
    {
      Object param = m_parameters.remove( 0 );
      if ( param instanceof Integer )
      {
        result = ( ( Integer )param ).intValue();
      }
    }
    return Math.max( defaultValue, result );
  }

  /**
   * Returns all integer parameters as array and clears the current parameter
   * stack.
   * 
   * @return an array of integer values, never <code>null</code>.
   */
  private int[] getIntegerParameters( int... defaultValues )
  {
    int count = 0;
    for ( Object param : m_parameters )
    {
      if ( param instanceof Integer )
      {
        count++;
      }
    }

    int[] result;
    if ( count == 0 )
    {
      result = Arrays.copyOf( defaultValues, defaultValues.length );
    }
    else
    {
      result = new int[count];
      int idx = 0;
      while ( hasParameters() )
      {
        Object param = m_parameters.remove( 0 );
        if ( param instanceof Integer )
        {
          result[idx++] = ( ( Integer )param ).intValue();
        }
      }
    }

    return result;
  }

  /**
   * @return <code>true</code> if the parameter stack is not empty,
   *         <code>false</code> otherwise.
   */
  private boolean hasParameters()
  {
    return !m_parameters.isEmpty();
  }

  /**
   * Returns whether or not the CSI is DEC specific, which means that after the
   * CSI a '?' is found.
   * 
   * @return <code>true</code> if the CSI is DEC specific, <code>false</code>
   *         otherwise.
   */
  private boolean isDecSpecific()
  {
    return m_designator == '?';
  }

  /**
   * Returns the first lookahead character.
   * 
   * @return the next character (as int) without changing the current index. Can
   *         be -1 if no next character is available (end of string).
   */
  private int la()
  {
    if ( m_i >= m_text.length() )
    {
      return -1;
    }
    return m_text.charAt( m_i );
  }

  /**
   * Returns the first lookbehind character.
   * 
   * @return the previous character (as int) without changing the current index.
   *         Can be -1 if no previous character is available (beginning of
   *         string).
   */
  private int lb()
  {
    if ( m_i < 2 )
    {
      return -1;
    }
    return m_text.charAt( m_i - 2 );
  }

  /**
   * Logs a part of the given character sequence.
   * 
   * @param text
   *          the character sequence to log, cannot be <code>null</code>;
   * @param start
   *          the start position (inclusive) of the text to log;
   * @param end
   *          the end position (inclusive) of the text to log.
   */
  private void log( CharSequence text, int start, int end )
  {
    if ( m_logLevel < 1 )
    {
      return;
    }

    int length = text.length();
    if ( start >= length )
    {
      return;
    }
    end = Math.min( end, length );

    StringBuilder sb = new StringBuilder( "LOG> " );
    for ( int i = start; i < end; i++ )
    {
      char c = text.charAt( i );
      if ( c >= ' ' && c <= '~' )
      {
        sb.append( c );
      }
      else
      {
        sb.append( "<" ).append( ( int )c ).append( ">" );
      }
    }
    System.out.println( sb.toString() );
  }

  /**
   * Returns the next character.
   * 
   * @return the next character (as int) after which the current index is
   *         incremented. Can be -1 if no next character is available (end of
   *         string).
   */
  private int nextChar()
  {
    if ( m_i >= m_text.length() )
    {
      return -1;
    }
    return m_text.charAt( m_i++ );
  }

  /**
   * Pushes the given value onto the parameter stack.
   * 
   * @param value
   *          the value to push.
   */
  private void pushParameter( final int value )
  {
    m_parameters.push( Integer.valueOf( value ) );
  }
}
