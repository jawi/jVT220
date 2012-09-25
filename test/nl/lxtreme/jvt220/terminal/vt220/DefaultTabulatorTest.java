/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;

import java.io.*;

import junit.framework.*;
import nl.lxtreme.jvt220.terminal.vt220.AbstractTerminal.DefaultTabulator;


/**
 * Test cases for {@link DefaultTabulator}.
 */
public class DefaultTabulatorTest extends TestCase
{
  // VARIABLES

  private DefaultTabulator tabulator;

  // METHODS

  /**
   * Set up for each test case.
   */
  protected void setUp()
  {
    PlainTerminal term = new PlainTerminal( new ByteArrayOutputStream(), 80, 24 );
    this.tabulator = term.new DefaultTabulator( true /* aUseDefaultTabStops */);
  }

  /**
   * Tests that we can clear all set tab stops.
   */
  public void testClearAll()
  {
    this.tabulator.set( 1 );
    this.tabulator.set( 3 );

    assertFalse( this.tabulator.getTabStops().isEmpty() );

    this.tabulator.clearAll();

    assertTrue( this.tabulator.getTabStops().isEmpty() );
  }

  /**
   * Tests that we can use the default tab stops.
   */
  public void testNextTabWithoutAnyTabStops()
  {
    int tabStop = 4; // positions
    this.tabulator.setDefault( tabStop );

    for ( int startPos = 0; startPos < tabStop; startPos++ )
    {
      for ( int i = startPos, j = 4; i < 76; i += tabStop, j += tabStop )
      {
        assertEquals( "StartPos = " + startPos + ", i = " + i, j, this.tabulator.nextTab( i ) );
      }
      assertEquals( 79, this.tabulator.nextTab( 77 ) );
    }
  }

  /**
   * Tests that we can use the tab stops.
   */
  public void testNextTabWithTabStops()
  {
    this.tabulator.set( 1 );
    this.tabulator.set( 3 );
    this.tabulator.set( 5 );
    this.tabulator.set( 7 );
    this.tabulator.set( 9 );

    assertEquals( 1, this.tabulator.nextTab( 0 ) );
    assertEquals( 3, this.tabulator.nextTab( 2 ) );
    assertEquals( 5, this.tabulator.nextTab( 4 ) );
    assertEquals( 7, this.tabulator.nextTab( 6 ) );
    assertEquals( 9, this.tabulator.nextTab( 8 ) );
    assertEquals( 16, this.tabulator.nextTab( 13 ) );
    assertEquals( 79, this.tabulator.nextTab( 79 ) );
  }
}
