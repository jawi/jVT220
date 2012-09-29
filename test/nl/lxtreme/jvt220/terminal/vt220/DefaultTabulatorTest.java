/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import junit.framework.*;
import nl.lxtreme.jvt220.terminal.vt220.AbstractTerminal.DefaultTabulator;


/**
 * Test cases for {@link DefaultTabulator}.
 */
public class DefaultTabulatorTest extends TestCase
{
  // VARIABLES

  private PlainTerminal m_term;
  private DefaultTabulator m_tabulator;

  // METHODS

  /**
   * Set up for each test case.
   */
  protected void setUp()
  {
    m_term = new PlainTerminal( 80, 24 );
    m_tabulator = m_term.new DefaultTabulator( 80, 8 );
  }

  /**
   * Tests that we can clear all set tab stops.
   */
  public void testClearAll()
  {
    m_tabulator.set( 1 );
    m_tabulator.set( 3 );

    assertFalse( m_tabulator.getTabStops().isEmpty() );

    m_tabulator.clearAll();

    assertTrue( m_tabulator.getTabStops().isEmpty() );
  }

  /**
   * Tests that we can use the default tab stops.
   */
  public void testNextTabWithoutAnyTabStops()
  {
    int tabStop = 4; // positions
    m_tabulator = m_term.new DefaultTabulator( 80, tabStop );

    for ( int startPos = 0; startPos < tabStop; startPos++ )
    {
      for ( int i = startPos, j = 4; i < 76; i += tabStop, j += tabStop )
      {
        assertEquals( "StartPos = " + startPos + ", i = " + i, j, m_tabulator.nextTab( i ) );
      }
      assertEquals( 79, m_tabulator.nextTab( 77 ) );
    }
  }

  /**
   * Tests that we can use the tab stops.
   */
  public void testNextTabWithTabStops()
  {
    m_tabulator.clearAll();
    m_tabulator.set( 1 );
    m_tabulator.set( 3 );
    m_tabulator.set( 5 );
    m_tabulator.set( 7 );
    m_tabulator.set( 9 );
    m_tabulator.set( 16 );

    assertEquals( 1, m_tabulator.nextTab( 0 ) );
    assertEquals( 3, m_tabulator.nextTab( 2 ) );
    assertEquals( 5, m_tabulator.nextTab( 4 ) );
    assertEquals( 7, m_tabulator.nextTab( 6 ) );
    assertEquals( 9, m_tabulator.nextTab( 8 ) );
    assertEquals( 16, m_tabulator.nextTab( 13 ) );
    assertEquals( 79, m_tabulator.nextTab( 16 ) );
    assertEquals( 79, m_tabulator.nextTab( 79 ) );
  }
}
