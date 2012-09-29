/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.swing;


import java.awt.*;
import java.util.concurrent.atomic.*;

import nl.lxtreme.jvt220.terminal.*;


/**
 * Represents the default color scheme, derived from the one used by XTerm.
 */
public final class XtermColorScheme implements ITerminalColorScheme
{
  // CONSTANTS

  private static final Color PLAIN_TEXT_COLOR = new Color( 0xE6, 0xE6, 0xE6 );
  private static final Color BACKGROUND_COLOR = new Color( 0x1E, 0x21, 0x26 );

  private static final Color[] XTERM_COLORS = { BACKGROUND_COLOR, // Off-Black
      new Color( 205, 0, 0 ), // Red
      new Color( 0, 205, 0 ), // Green
      new Color( 205, 205, 0 ), // Yellow
      new Color( 0, 0, 238 ), // Blue
      new Color( 205, 0, 205 ), // Magenta
      new Color( 0, 205, 205 ), // Cyan
      new Color( 229, 229, 229 ), // White
  };

  // VARIABLES

  private final AtomicBoolean inverted;

  // CONSTRUCTORS

  /**
   * Creates a new {@link XtermColorScheme} instance.
   */
  public XtermColorScheme()
  {
    this.inverted = new AtomicBoolean( false );
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public Color getBackgroundColor()
  {
    return isInverted() ? PLAIN_TEXT_COLOR : BACKGROUND_COLOR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Color getColorByIndex( int aIndex )
  {
    return XTERM_COLORS[aIndex % XTERM_COLORS.length];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Color getTextColor()
  {
    return isInverted() ? BACKGROUND_COLOR : PLAIN_TEXT_COLOR;
  }

  /**
   * @return <code>true</code> if the foreground and background colors are to be
   *         swapped, <code>false</code> otherwise.
   */
  public boolean isInverted()
  {
    return this.inverted.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setInverted( boolean aInverted )
  {
    this.inverted.set( aInverted );
  }
}
