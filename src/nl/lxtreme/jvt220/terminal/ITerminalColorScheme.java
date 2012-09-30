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
