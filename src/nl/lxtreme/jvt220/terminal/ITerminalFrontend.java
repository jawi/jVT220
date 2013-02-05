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
import java.io.*;
import java.util.*;

import nl.lxtreme.jvt220.terminal.ITerminal.ITextCell;


/**
 * Denotes a front end for a terminal, which is responsible for representing the
 * contents of a terminal visually, e.g, by means of an GUI.
 * <p>
 * A terminal front end is considered to be responsible for handling the I/O
 * between user and terminal.
 * </p>
 */
public interface ITerminalFrontend
{
  // METHODS

  /**
   * Connects this front end to the given input and output streams.
   * <p>
   * This method will start a background thread to read continuously from the
   * given input stream.
   * </p>
   * 
   * @param inputStream
   *          the input stream to read the data that should be passed to the
   *          terminal, cannot be <code>null</code>;
   * @param outputStream
   *          the output stream to write the data back coming from the terminal,
   *          cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems connecting the given input and output
   *           streams.
   */
  void connect( InputStream inputStream, OutputStream outputStream ) throws IOException;

  /**
   * Connects this frontend to a given output stream.
   * <p>
   * NOTE: when using this method, you need to explicitly call
   * {@link #writeCharacters(Integer...)} yourself in order to let anything
   * appear on the terminal.
   * </p>
   * 
   * @param outputStream
   *          the output stream to write the data back coming from the terminal,
   *          cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems connecting the given output streams.
   */
  void connect( OutputStream outputStream ) throws IOException;

  /**
   * Disconnects this front end from a connected input and output stream.
   * 
   * @throws IOException
   *           in case of I/O problems disconnecting.
   */
  void disconnect() throws IOException;

  /**
   * Returns the maximum possible size of the terminal in columns and lines to
   * fit on this frontend.
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
   * Returns the {@link Writer} to write responses from the terminal to.
   * 
   * @return a {@link Writer}, can be <code>null</code> in case this front end
   *         is not connected yet, or disconnected.
   */
  Writer getWriter();

  /**
   * @return <code>true</code> if this frontend is able to listen to changes,
   *         <code>false</code> if not.
   */
  boolean isListening();

  /**
   * Sets whether or not the foreground and background colors should be
   * reversed.
   * 
   * @param reverse
   *          <code>true</code> to reverse the foreground and background colors,
   *          <code>false</code> otherwise.
   */
  void setReverse( boolean reverse );

  /**
   * Sets the terminal dimensions in pixels.
   * 
   * @param width
   *          the new width in pixels, > 0;
   * @param height
   *          the new height in pixels, > 0.
   */
  void setSize( int width, int height );

  /**
   * Sets the terminal for this frontend.
   * 
   * @param terminal
   *          the terminal to connect, cannot be <code>null</code>.
   */
  void setTerminal( ITerminal terminal );

  /**
   * Called by {@link ITerminal} to notify this frontend that it has changed.
   * 
   * @param cells
   *          the array with text cells representing the contents of the
   *          terminal, never <code>null</code>;
   * @param heatMap
   *          the "heat" map, representing all changed cells of the terminal,
   *          never <code>null</code>.
   */
  void terminalChanged( ITextCell[] cells, BitSet heatMap );

  /**
   * Called by {@link ITerminal} to notify the dimensions of the terminal have
   * changed.
   * 
   * @param columns
   *          the new number of columns, > 0;
   * @param alines
   *          the new number of lines, > 0.
   */
  void terminalSizeChanged( int columns, int alines );

  /**
   * Writes the given array of characters directly to the terminal, similar as
   * writing to the standard output.
   * 
   * @param chars
   *          the array with characters to write, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems writing to the terminal.
   */
  void writeCharacters( Integer... chars ) throws IOException;

  /**
   * Writes the given sequence of characters directly to the terminal, similar
   * as writing to the standard output.
   * 
   * @param chars
   *          the sequence of characters to write, cannot be <code>null</code>.
   * @throws IOException
   *           in case of I/O problems writing to the terminal.
   */
  void writeCharacters( CharSequence chars ) throws IOException;
}
