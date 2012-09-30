/**
 * jVT220 - Java VT220 terminal emulator.
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
