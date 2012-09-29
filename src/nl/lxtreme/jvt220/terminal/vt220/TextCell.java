/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import nl.lxtreme.jvt220.terminal.ITerminal.ITextCell;


/**
 * Provides an implementation of {@link ITextCell} that packs all its
 * information in a character and short value.
 */
final class TextCell extends TextAttributes implements ITextCell
{
  // VARIABLES

  private char m_ch;

  // CONSTRUCTORS

  /**
   * Creates a new, empty {@link TextCell} instance.
   */
  public TextCell()
  {
    this( ' ', ( short )0 );
  }

  /**
   * Creates a new {@link TextCell} instance with the given contents and
   * attributes.
   * 
   * @param ch
   *          the contents of this cell;
   * @param attributes
   *          the attributes of this cell.
   */
  public TextCell( char ch, short attributes )
  {
    m_ch = ch;
    setAttributes( attributes );
  }

  /**
   * Creates a new {@link TextCell} instance as copy of the given text cell.
   * 
   * @param cell
   *          the cell to copy the content + attributes from, cannot be
   *          <code>null</code>.
   */
  public TextCell( TextCell cell )
  {
    this( cell.getChar(), cell.getAttributes() );
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public char getChar()
  {
    return m_ch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "[" + m_ch + "]";
  }
}
