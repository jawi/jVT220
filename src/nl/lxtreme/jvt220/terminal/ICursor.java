/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


/**
 * Denotes a cursor, or the current write-position of a terminal.
 */
public interface ICursor extends Cloneable
{
  // METHODS

  /**
   * Returns a clone of this cursor instance.
   * 
   * @return a clone of this cursor, never <code>null</code>.
   * @see Cloneable
   */
  ICursor clone();

  /**
   * Returns the blinking rate of this cursor.
   * 
   * @return the current blinking rate, in milliseconds.
   * @see #setBlinkRate(int)
   */
  int getBlinkRate();

  /**
   * Returns the X-position of the cursor.
   * 
   * @return a X-position, >= 0.
   */
  int getX();

  /**
   * Returns the Y-position of the cursor.
   * 
   * @return a Y-position, >= 0.
   */
  int getY();

  /**
   * Returns whether or not this cursor is visible on screen.
   * 
   * @return <code>true</code> if this cursor is currently visible,
   *         <code>false</code> otherwise.
   */
  boolean isVisible();

  /**
   * Sets the blinking rate of this cursor.
   * 
   * @param rate
   *          a blinking rate, in milliseconds. A rate of 0 means no blinking.
   */
  void setBlinkRate( int rate );

  /**
   * Sets the visibility of the cursor.
   * 
   * @param visible
   *          <code>true</code> to make the cursor visible, <code>false</code>
   *          to hide the cursor.
   */
  void setVisible( boolean visible );
}
