package wifi;
import java.nio.ByteBuffer;
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

    // Constructor 1
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

    // Constructor from byte array
    public Frame(byte[] fullFrame) {
        // TODO check size

        // Extracting bits for frameType
        frameType = (byte) ((fullFrame[0] >> 5) & 0x07);
        // Extracting the 4th bit for retry
        retry = ((fullFrame[0] >> 4) & 0x01) != 0;
        // Extracting 12 bits for seqnum (assuming seqnum is split between two bytes)
        seqNum = ((fullFrame[0] & 0x0F) << 8) | (fullFrame[1] & 0xFF);
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
        crc = ByteBuffer.wrap(fullFrame, dataStartIndex + dataLength, 4).getInt();
    }

    // Convert the frame to a byte array
    public byte[] toByteArray() {
        byte[] frame = new byte[data.length + 10]; // 10 bytes for MAC header
        frame[0] = (byte) ((frameType & 0x07) << 5); // Set the first 3 bits for frameType
        frame[0] |= (byte) ((retry ? 1 : 0) << 4); // Set the 4th bit for retry
        frame[0] |= (byte) ((seqNum >> 8) & 0x0F); // Set the upper 4 bits of seqNum
        frame[1] = (byte) (seqNum & 0xFF); // Set the lower 8 bits of seqNum
        frame[2] = (byte) (destAddr & 0xFF); // Set the lower 8 bits of destAddr
        frame[3] = (byte) ((destAddr >> 8) & 0xFF); // Set the upper 8 bits of destAddr
        frame[4] = (byte) (srcAddr & 0xFF); // Set the lower 8 bits of srcAddr
        frame[5] = (byte) ((srcAddr >> 8) & 0xFF); // Set the upper 8 bits of srcAddr

        // Copy the data payload into the frame
        System.arraycopy(data, 0, frame, 6, data.length);

        // Calculate and set CRC32
        CRC32 crc32 = new CRC32();
        crc32.update(frame, 0, data.length + 6);
        int calculatedCRC = (int) crc32.getValue();
        ByteBuffer.wrap(frame, data.length + 6, 4).putInt(calculatedCRC);

        return frame;
    }

    // Calculate CRC32 checksum for the data payload
    private int calculateChecksum(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return (int) crc32.getValue();
    }
    
    public boolean validateChecksum() {
        CRC32 crc32 = new CRC32();

        // Calculate CRC for the data payload
        crc32.update(data);

        // Get the calculated CRC value
        long calculatedCrc = crc32.getValue();

        // Compare with the stored CRC in the frame
        return calculatedCrc == crc;
    }
}
