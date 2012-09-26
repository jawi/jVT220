/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


import java.awt.*;

import nl.lxtreme.jvt220.terminal.ITerminal.ITextCell;


/**
 * Denotes a frontend for a terminal, which is responsible for representing the
 * terminal in a textual or graphical manner.
 */
public interface ITerminalFrontend
{
  // INNER TYPES

  public static interface IHeatMap extends Iterable<Integer>
  {

  }

  // METHODS

  /**
   * Returns the maximum possible size of the terminal in columns and lines to fit on this frontend.
   * 
   * @return the maximum dimensions, never <code>null</code>.
   */
  Dimension getMaximumTerminalSize();

  /**
   * Returns the width and height of the terminal in pixels.
   * 
   * @return the current dimensions, never <code>null</code>.
   */
  Dimension getSize();

  /**
   * @return <code>true</code> if this frontend is able to listen to changes,
   *         <code>false</code> if not.
   */
  boolean isListening();

  /**
   * Sets the terminal dimensions in pixels.
   * 
   * @param aWidth
   *          the new width in pixels, > 0;
   * @param aHeight
   *          the new height in pixels, > 0.
   */
  void setSize( int aWidth, int aHeight );

  /**
   * Sets the terminal for this frontend.
   * 
   * @param aTerminal
   *          the terminal to connect, cannot be <code>null</code>.
   */
  void setTerminal( ITerminal aTerminal );

  /**
   * Called by {@link ITerminal} to notify this frontend that it has changed.
   * 
   * @param aCells
   *          the array with text cells representing the contents of the
   *          terminal, never <code>null</code>;
   * @param aHeatMap
   *          the "heat" map, representing all changed cells of the terminal,
   *          never <code>null</code>.
   */
  void terminalChanged( ITextCell[] aCells, boolean[] aHeatMap );

  /**
   * Called by {@link ITerminal} to notify the dimensions of the terminal have
   * changed.
   * 
   * @param aColumns
   *          the new number of columns, > 0;
   * @param aLines
   *          the new number of lines, > 0.
   */
  void terminalSizeChanged( int aColumns, int aLines );
}
