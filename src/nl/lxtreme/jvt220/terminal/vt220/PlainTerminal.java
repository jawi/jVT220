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
package nl.lxtreme.jvt220.terminal.vt220;

import static nl.lxtreme.jvt220.terminal.vt220.CharacterSets.*;

import java.awt.*;
import java.util.concurrent.atomic.*;


/**
 * Provides a plain terminal, that escapes any non-printable ASCII characters.
 */
public class PlainTerminal extends AbstractTerminal
{
  // VARIABLES

  private final AtomicBoolean m_rawMode;

  // CONSTRUCTORS

  /**
   * Creates a new {@link PlainTerminal} instance.
   * 
   * @param aColumns
   *          the initial number of columns in this terminal, > 0;
   * @param aLines
   *          the initial number of lines in this terminal, > 0.
   */
  public PlainTerminal( final int aColumns, final int aLines )
  {
    super( aColumns, aLines );

    m_rawMode = new AtomicBoolean();
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
    return m_rawMode.get();
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
      old = m_rawMode.get();
    }
    while ( !m_rawMode.compareAndSet( old, aRawMode ) );
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
          idx = removeChar( --idx, false /* aKeepProtectedCells */);
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
