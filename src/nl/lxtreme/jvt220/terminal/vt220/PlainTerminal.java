/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


import java.awt.*;
import java.io.*;
import java.util.concurrent.atomic.*;


/**
 * Provides a plain terminal, that escapes any non-printable ASCII characters.
 */
public class PlainTerminal extends AbstractTerminal
{
  // CONSTANTS

  private static final String[] ASCII_NAMES = { "<nul>", "<soh>", "<stx>", "<etx>", "<eot>", "<enq>", "<ack>",
      "<bell>", "\b", "\t", "\n", "<vt>", "<np>", "\r", "<so>", "<si>", "<dle>", "<dc1>", "<dc2>", "<dc3>", "<dc4>",
      "<nak>", "<syn>", "<etb>", "<can>", "<em>", "<sub>", "<esc>", "<fs>", "<gs>", "<rs>", "<us>", " ", "!", "\"",
      "#", "$", "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7", "8",
      "9", ":", ";", "<", "=", ">", "?", "@", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N",
      "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[", "\\", "]", "^", "_", "`", "a", "b", "c", "d",
      "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
      "{", "|", "}", "~", "<del>" };

  // VARIABLES

  private final AtomicBoolean rawMode;

  // CONSTRUCTORS

  /**
   * Creates a new {@link PlainTerminal} instance.
   * 
   * @param aOutputStream
   *          the output stream to write back to, cannot be <code>null</code>;
   * @param aColumns
   *          the initial number of columns in this terminal, > 0;
   * @param aLines
   *          the initial number of lines in this terminal, > 0.
   */
  public PlainTerminal( final OutputStream aOutputStream, final int aColumns, final int aLines )
  {
    super( aOutputStream, aColumns, aLines );

    this.rawMode = new AtomicBoolean();
  }

  // METHODS

  /**
   * Returns whether or not this terminal is in "raw" mode. In non-raw mode, the
   * non-printable ASCII characters are represented by their name.
   * 
   * @return <code>true</code> if this terminal is in 'raw' mode,
   *         <code>false</code> (the default) otherwise.
   */
  public boolean isRawMode()
  {
    return this.rawMode.get();
  }

  /**
   * Sets whether or not this terminal should display non-printable ASCII
   * characters by their name.
   * 
   * @param aRawMode
   *          <code>false</code> if the names for non-printable ASCII characters
   *          should be displayed, <code>true</code> otherwise.
   */
  public void setRawMode( boolean aRawMode )
  {
    boolean old;
    do
    {
      old = this.rawMode.get();
    }
    while ( !this.rawMode.compareAndSet( old, aRawMode ) );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int doReadInput( final CharSequence aChars )
  {
    if ( aChars == null )
    {
      throw new IllegalArgumentException( "Chars cannot be null!" );
    }

    int idx = getAbsoluteCursorIndex();

    for ( int i = 0; i < aChars.length(); i++ )
    {
      char c = aChars.charAt( i );

      switch ( c )
      {
        case '\010':
          // Backspace
          idx = removeChar( --idx, false /* aKeepProtectedCells */ );
          break;

        case '\007':
          // Bell
          Toolkit.getDefaultToolkit().beep();
          break;

        case '\012':
          // Newline
          idx += getWidth();
          break;

        case '\015':
          // Carriage return
          idx -= ( idx % getWidth() );
          break;

        default:
          if ( !isRawMode() && ( c < ASCII_NAMES.length ) )
          {
            String name = ASCII_NAMES[c];
            for ( int j = 0; j < name.length(); j++ )
            {
              idx = writeChar( idx, name.charAt( j ) );
            }
          }
          else
          {
            idx = writeChar( idx, c );
          }
          break;
      }
    }

    updateCursorByAbsoluteIndex( idx );
    return aChars.length();
  }
}
