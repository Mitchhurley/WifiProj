package wifi;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Dictionary;
import java.util.Hashtable;

import rf.RF;

/**
 * Use this layer as a starting point for your project code.  See {@link Dot11Interface} for more
 * details on these routines.
 * @authors Mitchell Hurley & Jonah Kelsey
 * @version 1.3.1 11/17/23
 */
public class LinkLayer implements Dot11Interface 
{
    private RF theRF;           // You'll need one of these eventually
    private ArrayBlockingQueue<Frame> sendQueue;
    private ArrayBlockingQueue<Frame> ackQueue;
    private ArrayBlockingQueue<Transmission> incQueue;
    private short ourMAC;       // Our MAC addrescs
    private PrintWriter output; // The output stream we'll write to
    private Dictionary<Short, Short> sequenceDict;

    // TODO: neither of these are fully working. I can finish them
    private int status; // status code for our link layer
    private int debug; // set to 0 if we want no debug output, 1 if we want all debug

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
        incQueue = new ArrayBlockingQueue<Transmission>(10);
        sequenceDict = new Hashtable<Short,Short>();
        this.ourMAC = ourMAC;
        this.output = output;
        // Initialize threads
        writer = new Writer(theRF, sendQueue, ackQueue, ourMAC, output);
        reader = new Reader(theRF, sendQueue, ackQueue, output, ourMAC, incQueue);
        // start threads
        Thread writerThread = new Thread(writer);
        Thread readerThread = new Thread(reader);
        writerThread.start();
        readerThread.start();

        //output.println("LinkLayer: Constructor ran.");
    }

    /**
     * Send method takes a destination, a buffer (array) of data, and the number
     * of bytes to send.  See docs for full description.
     */
    public int send(short dest, byte[] data, int len) {
        // check len of frame
        if (len > 2038) {
            // frame is too big
            status = 9;
            return 0;
        }
        // check if sendqueue is full
        if (sendQueue.size() >= 10) {
            status = 10;
            return 0;
        }
        short seq = 0;
        //Checking sequence numbers
        if (sequenceDict.get(dest) != null) {
        	
        	seq = sequenceDict.get(dest);
        	sequenceDict.put(dest, (short)(seq + 1));
        }else {
        	// if the dest hasn't been transmitted to yet, then we add a hashmap entry for the next time it is called
        	sequenceDict.put(dest, (short)1);
        }
        
        Frame outgoingFrame = new Frame(0, seq, dest, ourMAC, data);
        sendQueue.offer(outgoingFrame);
        
        status = 1;
        return 0;
    }

    /**
     * Recv method blocks until data arrives, then writes it an address info into
     * the Transmission object.  See docs for full description.
     */

    public int recv(Transmission t) {
        // Create the Reader thread
        output.println("LinkLayer: Attempting to recieve data");
        // Check If incQueue 
        while (true)
            while (incQueue.peek() != null) {
                try
                {
                    t = incQueue.take();
                    output.println("LinkLayer: Found Val in Incoming Queue");
                    // set status to good
                    status = 1;
                    //Figure out if its too big
                    return t.getBuf().length;
                }
                catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                    // set status to 
                    return -1;
                }

                //figure out max value
            }
    }

    /**
     * Returns a current status code.  See docs for full description.
     */
    public int status() {
        return status;
    }

    /**
     * Passes command info to your link layer.  See docs for full description.
     */
    public int command(int cmd, int val) {
        if (cmd == 0) {
            output.println("-------------- Commands and Settings -----------------");
            output.println("Cmd #0: Display command options and current settings");
            output.println("Cmd #1: Set debug level.  Currently at " + this.debug + "\n        Use 1 for full debug output, 0 for no output");
            output.println("Cmd #2: Set slot selection method.  Currently " + (writer.getUseMaxCw() ? "max" : "random") + "\n        Use 0 for random slot selection, any other value to use maxCW");
            output.println("Cmd #3: Set beacon interval.  Currently at " + writer.getBeaconInterval() + " seconds" + "\n        Value specifies seconds between the start of beacons; -1 disables");
            output.println("------------------------------------------------------");
            return 0;
        } else if (cmd == 1) {
            if (val == 0 || val == 1) {
                debug = val;
                output.println("Setting debug to " + val);
                return 0;
            } else {
                output.println("invalid argument for debug value, please use 1 for full debug output and 0 for no output");
                return -1;
            }
        } else if (cmd == 2) {
            if (val == 0) {
                writer.setUseMaxCw(false);
                output.println("Using a random Collision Window value");
                return 0;
            } else {
                writer.setUseMaxCw(true);
                output.println("Using the maximum Collision Window value");
                return 0;
            }
        } else if (cmd == 3) {
            if (val < 0) {
                writer.setBeaconInterval(0);
                output.println("Beacon frames will never be sent");
                return 0;
            } else {
                writer.setBeaconInterval(val);
                output.println("Beacon frames will be sent every " + val + " seconds");
                return 0;
            }
        }
        output.println("Command not recognized");
        return -1;
    }
}
