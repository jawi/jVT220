/**
 * jVT220 - Java VT220 terminal emulator.
 *
 * (C) Copyright 2012 - J.W. Janssen, <j.w.janssen@lxtreme.nl>.
 */
package nl.lxtreme.test;


import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import jpty.*;
import nl.lxtreme.jvt220.terminal.*;
import nl.lxtreme.jvt220.terminal.swing.*;
import nl.lxtreme.jvt220.terminal.vt220.*;
import purejavacomm.*;


/**
 * Provides a test console.
 */
public class TestConsole
{
  // CONSTANTS

  // private static final String[] CMD = { "/Users/jawi/bin/vttest", "-s" };
  private static final String[] CMD = { "/bin/bash", "-l" };

  // VARIABLES

  private SwingFrontend frontend;
  private JFrame frame;

  private volatile Pty process;
  private volatile SerialPort port;

  // CONSTRUCTORS

  /**
   * Creates a new {@link TestConsole} instance.
   */
  public TestConsole()
  {
  }

  // METHODS

  /**
   * MAIN ENTRY POINT
   * 
   * @param aArgs
   *          the command line arguments.
   */
  public static void main( String[] aArgs ) throws Exception
  {
    final TestConsole console = new TestConsole();

    SwingUtilities.invokeLater( new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          console.initUI();

          console.runConsole( true /* aLocal */, false /* aPlain */);

          console.runUI();
        }
        catch ( Exception e )
        {
          System.err.println( "Console failed!\n" + e );
          e.printStackTrace();
        }
      }
    } );

    do
    {
      Thread.sleep( 10 );
    }
    while ( console.process == null );

    System.out.println( "Process alive ..." );

    int result = console.process.waitFor();

    System.out.printf( "Process stopped: [%d]%n", result );

    Thread.sleep( 1000 );

    console.cleanup();
  }

  /**
   * Cleans up.
   */
  void cleanup()
  {
    try
    {
      try
      {
        this.frontend.disconnect();
      }
      catch ( IOException e1 )
      {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      if ( this.process != null )
      {
        System.out.println( "Ending process ..." );

        try
        {
          this.process.close();
        }
        catch ( Exception e )
        {
          e.printStackTrace();
        }
      }
      else if ( this.port != null )
      {
        System.out.println( "Closing port ..." );

        try
        {
          this.port.getOutputStream().close();
          this.port.getInputStream().close();
        }
        catch ( IOException exception )
        {
          exception.printStackTrace();
        }

        this.port.close();
      }
    }
    finally
    {
      this.frame.setVisible( false );
      this.frame.dispose();
    }
  }

  /**
   * Initializes this UI.
   */
  void initUI()
  {
    this.frontend = new SwingFrontend();
    this.frontend.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );

    this.frame = new JFrame( "Test console" );
    this.frame.setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
    this.frame.addWindowListener( new WindowAdapter()
    {
      @Override
      public void windowClosing( WindowEvent aEvent )
      {
        cleanup();
      }
    } );

    this.frontend.addComponentListener( new ComponentAdapter()
    {
      @Override
      public void componentResized( ComponentEvent aEvent )
      {
        resizeFrameToFitContent();
      }
    } );

    this.frame.setContentPane( this.frontend );
    this.frame.pack();
  }

  /**
   * Resizes the frame to fix its contents. When the frame is only partially
   * visible after resizing, it will be moved to make most of it visible.
   */
  void resizeFrameToFitContent()
  {
    final Dimension frontendSize = this.frontend.getSize();
    final Insets frameInsets = this.frame.getInsets();

    int width = frameInsets.left + frameInsets.right + frontendSize.width;
    int height = frameInsets.top + frameInsets.bottom + frontendSize.height;

    this.frame.setSize( width, height );

    Rectangle screenBounds = this.frame.getGraphicsConfiguration().getBounds();

    Rectangle frameBounds = this.frame.getBounds();
    if ( frameBounds.x + frameBounds.width > screenBounds.width )
    {
      frameBounds.x = screenBounds.x;
    }
    if ( frameBounds.y + frameBounds.height > screenBounds.height )
    {
      frameBounds.y = screenBounds.y;
    }
    this.frame.setBounds( frameBounds );
  }

  /**
   * Runs the main console.
   */
  void runConsole( boolean aLocal, boolean aPlain ) throws Exception
  {
    OutputStream os;
    InputStream is;

    if ( aLocal )
    {
      this.process = JPty.execInPTY( CMD[0], CMD, new String[] { "TERM=vt220", "USER=jawi", "HOME=/Users/jawi" } );

      os = this.process.getOutputStream();
      is = this.process.getInputStream();
    }
    else
    {
      CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier( "/dev/tty.usbserial" );

      this.port = ( SerialPort )portId.open( "TestConsole", 1000 );
      this.port.setSerialPortParams( 115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE );
      this.port.setInputBufferSize( 4096 );
      this.port.setFlowControlMode( SerialPort.FLOWCONTROL_RTSCTS_IN );

      is = this.port.getInputStream();
      os = this.port.getOutputStream();
    }

    ITerminal term;
    if ( !aPlain )
    {
      term = new VT220Terminal( 80, 24 );
    }
    else
    {
      term = new PlainTerminal( 80, 24 );
    }

    this.frontend.connect( is, os );
    this.frontend.setTerminal( term );
  }

  /**
   * Makes the UI visible.
   */
  void runUI()
  {
    this.frame.setVisible( true );
  }
}
