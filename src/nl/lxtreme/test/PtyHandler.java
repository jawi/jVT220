/**
 * 
 */
package nl.lxtreme.test;


import java.io.*;
import java.util.*;

import jpty.*;


/**
 * Provides a front end for {@link Pty}.
 */
public class PtyHandler implements Closeable
{
  // INNER TYPES

  /**
   * Provides a callback for when a number of bytes has been read from the PTY.
   */
  public static interface IPtyReadCallback
  {
    // METHODS

    /**
     * Called when a number of bytes has been read.
     * 
     * @param aBuffer
     *          the read buffer, never <code>null</code>.
     */
    void onRead( byte[] aBuffer );
  }

  // VARIABLES

  private final Pty pty;

  private volatile Thread readThread;

  // CONSTRUCTORS

  /**
   * Creates a new {@link PtyHandler} instance.
   */
  public PtyHandler( final Pty aPty )
  {
    this.pty = aPty;
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws IOException
  {
    try
    {
      this.readThread.interrupt();
      this.readThread.join( 5000 );
    }
    catch ( InterruptedException e )
    {
      // Ignore; we're stopping this thread anyway...
    }
    finally
    {
      this.readThread = null;
      this.pty.close();
    }
  }

  /**
   * Returns the output stream to write to the PTY.
   * 
   * @return a output stream, never <code>null</code>.
   */
  public OutputStream getOutputStream()
  {
    return this.pty.getOutputStream();
  }

  /**
   * Starts reading from the PTY calling the given callback when data has been
   * read from the PTY.
   * 
   * @param aCallback
   *          the callback to read, cannot be <code>null</code>.
   */
  public void start( final IPtyReadCallback aCallback )
  {
    if ( aCallback == null )
    {
      throw new IllegalArgumentException( "Callback cannot be null!" );
    }
    if ( ( this.readThread != null ) && this.readThread.isAlive() )
    {
      throw new IllegalStateException( "Handler is already started!" );
    }

    this.readThread = new Thread( new Runnable()
    {
      private final InputStream is = pty.getInputStream();
      private final byte[] buffer = new byte[1024];

      public void run()
      {
        try
        {
          while ( !Thread.currentThread().isInterrupted() )
          {
            int i = this.is.available();
            if ( i > 0 )
            {
              final int read = this.is.read( this.buffer, 0, i );
              aCallback.onRead( Arrays.copyOf( this.buffer, read ) );
            }
          }
        }
        catch ( IOException e )
        {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    } );
    this.readThread.start();
  }
}
