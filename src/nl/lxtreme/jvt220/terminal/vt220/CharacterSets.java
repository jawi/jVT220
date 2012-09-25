/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.vt220;


/**
 * Provides the (graphical) character sets.
 */
public final class CharacterSets
{
  // INNER TYPES

  /**
   * Provides an enum with names for the supported character sets.
   */
  static enum CharacterSet
  {
    // CONSTANTS

    ASCII( 'B' )
    {
      @Override
      public int map( int aIndex )
      {
        return -1;
      }
    },
    BRITISH( 'A' )
    {
      @Override
      public int map( int aIndex )
      {
        if ( aIndex == 3 )
        {
          // Pound sign...
          return '\u00a3';
        }
        return -1;
      }
    },
    DANISH( 'E', '6' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 32:
            return '\u00c4';
          case 59:
            return '\u00c6';
          case 60:
            return '\u00d8';
          case 61:
            return '\u00c5';
          case 62:
            return '\u00dc';
          case 64:
            return '\u00e4';
          case 91:
            return '\u00e6';
          case 92:
            return '\u00f8';
          case 93:
            return '\u00e5';
          case 94:
            return '\u00fc';
          default:
            return -1;
        }
      }
    },
    DEC_SPECIAL_GRAPHICS( '0', '2' )
    {
      @Override
      public int map( int aIndex )
      {
        if ( aIndex >= 64 && aIndex < 96 )
        {
          return ( ( Character )DEC_SPECIAL_CHARS[aIndex - 64][0] ).charValue();
        }
        return -1;
      }
    },
    DEC_SUPPLEMENTAL( 'U', '<' )
    {
      @Override
      public int map( int aIndex )
      {
        if ( aIndex >= 0 && aIndex < 64 )
        {
          // Set the 8th bit...
          return aIndex + 160;
        }
        return -1;
      }
    },
    DUTCH( '4' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 3:
            return '\u00a3';
          case 32:
            return '\u00be';
          case 59:
            return '\u0133';
          case 60:
            return '\u00bd';
          case 61:
            return '|';
          case 91:
            return '\u00a8';
          case 92:
            return '\u0192';
          case 93:
            return '\u00bc';
          case 94:
            return '\u00b4';
          default:
            return -1;
        }
      }
    },
    FINNISH( 'C', '5' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 59:
            return '\u00c4';
          case 60:
            return '\u00d4';
          case 61:
            return '\u00c5';
          case 62:
            return '\u00dc';
          case 64:
            return '\u00e9';
          case 91:
            return '\u00e4';
          case 92:
            return '\u00f6';
          case 93:
            return '\u00e5';
          case 94:
            return '\u00fc';
          default:
            return -1;
        }
      }
    },
    FRENCH( 'R' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 3:
            return '\u00a3';
          case 32:
            return '\u00e0';
          case 59:
            return '\u00b0';
          case 60:
            return '\u00e7';
          case 61:
            return '\u00a6';
          case 91:
            return '\u00e9';
          case 92:
            return '\u00f9';
          case 93:
            return '\u00e8';
          case 94:
            return '\u00a8';
          default:
            return -1;
        }
      }
    },
    FRENCH_CANADIAN( 'Q' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 32:
            return '\u00e0';
          case 59:
            return '\u00e2';
          case 60:
            return '\u00e7';
          case 61:
            return '\u00ea';
          case 62:
            return '\u00ee';
          case 91:
            return '\u00e9';
          case 92:
            return '\u00f9';
          case 93:
            return '\u00e8';
          case 94:
            return '\u00fb';
          default:
            return -1;
        }
      }
    },
    GERMAN( 'K' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 32:
            return '\u00a7';
          case 59:
            return '\u00c4';
          case 60:
            return '\u00d6';
          case 61:
            return '\u00dc';
          case 91:
            return '\u00e4';
          case 92:
            return '\u00f6';
          case 93:
            return '\u00fc';
          case 94:
            return '\u00df';
          default:
            return -1;
        }
      }
    },
    ITALIAN( 'Y' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 3:
            return '\u00a3';
          case 32:
            return '\u00a7';
          case 59:
            return '\u00ba';
          case 60:
            return '\u00e7';
          case 61:
            return '\u00e9';
          case 91:
            return '\u00e0';
          case 92:
            return '\u00f2';
          case 93:
            return '\u00e8';
          case 94:
            return '\u00ec';
          default:
            return -1;
        }
      }
    },
    SPANISH( 'Z' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 3:
            return '\u00a3';
          case 32:
            return '\u00a7';
          case 59:
            return '\u00a1';
          case 60:
            return '\u00d1';
          case 61:
            return '\u00bf';
          case 91:
            return '\u00b0';
          case 92:
            return '\u00f1';
          case 93:
            return '\u00e7';
          default:
            return -1;
        }
      }
    },
    SWEDISH( 'H', '7' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 32:
            return '\u00c9';
          case 59:
            return '\u00c4';
          case 60:
            return '\u00d6';
          case 61:
            return '\u00c5';
          case 62:
            return '\u00dc';
          case 64:
            return '\u00e9';
          case 91:
            return '\u00e4';
          case 92:
            return '\u00f6';
          case 93:
            return '\u00e5';
          case 94:
            return '\u00fc';
          default:
            return -1;
        }
      }
    },
    SWISS( '=' )
    {
      @Override
      public int map( int aIndex )
      {
        switch ( aIndex )
        {
          case 3:
            return '\u00f9';
          case 32:
            return '\u00e0';
          case 59:
            return '\u00e9';
          case 60:
            return '\u00e7';
          case 61:
            return '\u00ea';
          case 62:
            return '\u00ee';
          case 63:
            return '\u00e8';
          case 64:
            return '\u00f4';
          case 91:
            return '\u00e4';
          case 92:
            return '\u00f6';
          case 93:
            return '\u00fc';
          case 94:
            return '\u00fb';
          default:
            return -1;
        }
      }
    };

    // VARIABLES

    private final int[] designations;

    // CONSTRUCTORS

    /**
     * Creates a new {@link CharacterSet} instance.
     */
    private CharacterSet( int... aDesignations )
    {
      this.designations = aDesignations;
    }

    // METHODS

    /**
     * Returns the {@link CharacterSet} for the given character.
     * 
     * @param aDesignation
     *          the character to translate to a {@link CharacterSet}.
     * @return a character set name corresponding to the given character,
     *         defaulting to ASCII if no mapping could be made.
     */
    public static CharacterSet valueOf( char aDesignation )
    {
      for ( CharacterSet csn : values() )
      {
        if ( csn.isDesignation( aDesignation ) )
        {
          return csn;
        }
      }
      return ASCII;
    }

    /**
     * Maps the character with the given index to a character in this character
     * set.
     * 
     * @param aIndex
     *          the index of the character set, >= 0 && < 128.
     * @return a mapped character, or -1 if no mapping could be made and the
     *         ASCII value should be used.
     */
    public abstract int map( int aIndex );

    /**
     * Returns whether or not the given designation character belongs to this
     * character set's set of designations.
     * 
     * @param aDesignation
     *          the designation to test for.
     * @return <code>true</code> if the given designation character maps to this
     *         character set, <code>false</code> otherwise.
     */
    private boolean isDesignation( char aDesignation )
    {
      for ( int i = 0; i < this.designations.length; i++ )
      {
        if ( this.designations[i] == aDesignation )
        {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Denotes how a graphic set is designated.
   */
  static class GraphicSet
  {
    // VARIABLES

    private final int index; // 0..3
    private CharacterSet designation;

    // CONSTRUCTORS

    /**
     * Creates a new {@link GraphicSet} instance.
     */
    public GraphicSet( int aIndex )
    {
      if ( aIndex < 0 || aIndex > 3 )
      {
        throw new IllegalArgumentException( "Invalid index!" );
      }
      this.index = aIndex;
      // The default mapping, based on XTerm...
      this.designation = CharacterSet.valueOf( ( aIndex == 1 ) ? '0' : 'B' );
    }

    // METHODS

    /**
     * @return the designation of this graphic set.
     */
    public CharacterSet getDesignation()
    {
      return this.designation;
    }

    /**
     * @return the index of this graphics set.
     */
    public int getIndex()
    {
      return this.index;
    }

    /**
     * Maps a given character index to a concrete character.
     * 
     * @param aOriginal
     *          the original character to map;
     * @param aIndex
     *          the index of the character to map.
     * @return the mapped character, or -1 if no mapping could be made.
     */
    public int map( char aOriginal, int aIndex )
    {
      int result = this.designation.map( aIndex );
      if ( result < 0 )
      {
        result = aOriginal;
      }
      return result;
    }

    /**
     * @param aDesignation
     *          the designation to set
     */
    public void setDesignation( CharacterSet aDesignation )
    {
      if ( aDesignation == null )
      {
        throw new IllegalArgumentException( "Designation cannot be null!" );
      }
      this.designation = aDesignation;
    }
  }

  // CONSTANTS

  private static final int C0_START = 0;
  private static final int C0_END = 31;
  private static final int C1_START = 128;
  private static final int C1_END = 159;
  private static final int GL_START = 32;
  private static final int GL_END = 127;
  private static final int GR_START = 160;
  private static final int GR_END = 255;

  public static final String[] ASCII_NAMES = { "<nul>", "<soh>", "<stx>", "<etx>", "<eot>", "<enq>", "<ack>", "<bell>",
      "\b", "\t", "\n", "<vt>", "<ff>", "\r", "<so>", "<si>", "<dle>", "<dc1>", "<dc2>", "<dc3>", "<dc4>", "<nak>",
      "<syn>", "<etb>", "<can>", "<em>", "<sub>", "<esc>", "<fs>", "<gs>", "<rs>", "<us>", " ", "!", "\"", "#", "$",
      "%", "&", "'", "(", ")", "*", "+", ",", "-", ".", "/", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":",
      ";", "<", "=", ">", "?", "@", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
      "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[", "\\", "]", "^", "_", "`", "a", "b", "c", "d", "e", "f",
      "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|",
      "}", "~", "<del>" };

  /**
   * Denotes the mapping for C0 characters.
   */
  private static Object C0_CHARS[][] = { { 0, "nul" }, //
      { 0, "soh" }, //
      { 0, "stx" }, //
      { 0, "etx" }, //
      { 0, "eot" }, //
      { 0, "enq" }, //
      { 0, "ack" }, //
      { 0, "bel" }, //
      { '\b', "bs" }, //
      { '\t', "ht" }, //
      { '\n', "lf" }, //
      { 0, "vt" }, //
      { 0, "ff" }, //
      { '\r', "cr" }, //
      { 0, "so" }, //
      { 0, "si" }, //
      { 0, "dle" }, //
      { 0, "dc1" }, //
      { 0, "dc2" }, //
      { 0, "dc3" }, //
      { 0, "dc4" }, //
      { 0, "nak" }, //
      { 0, "syn" }, //
      { 0, "etb" }, //
      { 0, "can" }, //
      { 0, "em" }, //
      { 0, "sub" }, //
      { 0, "esq" }, //
      { 0, "fs" }, //
      { 0, "gs" }, //
      { 0, "rs" }, //
      { 0, "us" } };

  /**
   * Denotes the mapping for C1 characters.
   */
  private static Object C1_CHARS[][] = { { 0, null }, //
      { 0, null }, //
      { 0, null }, //
      { 0, null }, //
      { 0, "ind" }, //
      { 0, "nel" }, //
      { 0, "ssa" }, //
      { 0, "esa" }, //
      { 0, "hts" }, //
      { 0, "htj" }, //
      { 0, "vts" }, //
      { 0, "pld" }, //
      { 0, "plu" }, //
      { 0, "ri" }, //
      { 0, "ss2" }, //
      { 0, "ss3" }, //
      { 0, "dcs" }, //
      { 0, "pu1" }, //
      { 0, "pu2" }, //
      { 0, "sts" }, //
      { 0, "cch" }, //
      { 0, "mw" }, //
      { 0, "spa" }, //
      { 0, "epa" }, //
      { 0, null }, //
      { 0, null }, //
      { 0, null }, //
      { 0, "csi" }, //
      { 0, "st" }, //
      { 0, "osc" }, //
      { 0, "pm" }, //
      { 0, "apc" } };

  /**
   * The DEC special characters (only the last 32 characters).
   */
  private static Object DEC_SPECIAL_CHARS[][] = { { '\u25c6', null }, // black_diamond
      { '\u2592', null }, // Medium Shade
      { '\u2409', null }, // Horizontal tab (HT)
      { '\u240c', null }, // Form Feed (FF)
      { '\u240d', null }, // Carriage Return (CR)
      { '\u240a', null }, // Line Feed (LF)
      { '\u00b0', null }, // Degree sign
      { '\u00b1', null }, // Plus/minus sign
      { '\u2424', null }, // New Line (NL)
      { '\u240b', null }, // Vertical Tab (VT)
      { '\u2518', null }, // Forms light up and left
      { '\u2510', null }, // Forms light down and left
      { '\u250c', null }, // Forms light down and right
      { '\u2514', null }, // Forms light up and right
      { '\u253c', null }, // Forms light vertical and horizontal
      { '\u23ba', null }, // Scan 1
      { '\u23bb', null }, // Scan 3
      { '\u2500', null }, // Scan 5 / Horizontal bar
      { '\u23bc', null }, // Scan 7
      { '\u23bd', null }, // Scan 9
      { '\u251c', null }, // Forms light vertical and right
      { '\u2524', null }, // Forms light vertical and left
      { '\u2534', null }, // Forms light up and horizontal
      { '\u252c', null }, // Forms light down and horizontal
      { '\u2502', null }, // vertical bar
      { '\u2264', null }, // less than or equal sign
      { '\u2265', null }, // greater than or equal sign
      { '\u03c0', null }, // pi
      { '\u2260', null }, // not equal sign
      { '\u00a3', null }, // pound sign
      { '\u00b7', null }, // middle dot
      { ' ', null }, //
  };

  // CONSTRUCTORS

  /**
   * Creates a new {@link CharacterSets} instance, never used.
   */
  private CharacterSets()
  {
    // Nop
  }

  // METHODS

  /**
   * Returns the character mapping for a given original value using the given
   * graphic sets GL and GR.
   * 
   * @param aOriginal
   *          the original character to map;
   * @param aGl
   *          the GL graphic set, cannot be <code>null</code>;
   * @param aGr
   *          the GR graphic set, cannot be <code>null</code>.
   * @return the mapped character.
   */
  public static char getChar( char aOriginal, GraphicSet aGl, GraphicSet aGr )
  {
    Object[] mapping = getMapping( aOriginal, aGl, aGr );

    int ch = ( ( Integer )mapping[0] ).intValue();
    if ( ch > 0 )
    {
      return ( char )ch;
    }

    return ' ';
  }

  /**
   * @param aOriginal
   * @param aGl
   * @param aGr
   * @return
   */
  public static String getCharName( char aOriginal, GraphicSet aGl, GraphicSet aGr )
  {
    Object[] mapping = getMapping( aOriginal, aGl, aGr );

    String name = ( String )mapping[1];
    if ( name == null )
    {
      name = String.format( "<%d>", ( int )aOriginal );
    }

    return name;
  }

  /**
   * @param aOriginal
   * @param aGl
   * @param aGr
   * @return
   */
  private static Object[] getMapping( char aOriginal, GraphicSet aGl, GraphicSet aGr )
  {
    int mappedChar = aOriginal;
    if ( aOriginal >= C0_START && aOriginal <= C0_END )
    {
      int idx = aOriginal - C0_START;
      return C0_CHARS[idx];
    }
    else if ( aOriginal >= C1_START && aOriginal <= C1_END )
    {
      int idx = aOriginal - C1_START;
      return C1_CHARS[idx];
    }
    else if ( aOriginal >= GL_START && aOriginal <= GL_END )
    {
      int idx = aOriginal - GL_START;
      mappedChar = aGl.map( aOriginal, idx );
    }
    else if ( aOriginal >= GR_START && aOriginal <= GR_END )
    {
      int idx = aOriginal - GR_START;
      mappedChar = aGr.map( aOriginal, idx );
    }

    return new Object[] { mappedChar, null };
  }
}
