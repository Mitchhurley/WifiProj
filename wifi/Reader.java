package wifi;
import rf.RF;
import java.io.PrintWriter;

/**
 * Uses the simulated RF layer to listen for messages and store them in the transmission
 *
 * @author Jonah Kelsey
 * @version 1.0 11/6/23
 */
public class Reader implements Runnable
{
    private RF theRF;
    private Transmission t;
    private PrintWriter output;
    
    public Reader(RF theRF, PrintWriter output, Transmission t){
        this.theRF = theRF;
        this.output = output;
        this.t = t;
    }
    
    @Override
    public void run(){
        // Call receive and block until a transmission arrives
        output.println("Listening for a transmission...");
        byte[] receivedPacket = theRF.receive();
        
        // Extract dest and source adresses
        short destAddr = (short) ((receivedPacket[2] & 0xFF) | ((receivedPacket[3] & 0xFF) << 8));
        short sourceAddr = (short) ((receivedPacket[4] & 0xFF) | ((receivedPacket[5] & 0xFF) << 8));
        // Determine the position where the data payload starts in the frame
        int dataStartIndex = 6;
        int checkSumLength = 4;
        // Calculate the length of the data payload
        int dataLength = receivedPacket.length - dataStartIndex - checkSumLength;

        // Extract the data payload into a separate byte array from start of data to checksum
        byte[] data = new byte[dataLength];
        System.arraycopy(receivedPacket, dataStartIndex, data, 0, dataLength);
        
        // Set t to appropriate vals
        t.setBuf(data);
        t.setDestAddr(destAddr);
        t.setSourceAddr(sourceAddr);
        
        // exit
        return;
    }
}
