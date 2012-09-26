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

  private short attr;

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals( Object aObject )
  {
    if ( this == aObject )
    {
      return true;
    }

    if ( ( aObject == null ) || getClass() != aObject.getClass() )
    {
      return false;
    }

    final TextAttributes other = ( TextAttributes )aObject;
    return ( this.attr == other.attr );
  }

  /**
   * {@inheritDoc}
   */
  public int getBackground()
  {
    int bg = ( ( this.attr >> 5 ) & COLOR_MASK );
    return bg;
  }

  /**
   * {@inheritDoc}
   */
  public int getForeground()
  {
    int fg = ( this.attr & COLOR_MASK );
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
    result = prime * result + this.attr;
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isBold()
  {
    return ( this.attr & BOLD_MASK ) != 0;
  }

  /**
   * @return
   */
  public boolean isHidden()
  {
    return ( this.attr & HIDDEN_MASK ) != 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isItalic()
  {
    return ( this.attr & ITALIC_MASK ) != 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isProtected()
  {
    return ( this.attr & PROTECTED_MASK ) != 0;
  }

  /**
   * @return
   */
  public boolean isReverse()
  {
    return ( this.attr & REVERSE_MASK ) != 0;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isUnderline()
  {
    return ( this.attr & UNDERLINE_MASK ) != 0;
  }

  /**
   * Resets all attributes to their default values, except for the foreground
   * and background color.
   */
  public void reset()
  {
    this.attr &= ~( ( 1 << 10 ) - 1 );
  }

  /**
   * Resets all attributes to their default values.
   */
  public void resetAll()
  {
    this.attr = 0;
  }

  /**
   * Sets the background color.
   * 
   * @param aIndex
   *          the index of the background color, >= 0 && < 32.
   */
  public void setBackground( int aIndex )
  {
    int bg = ( aIndex & COLOR_MASK ) << 5;
    this.attr &= 0xFE1F; // clear bg color bits...
    this.attr |= bg;
  }

  /**
   * @param aEnabled
   *          <code>true</code> to enable the bold representation,
   *          <code>false</code> to disable it.
   */
  public void setBold( boolean aEnabled )
  {
    setAttrBit( aEnabled, BOLD_MASK );
  }

  /**
   * Sets the foreground color.
   * 
   * @param aIndex
   *          the index of the foreground color, >= 0 && < 32.
   */
  public void setForeground( int aIndex )
  {
    int fg = aIndex & COLOR_MASK;
    this.attr &= 0xFFE0; // clear fg color bits...
    this.attr |= fg;
  }

  /**
   * @param aEnabled
   */
  public void setHidden( boolean aEnabled )
  {
    setAttrBit( aEnabled, HIDDEN_MASK );
  }

  /**
   * @param aEnabled
   */
  public void setItalic( boolean aEnabled )
  {
    setAttrBit( aEnabled, ITALIC_MASK );
  }

  /**
   * @param aEnabled
   */
  public void setProtected( boolean aEnabled )
  {
    setAttrBit( aEnabled, PROTECTED_MASK );
  }

  /**
   * @param aEnabled
   */
  public void setReverse( boolean aEnabled )
  {
    setAttrBit( aEnabled, REVERSE_MASK );
  }

  /**
   * @param aEnabled
   */
  public void setUnderline( boolean aEnabled )
  {
    setAttrBit( aEnabled, UNDERLINE_MASK );
  }

  /**
   * @return
   */
  short getAttributes()
  {
    return this.attr;
  }

  /**
   * @param aAttributes
   */
  void setAttributes( short aAttributes )
  {
    this.attr = aAttributes;
  }

  /**
   * Sets or resets the bit in the attributes denoted by the given mask.
   * 
   * @param aEnabled
   *          <code>true</code> to set the bit, <code>false</code> to reset it;
   * @param aMask
   *          the mask of the bit to set or reset.
   */
  private void setAttrBit( boolean aEnabled, int aMask )
  {
    if ( aEnabled )
    {
      this.attr |= aMask;
    }
    else
    {
      this.attr &= ~aMask;
    }
  }
}
