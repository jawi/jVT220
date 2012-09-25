/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


import java.awt.*;


/**
 * Provides the default terminal colors.
 */
public interface ITerminalColorScheme
{

  // METHODS

  /**
   * @return the background color, never <code>null</code>.
   */
  Color getBackgroundColor();

  /**
   * Returns the foreground color by its numeric index. There are supposed to be
   * 8 different foreground colors.
   * 
   * @param aIndex
   *          the index of the color to return, >= 0 && < 8.
   * @return a foreground color, never <code>null</code>.
   */
  Color getColorByIndex( int aIndex );

  /**
   * @return the text color for visible escape sequences, never
   *         <code>null</code>.
   */
  Color getEscapedTextColor();

  /**
   * @return the plain text color, never <code>null</code>.
   */
  Color getPlainTextColor();

  /**
   * @return the status text color, never <code>null</code>.
   */
  Color getStatusTextColor();
}
