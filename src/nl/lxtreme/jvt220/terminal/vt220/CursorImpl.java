/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import nl.lxtreme.jvt220.terminal.*;


/**
 * Provides an implementation of {@link ICursor}.
 */
public class CursorImpl implements ICursor
{
  // VARIABLES

  private int blinkRate;
  private boolean visible;
  private int x;
  private int y;

  // CONSTRUCTORS

  /**
   * Creates a new {@link CursorImpl} instance.
   */
  public CursorImpl()
  {
    this.blinkRate = 500;
    this.visible = true;
    this.x = 0;
    this.y = 0;
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public ICursor clone()
  {
    try
    {
      CursorImpl clone = ( CursorImpl )super.clone();
      clone.blinkRate = this.blinkRate;
      clone.visible = this.visible;
      clone.x = this.x;
      clone.y = this.y;
      return clone;
    }
    catch ( CloneNotSupportedException e )
    {
      throw new RuntimeException( "Cloning not supported!?!" );
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int getBlinkRate()
  {
    return this.blinkRate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getX()
  {
    return this.x;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getY()
  {
    return this.y;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isVisible()
  {
    return this.visible;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setBlinkRate( final int aRate )
  {
    if ( aRate < 0 )
    {
      throw new IllegalArgumentException( "Invalid blink rate!" );
    }
    this.blinkRate = aRate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setVisible( final boolean aVisible )
  {
    this.visible = aVisible;
  }

  /**
   * {@inheritDoc}
   */
  final void setPosition( final int aX, final int aY )
  {
    this.x = aX;
    this.y = aY;
  }
}
