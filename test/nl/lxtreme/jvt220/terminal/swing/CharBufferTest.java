/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.jvt220.terminal.swing;


import java.util.concurrent.*;

import junit.framework.*;


/**
 * Test cases for {@link CharBuffer}.
 */
public class CharBufferTest extends TestCase
{
  // METHODS

  /**
   * Tests that calling {@link CharBuffer#append(java.util.List)} with a
   * non-null value succeeds.
   */
  public void testAppendNotNullOk()
  {
    CharBuffer charBuffer = new CharBuffer();
    charBuffer.append( 1, 2, 3 );

    for ( int i = 0; i < charBuffer.length(); i++ )
    {
      assertEquals( ( char )( i + 1 ), charBuffer.charAt( i ) );
    }
  }

  /**
   * Tests that calling {@link CharBuffer#append(java.util.List)} with a null
   * value fails.
   */
  public void testAppendNullFail()
  {
    try
    {
      new CharBuffer().append( ( Integer[] )null );
      fail( "Expected IllegalArgumentException!" );
    }
    catch ( IllegalArgumentException e )
    {
      // Ok; expected...
    }
  }

  /**
   * Tests that calling {@link CharBuffer#append(java.util.List)} with a
   * non-null value succeeds.
   */
  public void testAppendWillResizeInternalArrayOk()
  {
    CharBuffer charBuffer = new CharBuffer();
    for ( int i = 0; i < 100; i++ )
    {
      charBuffer.append( i + 1 );
    }

    for ( int i = 0; i < charBuffer.length(); i++ )
    {
      assertEquals( ( char )( i + 1 ), charBuffer.charAt( i ) );
    }
  }

  /**
   * Tests that we cannot call {@link CharBuffer#charAt(int)} on an empty
   * buffer.
   */
  public void testCharAtEmptyBufferFail()
  {
    try
    {
      new CharBuffer().charAt( 0 );
      fail( "Expected IndexOutOfBoundsException!" );
    }
    catch ( IndexOutOfBoundsException e )
    {
      // Ok; expected...
    }
  }

  /**
   * Tests that we cannot call {@link CharBuffer#charAt(int)} with index two
   * succeeds.
   */
  public void testCharAtIndexTwoOk()
  {
    assertEquals( ( char )3, new CharBuffer( 1, 2, 3 ).charAt( 2 ) );
  }

  /**
   * Tests that we cannot call {@link CharBuffer#charAt(int)} with index zero
   * succeeds.
   */
  public void testCharAtIndexZeroOk()
  {
    assertEquals( ( char )1, new CharBuffer( 1, 2, 3 ).charAt( 0 ) );
  }

  /**
   * Tests that we cannot call {@link CharBuffer#charAt(int)} with an index
   * beyond the end of the buffer fails.
   */
  public void testCharAtWithIndexBeyondLastPositionFail()
  {
    try
    {
      new CharBuffer( 1, 2, 3 ).charAt( 3 );
      fail( "Expected IndexOutOfBoundsException!" );
    }
    catch ( IndexOutOfBoundsException e )
    {
      // Ok; expected
    }
  }

  /**
   * Tests that we cannot call {@link CharBuffer#charAt(int)} with a negative
   * index fails.
   */
  public void testCharWithNegativeIndexFail()
  {
    try
    {
      new CharBuffer( 1, 2, 3 ).charAt( -1 );
      fail( "Expected IndexOutOfBoundsException!" );
    }
    catch ( IndexOutOfBoundsException e )
    {
      // Ok; expected
    }
  }

  /**
   * Tests that if two threads are concurrently appending and removing data,
   * that these appends occur atomically and do not fail.
   */
  public void testConcurrentAppendAndRemoveOk() throws Exception
  {
    final CountDownLatch startLatch = new CountDownLatch( 1 );
    final CountDownLatch stopLatch = new CountDownLatch( 2 );

    final CharBuffer cb = new CharBuffer();
    final int runCount = 10000;

    Thread t1 = new Thread()
    {
      @Override
      public void run()
      {
        Integer[] buffer = new Integer[] { 1, 2, 3, 4 };
        int rc = runCount;

        try
        {
          startLatch.await( 5L, TimeUnit.SECONDS );
        }
        catch ( InterruptedException exception )
        {
          throw new RuntimeException( "Await failed!" );
        }

        while ( rc-- > 0 )
        {
          cb.append( buffer );
        }

        stopLatch.countDown();
      }
    };

    Thread t2 = new Thread()
    {
      @Override
      public void run()
      {
        int rc = runCount;

        try
        {
          startLatch.await( 5L, TimeUnit.SECONDS );
        }
        catch ( InterruptedException exception )
        {
          throw new RuntimeException( "Await failed!" );
        }

        while ( rc-- > 0 )
        {
          if ( cb.length() > 4 )
          {
            cb.removeUntil( 4 );
          }
        }

        stopLatch.countDown();
      }
    };

    t1.start();
    t2.start();

    startLatch.countDown();

    stopLatch.await( 10L, TimeUnit.SECONDS );

    t1.join();
    t2.join();

    // Check that all appends & removes are performed atomically...
    for ( int i = 0; i < cb.length(); i += 4 )
    {
      for ( int j = 0; j < 4; j++ )
      {
        assertEquals( j + 1, cb.charAt( j + i ) );
      }
    }
  }

  /**
   * Tests that if two threads are concurrently appending data, that these
   * appends occur atomically and do not fail.
   */
  public void testConcurrentAppendsOk() throws Exception
  {
    final CountDownLatch startLatch = new CountDownLatch( 1 );
    final CountDownLatch stopLatch = new CountDownLatch( 2 );

    final CharBuffer cb = new CharBuffer();
    final int runCount = 10000;

    Thread t1 = new Thread()
    {
      @Override
      public void run()
      {
        Integer[] buffer = new Integer[] { 1, 2, 3, 4 };
        int rc = runCount;

        try
        {
          startLatch.await( 5L, TimeUnit.SECONDS );
        }
        catch ( InterruptedException exception )
        {
          throw new RuntimeException( "Await failed!" );
        }

        while ( rc-- > 0 )
        {
          cb.append( buffer );
        }

        stopLatch.countDown();
      }
    };

    Thread t2 = new Thread()
    {
      @Override
      public void run()
      {
        Integer[] buffer = new Integer[] { 5, 6, 7, 8 };
        int rc = runCount;

        try
        {
          startLatch.await( 5L, TimeUnit.SECONDS );
        }
        catch ( InterruptedException exception )
        {
          throw new RuntimeException( "Await failed!" );
        }

        while ( rc-- > 0 )
        {
          cb.append( buffer );
        }

        stopLatch.countDown();
      }
    };

    t1.start();
    t2.start();

    startLatch.countDown();

    stopLatch.await( 10L, TimeUnit.SECONDS );

    t1.join();
    t2.join();

    // Check that we've got 2 * 4 * runCount characters in our buffer...
    assertEquals( 8 * runCount, cb.length() );

    // Check that all appends are performed atomically...
    for ( int i = 0; i < cb.length(); i += 4 )
    {
      assertTrue( ( cb.charAt( i ) == 1 ) || ( cb.charAt( i ) == 5 ) );

      int offset = cb.charAt( i ) == 1 ? 1 : 5;
      for ( int j = i + 1; j < 4; j++ )
      {
        assertEquals( j + offset, cb.charAt( j ) );
      }
    }
  }

  /**
   * Tests that the length for an empty buffer is zero.
   */
  public void testLengthEmptyBufferOk()
  {
    assertEquals( 0, new CharBuffer().length() );
  }

  /**
   * Tests that the length for a non-empty buffer is correct.
   */
  public void testLengthOk()
  {
    assertEquals( 3, new CharBuffer( 1, 2, 3 ).length() );
  }

  /**
   * Tests that the length for buffer that is resized at least once is correct.
   */
  public void testLengthWithInternalResizedArrayOk()
  {
    CharBuffer cb = new CharBuffer();
    for ( int i = 0; i < 100; i++ )
    {
      cb.append( i );
    }
    assertEquals( 100, cb.length() );
  }

  /**
   * Tests that calling {@link CharBuffer#removeUntil(int)} with a negative
   * position fails.
   */
  public void testRemoveUntilWithNegativePositionFails()
  {
    try
    {
      new CharBuffer( 1, 2, 3 ).removeUntil( -1 );
      fail( "Expected IndexOutOfBoundsException!" );
    }
    catch ( IndexOutOfBoundsException e )
    {
      // Ok; expected...
    }
  }

  /**
   * Tests that calling {@link CharBuffer#removeUntil(int)} with a negative
   * position fails.
   */
  public void testRemoveUntilWithPositionBeyondLengthFails()
  {
    try
    {
      new CharBuffer( 1, 2, 3 ).removeUntil( 4 );
      fail( "Expected IndexOutOfBoundsException!" );
    }
    catch ( IndexOutOfBoundsException e )
    {
      // Ok; expected...
    }
  }

  /**
   * Tests that calling {@link CharBuffer#removeUntil(int)} with a position of
   * two yields an empty charbuffer.
   */
  public void testRemoveUntilWithPositionOneOk()
  {
    CharBuffer cb = new CharBuffer( 1, 2, 3 );
    cb.removeUntil( 1 );

    assertEquals( 2, cb.length() );
    for ( int i = 0; i < cb.length(); i++ )
    {
      assertEquals( ( char )( i + 2 ), cb.charAt( i ) );
    }
  }

  /**
   * Tests that calling {@link CharBuffer#removeUntil(int)} with a position of
   * two yields an empty charbuffer.
   */
  public void testRemoveUntilWithPositionThreeOk()
  {
    CharBuffer cb = new CharBuffer( 1, 2, 3 );
    cb.removeUntil( 3 );

    assertEquals( 0, cb.length() );
  }

  /**
   * Tests that calling {@link CharBuffer#removeUntil(int)} with a position of
   * two yields an empty charbuffer.
   */
  public void testRemoveUntilWithPositionTwoOk()
  {
    CharBuffer cb = new CharBuffer( 1, 2, 3 );
    cb.removeUntil( 2 );

    assertEquals( 1, cb.length() );
    assertEquals( ( char )3, cb.charAt( 0 ) );
  }

  /**
   * Tests that calling {@link CharBuffer#removeUntil(int)} with a position of
   * zero yields the same charbuffer.
   */
  public void testRemoveUntilWithPositionZeroOk()
  {
    CharBuffer cb = new CharBuffer( 1, 2, 3 );
    cb.removeUntil( 0 );

    assertEquals( 3, cb.length() );
    for ( int i = 0; i < 3; i++ )
    {
      assertEquals( ( char )( i + 1 ), cb.charAt( i ) );
    }
  }
}
