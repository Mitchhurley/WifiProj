package wifi;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @author richards
 */
public class LinkLayer implements Dot11Interface 
{
    private RF theRF;           // You'll need one of these eventually
    private ArrayBlockingQueue<Frame> sendQueue;
    private ArrayBlockingQueue<Frame> ackQueue;
    private short ourMAC;       // Our MAC address
    private PrintWriter output; // The output stream we'll write to

    private Writer writer;
    private Reader reader;

    /**
     * Constructor takes a MAC address and the PrintWriter to which our output will
     * be written.
     * @param ourMAC  MAC address
     * @param output  Output stream associated with GUI
     */
    public LinkLayer(short ourMAC, PrintWriter output) {
        // Initialize stuff
        theRF = new RF(null, null);
        sendQueue = new ArrayBlockingQueue<Frame>(10);
        ackQueue = new ArrayBlockingQueue<Frame>(10);
        this.ourMAC = ourMAC;
        this.output = output;
        // Initialize threads
        writer = new Writer(theRF, sendQueue, ackQueue, ourMAC, output);
        reader = new Reader(theRF, sendQueue, ackQueue, ourMAC, output);
        // start threads
        Thread writerThread = new Thread(writer);
        Thread readerThread = new Thread(reader);
        writerThread.start();
        readerThread.start();
        
        output.println("LinkLayer: Constructor ran.");
    }

    /**
     * Send method takes a destination, a buffer (array) of data, and the number
     * of bytes to send.  See docs for full description.
     */
    public int send(short dest, byte[] data, int len) {
        // Make a new writer object

        // Run it

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
