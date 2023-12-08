package wifi;
import rf.RF;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * A thread that creates and sends an 802.11 frame with the specified data
 *
 * @author Jonah Kelsey & Mitchell Hurley
 * @version 1.3.1 11/17/23
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
    final int DIFS = 2000;
    final int defaultTimeout = 2000;

    // Represents the state of our sender thread
    private String state;
    // How many retries we have done on current transmission
    private int retries;
    // Our current frame to be transmitted
    private Frame currentFrame;
    // Window properties
    private volatile boolean useMaxCw = true;
    private int contentionWindow;
    private int slotCount;
    // Beacon interval
    private volatile int beaconInterval = 20000;
    private long nextBeaconTime;
    private final int beaconDelay = 0;
    // Debug output toggle
    private volatile boolean outputDebug = true;
    // clock offset
    private ClockOffsetManager offsetManager;

    public Writer(RF theRF, ArrayBlockingQueue<Frame> sendQueue, ArrayBlockingQueue<Frame> ackQueue, short ourMAC, PrintWriter output, ClockOffsetManager offsetManager){
        this.theRF = theRF;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.ourMAC = ourMAC;
        this.output = output;
        this.offsetManager = offsetManager;

        state = "Await data";
        retransmit = false;
        retries = 0;
        slotCount = 0;
        Frame currentFrame = null;
        contentionWindow = RF.aCWmin;

        //initial reset of beacon
        resetBeacon();
        //output.println("Writer: Writer constructor ran.");
    }

    //TODO finish
    @Override
    public void run(){
        // Runs forever
        while (true) {
            // Test what state we are in
            if (state == "Await data") {
                printDebug("awaiting data.\n");
                printDebug("Time: " + theRF.clock());
                retries = 0;
                // TODO: not sure if this is safe
                currentFrame = null;
                // our idle state, this while loop ensures we wait until it is time to send a beacon or we have recieved data
                while (currentFrame == null) {
                    if (timeToSendBeacon()) {
                        //if we do, send that
                        currentFrame = buildBeacon();
                        resetBeacon();
                        printDebug("Time to send beacon frame!");
                    } else if (sendQueue.peek() != null) {
                        printDebug("Time to send data frame!");
                        // if we don't, take frame from queue
                        // Block until there is a message on queue, and then take it and send
                        try
                        {
                            currentFrame = sendQueue.take();
                        }
                        catch (InterruptedException ie)
                        {
                            ie.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {}
                }

                //If its an ACK, just wait a little and send

                // if the RF is not in use move to idle wait
                if (!theRF.inUse()) {
                    printDebug("channel free, moving to idle DIFS wait");
                    state = "Idle DIFS wait";
                } else { // if the RF is in use move to busy wait
                    printDebug("channel busy, moving to busy DIFS wait");
                    state = "Busy DIFS wait";
                }
            } else if (state == "Idle DIFS wait") {
                if(currentFrame.frameType == 1) {
                    printDebug("Writer found ACK, attempting to send.\n");
                    try {
                        Thread.sleep(SIFS);
                        transmit(currentFrame);
                        printDebug("Writer sending ACK,Dest is " + currentFrame.destAddr);
                        state = "Await ACK";
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }else {
                    // We do our wait
                    try {
                        Thread.sleep(DIFS);
                    }
                    catch (InterruptedException ex) {}
                    // Check if the channel is idle
                    if (theRF.inUse()) { // if it was in use move to busy wait
                        state = "Busy DIFS wait";
                    } else { // if not transmit and await ack
                        //  transmit

                        int sent = transmit(currentFrame);
                        printDebug("Writer Done with wait and transmitting data, sent "+ sent+" bytes\n");
                        // do not go to await ack if its to broadcast
                        if (currentFrame.destAddr == -1) {
                            printDebug("general broadcast, skipping ack");
                            state = "Await data";
                        }else {
                            printDebug("Sent, waiting for ack");
                            state = "Await ACK";
                        }
                    }
                }
            } else if (state == "Busy DIFS wait") {
                // wait for idle
                //TODO check if this a retransmisssion
                while (theRF.inUse()) {
                    // wait till free
                }
                // move to slot wait
                state = "Slot wait";
            } else if (state == "Slot wait") {
                // wait for our number of slots
                int backoffTime = (int) (Math.random() * contentionWindow * SLOT);
                try {
                    Thread.sleep(backoffTime);
                } catch (InterruptedException ex) {

                }
                // Check channel
                if (theRF.inUse()) { // if busy 
                    state = "Busy DIFS wait";
                } else { // if not transmit and await ack
                    transmit(currentFrame);
                    printDebug("Writer Done with slot wait and transmitting data.");
                    state = "Await ACK";
                }
            } else if (state == "Await ACK") {
                // first check if we even need to wait
                if (currentFrame.frameType != 1 && currentFrame.frameType != 2) {
                    // wait for timeout or ack arrival
                    int timeout = defaultTimeout;
                    while (ackQueue.peek() == null && timeout > 0) {
                        try {
                            Thread.sleep(20L);
                            timeout-= 20;
                        }
                        catch (InterruptedException ex) {}
                    }
                    // Check ack queue
                    if (ackQueue.peek() != null) {
                        // if we have an ack, remove from queue and reset
                        printDebug("Writer Found ACK and validating.");
                        contentionWindow = RF.aCWmin;
                        retries = 0;
                        try
                        {
                            ackQueue.take();
                        }
                        catch (InterruptedException ie)
                        {
                            ie.printStackTrace();
                        }
                        //TODO check ACK to confirm that our packet got through
                        state = "Await data";
                    } else {
                        // check if we have reached retry limit
                        if (retries >= RF.dot11RetryLimit) {
                            printDebug("Retry limit reached, packet has been abandoned");
                            // if we have reached the limit, give up
                            retries = 0;
                            // TODO contention window?
                            state = "Await data";
                        } else {
                            printDebug("No ACK found, attempting retry " + retries);
                            retries++;
                            currentFrame.retry = true;
                            //Double collision window w/o exceeding max
                            contentionWindow = Math.min(contentionWindow * 2, RF.aCWmax);
                            state = "Busy DIFS wait";
                        }
                    }
                }
            } else {
                //output.print("State Error: Current state is "+ state);
            }
            //Print statements for debugging
            //printDebug("Current State: " + state);
            //printDebug("Retransmit: " + retransmit);
            //printDebug("Retries: " + retries);
            //printDebug("Contentions Window: " + contentionWindow);
            //printDebug("Slot Count: " + slotCount + "\n");
            if (currentFrame != null) {
                //printDebug("Current Frame - Frame Type: " + currentFrame.frameType +
                //    ", SeqNum: " + currentFrame.seqNum +
                //    ", DestAddr: " + currentFrame.destAddr +
                //    ", SrcAddr: " + currentFrame.srcAddr +
                //    ", Data Length: " + currentFrame.data.length);
            }

        }
    }

    private int transmit(Frame outgoingFrame) {
        // Convert the Frame to a byte array
        byte[] data = outgoingFrame.toByteArray();

        //output.println("Writer: Transmitting Frame - Frame Type: " + outgoingFrame.frameType +
        //        ", SeqNum: " + outgoingFrame.seqNum +
        //        ", DestAddr: " + outgoingFrame.destAddr +
        //        ", SrcAddr: " + outgoingFrame.srcAddr +
        //        ", Data Length: " + outgoingFrame.data.length);
        // Transmit the data using the RF instance
        int bytesSent = theRF.transmit(data);

        // Perform any additional actions based on the result, e.g., update statistics
        printDebug("frame sent");

        return bytesSent;
    }

    public void setUseMaxCw(boolean useMaxCw) {
        this.useMaxCw = useMaxCw;
    }

    public boolean getUseMaxCw() {
        return this.useMaxCw;
    }
    // sets the beacon interval
    public void setBeaconInterval(int beaconInterval) {
        this.beaconInterval = beaconInterval;
    }
    // gets the beacon interval
    public int getBeaconInterval() {
        return this.beaconInterval;
    }

    private boolean timeToSendBeacon() {
        return theRF.clock() > nextBeaconTime;
    }

    private void resetBeacon() {
        nextBeaconTime = theRF.clock() + beaconInterval;
    }

    // TODO: not sure how seq num factors in
    private Frame buildBeacon() {
        byte[] timeStamp = new byte[8];
        long time = theRF.clock() + beaconDelay;

        // Manually convert the 'time' long value to a byte array
        for (int i = 0; i < 8; ++i) {
            timeStamp[i] = (byte) (time >> (i * 8)); // Shift and extract each byte
        }

        return new Frame(2, (short) 0, (short) -1, ourMAC, timeStamp);
    }

    private void printDebug(String message) {
        if (outputDebug) {
            output.println("Writer: " + message);
            output.flush();
        }
    }

    public void setDebug(boolean outputDebug) {
        this.outputDebug = outputDebug;
    }

    public boolean getDebug() {
        return this.outputDebug;
    }
}
