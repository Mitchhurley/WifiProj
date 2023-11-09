package wifi;
import java.io.PrintWriter;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface 
{
    private RF theRF;           // You'll need one of these eventually
    private short ourMAC;       // Our MAC address
    private PrintWriter output; // The output stream we'll write to

    /**
     * Constructor takes a MAC address and the PrintWriter to which our output will
     * be written.
     * @param ourMAC  MAC address
     * @param output  Output stream associated with GUI
     */
    public LinkLayer(short ourMAC, PrintWriter output) {
        this.ourMAC = ourMAC;
        this.output = output;      
        theRF = new RF(null, null);
        output.println("LinkLayer: Constructor ran.");
    }

    /**
     * Send method takes a destination, a buffer (array) of data, and the number
     * of bytes to send.  See docs for full description.
     */
    public int send(short dest, byte[] data, int len) {
        // Make a new writer object
        Writer myWriter = new Writer(theRF, output, ourMAC, dest, data, len);
        // Run it
        new Thread(myWriter).start();
        return len;
    }

    /**
     * Recv method blocks until data arrives, then writes it an address info into
     * the Transmission object.  See docs for full description.
     */
    
    public int recv(Transmission t) {
        // Create the Reader thread
        Reader myReader = new Reader(theRF, output, t);
        
        // Start the Reader thread
        Thread readerThread = new Thread(myReader);
        readerThread.start();
        
        try {
            // Wait for the Reader thread to finish
            readerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return t.getBuf().length;
        // return 0;
    }

    /**
     * Returns a current status code.  See docs for full description.
     */
    public int status() {
        output.println("LinkLayer: Faking a status() return value of 0");
        return 0;
    }

    /**
     * Passes command info to your link layer.  See docs for full description.
     */
    public int command(int cmd, int val) {
        output.println("LinkLayer: Sending command "+cmd+" with value "+val);
        return 0;
    }
}
