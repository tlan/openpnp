package org.openpnp.machine.chmt36va;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import org.openpnp.machine.chmt36va.Commands.CmdToSetPos;
import org.openpnp.machine.chmt36va.Numerics.PositionReport;

import jssc.SerialPort;

public class CHMT36VADriver implements Runnable {
    static SerialPort port;
    
    static final byte[] HEADER = { (byte) 0xeb, (byte) 0xfb, (byte) 0xeb, (byte) 0xfb };
    static final byte[] FOOTER = { (byte) 0xcb, (byte) 0xdb, (byte) 0xcb, (byte) 0xdb };
    
    static byte[] key1;
    static byte[] key2;
    
    //  tableId,packetId,direction
    //  001,000,R
    //  001,002,W
    //  001,130,R
    //  001,130,W
    //  002,002,W
    //  002,128,R
    //  002,130,R
    //  003,130,W
    //  006,128,W
    //  007,128,R
    //  007,128,W
    //  007,129,W
    //  032,009,R
    //  050,017,W
    //  051,017,R
    //  051,017,W
    public interface Packet {
        byte[] encode() throws Exception;
        void decode(ByteBuffer bytes) throws Exception;
    }
    
    public static Packet decode(ByteBuffer packet) throws Exception {
        int tableId = packet.getShort(9);
        Packet p = null;
        if (tableId == 7) {
            p = new Numerics.PositionReport();
        }
        else if (tableId == 32) {
            p = new Statuses.UnknownStatus1();
        }
        else if (tableId == 51) {
            int paramId = packet.getShort(11);
            if (paramId == 6) {
                p = new Commands.CmdReqProcessInfo();
            }
        }
        
        if (p != null) {
            p.decode(packet);
            return p;
        }

        return null;
    }
    
    public static void main(String[] args) throws Exception {
        loadKeys("/Users/jason/Projects/openpnp/CHMT36VA/Purchase Info/CHMT36VA USB Stick/LICENSE.smt");
        
        // connect to serial port
        port = new SerialPort("/dev/tty.usbserial-FTAMPCBF");
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        port.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, false, true);
        port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        
        // start reader thread
        new Thread(new CHMT36VADriver()).start();
        
        /**
         * Findings:
         * * The below loop will cause the same responses to come back over and over which is pretty
         *   handy for testing various ideas.
         * * Sending an invalid CRC results in no response - so it does seem to check the CRC.
         * * Changing unknown1 (byte 9) to various values doesn't seem to matter
         * * Changing encryption key from e5 to e4 seems to work, which seems odd?
         * * Ran CmdDownLamp on and off with unknown1 0 - 255 and all worked fine. I suspect
         *   unknown1 is a salt that was never implemented.
         * * While messing around trying to figure out the encryption I seem to have confused
         *   the machine. It would still respond to commands but would not send a response. To
         *   fix it I had to pull all the plugs and power. 
         * * Tested with encryption subkey 0 and all four variants, and all worked. So, in other
         *   words I tested with the encryption key byte set to 0, 1, 2, 3.
         * * Tested with all 255 packetTypes and it doesn't seem to care about that either.
         *   I suspect packetType might have something to do with the index db table.
         */
        
        Commands.CmdToOrigZero cmd = new Commands.CmdToOrigZero();
        send(cmd);
        Thread.sleep(15 * 1000);
        
        moveTo(100, 100);
        Thread.sleep(2000);
        
        for (int i = 0; i < 5; i++) {
            moveTo(100, 200);
            Thread.sleep(500);
            moveTo(200, 200);
            Thread.sleep(500);
            moveTo(200, 100);
            Thread.sleep(500);
            moveTo(100, 100);
            Thread.sleep(500);
        }
    }
    
    public static void moveTo(double x, double y) throws Exception {
        
        PositionReport p = new PositionReport();
        p.startX = (int) (x * 100.);
        p.startY = (int) (y * 100.);
        p.deltaX = 0;
        p.deltaY = 0;
        p.curDeviceSelId = 3;
        p.curDebugSpeed = 1;
        send(p);
        CmdToSetPos p1 = new CmdToSetPos();
        send(p1);
    }
    
    public static void send(Packet p) throws Exception {
        byte[] b = p.encode();
        System.out.println(">> " + bytesToHexString(b));
        System.out.println(">> " + p);
        port.writeBytes(b);
    }

    public static int deleteUpTo(byte[] a, int offset) {
        int length = a.length - offset;
        System.arraycopy(a, offset, a, 0, length);
        for (int i = length; i < a.length; i++) {
            a[i] = 0;
        }
        return offset;
    }
    
    public void run() {
        byte[] buffer = new byte[10 * 1024];
        int offset = 0;
        while (true) {
            try {
                Thread.sleep(50);
                byte[] bytes = port.readBytes();
                if (bytes == null) {
                    continue;
                }
                
                System.arraycopy(bytes, 0, buffer, offset, bytes.length);
                offset += bytes.length;
                
                do {
                    // Search for a footer in the buffer
                    int footerIndex = indexOf(buffer, 0, FOOTER);
                    if (footerIndex == -1) {
                        break;
                    }
                    
                    // If a footer is found, search for it's nearest header
                    int headerIndex = lastIndexOf(buffer, footerIndex, HEADER);
                    if (headerIndex == -1) {
                        // If there is a footer in the buffer but no header we've received an incomplete
                        // packet, so dump everything up to and including the footer by moving all the
                        // bytes past the footer down to the beginning of the buffer.
                        offset -= deleteUpTo(buffer, footerIndex + 4);
                        break;
                    }
                    
                    // Extract the packet
                    ByteBuffer packet = ByteBuffer.allocate(footerIndex - headerIndex + 4);
                    packet.order(ByteOrder.LITTLE_ENDIAN);
                    packet.put(buffer, headerIndex, packet.limit());
                    offset -= deleteUpTo(buffer, footerIndex + 4);
                    
                    // Decrypt the packet and check it's CRC
                    decryptFrame(packet.array(), key1, key2);
                    int crc = packet.getShort(packet.limit() - 6);
                    int crcCalc = crc16(packet.array(), 11, packet.limit() - 6 - 11);
                    if (crc != crcCalc) {
                        System.out.println(String.format("Bad CRC. %04x != %04x: %s", crc, crcCalc, bytesToHexString(packet.array())));
                        continue;
                    }
                    
                    // Decode the packet
                    Packet p = decode(packet);
                    if (p != null) {
                        System.out.println("<< " + p.getClass().getSimpleName() + ": " + p);
                    }
                    else {
                        int tableId = packet.getShort(9);
                        System.out.println("<< " + String.format("Unknown tableId %d: %s", tableId, bytesToHexString(packet.array())));
                    }
                } while (true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    
    public static int indexOf(byte[] haystack, int offset, byte[] needle) {
        int needleOffset = 0;
        int matchStart = offset;
        for (int i = offset; i < haystack.length - offset; i++) {
            if (haystack[i] == needle[needleOffset]) {
                needleOffset++;
                if (needleOffset == needle.length) {
                    return matchStart;
                }
            }
            else {
                needleOffset = 0;
                matchStart = i + 1;
            }
        }
        return -1;
    }
    
    public static int lastIndexOf(byte[] haystack, int offset, byte[] needle) {
        int needleOffset = needle.length - 1;
        int matchStart = offset;
        for (int i = offset; i >= 0; i--) {
            if (haystack[i] == needle[needleOffset]) {
                needleOffset--;
                if (needleOffset == -1) {
                    return matchStart;
                }
                matchStart--;
            }
            else {
                needleOffset = needle.length - 1;
                matchStart = i - 1;
            }
        }
        return -1;
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
    
    public static void loadKeys(String _filename) throws Exception
    {
        key1 = new byte[12];
        key2 = new byte[1024];

        byte[] fileBytes = Files.readAllBytes(new File(_filename).toPath());
        for (int i=0; i<12; i++)
        {
            key1[i] = fileBytes[1024 + i];
            key1[i] ^= 0xaa;
        }
        for (int i = 0; i < 1024; i++)
        {
            key2[i] = fileBytes[4096 + i];
            key2[i] ^= key1[i % 12];
        }
    }

    static void decryptFrame(byte[] _frame, byte[] _key1, byte[] _key2)
    {
        int subkey;
        int variant;
        int offset = 9;
        
        subkey = _frame[7] & 0xfc;
        subkey <<= 2;
        variant = _frame[7] & 0x03;
        for (int index = 0; index < _frame.length - 13; index++)
        {
            switch (variant)
            {
                case 0:
                    _frame[offset + index] ^= _key1[index % 0xC];
                    break;
                case 1:
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    _frame[offset + index] ^= _key1[index % 0xC];
                    break;
                case 2:
                    _frame[offset + index] += _key1[index % 0xC];
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    break;
                case 3:
                    _frame[offset + index] -= _key1[index % 0xC];
                    _frame[offset + index] ^= _key1[index % 0xC];
                    break;
            }
        }
        for (int index=0; index < _frame.length - 13; index++)
        {
            switch (variant)
            {
                case 0:
                    _frame[offset + index] ^= _key2[subkey + (index % 0x10)];
                    break;
                case 1:
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    _frame[offset + index] ^= _key2[subkey + (index % 0x10)];
                    break;

                case 2:
                    _frame[offset + index] += _key2[subkey + (index % 0x10)];
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    break;
                case 3:
                    _frame[offset + index] -= _key2[subkey + (index % 0x10)];
                    _frame[offset + index] ^= _key2[subkey + (index % 0x10)];
                    break;
            }
        }
    }    
    
    /**
     * The inverse of decryptFrame. decryptFrame came first, and this is just the same code
     * but with each step in reverse.
     * 
     * @param _frame
     * @param _key1
     * @param _key2
     */
    static void encryptFrame(byte[] _frame, byte[] _key1, byte[] _key2)
    {
        int subkey;
        int variant;
        int offset = 9;
        
        subkey = _frame[7] & 0xfc;
        subkey <<= 2;
        variant = _frame[7] & 0x03;
        
        for (int index=0; index < _frame.length - 13; index++)
        {
            switch (variant)
            {
                case 0:
                    _frame[offset + index] ^= _key2[subkey + (index % 0x10)];
                    break;
                case 1:
                    _frame[offset + index] ^= _key2[subkey + (index % 0x10)];
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    break;

                case 2:
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    _frame[offset + index] -= _key2[subkey + (index % 0x10)];
                    break;
                case 3:
                    _frame[offset + index] ^= _key2[subkey + (index % 0x10)];
                    _frame[offset + index] += _key2[subkey + (index % 0x10)];
                    break;
            }
        }
        for (int index = 0; index < _frame.length - 13; index++)
        {
            switch (variant)
            {
                case 0:
                    _frame[offset + index] ^= _key1[index % 0xC];
                    break;
                case 1:
                    _frame[offset + index] ^= _key1[index % 0xC];
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    break;
                case 2:
                    _frame[offset + index] = (byte)(255 - _frame[offset + index]);
                    _frame[offset + index] -= _key1[index % 0xC];
                    break;
                case 3:
                    _frame[offset + index] ^= _key1[index % 0xC];
                    _frame[offset + index] += _key1[index % 0xC];
                    break;
            }
        }
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
}
