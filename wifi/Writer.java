package wifi;
import rf.RF;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A thread that creates and sends an 802.11 frame with the specified data
 *
 * @author Jonah Kelsey
 * @version 1.0 11/6/23
 */
public class Writer implements Runnable
{
    private RF theRF;
    private ArrayBlockingQueue<Frame> sendQueue;
    private ArrayBlockingQueue<Frame> ackQueue;
    private short ourMAC;
    private PrintWriter output;

    // Some timings
    final int SIFS = 100;
    final int SLOT = 200;
    boolean retransmit;
    final int DIFS = 20000;
    final int defaultTimeout = 2000;


    // Represents the state of our sender thread
    private String state;
    // How many retries we have done on current transmission
    private int retries;
    // Our current frame to be transmitted
    private Frame currentFrame;
    // Window properties
    private int collisionWindow;
    // Slot number
    private int slotNum = 0;

    public Writer(RF theRF, ArrayBlockingQueue<Frame> sendQueue, ArrayBlockingQueue<Frame> ackQueue, short ourMAC, PrintWriter output){
        this.theRF = theRF;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.ourMAC = ourMAC;
        this.output = output;

        state = "Await data";
        retransmit = false;
        retries = 0;
        currentFrame = null;
        collisionWindow = theRF.aCWmin;
    }

    //TODO finish
    @Override
    public void run(){
        // Runs forever
        while (true) {
            // Test what state we are in
            if (state == "Await data") {
                retries = 0;
                // Block until there is a message on queue, and then take it and send
                try
                {
                    currentFrame = sendQueue.take();
                }
                catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                }

                // if the RF is not in use move to idle wait
                if (!theRF.inUse()) {
                    state = "Idle DIFS wait";
                } else { // if the RF is in use move to busy wait
                    state = "Busy DIFS wait";
                }
            } else if (state == "Idle DIFS wait") {
                // We do our wait
                try {
                    Thread.sleep(DIFS);
                }
                catch (InterruptedException ex) {}
                // Check if the channel is idle
                if (theRF.inUse()) { // if it was in use move to busy wait
                    state = "Busy DIFS wait";
                } else { // if not transmit and await ack
                    // TODO transmit
                    state = "Await ACK";
                }
            } else if (state == "Busy DIFS wait") {
                // wait for idle
                while (theRF.inUse()) {
                    // wait till free
                }
                // move to slot wait
                state = "Slot wait";
            } else if (state == "Slot wait") {
                // wait for our number of slots
                // TODO maybe not accurate? need to see if channel was idle while waiting?
                for (int i = 0; i < slotNum; i++) {
                    try {
                        Thread.sleep(SLOT);
                    }
                    catch (InterruptedException ex) {}
                }
                // Check channel
                if (theRF.inUse()) { // if busy 
                    state = "Busy DIFS wait";
                } else { // if not transmit and await ack
                    // TODO transmit
                    state = "Await ACK";
                }
            } else if (state == "Await ACK") {
                // wait for timeout or ack arrival
                int timeout = defaultTimeout;
                while (ackQueue.peek() == null && timeout > 0) {
                    try {
                        Thread.sleep(20L);
                    }
                    catch (InterruptedException ex) {}
                }
                // Check ack queue
                if (ackQueue.peek() != null) {
                    // if we have an ack, remove from queue and reset
                    collisionWindow = theRF.aCWmin;
                    retries = 0;
                    try
                    {
                        ackQueue.take();
                    }
                    catch (InterruptedException ie)
                    {
                        ie.printStackTrace();
                    }
                    state = "Await data";
                } else {
                    // check if we have reached rery limit
                    if (retries >= theRF.dot11RetryLimit) {
                        // if we have reached the limit, give up
                        retries = 0;
                        // TODO cw?
                        state = "Await data";
                    }
                    retries++;
                    
                    // go to busy difs wait
                    state = "Busy DIFS wait";
                }
            }

        }
    }

    private int transmit(Frame outgoingFrame) {
        return 0;
    }
}
