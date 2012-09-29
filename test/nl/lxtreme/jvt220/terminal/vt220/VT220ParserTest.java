/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;

import java.io.*;
import java.util.*;

import junit.framework.*;
import nl.lxtreme.jvt220.terminal.vt220.VT220Parser.CSIType;
import nl.lxtreme.jvt220.terminal.vt220.VT220Parser.VT220ParserHandler;


/**
 * Test cases for {@link VT220Parser}.
 */
public class VT220ParserTest extends TestCase
{
  // INNER TYPES

  static class VT220ParserTestAdapter implements VT220ParserHandler
  {
    @Override
    public void handleCharacter( char aChar ) throws IOException
    {
      fail( String.format( "Character (%c) seen?!", aChar ) );
    }

    @Override
    public void handleControl( char aControlChar ) throws IOException
    {
      fail( String.format( "Control (%d) seen?!", ( int )aControlChar ) );
    }

    @Override
    public void handleCSI( CSIType aType, int... aParameters ) throws IOException
    {
      fail( String.format( "CSI (%s %s) seen?!", aType, Arrays.toString( aParameters ) ) );
    }

    @Override
    public void handleESC( char aDesignator, int... aParameters ) throws IOException
    {
      fail( String.format( "ESC %c %s seen?!", aDesignator, Arrays.toString( aParameters ) ) );
    }
  }

  // VARIABLES

  private VT220Parser m_parser;

  // METHODS

  /**
   * Tests that the parser can handle control characters inside CSI sequences.
   */
  public void testParseCSIWithControlCharactersOk() throws Exception
  {
    final int[] count = { 0 };

    m_parser.parse( "\033[2\010C", new VT220ParserTestAdapter()
    {
      @Override
      public void handleControl( char aControlChar ) throws IOException
      {
        assertEquals( '\010', aControlChar );
        count[0]++;
      }

      @Override
      public void handleCSI( CSIType aType, int... aParameters ) throws IOException
      {
        assertEquals( CSIType.CUF, aType );
        assertEquals( 2, aParameters[0] );
        count[0]++;
      }
    } );

    assertEquals( 2, count[0] );
  }

  /**
   * Test method for parsing CUP sequences.
   */
  public void testParseCUP() throws Exception
  {
    final int count[] = { 0 };
    m_parser.parse( "\033[1;2H", new VT220ParserTestAdapter()
    {
      @Override
      public void handleCSI( CSIType aType, int... aParameters ) throws IOException
      {
        assertEquals( CSIType.CUP, aType );
        assertEquals( 1, aParameters[0] );
        assertEquals( 2, aParameters[1] );
        count[0]++;
      }
    } );
    assertEquals( 1, count[0] );

    count[0] = 0;
    m_parser.parse( "\033[10;20H", new VT220ParserTestAdapter()
    {
      @Override
      public void handleCSI( CSIType aType, int... aParameters ) throws IOException
      {
        assertEquals( CSIType.CUP, aType );
        assertEquals( 10, aParameters[0] );
        assertEquals( 20, aParameters[1] );
        count[0]++;
      }
    } );
    assertEquals( 1, count[0] );
  }

  /**
   * Tests that the parsing for character set designations works.
   */
  public void testParseDesignateCharacterSetOk() throws Exception
  {
    final int count[] = { 0 };
    m_parser.parse( "\033(A", new VT220ParserTestAdapter()
    {
      @Override
      public void handleESC( char aDesignator, int... aParameters ) throws IOException
      {
        assertEquals( '(', aDesignator );
        assertEquals( 'A', aParameters[0] );
        count[0]++;
      }
    } );
    assertEquals( 1, count[0] );

    count[0] = 0;
    m_parser.parse( "\033)B", new VT220ParserTestAdapter()
    {
      @Override
      public void handleESC( char aDesignator, int... aParameters ) throws IOException
      {
        assertEquals( ')', aDesignator );
        assertEquals( 'B', aParameters[0] );
        count[0]++;
      }
    } );
    assertEquals( 1, count[0] );

    count[0] = 0;
    m_parser.parse( "\033*C", new VT220ParserTestAdapter()
    {
      @Override
      public void handleESC( char aDesignator, int... aParameters ) throws IOException
      {
        assertEquals( '*', aDesignator );
        assertEquals( 'C', aParameters[0] );
        count[0]++;
      }
    } );
    assertEquals( 1, count[0] );

    count[0] = 0;
    m_parser.parse( "\033+D", new VT220ParserTestAdapter()
    {
      @Override
      public void handleESC( char aDesignator, int... aParameters ) throws IOException
      {
        assertEquals( '+', aDesignator );
        assertEquals( 'D', aParameters[0] );
        count[0]++;
      }
    } );
    assertEquals( 1, count[0] );
  }

  /**
   * Tests that a save-cursor directive followed by a CSI is correctly parsed.
   */
  public void testParseSaveCursorSequenceWithCUP() throws Exception
  {
    final int[] count = { 0 };

    m_parser.parse( "\0337\033[1;1H", new VT220ParserTestAdapter()
    {
      @Override
      public void handleCSI( CSIType aType, int... aParameters ) throws IOException
      {
        assertEquals( CSIType.CUP, aType );
        assertEquals( 1, aParameters[0] );
        assertEquals( 1, aParameters[1] );
        count[0]++;
      }

      @Override
      public void handleESC( char aDesignator, int... aParameters ) throws IOException
      {
        assertEquals( '7', aDesignator );
        count[0]++;
      }
    } );

    assertEquals( 2, count[0] );
  }

  /**
   * Set up for each test case.
   */
  protected void setUp() throws Exception
  {
    m_parser = new VT220Parser();
    m_parser.setLogLevel( 1 );
  }
}
