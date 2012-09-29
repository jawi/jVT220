/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


import java.awt.*;
import java.io.*;

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
  void terminalChanged( ITextCell[] cells, boolean[] heatMap );

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
}
