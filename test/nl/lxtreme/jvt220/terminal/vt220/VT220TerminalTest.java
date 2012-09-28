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

  /**
   * Tests that the auto-wrap mode is handled correctly in combination with the
   * backward movement of the cursor.
   */
  public void testAutoWrapMoveCursorBackOk() throws IOException
  {
    this.terminal.reset();
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
    this.terminal.readInput( "\033[2J\033[2;1H*\033[2;80H*\033[10D\015\012*" );

    assertEquals( '*', this.terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', this.terminal.getCellAt( 0, 2 ).getChar() );
  }

  /**
   * Tests that the auto-wrap mode is handled correctly in various
   * circumstances.
   */
  public void testAutoWrapOk() throws IOException
  {
    this.terminal.readInput( "\033[3;21r\033[?6h" );

    for ( int i = 0; i < 26; i++ )
    {
      char rightChar = ( char )( i + 'a' );
      char leftChar = ( char )( i + 'A' );

      switch ( i % 4 )
      {
        case 0:
          /* draw characters as-is, for reference */
          this.terminal.readInput( "\033[19;1H" + leftChar );
          this.terminal.readInput( "\033[19;80H" + rightChar );
          this.terminal.readInput( "\015\012" );
          break;
        case 1:
          /* simple wrapping */
          this.terminal.readInput( "\033[18;80H" + ( char )( rightChar - 1 ) + leftChar );
          this.terminal.readInput( "\033[19;80H" + leftChar + "\010 " + rightChar );
          this.terminal.readInput( "\015\012" );
          break;
        case 2:
          /* tab to right margin */
          this.terminal.readInput( "\033[19;80H" + leftChar + "\010\010\011\011" + rightChar );
          this.terminal.readInput( "\033[19;2H\010" + leftChar );
          this.terminal.readInput( "\015\012" );
          break;
        default:
          /* newline at right margin */
          this.terminal.readInput( "\033[19;80H\015\012" );
          this.terminal.readInput( "\033[18;1H" + leftChar );
          this.terminal.readInput( "\033[18;80H" + rightChar );
          break;
      }
    }

    // At column zero we should have I..Z, and at the last column, we should
    // have i..z;
    for ( int row = 2; row < 20; row++ )
    {
      char left = ( char )( 'G' + row );
      char right = ( char )( 'g' + row );

      assertEquals( left, this.terminal.getCellAt( 0, row ).getChar() );
      assertEquals( right, this.terminal.getCellAt( 79, row ).getChar() );
    }
  }

  /**
   * Tests that the scroll down function works correctly.
   */
  public void testScrollDownOk() throws IOException
  {
    int max = this.terminal.getLastScrollLine();
    int last = max - 3;

    for ( int n = 1; n < last; n++ )
    {
      this.terminal.readInput( String.format( "\033[%1$d;%1$dH", n ) );
      this.terminal.readInput( "*" );
      this.terminal.readInput( "\033[1T" );
    }
    this.terminal.readInput( String.format( "\033[%d;1H", last + 1 ) );
    this.terminal.readInput( "----+----1----+----2----+----3----+----4----+----5----+----6----+----7----+----8\015\012" );

    for ( int n = 1; n < last; n++ )
    {
      assertEquals( '*', this.terminal.getCellAt( n - 1, last - 1 ).getChar() );
    }
  }

  /**
   * Tests that the movement of the cursor is bound to the terminal dimensions.
   */
  public void testMoveCursorBoundToTerminalDimensionsOk() throws IOException
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
