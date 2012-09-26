/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


/**
 * Provides a tabulator that keeps track of the tab stops of a terminal.
 */
public interface ITabulator
{
  // METHODS

  /**
   * Clears the tab stop at the given position.
   * 
   * @param aPosition
   *          the column position used to determine the next tab stop, > 0.
   */
  void clear( int aPosition );

  /**
   * Clears all tab stops.
   */
  void clearAll();

  /**
   * Returns the width of the tab stop that is at or after the given position.
   * 
   * @param aPosition
   *          the column position used to determine the next tab stop, >= 0.
   * @return the next tab stop width, >= 0.
   */
  int getNextTabWidth( int aPosition );

  /**
   * Returns the width of the tab stop that is before the given position.
   * 
   * @param aPosition
   *          the column position used to determine the previous tab stop, >= 0.
   * @return the previous tab stop width, >= 0.
   */
  int getPreviousTabWidth( int aPosition );

  /**
   * Returns the next tab stop that is at or after the given position.
   * 
   * @param aPosition
   *          the column position used to determine the next tab stop, >= 0.
   * @return the next tab stop, >= 0.
   */
  int nextTab( int aPosition );

  /**
   * Returns the previous tab stop that is before the given position.
   * 
   * @param aPosition
   *          the column position used to determine the previous tab stop, >= 0.
   * @return the previous tab stop, >= 0.
   */
  int previousTab( int aPosition );

  /**
   * Sets the tab stop to the given position.
   * 
   * @param aPosition
   *          the position of the (new) tab stop, > 0.
   */
  void set( int aPosition );
}
