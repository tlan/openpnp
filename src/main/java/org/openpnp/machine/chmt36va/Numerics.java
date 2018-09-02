package org.openpnp.machine.chmt36va;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Numerics {

    public static class PositionReport implements CHMT36VADriver.Packet {
        public int startX;
        public int startY;
        public int deltaX;
        public int deltaY;
        public int curDeviceSelId;
        public int curDebugSpeed;
        
        public byte[] encode() throws Exception {
            ByteBuffer buffer = ByteBuffer.allocate(37);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            
            buffer.put(CHMT36VADriver.HEADER);      // header
            buffer.put((byte) 0x80);                // packet type
            buffer.putShort((short) 20);            // payload length
            buffer.put((byte) 0xd9);                // encryption key
            buffer.put((byte) 0x00);                // unknown1
            buffer.putShort((short) 7);             // table id
            buffer.putInt(startX);
            buffer.putInt(startY);
            buffer.putInt(deltaX);
            buffer.putInt(deltaY);
            buffer.put((byte) curDeviceSelId);
            buffer.put((byte) curDebugSpeed);
            buffer.put((byte) 0xff);                // unknown
            buffer.put((byte) 0xff);                // unknown
            buffer.putShort((short) 
                    CHMT36VADriver.crc16(buffer.array(), 11, 20));  // CRC
            buffer.put(CHMT36VADriver.FOOTER);                     // footer
            
            System.out.println(CHMT36VADriver.bytesToHexString(buffer.array()));
    
            CHMT36VADriver.encryptFrame(buffer.array(), CHMT36VADriver.key1, CHMT36VADriver.key2);
            
            return buffer.array();
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
            startX = bytes.getInt(11);
            startY = bytes.getInt(15);
            deltaX = bytes.getInt(19);
            deltaY = bytes.getInt(23);
            curDeviceSelId = bytes.get(27);
            curDebugSpeed = bytes.get(28);
        }
        
        @Override
        public String toString() {
            return String.format("startX %6.2f, startY %6.2f, deltaX %6.2f, deltaY %6.2f, curDeviceSelId %d, curDebugSpeed %d", 
                    startX / 100., startY / 100., deltaX / 100., deltaY / 100., curDeviceSelId, curDebugSpeed);
        }
    }

}
