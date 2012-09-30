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

  private VT220Terminal m_terminal;
  private ByteArrayOutputStream m_buffer;

  // METHODS

  /**
   * Tests that the insert mode works.
   */
  public void testInsertModeOk() throws IOException
  {
    m_terminal.read( "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" );
    m_terminal.read( "\033[2;1H\033[0J\033[1;2H" );
    m_terminal.read( "B" );
    m_terminal.read( "\033[1D\033[4h" );
    m_terminal.read( "******************************************************************************" );
    m_terminal.read( "\033[4l\033[4;1H" );

    assertEquals( 'A', m_terminal.getCellAt( 0, 0 ).getChar() );
    assertEquals( 'B', m_terminal.getCellAt( 79, 0 ).getChar() );
  }
  

  /**
   * Tests that the auto-wrap mode is handled correctly in combination with the
   * backward movement of the cursor.
   */
  public void testAutoWrapMoveCursorBackOk() throws IOException
  {
    // Cursor back after a wrap-around means we're going back onto the previous
    // line...
    m_terminal.read( "\033[2J\033[2;1H*\033[2;80H*\033[10D\033E*" );

    assertEquals( '*', m_terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', m_terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', m_terminal.getCellAt( 0, 2 ).getChar() );

    // If a character is written after a wrap-around, cursor back won't go back
    // onto previous line...
    m_terminal.read( "\033[2J\033[2;1H*\033[2;80H**\033[10D\033E*" );

    assertEquals( '*', m_terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', m_terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', m_terminal.getCellAt( 0, 3 ).getChar() );

    // Cursor back after a wrap-around means we're going back onto the previous
    // line...
    m_terminal.read( "\033[2J\033[2;1H*\033[2;80H*\033[10D\015\012*" );

    assertEquals( '*', m_terminal.getCellAt( 0, 1 ).getChar() );
    assertEquals( '*', m_terminal.getCellAt( 79, 1 ).getChar() );
    assertEquals( '*', m_terminal.getCellAt( 0, 2 ).getChar() );
  }

  /**
   * Tests that the auto-wrap mode is handled correctly in various
   * circumstances.
   */
  public void testAutoWrapOk() throws IOException
  {
    m_terminal.read( "\033[3;21r\033[?6h" );

    for ( int i = 0; i < 26; i++ )
    {
      char rightChar = ( char )( i + 'a' );
      char leftChar = ( char )( i + 'A' );

      switch ( i % 4 )
      {
        case 0:
          /* draw characters as-is, for reference */
          m_terminal.read( "\033[19;1H" + leftChar );
          m_terminal.read( "\033[19;80H" + rightChar );
          m_terminal.read( "\015\012" );
          break;
        case 1:
          /* simple wrapping */
          m_terminal.read( "\033[18;80H" + ( char )( rightChar - 1 ) + leftChar );
          m_terminal.read( "\033[19;80H" + leftChar + "\010 " + rightChar );
          m_terminal.read( "\015\012" );
          break;
        case 2:
          /* tab to right margin */
          m_terminal.read( "\033[19;80H" + leftChar + "\010\010\011\011" + rightChar );
          m_terminal.read( "\033[19;2H\010" + leftChar );
          m_terminal.read( "\015\012" );
          break;
        default:
          /* newline at right margin */
          m_terminal.read( "\033[19;80H\015\012" );
          m_terminal.read( "\033[18;1H" + leftChar );
          m_terminal.read( "\033[18;80H" + rightChar );
          break;
      }
    }

    // At column zero we should have I..Z, and at the last column, we should
    // have i..z;
    for ( int row = 2; row < 20; row++ )
    {
      char left = ( char )( 'G' + row );
      char right = ( char )( 'g' + row );

      assertEquals( left, m_terminal.getCellAt( 0, row ).getChar() );
      assertEquals( right, m_terminal.getCellAt( 79, row ).getChar() );
    }
  }

  /**
   * Tests that the scroll down function works correctly.
   */
  public void testScrollDownOk() throws IOException
  {
    int max = m_terminal.getLastScrollLine();
    int last = max - 3;

    for ( int n = 1; n < last; n++ )
    {
      m_terminal.read( String.format( "\033[%1$d;%1$dH", n ) );
      m_terminal.read( "*" );
      m_terminal.read( "\033[1T" );
    }

    for ( int n = 1; n < last; n++ )
    {
      assertEquals( '*', m_terminal.getCellAt( n - 1, last - 1 ).getChar() );
    }
  }

  /**
   * Tests that the movement of the cursor is bound to the terminal dimensions.
   */
  public void testMoveCursorBoundToTerminalDimensionsOk() throws IOException
  {
    m_terminal.read( "\033[999;999H" );
    m_terminal.read( "\033[6n" );

    String response = m_buffer.toString();
    assertEquals( "\033[24;80R", response );
  }

  /**
   * Set up for this test case.
   */
  protected void setUp() throws Exception
  {
    m_buffer = new ByteArrayOutputStream();
    m_terminal = new VT220Terminal( 80, 24 )
    {
      @Override
      public int write( CharSequence aResponse ) throws IOException
      {
        for ( int i = 0; i < aResponse.length(); i++ )
        {
          m_buffer.write( aResponse.charAt( i ) );
        }
        return aResponse.length();
      }
    };
  }
}
