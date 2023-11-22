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
    public Reader(RF theRF, ArrayBlockingQueue<Frame> sendQueue, ArrayBlockingQueue<Frame> ackQueue, PrintWriter output, short ourMAC,
    		ArrayBlockingQueue<Transmission> incQueue){
        this.theRF = theRF;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.incQueue = incQueue;
        this.ourMAC = ourMAC;
        this.output = output;
        this.seqNumMap = new HashMap<Short, Integer>();
        //output.println("Reader: Reader constructor ran.");
    }

    @Override
    public void run(){
        // run forever
        //output.println("Reader: Reader constructor ran.");

        while (true) {
        	byte[] incomingBytes = theRF.receive();
            // create frame obj with it
            Frame incomingFrame = new Frame(incomingBytes);
//            Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
//            incQueue.offer(t);

            // validate it
            output.println("Reader: Received Frame - Frame Type: " + incomingFrame.frameType +
                    ", SeqNum: " + incomingFrame.seqNum +
                    ", DestAddr: " + incomingFrame.destAddr +
                    ", SrcAddr: " + incomingFrame.srcAddr +
                    ", Data Length: " + incomingFrame.data.length);
            output.flush();
            if (true) { //incomingFrame.validateChecksum()
                // Now check what type of message it is
            	output.println("Reader: Received Frame,Checking Type");
                if (incomingFrame.frameType == 0) { // it is data. send to the layer above and send ack
                	output.println("Reader: Received Frame of normal transmission");
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
//                			output.print("CANNOT ADD ACK TO QUEUE");
                		}
                		addedACK = ackQueue.offer(incomingFrame);
                	}
                } else if (incomingFrame.frameType == 3) { // it is a beacon
                	//TODO do not send ack back

                } else if (incomingFrame.frameType == 1) {

                } else if (incomingFrame.frameType == 1) {

                }
            }  
//            else {
//                output.print("Failed Checksum");
//            }
            output.println("Reader: Got to end ,Going back");
        }
    }

    private void dataReceived(Frame incomingFrame) {
        // Validate sequence number
        // TODO: Implement sequence number validation
        output.print("Reader:Data Recieved");
        // Check if the destination MAC address matches our MAC address
        if (incomingFrame.destAddr == ourMAC) {
            // Send data to the layer above
        	//TODO maybe implement a removal from the dict if the size of transmission is less than max?
            //See if we've received a transmission from somewhere before
        	output.print("Reader:Correct Mac");
        	if (seqNumMap.containsKey(incomingFrame.srcAddr)) {
        		if (incomingFrame.seqNum == seqNumMap.get(incomingFrame.srcAddr)) {
        			Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
    	        	incQueue.offer(t);
    	            // Send ACK back
    	            sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
    	            //update expected seq num
    	        	seqNumMap.put(incomingFrame.srcAddr, incomingFrame.seqNum + 1);
        		}else {
        			output.print("Gap detected in Sequence Numbers from Address " + incomingFrame.srcAddr
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
        			output.print("Gap detected in Sequence Numbers from Address " + incomingFrame.srcAddr
        					+ "\tExpected " + seqNumMap.get(incomingFrame.srcAddr) + " and got "+incomingFrame.seqNum);
        			Transmission t = new Transmission(incomingFrame.srcAddr,incomingFrame.destAddr, incomingFrame.data);
        			incQueue.offer(t);
    	            sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
        		}
        		}
        //Broadcast address
        } else if (incomingFrame.destAddr == 65535 || incomingFrame.destAddr == -1){
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
}
