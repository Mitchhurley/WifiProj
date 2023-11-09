package wifi;

/**
 * An object used to hold data for outgoing transmissions
 *
 * @author Jonah Kelsey
 * @version 1.0 11/6/23
 */

public class TransmissionData {
	//Short representing destination mac address
    private final short dest;
    //Byte array of actual data
    private final byte[] data;
    private final int len;

    public TransmissionData(short dest, byte[] data, int len) {
        this.dest = dest;
        this.data = data;
        this.len = len;
    }

    public short getDest() {
        return dest;
    }

    public byte[] getData() {
        return data;
    }

    public int getLen() {
        return len;
    }
}
