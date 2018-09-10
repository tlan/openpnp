package org.openpnp.machine.chmt36va;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


//tableId,packetId,direction
//001,000,R
//001,002,W
//001,130,R
//001,130,W
//002,002,W
//002,128,R
//002,130,R
//003,130,W
//006,128,W
//007,128,R
//007,128,W
//007,129,W
//032,009,R
//050,017,W
//051,017,R
//051,017,W

/**
 * ebfbebfb110400ca0032002a0001012c00cbdbcbdb is:
 * 00: ebfbebfb    : header
 * 04: 11          : I call this packetType, but not completely sure on it yet, see note 1.
 * 05: 0400        : payload size, 16 bit unsigned little endian
 * 07: ca          : encryption key / subkey info
 * 08: 00          : unknown1, see note 2.
 * 09: 3200        : tableId, 16 bit unsigned little endian
 * 11: 2a00 01 01  : payload (paramId, data type, field value)
 * -6: 2c00        : crc16, 16 bit unsigned little endian
 * -4: cbdbcbdb    : footer
 */
public class Protocol {
    static final byte[] HEADER = { (byte) 0xeb, (byte) 0xfb, (byte) 0xeb, (byte) 0xfb };
    static final byte[] FOOTER = { (byte) 0xcb, (byte) 0xdb, (byte) 0xcb, (byte) 0xdb };
    
    byte[] key1;
    byte[] key2;
    
    static List<Packet> packetTypes = new ArrayList<>();
    
    static {
        packetTypes.add(new Commands.CmdDownLamp());
        packetTypes.add(new Commands.CmdReqProcessInfo());
        packetTypes.add(new Commands.CmdToOrigZero());
        packetTypes.add(new Commands.CmdToSetPos());
        packetTypes.add(new Commands.CmdUpLamp());
        packetTypes.add(new Numerics.PositionReport());
        packetTypes.add(new Statuses.UnknownStatus1());
//        Packets.register(packetTypes);
    }
    
    public Protocol(File licenseFile) throws Exception {
        loadKeys(licenseFile);
        System.out.println(bytesToHexString(key1));
        System.out.println(bytesToHexString(key2));
    }
    
    private void loadKeys(File licenseFile) throws Exception
    {
        key1 = new byte[12];
        key2 = new byte[1024];
    
        byte[] fileBytes = Files.readAllBytes(licenseFile.toPath());
        for (int i = 0; i < 12; i++) {
            key1[i] = fileBytes[1024 + i];
            key1[i] ^= 0xaa;
        }
        for (int i = 0; i < 1024; i++) {
            key2[i] = fileBytes[4096 + i];
            key2[i] ^= key1[i % 12];
        }
    }

    /**
     * Decode a byte array containing one complete packet and return the decoded Packet object.
     * The array is expected to begin with the header (0xebfbebfb) and end with the footer
     * (0xcbdbcbdb). If the packet has an invalid CRC an exception will be thrown.
     * @param packet
     * @return
     * @throws Exception
     */
    public Packet decode(byte[] bytes) throws Exception {
        // Make sure the packet starts with the header and ends with the footer.
        // TODO STOPSHIP
        
        // Decrypt the packet 
        decrypt(bytes);
        
        // Check the CRC
        int crc = readUint16(bytes, bytes.length - 6);
        int crcCalc = crc16(bytes, 11, bytes.length - 6 - 11);
        if (crc != crcCalc) {
            throw new Exception(String.format("Invalid CRC: %04x != %04x", crc, crcCalc));
        }

        // Extract the tableId so we can determine what kind of packet this is
        int payloadLength = readUint16(bytes, 5);
        int tableId = readUint16(bytes, 9);
        
        // Look up the packet type and create one.
        Packet p = null;
        for (Packet packetType : packetTypes) {
            if (tableId == packetType.getTableId()) {
                if (packetType instanceof Command) {
                    int paramId = readUint16(bytes, 11);
                    if (paramId == ((Command) packetType).getParamId()) {
                        p = packetType.getClass().newInstance();
                        break;
                    }
                }
                else {
                    p = packetType.getClass().newInstance();
                    break;
                }
            }
        }
        
        // Decode
        if (p != null) {
            byte[] payload = new byte[payloadLength];
            if (p instanceof Command) {
                System.arraycopy(bytes, 13, payload, 0, payloadLength);
            }
            else {
                System.arraycopy(bytes, 11, payload, 0, payloadLength);
            }
            p.decode(payload);
        }
    
        return p;
    }
    
    /**
     * Encode a Packet object into an array of bytes. This method handles calculating the CRC
     * and encrypting the packet. The resulting array will begin with the header (0xebfbebfb)
     * and end with the footer (0xcbdbcbdb) and is ready to be sent to the machine.  
     * @param packet
     * @return
     * @throws Exception
     */
    public byte[] encode(Packet packet) throws Exception {
        byte[] payload = packet.encode();
        int length = payload.length + 11 + 6;
        if (packet instanceof Command) {
            length += 2;
        }
        byte[] bytes = new byte[length];
        
        System.arraycopy(HEADER, 0, bytes, 0, 4);
        if (packet instanceof Command) {
            bytes[4] = 0x11;
            writeUint16(bytes, 5, payload.length + 2);
            bytes[7] = 0;
            bytes[8] = 0;
            writeUint16(bytes, 9, packet.getTableId());
            writeUint16(bytes, 11, ((Command) packet).getParamId());
            System.arraycopy(payload, 0, bytes, 13, payload.length);
        }
        else {
            bytes[4] = (byte) 0x80;
            writeUint16(bytes, 5, payload.length);
            bytes[7] = 0;
            bytes[8] = 0;
            writeUint16(bytes, 9, packet.getTableId());
            System.arraycopy(payload, 0, bytes, 11, payload.length);
        }
        writeUint16(bytes, bytes.length - 6, crc16(bytes, 11, bytes.length - 6 - 11));
        System.arraycopy(FOOTER, 0, bytes, bytes.length - 4, 4);
        
        encrypt(bytes);
        return bytes;
    }

    /**
     * The inverse of decryptFrame. decryptFrame came first, and this is just the same code but with
     * each step in reverse.
     * 
     * @param packet
     * @param key1
     * @param key2
     */
    public void encrypt(byte[] packet) {
        int subkey;
        int variant;
        int offset = 9;

        subkey = packet[7] & 0xfc;
        subkey <<= 2;
        variant = packet[7] & 0x03;

        for (int index = 0; index < packet.length - 13; index++) {
            switch (variant) {
                case 0:
                    packet[offset + index] ^= key2[subkey + (index % 0x10)];
                    break;
                case 1:
                    packet[offset + index] ^= key2[subkey + (index % 0x10)];
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    break;

                case 2:
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    packet[offset + index] -= key2[subkey + (index % 0x10)];
                    break;
                case 3:
                    packet[offset + index] ^= key2[subkey + (index % 0x10)];
                    packet[offset + index] += key2[subkey + (index % 0x10)];
                    break;
            }
        }
        for (int index = 0; index < packet.length - 13; index++) {
            switch (variant) {
                case 0:
                    packet[offset + index] ^= key1[index % 0xC];
                    break;
                case 1:
                    packet[offset + index] ^= key1[index % 0xC];
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    break;
                case 2:
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    packet[offset + index] -= key1[index % 0xC];
                    break;
                case 3:
                    packet[offset + index] ^= key1[index % 0xC];
                    packet[offset + index] += key1[index % 0xC];
                    break;
            }
        }
    }

    public void decrypt(byte[] packet) {
        int subkey;
        int variant;
        int offset = 9;

        subkey = packet[7] & 0xfc;
        subkey <<= 2;
        variant = packet[7] & 0x03;
        for (int index = 0; index < packet.length - 13; index++) {
            switch (variant) {
                case 0:
                    packet[offset + index] ^= key1[index % 0xC];
                    break;
                case 1:
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    packet[offset + index] ^= key1[index % 0xC];
                    break;
                case 2:
                    packet[offset + index] += key1[index % 0xC];
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    break;
                case 3:
                    packet[offset + index] -= key1[index % 0xC];
                    packet[offset + index] ^= key1[index % 0xC];
                    break;
            }
        }
        for (int index = 0; index < packet.length - 13; index++) {
            switch (variant) {
                case 0:
                    packet[offset + index] ^= key2[subkey + (index % 0x10)];
                    break;
                case 1:
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    packet[offset + index] ^= key2[subkey + (index % 0x10)];
                    break;

                case 2:
                    packet[offset + index] += key2[subkey + (index % 0x10)];
                    packet[offset + index] = (byte) (255 - packet[offset + index]);
                    break;
                case 3:
                    packet[offset + index] -= key2[subkey + (index % 0x10)];
                    packet[offset + index] ^= key2[subkey + (index % 0x10)];
                    break;
            }
        }
    }
    
    public static void writeInt32(byte[] bytes, int offset, int value) {
        // TODO STOPSHIP hack to get this finished for now, convert to direct read later.
        ByteBuffer b = ByteBuffer.wrap(bytes);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(offset,  value);
    }
    
    public static int readInt32(byte[] bytes, int offset) {
        return ((bytes[offset + 3] & 0xff) << 24) 
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 1] & 0xff) << 8) 
                | ((bytes[offset + 0] & 0xff) << 0);
    }
    
    public static void writeUint16(byte[] bytes, int offset, int value) {
        bytes[offset + 0] = (byte) ((value >> 0) & 0xff);
        bytes[offset + 1] = (byte) ((value >> 8) & 0xff);
    }
    
    public static int readUint16(byte[] bytes, int offset) {
        return (((bytes[offset + 1] & 0xff) << 8) 
                | (bytes[offset] & 0xff)) & 0xffff;
    }
    
    public static int crc16(byte[] packet, int offset, int length) {
        int crc = 0;
        for (int i = offset; i < offset + length; i++) {
            int b = (packet[i] & 0xff);
            crc += b;
            crc &= 0xffff;
        }
        return crc;
    }

    public static byte[] hexStringToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static String bytesToHexString(byte[] bytes) {
        final char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
