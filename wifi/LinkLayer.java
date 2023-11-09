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
    private short ourMAC;       // Our MAC address
    private PrintWriter output; // The output stream we'll write to
    private ArrayBlockingQueue<TransmissionData> outgoingQueue;
    private ArrayBlockingQueue<Transmission> incomingQueue;
    private Writer writer;
    private Reader reader;
    

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
        this.outgoingQueue = new ArrayBlockingQueue<TransmissionData>(10);
        this.incomingQueue = new ArrayBlockingQueue<Transmission>(10);
        this.writer = new Writer(outgoingQueue, theRF, output, ourMAC);
        this.reader = new Reader(incomingQueue, theRF, output);
        Thread writerThread = new Thread(writer);
        Thread readerThread = new Thread(reader);
        writerThread.start(); // Start the writer thread
        output.println("LinkLayer: Constructor ran.");
    }

    /**
     * Send method takes a destination, a buffer (array) of data, and the number
     * of bytes to send.  See docs for full description.
     */
    public int send(short dest, byte[] data, int len) {
    	
    	

    	TransmissionData transmissionData = new TransmissionData(dest, data, len);
        try {
            outgoingQueue.put(transmissionData);
            return len;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /**
     * Recv method blocks until data arrives, then writes it an address info into
     * the Transmission object.  See docs for full description.
     */
    
    public int recv(Transmission t) {
        // Create the Reader thread
        Reader myReader = new Reader(incomingQueue, theRF, output);
        
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
