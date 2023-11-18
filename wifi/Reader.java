package wifi;
import rf.RF;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

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
    private short ourMAC;

    public Reader(RF theRF, ArrayBlockingQueue<Frame> sendQueue, ArrayBlockingQueue<Frame> ackQueue, PrintWriter output, short ourMAC, Transmission t){
        this.theRF = theRF;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.ourMAC = ourMAC;
        this.output = output;
    }

    @Override
    public void run(){
        // run forever
        while (true) {
            // listen for transmission
            byte[] incomingBytes = theRF.receive();
            // create frame obj with it
            Frame incomingFrame = new Frame(incomingBytes);
            // validate it
            if (incomingFrame.validateChecksum()) {
                // Now check what type of message it is
                if (incomingFrame.frameType == 0) { // it is data. send to the layer above and send ack
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
                			output.print("CANNOT ADD ACK TO QUEUE");
                		}
                		addedACK = ackQueue.offer(incomingFrame);
                	};                
                } else if (incomingFrame.frameType == 3) { // it is a beacon
                	//TODO do not send ack back

                } else if (incomingFrame.frameType == 1) {
                    
                } else if (incomingFrame.frameType == 1) {
                    
                }
            }  else {
                // TODO: handle invalid frame
            }
        }
    }
    
    private void dataReceived(Frame incomingFrame) {
        // Validate sequence number
        // TODO: Implement sequence number validation

        // Check if the destination MAC address matches our MAC address
        if (incomingFrame.destAddr == ourMAC) {
            // Send data to the layer above
            // TODO: Implement the logic to pass data to the layer above

            // Send ACK back
            sendQueue.offer(createAckFrame(incomingFrame.seqNum, incomingFrame.srcAddr));
        } else {
            // TODO: Handle the case where the destination MAC address doesn't match ours
        }
    }
    
    private Frame createAckFrame(int seqNum, short destAddr) {
        // Create an ACK frame with the specified sequence number and destination address
        return new Frame(1, (short) seqNum, destAddr, ourMAC, new byte[0]);
    }
}
