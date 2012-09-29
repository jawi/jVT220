package nl.lxtreme.jvt220.terminal.vt220;


import java.io.*;

import junit.framework.*;
import nl.lxtreme.jvt220.terminal.ITerminal.ITextCell;


/**
 * Test cases for {@link AbstractTerminal}.
 */
public class PlainTerminalTest extends TestCase
{
  // METHODS

  public void testClearEnitreScreenOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 2, 1 );

    term.clearScreen( 2 );
    assertEquals( "               ", getTermText( term ) );

    assertEquals( 2, term.getCursor().getX() );
    assertEquals( 1, term.getCursor().getY() );
  }

  public void testClearFirstLineOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 0, 0 );

    term.clearLine( 2 );
    assertEquals( "     1234512345", getTermText( term ) );

    assertEquals( 0, term.getCursor().getX() );
    assertEquals( 0, term.getCursor().getY() );
  }

  public void testClearLinePartiallyOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 1 );

    term.clearLine( 1 );
    assertEquals( "12345  34512345", getTermText( term ) );

    assertEquals( 1, term.getCursor().getX() );
    assertEquals( 1, term.getCursor().getY() );

    term.clearLine( 0 );
    assertEquals( "12345     12345", getTermText( term ) );

    assertEquals( 1, term.getCursor().getX() );
    assertEquals( 1, term.getCursor().getY() );
  }

  public void testClearScreenAboveOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 2, 1 );

    term.clearScreen( 1 );
    assertEquals( "        4512345", getTermText( term ) );

    assertEquals( 2, term.getCursor().getX() );
    assertEquals( 1, term.getCursor().getY() );
  }

  public void testClearScreenBelowOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 2, 1 );

    term.clearScreen( 0 );
    assertEquals( "1234512        ", getTermText( term ) );

    assertEquals( 2, term.getCursor().getX() );
    assertEquals( 1, term.getCursor().getY() );
  }

  public void testClearSecondLineOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 2, 1 );

    term.clearLine( 2 );
    assertEquals( "12345     12345", getTermText( term ) );

    assertEquals( 2, term.getCursor().getX() );
    assertEquals( 1, term.getCursor().getY() );
  }

  public void testClearThirdLineOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 2 );

    term.clearLine( 2 );
    assertEquals( "1234512345     ", getTermText( term ) );

    assertEquals( 1, term.getCursor().getX() );
    assertEquals( 2, term.getCursor().getY() );
  }

  public void testScrollDownOneLineOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 2 );

    term.scrollDown( 1 );
    assertEquals( "     1234512345", getTermText( term ) );
  }

  public void testScrollDownThreeLinesOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 2 );

    term.scrollDown( 3 );
    assertEquals( "               ", getTermText( term ) );
  }

  public void testScrollDownTwoLinesOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 2 );

    term.scrollDown( 2 );
    assertEquals( "          12345", getTermText( term ) );
  }

  public void testScrollDownWithScrollRegionOk() throws IOException
  {
    AbstractTerminal term = createTerminal( 5, 5 );
    term.setAutoWrap( false );
    term.read( "11111\r\n22222\r\n33333\r\n44444\r\n55555" );
    term.setScrollRegion( 1, 3 ); // fixate first and last line...
    term.setOriginMode( true ); // make sure origin is retained...

    term.scrollDown( 2 );
    assertEquals( "11111          2222255555", getTermText( term ) );

    term.setScrollRegion( 0, 5 ); // default...

    term.scrollDown( 1 );
    assertEquals( "     11111          22222", getTermText( term ) );
  }

  public void testScrollUpOneLineOk() throws IOException
  {
    AbstractTerminal term = createTerminal( 5, 5 );
    term.setAutoWrap( false );
    term.read( "11111\r\n22222\r\n33333\r\n44444\r\n55555" );
    term.moveCursorAbsolute( 1, 2 );

    term.scrollUp( 1 );
    assertEquals( "22222333334444455555     ", getTermText( term ) );
  }

  public void testScrollUpThreeLinesOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 2 );

    term.scrollUp( 3 );
    assertEquals( "               ", getTermText( term ) );
  }

  public void testScrollUpTwoLinesOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.moveCursorAbsolute( 1, 2 );

    term.scrollUp( 2 );
    assertEquals( "12345          ", getTermText( term ) );
  }

  public void testScrollUpWithScrollRegionOk() throws IOException
  {
    AbstractTerminal term = createTerminal( 5, 5 );
    term.setAutoWrap( false );
    term.read( "11111\r\n22222\r\n33333\r\n44444\r\n55555" );
    term.setScrollRegion( 1, 3 ); // fixate first and last line...
    term.setOriginMode( true ); // make sure origin is retained...

    term.scrollUp( 2 );
    assertEquals( "1111144444          55555", getTermText( term ) );

    term.setScrollRegion( 0, 5 ); // default...

    term.scrollUp( 1 );
    assertEquals( "44444          55555     ", getTermText( term ) );
  }

  public void testSetDimensionsEqualOk() throws IOException {
    AbstractTerminal term = createTerminal( "11111\r\n22222\r\n33333" );
    
    assertEquals( "111112222233333", getTermText( term ) );
    
    term.setDimensions( 5, 3 );
    
    assertEquals( "111112222233333", getTermText( term ) );
  }

  public void testSetDimensionsLargerOk() throws IOException {
    AbstractTerminal term = createTerminal( "11111\r\n22222\r\n33333" );
    
    assertEquals( "111112222233333", getTermText( term ) );
    
    term.setDimensions( 6, 3 );
    
    assertEquals( "11111 22222 33333 ", getTermText( term ) );
    
    term.setDimensions( 6, 4 );
    
    assertEquals( "11111 22222 33333       ", getTermText( term ) );
  }

  public void testSetDimensionsSmallerOk() throws IOException {
    AbstractTerminal term = createTerminal( "11111\r\n22222\r\n33333" );
    
    assertEquals( "111112222233333", getTermText( term ) );
    
    term.setDimensions( 4, 3 );
    
    assertEquals( "111122223333", getTermText( term ) );
    
    term.setDimensions( 4, 2 );
    
    assertEquals( "11112222", getTermText( term ) );
  }

  public void testWriteBackspacesAtLastPositionDoesNotScrollUpOk() throws IOException
  {
    AbstractTerminal term = createTerminal( "abcde\r\nfghij\r\nklmno" );
    term.setAutoWrap( false );
    term.moveCursorAbsolute( 5, 3 );
    assertEquals( "abcdefghijklmno", getTermText( term ) );

    term.read( "\b\b" );
    assertEquals( "abcdefghijklm  ", getTermText( term ) );

    term.read( "op" );
    assertEquals( "abcdefghijklmop", getTermText( term ) );
  }

  public void testWriteTextAtLastPositionScrollUpOk() throws IOException
  {
    AbstractTerminal term = createTerminal( "abcde\r\nfghij\r\nklmno" );
    term.moveCursorAbsolute( 5, 3 );
    assertEquals( "abcdefghijklmno", getTermText( term ) );

    term.read( "pq" );
    assertEquals( "fghijklmnopq   ", getTermText( term ) );
  }

  public void testWriteTextHandlesNewlinesOk() throws IOException
  {
    AbstractTerminal term = createTerminal();
    term.clearScreen( 2 );
    term.moveCursorAbsolute( 0, 0 );
    term.read( "ab\ncd\r\nef" );
    assertEquals( "ab     cd ef   ", getTermText( term ) );
  }

  /**
   * @return a new {@link AbstractTerminal} instance, never <code>null</code>.
   */
  private PlainTerminal createTerminal() throws IOException
  {
    return createTerminal( "12345\r\n12345\r\n12345" );
  }

  /**
   * @param aColumns
   * @param aLines
   * @return
   */
  private PlainTerminal createTerminal( int aColumns, int aLines )
  {
    return new PlainTerminal( aColumns, aLines );
  }

  /**
   * @return a new {@link AbstractTerminal} instance, never <code>null</code>.
   */
  private PlainTerminal createTerminal( String aText ) throws IOException
  {
    PlainTerminal term = createTerminal( 5, 3 );
    term.setAutoWrap( false );
    term.read( aText );
    term.setAutoWrap( true );
    return term;
  }

  /**
   * @param aTerm
   * @return
   */
  private String getTermText( final AbstractTerminal aTerm )
  {
    StringBuilder sb = new StringBuilder();
    for ( int idx = aTerm.getFirstAbsoluteIndex(); idx <= aTerm.getLastAbsoluteIndex(); idx++ )
    {
      ITextCell cell = aTerm.getCellAt( idx );
      sb.append( cell == null ? ' ' : cell.getChar() );
    }
    return sb.toString();
  }
}
