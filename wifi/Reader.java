package wifi;
import rf.RF;
import java.io.PrintWriter;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Uses the simulated RF layer to listen for messages and store them in the transmission
 *
 * @author Jonah Kelsey
 * @version 1.0 11/6/23
 */
public class Reader implements Runnable
{
    private RF theRF;
    private ArrayBlockingQueue<Frame> ackQueue;
    private ArrayBlockingQueue<Frame> sendQueue;
    private PrintWriter output;
    private short ourMAC;
    private Transmission t;

    public Reader(RF theRF, ArrayBlockingQueue<Frame> sendQueue, ArrayBlockingQueue<Frame> ackQueue, PrintWriter output, short ourMAC, Transmission t){
        this.theRF = theRF;
        this.sendQueue = sendQueue;
        this.ackQueue = ackQueue;
        this.ourMAC = ourMAC;
        this.output = output;
        this.t = t;
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
                    //TODO validate seq num

                } else if (incomingFrame.frameType == 1) { // it is an ACK, queue it for the writer thread to see

                } else if (incomingFrame.frameType == 1) { // it is a beacon

                } else if (incomingFrame.frameType == 1) {
                    
                } else if (incomingFrame.frameType == 1) {
                    
                }
            }
        }
    }
    
    private void dataReceived(Frame incomingFrame) {
        
    }
}
