package wifi;
import java.util.zip.CRC32;

public class Frame
{
    public int frameType;
    public boolean retry;
    public int seqNum;
    public short destAddr;
    public short srcAddr;
    public byte[] data;
    public int crc;

    // TODO constructor 1
    public Frame(int frameType, short seqNum, short destAddr, short srcAddr, byte[] data) {
        // if data is empty create new data
        if (data == null) {
            data = new byte[0];
        }
        // if data is too big throw exception
        if (data.length > 2038) {
            throw new IllegalArgumentException("Can't create a frame that size");
        }

        // set params
        this.frameType = frameType;
        this.seqNum = seqNum;
        this.destAddr = destAddr;
        this.srcAddr = srcAddr;
        this.data = data;
        this.crc = calculateChecksum(data);

    }

    // constructor from byte array
    public Frame(byte[] fullFrame) {
        //TODO check size

        // Extracting bits for frameType
        frameType = (byte) ((fullFrame[0] >> 5) & 0x07);
        // Extracting the 4th bit for retry
        retry = ((fullFrame[0] >> 4) & 0x01) != 0;
        // Extracting 12 bits for seqnum (assuming seqnum is split between two bytes)
        seqNum = ((data[0] & 0x0F) << 8) | (fullFrame[1] & 0xFF);
        // Get dest and source addresses
        destAddr = (short) ((fullFrame[2] & 0xFF) | ((fullFrame[3] & 0xFF) << 8));
        srcAddr = (short) ((fullFrame[4] & 0xFF) | ((fullFrame[5] & 0xFF) << 8));
        // Determine the position where the data payload starts in the frame
        int dataStartIndex = 6;
        int checkSumLength = 4;
        // Calculate the length of the data payload
        int dataLength = fullFrame.length - dataStartIndex - checkSumLength;

        // Extract the data payload into a separate byte array from start of data to checksum
        data = new byte[dataLength];
        System.arraycopy(fullFrame, dataStartIndex, data, 0, dataLength);

        // Extract crc
        System.arraycopy(fullFrame, dataStartIndex + dataLength, crc, 0, 4);

    }

    //TODO finish
    public byte[] toByteArray() {
        byte[] frame = new byte[data.length + 10]; // 10 bytes for MAC header
        frame[0] = (byte) ((frameType & 0x07) << 5); // Set the first 3 bits of frame to frameType
        // Set the next bit to retry (4th bit)
        if (retry) {
            frame[0] |= (byte) (1 << 4);
        } else {
            frame[0] &= (byte) ~(1 << 4);
        }
        frame[0] |= (byte) ((seqNum >> 8) & 0x0F); // Set the next 12 bits to seqNum
        frame[1] = (byte) (seqNum & 0xFF);
        frame[2] = (byte) destAddr; // Destination MAC address (lower byte)
        frame[3] = (byte) (destAddr >> 8); // Destination MAC address (upper byte)
        frame[4] = (byte) srcAddr; // Source MAC address (lower byte)
        frame[5] = (byte) (srcAddr >> 8); // Source MAC address (upper byte)

        // Copy the data into the frame
        System.arraycopy(data, 0, frame, 6, data.length);
        // Copy in checksum
        System.arraycopy(crc, 0, frame, frame.length - 4, 4);

        return frame;
    }
    
    public boolean validateChecksum() {
        return crc == calculateChecksum(data);
    }
    
    private int calculateChecksum(byte[] data) {
        CRC32 checker = new CRC32();
        checker.update(data);
        return (int) checker.getValue();
    }
}
