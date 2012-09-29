/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


import java.awt.*;


/**
 * Provides the terminal colors.
 */
public interface ITerminalColorScheme
{
  // METHODS

  /**
   * Returns the background color.
   * 
   * @return the background color, never <code>null</code>.
   * @see #setInverted(boolean)
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
   * Returns the foreground color.
   * 
   * @return the plain text color, never <code>null</code>.
   * @see #setInverted(boolean)
   */
  Color getTextColor();

  /**
   * Sets whether or not the foreground and background color are to be swapped.
   * 
   * @param inverted
   *          <code>true</code> if the background color should become the
   *          foreground color and the other way around, <code>false</code>
   *          otherwise.
   */
  void setInverted( boolean inverted );
}
