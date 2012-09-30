/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


/**
 * Denotes a container for text attributes.
 */
class TextAttributes
{
  // CONSTANTS

  static final int COLOR_MASK = ( 1 << 5 ) - 1;
  static final int BOLD_MASK = 1 << 10;
  static final int ITALIC_MASK = 1 << 11;
  static final int UNDERLINE_MASK = 1 << 12;
  static final int REVERSE_MASK = 1 << 13;
  static final int HIDDEN_MASK = 1 << 14;
  static final int PROTECTED_MASK = 1 << 15;

  // VARIABLES

  private short m_attr;

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals( Object object )
  {
    if ( this == object )
    {
      return true;
    }

    if ( ( object == null ) || getClass() != object.getClass() )
    {
      return false;
    }

    final TextAttributes other = ( TextAttributes )object;
    return ( m_attr == other.m_attr );
  }

  /**
   * Returns the encoded attributes.
   * 
   * @return the encoded attributes as short value.
   */
  public short getAttributes()
  {
    return m_attr;
  }

  /**
   * {@inheritDoc}
   */
  public int getBackground()
  {
    int bg = ( ( m_attr >> 5 ) & COLOR_MASK );
    return bg;
  }

  /**
   * {@inheritDoc}
   */
  public int getForeground()
  {
    int fg = ( m_attr & COLOR_MASK );
    return fg;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result + m_attr;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isBold()
  {
    return ( m_attr & BOLD_MASK ) != 0;
  }

  /**
   * @return
   */
  public boolean isHidden()
  {
    return ( m_attr & HIDDEN_MASK ) != 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isItalic()
  {
    return ( m_attr & ITALIC_MASK ) != 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isProtected()
  {
    return ( m_attr & PROTECTED_MASK ) != 0;
  }

  /**
   * @return
   */
  public boolean isReverse()
  {
    return ( m_attr & REVERSE_MASK ) != 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isUnderline()
  {
    return ( m_attr & UNDERLINE_MASK ) != 0;
  }

  /**
   * Resets all attributes to their default values, except for the foreground
   * and background color.
   */
  public void reset()
  {
    m_attr &= 0x3FF; // keep lower 10 bits...
  }

  /**
   * Resets all attributes to their default values.
   */
  public void resetAll()
  {
    m_attr = 0;
  }

  /**
   * Directly sets the attributes as encoded value.
   * 
   * @param attributes
   *          the attributes to set.
   */
  public void setAttributes( short attributes )
  {
    m_attr = attributes;
  }

  /**
   * Sets the background color.
   * 
   * @param index
   *          the index of the background color, >= 0 && < 32.
   */
  public void setBackground( int index )
  {
    int bg = ( index & COLOR_MASK ) << 5;
    m_attr &= 0xFE1F; // clear bg color bits...
    m_attr |= bg;
  }

  /**
   * @param enable
   *          <code>true</code> to enable the bold representation,
   *          <code>false</code> to disable it.
   */
  public void setBold( boolean enable )
  {
    setAttrBit( enable, BOLD_MASK );
  }

  /**
   * Sets the foreground color.
   * 
   * @param index
   *          the index of the foreground color, >= 0 && < 32.
   */
  public void setForeground( int index )
  {
    int fg = index & COLOR_MASK;
    m_attr &= 0xFFE0; // clear fg color bits...
    m_attr |= fg;
  }

  /**
   * @param enable
   *          <code>true</code> to enable the hidden property,
   *          <code>false</code> to disable it.
   */
  public void setHidden( boolean enable )
  {
    setAttrBit( enable, HIDDEN_MASK );
  }

  /**
   * @param enable
   *          <code>true</code> to enable the italic property,
   *          <code>false</code> to disable it.
   */
  public void setItalic( boolean enable )
  {
    setAttrBit( enable, ITALIC_MASK );
  }

  /**
   * @param enable
   *          <code>true</code> to enable the protected property,
   *          <code>false</code> to disable it.
   */
  public void setProtected( boolean enable )
  {
    setAttrBit( enable, PROTECTED_MASK );
  }

  /**
   * @param enable
   *          <code>true</code> to enable the reverse property,
   *          <code>false</code> to disable it.
   */
  public void setReverse( boolean enable )
  {
    setAttrBit( enable, REVERSE_MASK );
  }

  /**
   * @param enable
   *          <code>true</code> to enable the underline property,
   *          <code>false</code> to disable it.
   */
  public void setUnderline( boolean enable )
  {
    setAttrBit( enable, UNDERLINE_MASK );
  }

  /**
   * Sets or resets the bit in the attributes denoted by the given mask.
   * 
   * @param enable
   *          <code>true</code> to set the bit, <code>false</code> to reset it;
   * @param mask
   *          the mask of the bit to set or reset.
   */
  private void setAttrBit( boolean enable, int mask )
  {
    if ( enable )
    {
      m_attr |= mask;
    }
    else
    {
      m_attr &= ~mask;
    }
  }
}
