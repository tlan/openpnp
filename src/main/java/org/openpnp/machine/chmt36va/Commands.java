package org.openpnp.machine.chmt36va;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Commands {

    public static class CmdDownLamp implements CHMT36VADriver.Packet {
        public boolean on;
        
        // TODO STOPSHIP: Write a method that handles the header, footer, crc, encryption, etc.
        // and just have these encode functions emit the payload. Need to figure out the packet
        // type though.
        // Looks like packetType doesn't matter, so will be easy to break this out.
        public byte[] encode() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(21);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put(CHMT36VADriver.HEADER);      // header
            buffer.put((byte) 0x11);                // packet type
            buffer.putShort((short) 0x04);          // payload length
            buffer.put((byte) 0x00);                // encryption key
            buffer.put((byte) 0x00);                // unknown1
            buffer.putShort((short) 50);            // table id
            buffer.putShort((short) 8);             // param id, start of CRC (inclusive)
            buffer.put((byte) 0x01);                // data type
            buffer.put((byte) (on ? 0x01 : 0x00));  // value, end of CRC (inclusive)
            buffer.putShort((short) 
                    CHMT36VADriver.crc16(buffer.array(), 11, 4));  // CRC
            buffer.put(CHMT36VADriver.FOOTER);                     // footer
    
            CHMT36VADriver.encryptFrame(buffer.array(), CHMT36VADriver.key1, CHMT36VADriver.key2);
            
            return buffer.array();
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
            
        }
    }

    public static class CmdUpLamp implements CHMT36VADriver.Packet {
        public boolean on;
        
        public byte[] encode() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(21);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put(CHMT36VADriver.HEADER);                     // header
            buffer.put((byte) 0x11);                // packet type
            buffer.putShort((short) 0x04);          // payload length
            buffer.put((byte) 0xa7);                // encryption key
            buffer.put((byte) 0x00);                // unknown1
            buffer.putShort((short) 50);            // table id
            buffer.putShort((short) 24);            // param id, start of CRC (inclusive)
            buffer.put((byte) 0x01);                // data type
            buffer.put((byte) (on ? 0x01 : 0x00));  // value, end of CRC (inclusive)
            buffer.putShort((short) 
                    CHMT36VADriver.crc16(buffer.array(), 11, 4));  // CRC
            buffer.put(CHMT36VADriver.FOOTER);                     // footer
    
            CHMT36VADriver.encryptFrame(buffer.array(), CHMT36VADriver.key1, CHMT36VADriver.key2);
            
            return buffer.array();
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
            
        }
    }

    public static class CmdToOrigZero implements CHMT36VADriver.Packet {
        public boolean vision = true;
        
        public byte[] encode() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(21);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put(CHMT36VADriver.HEADER);                     // header
            buffer.put((byte) 0x11);                // packet type
            buffer.putShort((short) 0x04);          // payload length
            buffer.put((byte) 0xa7);                // encryption key
            buffer.put((byte) 0x00);                // unknown1
            buffer.putShort((short) 50);            // table id
            buffer.putShort((short) 21);            // param id, start of CRC (inclusive)
            buffer.put((byte) 0x01);                // data type
            /**
             * The database says:
             * CmdToOrigZeroV = 0
             * CmdToOrigZeroNoV = 1
             * And when the OEM software sends this command, it sends it with 0, which would
             * imply V (vision), but I don't think the machine has visual homing. In any case,
             * it's implemented here as the database says until we learn otherwise.
             * 
             * Running the command with vision = false results in a different, slower home
             * that doesn't seem to complete.
             */
            buffer.put((byte) (vision ? 0x00 : 0x01));  // value, end of CRC (inclusive)
            buffer.putShort((short) 
                    CHMT36VADriver.crc16(buffer.array(), 11, 4));  // CRC
            buffer.put(CHMT36VADriver.FOOTER);                     // footer
    
            CHMT36VADriver.encryptFrame(buffer.array(), CHMT36VADriver.key1, CHMT36VADriver.key2);
            
            return buffer.array();
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
            /**
             * ebfbebfb
             * 11
             * 0400
             * 80
             * e5
             * 3300
             * 06 00 01 00
             * 0700
             * cbdbcbdb
             */
        }
    }

    public static class CmdReqProcessInfo implements CHMT36VADriver.Packet {
        public byte[] encode() throws Exception {
            return null;
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
        }
    }
    
    public static class CmdToSetPos implements CHMT36VADriver.Packet {
        public byte[] encode() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(21);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put(CHMT36VADriver.HEADER);      // header
            buffer.put((byte) 0x11);                // packet type
            buffer.putShort((short) 0x04);          // payload length
            buffer.put((byte) 0xa7);                // encryption key
            buffer.put((byte) 0x00);                // unknown1
            buffer.putShort((short) 50);            // table id
            buffer.putShort((short) 23);            // param id, start of CRC (inclusive)
            buffer.put((byte) 0x01);                // data type
            buffer.put((byte) 0x00);                // value, end of CRC (inclusive)
            buffer.putShort((short) 
                    CHMT36VADriver.crc16(buffer.array(), 11, 4));  // CRC
            buffer.put(CHMT36VADriver.FOOTER);                     // footer
    
            CHMT36VADriver.encryptFrame(buffer.array(), CHMT36VADriver.key1, CHMT36VADriver.key2);
            
            return buffer.array();
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
            /**
             * ebfbebfb
             * 11
             * 0400
             * 80
             * e5
             * 3300
             * 06 00 01 00
             * 0700
             * cbdbcbdb
             */
        }
    }
}
