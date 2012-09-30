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
