/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import java.awt.*;

import nl.lxtreme.jvt220.terminal.*;


/**
 * Represents the default color scheme, derived from the one used by XTerm.
 */
public class XtermColorScheme implements ITerminalColorScheme
{
  // CONSTANTS

  private static final Color ESCAPED_TEXT_COLOR = new Color( 0x00, 0x80, 0xFF );
  private static final Color PLAIN_TEXT_COLOR = new Color( 0xE6, 0xE6, 0xE6 );
  private static final Color STATUS_TEXT_COLOR = new Color( 0xFF, 0x80, 0x00 );
  private static final Color BACKGROUND_COLOR = new Color( 0x1E, 0x21, 0x26 );

  private static final Color[] XTERM_COLORS = { new Color( 0, 0, 0 ), // Black
      new Color( 205, 0, 0 ), // Red
      new Color( 0, 205, 0 ), // Green
      new Color( 205, 205, 0 ), // Yellow
      new Color( 0, 0, 238 ), // Blue
      new Color( 205, 0, 205 ), // Magenta
      new Color( 0, 205, 205 ), // Cyan
      new Color( 229, 229, 229 ), // White
  };

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public Color getBackgroundColor()
  {
    return BACKGROUND_COLOR;
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
  public Color getEscapedTextColor()
  {
    return ESCAPED_TEXT_COLOR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Color getPlainTextColor()
  {
    return PLAIN_TEXT_COLOR;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Color getStatusTextColor()
  {
    return STATUS_TEXT_COLOR;
  }
}
