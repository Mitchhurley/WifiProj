package wifi;
import rf.RF;
import java.io.PrintWriter;

/**
 * A thread that creates and sends an 802.11 frame with the specified data
 *
 * @author Jonah Kelsey
 * @version 1.0 11/6/23
 */
public class Writer implements Runnable
{
    private RF theRF;
    private PrintWriter output;
    private short ourMAC;
    private short dest;
    private byte[] data;
    private int len;
    
    public Writer(RF theRF, PrintWriter output, short ourMAC, short dest, byte[] data, int len){
        this.theRF = theRF;
        this.output = output;
        this.ourMAC = ourMAC;
        this.dest = dest;
        this.data = data;
        this.len = len;
    }
    
    @Override
    public void run(){
        // Print
        output.println("LinkLayer: Sending " + len + " bytes to " + dest);
        
        // First build the frame
        byte[] frame = new byte[len + 10]; // 16 bytes for MAC header
        frame[0] = 0x0; // set frame type, retry, and sequence to 0
        frame[1] = 0x0; // set frame type, retry, and sequence to 0
        frame[2] = (byte) dest; // Destination MAC address (lower byte)
        frame[3] = (byte) (dest >> 8); // Destination MAC address (upper byte)
        frame[4] = (byte) ourMAC; // Source MAC address (lower byte)
        frame[5] = (byte) (ourMAC >> 8); // Source MAC address (upper byte)
        
        // Copy the data into the frame
        System.arraycopy(data, 0, frame, 6, len);
        
        // set crc to -1
        frame[frame.length - 1] = (byte) 0xff;
        frame[frame.length - 2] = (byte) 0xff;
        frame[frame.length - 3] = (byte) 0xff;
        frame[frame.length - 4] = (byte) 0xff;
        
        // This is an arbitrary loop, could be an endless while, but if we try 10k times there is probably something wrong, could be used for backoff later
        for (int i = 0; i < 10000; i++) {
            if (!theRF.inUse()) {
                theRF.transmit(frame);
                return;
            } else {
                // insert a delay or use a different mechanism to avoid busy-waiting.
                try {
                    Thread.sleep(10); // Sleep for 10 milliseconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // If we made it here, there's a problem
        return;
    }
}
