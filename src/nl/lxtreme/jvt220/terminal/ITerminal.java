/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal;


import java.io.*;


/**
 * Denotes a terminal, which is a text area of fixed dimensions (width and
 * height).
 */
public interface ITerminal
{
  // INNER TYPES

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
   * Returns the cursor of this terminal, denoting the current write-position
   * is.
   * 
   * @return the current cursor, never <code>null</code>.
   */
  ICursor getCursor();

  /**
   * Returns the terminal frontend, responsible for representing the contents of
   * the terminal in a textual or graphical manner.
   * 
   * @return the terminal frontend, cannot be <code>null</code>.
   */
  ITerminalFrontend getFrontend();

  /**
   * Returns the height of this terminal.
   * 
   * @return a height, in characters.
   */
  int getHeight();

  /**
   * Returns the tabulator for this terminal.
   * 
   * @return the tabulator, never <code>null</code>.
   */
  ITabulator getTabulator();

  /**
   * Returns the width of this terminal.
   * 
   * @return a width, in characters.
   */
  int getWidth();

  /**
   * Handles the given character sequence as text that should be handled be this
   * terminal, for example, by regarding it as literal text, or interpreting in
   * some other way.
   * 
   * @param aChars
   *          the character sequence to handle, cannot be <code>null</code>.
   * @return the index until which the given character sequence is handled.
   * @throws IOException
   *           in case of I/O exceptions handling the output.
   * @see #getCursor()
   */
  int readInput( CharSequence aChars ) throws IOException;

  /**
   * Resets this terminal to its initial values, meaning that its content will
   * be cleared, the cursor will be placed in the first (upper left) position
   * and the scroll region will also be reset.
   */
  void reset();

  /**
   * Sets the terminal frontend to use.
   * 
   * @param aFrontend
   *          the frontend to use, cannot be <code>null</code>.
   */
  void setFrontend( ITerminalFrontend aFrontend );

  /**
   * Handles the given character sequence as response from this terminal.
   * 
   * @param aChars
   *          the character sequence to handle, cannot be <code>null</code>.
   * @return the index until which the given character sequence is handled.
   * @throws IOException
   *           in case of I/O exceptions handling the input.
   */
  int writeResponse( CharSequence aChars ) throws IOException;
}
