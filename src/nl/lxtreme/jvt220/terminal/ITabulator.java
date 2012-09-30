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
 * Provides a tabulator that keeps track of the tab stops of a terminal.
 */
public interface ITabulator
{
  // METHODS

  /**
   * Clears the tab stop at the given position.
   * 
   * @param position
   *          the column position used to determine the next tab stop, > 0.
   */
  void clear( int position );

  /**
   * Clears all tab stops.
   */
  void clearAll();

  /**
   * Returns the width of the tab stop that is at or after the given position.
   * 
   * @param position
   *          the column position used to determine the next tab stop, >= 0.
   * @return the next tab stop width, >= 0.
   */
  int getNextTabWidth( int position );

  /**
   * Returns the width of the tab stop that is before the given position.
   * 
   * @param position
   *          the column position used to determine the previous tab stop, >= 0.
   * @return the previous tab stop width, >= 0.
   */
  int getPreviousTabWidth( int position );

  /**
   * Returns the next tab stop that is at or after the given position.
   * 
   * @param position
   *          the column position used to determine the next tab stop, >= 0.
   * @return the next tab stop, >= 0.
   */
  int nextTab( int position );

  /**
   * Returns the previous tab stop that is before the given position.
   * 
   * @param position
   *          the column position used to determine the previous tab stop, >= 0.
   * @return the previous tab stop, >= 0.
   */
  int previousTab( int position );

  /**
   * Sets the tab stop to the given position.
   * 
   * @param position
   *          the position of the (new) tab stop, > 0.
   */
  void set( int position );
}
