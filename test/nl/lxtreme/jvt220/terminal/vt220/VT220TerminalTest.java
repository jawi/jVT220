/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import java.io.*;

import junit.framework.*;


/**
 * Test cases for {@link VT220Terminal}.
 */
public class VT220TerminalTest extends TestCase
{
  // VARIABLES

  private VT220Terminal terminal;
  private ByteArrayOutputStream buffer;

  // METHODS

  public void testAutoWrapMoveCursorBackOk() throws IOException
  {
    this.terminal.setAutoWrap( true );
    // Cursor back after a wrap-around means we're going back onto the previous
    // line...
    this.terminal.readInput( "\033[2J\033[2;1H*\033[2;80H*\033[10D\033E*" );

    assertEquals( '*', this.terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 0, 2 ).getChar() );

    // If a character is written after a wrap-around, cursor back won't go back
    // onto previous line...
    this.terminal.readInput( "\033[2J\033[2;1H*\033[2;80H**\033[10D\033E*" );

    assertEquals( '*', this.terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 0, 3 ).getChar() );

    // Cursor back after a wrap-around means we're going back onto the previous
    // line...
    this.terminal.readInput( "\033[2J\033[2;1H*\033[2;80H*\033[10D\012*" );

    assertEquals( '*', this.terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 0, 2 ).getChar() );
  }

  public void testAutoWrapOk() throws IOException
  {
    StringBuilder sb = new StringBuilder();
    sb.append( "\033[?3l" ).append( "\033[?3l" );
    sb.append( "Test of autowrap, mixing control and print characters.\015\012" );
    sb.append( "The left/right margins should have letters in order:\015\012" );
    sb.append( "\033[3;29r" );
    sb.append( "\033[?6h" );

    for ( int i = 0; i < 26; i++ )
    {
      char rightChar = ( char )( i + 'a' );
      char leftChar = ( char )( i + 'A' );

      switch ( i % 4 )
      {
        case 0:
          /* draw characters as-is, for reference */
          sb.append( "\033[19;1H" ).append( leftChar );
          sb.append( "\033[19;80H" ).append( rightChar );
          sb.append( "\012" );
          break;
        case 1:
          /* simple wrapping */
          sb.append( "\033[18;80H" ).append( ( char )( rightChar - 1 ) ).append( leftChar );
          sb.append( "\033[19;80H" ).append( leftChar ).append( "\010 " ).append( rightChar );
          sb.append( "\012" );
          break;
        case 2:
          /* tab to right margin */
          sb.append( "\033[19;80H" ).append( leftChar ).append( "\010\010" ).append( "\011\011" ).append( rightChar );
          sb.append( "\033[19;2H" ).append( "\010" ).append( leftChar ).append( "\012" );
          break;
        default:
          /* newline at right margin */
          sb.append( "\033[19;80H" ).append( "\012" );
          sb.append( "\033[18;1H" ).append( leftChar );
          sb.append( "\033[18;80H" ).append( rightChar );
          break;
      }
    }

    sb.append( "\033[?6l" );
    sb.append( "\033[r" );
    sb.append( "\033[22;1H" );
    sb.append( "Push <RETURN>" );

    this.terminal.readInput( sb );

    System.out.println( this.terminal );
  }

  public void testMoveCursorOk() throws IOException
  {
    this.terminal.readInput( "\033[999;999H" );
    this.terminal.readInput( "\033[6n" );

    String response = this.buffer.toString();
    assertEquals( "\033[24;80R", response );
  }

  /**
   * Set up for this test case.
   */
  protected void setUp() throws Exception
  {
    this.buffer = new ByteArrayOutputStream();
    this.terminal = new VT220Terminal( this.buffer, 80, 24 );
  }
}
