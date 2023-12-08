package wifi;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import rf.RF;
/**
 * Uses the simulated RF layer to listen for messages and store them in the transmission
 *
 * @author Jonah Kelsey & Mitchell Hurley
 * @version 1.3.1 11/17/23
 */
public class Reader implements Runnable
{
    private RF theRF;
    private ArrayBlockingQueue<Frame> ackQueue;
    private ArrayBlockingQueue<Frame> sendQueue;
    private ArrayBlockingQueue<Transmission> incQueue;
    private PrintWriter output;
    private HashMap<Short, Integer> seqNumMap;
    private short ourMAC;
    // Debug output toggle
    private volatile boolean outputDebug = true;
    // For beacon frames
    private final long processingTime = 0;
    private ClockOffsetManager offsetManager;
    
    public Reader(RF theRF, ArrayBlockingQueue<Frame> sendQueue, ArrayBlockingQueue<Frame> ackQueue, PrintWriter output, short ourMAC,
    ArrayBlockingQueue<Transmission> incQueue, ClockOffsetManager offsetManager){
        this.theRF = theRF;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.incQueue = incQueue;
        this.ourMAC = ourMAC;
        this.output = output;
        this.seqNumMap = new HashMap<Short, Integer>();
        this.offsetManager = offsetManager;
        //output.println("Reader: Reader constructor ran.");
    }

    @Override
    public void run(){
        // run forever
        //output.println("Reader: Reader constructor ran.");

        while (true) {
            printDebug("listening...");
            byte[] incomingBytes = theRF.receive();
            // create frame obj with it
            Frame incomingFrame = new Frame(incomingBytes);
            //            Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
            //            incQueue.offer(t);

            // validate it
            printDebug("Reader: Received Frame - Frame Type: " + incomingFrame.frameType +
                ", SeqNum: " + incomingFrame.seqNum +
                ", DestAddr: " + incomingFrame.destAddr +
                ", SrcAddr: " + incomingFrame.srcAddr +
                ", Data Length: " + incomingFrame.data.length);
            output.flush();
            if (true) { //incomingFrame.validateChecksum()
                // Now check what type of message it is
                printDebug("Reader: Received Frame,Checking Type");
                if (incomingFrame.frameType == 0) { // it is data. send to the layer above and send ack
                    printDebug("Reader: Received Frame of normal transmission");
                    dataReceived(incomingFrame);
                } else if (incomingFrame.frameType == 1) { // it is an ACK, queue it for the writer thread to see
                    boolean addedACK = ackQueue.offer(incomingFrame);
                    int retries = 0;
                    while (!addedACK) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if (retries++ > 10) {
                            //                            output.print("CANNOT ADD ACK TO QUEUE");
                        }
                        addedACK = ackQueue.offer(incomingFrame);
                    }
                } else if (incomingFrame.frameType == 2) { // it is a beacon
                    //TODO do not send ack back
                    printDebug("Received Beacon Frame");
                    byte[] timeStamp = incomingFrame.data;
                    //TODO: check some stuff about the incoming frame, discard if it is corrupted or not 8 bytes
                    if (timeStamp.length == 8) {
                        long receivedTime = bytesToLong(timeStamp);
                        long adjustedTime = receivedTime + processingTime;
                        if (adjustedTime > getCurrentTime()) {
                            printDebug("beacon frame is ahead, adjusting time by " + (adjustedTime - getCurrentTime()));
                            offsetManager.adjustClockOffset(adjustedTime - getCurrentTime());
                        } else {
                            printDebug("Beacon frame was not ahead");
                        }
                    } else {
                        printDebug("Beacon frame was invalid");
                    }

                } else if (incomingFrame.frameType == 1) {

                } else if (incomingFrame.frameType == 1) {

                }
            }  
            //            else {
            //                output.print("Failed Checksum");
            //            }
            printDebug("Reader: Got to end ,Going back");
        }
    }

    private void dataReceived(Frame incomingFrame) {
        // Validate sequence number
        // TODO: Implement sequence number validation
        printDebug("Reader:Data Recieved");
        // Check if the destination MAC address matches our MAC address
        if (incomingFrame.destAddr == ourMAC) {
            // Send data to the layer above
            //TODO maybe implement a removal from the dict if the size of transmission is less than max?
            //See if we've received a transmission from somewhere before
            printDebug("Reader:Correct Mac");
            if (seqNumMap.containsKey(incomingFrame.srcAddr)) {
                if (incomingFrame.seqNum == seqNumMap.get(incomingFrame.srcAddr)) {
                    Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
                    incQueue.offer(t);
                    // Send ACK back
                    sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
                    //update expected seq num
                    seqNumMap.put(incomingFrame.srcAddr, incomingFrame.seqNum + 1);
                }else {
                    printDebug("Gap detected in Sequence Numbers from Address " + incomingFrame.srcAddr
                        + "\tExpected " + seqNumMap.get(incomingFrame.srcAddr) + " and got "+incomingFrame.seqNum);
                    Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
                    incQueue.offer(t);
                    sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
                }
            }else {
                //TODO maybe make sure seq num is 0
                //TODO figure out how to deal with larger size packets
                if  (incomingFrame.seqNum == 0) {
                    Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
                    incQueue.offer(t);
                    // Send ACK back
                    sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
                    seqNumMap.put(incomingFrame.srcAddr, incomingFrame.seqNum + 1);
                }else {
                    printDebug("Gap detected in Sequence Numbers from Address " + incomingFrame.srcAddr
                        + "\tExpected " + seqNumMap.get(incomingFrame.srcAddr) + " and got "+incomingFrame.seqNum);
                    Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
                    incQueue.offer(t);
                    sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
                }
            }
            //Broadcast address
        } else if (incomingFrame.destAddr == -1){
            Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
            //output.print("Broadcast Address Transmission Recieved");
            incQueue.offer(t);
        }
        // If its here the destination MAC address doesn't match ours or broadcast

    }

    private Frame createAckFrame(int seqNum, short destAddr) {
        // Create an ACK frame with the specified sequence number and destination address
        return new Frame(1, (short) seqNum, destAddr, ourMAC, new byte[0]);
    }

    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8; ++i) {
            value |= ((long) (bytes[i] & 0xFF)) << (i * 8);
        }
        return value;
    }
    
    private long getCurrentTime() {
        return theRF.clock() + offsetManager.getClockOffset();
    }

    private void printDebug(String message) {
        if (outputDebug) {
            output.println("Reader: " + message);
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
