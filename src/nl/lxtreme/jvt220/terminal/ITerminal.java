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


import java.awt.event.*;
import java.io.*;


/**
 * Denotes a terminal, which is a text area of fixed dimensions (width and
 * height).
 */
public interface ITerminal extends Closeable
{
  // INNER TYPES

  /**
   * Used to translate keyboard codes to this terminal.
   */
  public static interface IKeyMapper
  {
    // METHODS

    /**
     * Maps a given keycode + modifiers mask into a character sequence that can
     * be send from this terminal.
     * 
     * @param aKeyCode
     *          the key code, as defined in {@link KeyEvent} (<tt>VK_*</tt>);
     * @param aModifiers
     *          the bit mask with key modifiers, as defined in
     *          {@link InputEvent} (<tt>*_MASK</tt>).
     * @return a string that maps the given key code and modifiers in a sequence
     *         that is native to this terminal, or <code>null</code> if no
     *         mapping could be made.
     */
    String map( int aKeyCode, int aModifiers );
  }

  /**
   * Denotes a single 'cell' which contains a character and mark up attributes.
   */
  public static interface ITextCell
  {
    // METHODS

    /**
     * @return the background color index, >= 0 && < 32. A value of 0 means the
     *         default background color.
     */
    int getBackground();

    /**
     * @return the contents of this cell, as UTF-8 character.
     */
    char getChar();

    /**
     * @return the foreground color index, >= 0 && < 32. A value of 0 means the
     *         default background color.
     */
    int getForeground();

    /**
     * @return <code>true</code> if the text should be presented in bold,
     *         <code>false</code> otherwise.
     */
    boolean isBold();

    /**
     * @return <code>true</code> if the text should be presented in italic,
     *         <code>false</code> otherwise.
     */
    boolean isItalic();

    /**
     * @return <code>true</code> if the contents of this cell are protected from
     *         erasure, <code>false</code> otherwise.
     */
    boolean isProtected();

    /**
     * @return <code>true</code> if the text should be presented underlined,
     *         <code>false</code> otherwise.
     */
    boolean isUnderline();

    /**
     * @return <code>true</code> if the foreground and background color should
     *         be swapped, <code>false</code> otherwise.
     */
    boolean isReverse();

    /**
     * @return <code>true</code> if the text should be hidden,
     *         <code>false</code> otherwise.
     */
    boolean isHidden();

  }

  // METHODS

  /**
   * Closes this terminal and frees all of its resources.
   * <p>
   * After a terminal has been closed, it should not be used any more. Doing
   * this might result in unexpected behavior.
   * </p>
   * 
   * @throws IOException
   *           in case of I/O problems closing this terminal.
   */
  void close() throws IOException;

  /**
   * Returns the cursor of this terminal, denoting the current write-position
   * is.
   * 
   * @return the current cursor, never <code>null</code>.
   */
  ICursor getCursor();

  /**
   * Returns the terminal front end, responsible for representing the contents
   * of the terminal visually.
   * 
   * @return the terminal front end, can be <code>null</code> if not set.
   */
  ITerminalFrontend getFrontend();

  /**
   * Returns the height of this terminal.
   * 
   * @return a height, in lines.
   */
  int getHeight();

  /**
   * Returns a mapper that translated key codes.
   * 
   * @return a key mapper, never <code>null</code>.
   */
  IKeyMapper getKeyMapper();

  /**
   * Returns the tabulator for this terminal.
   * 
   * @return the tabulator, never <code>null</code>.
   */
  ITabulator getTabulator();

  /**
   * Returns the width of this terminal.
   * 
   * @return a width, in columns.
   */
  int getWidth();

  /**
   * Handles the given character sequence as text that should be handled be this
   * terminal, for example, by regarding it as literal text, or interpreting in
   * some other way.
   * 
   * @param chars
   *          the character sequence to handle, cannot be <code>null</code>.
   * @return the index until which the given character sequence is handled.
   * @throws IOException
   *           in case of I/O exceptions handling the output.
   * @see #getCursor()
   */
  int read( CharSequence chars ) throws IOException;

  /**
   * Resets this terminal to its initial values, meaning that its content will
   * be cleared, the cursor will be placed in the first (upper left) position
   * and all implementation specific options will be reset to their default
   * values.
   */
  void reset();

  /**
   * Sets the terminal front end to use.
   * 
   * @param frontend
   *          the front end to use, cannot be <code>null</code>.
   * @throws IllegalArgumentException
   *           in case the given front end was <code>null</code>.
   * @see #getFrontend()
   */
  void setFrontend( ITerminalFrontend frontend );

  /**
   * Handles the given character sequence as response from this terminal.
   * 
   * @param chars
   *          the character sequence to handle, cannot be <code>null</code>.
   * @return the index until which the given character sequence is handled.
   * @throws IOException
   *           in case of I/O exceptions handling the input.
   */
  int write( CharSequence chars ) throws IOException;
}
