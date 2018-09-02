package org.openpnp.machine.chmt36va;

public class Numerics {

    public static class PositionReport implements Packet {
        public int startX;
        public int startY;
        public int deltaX;
        public int deltaY;
        public byte curDeviceSelId;
        public byte curDebugSpeed;
        public byte unknown1 = (byte) 0xff;
        public byte unknown2 = (byte) 0xff;
        
        public int getTableId() {
            return 7;
        }

        // packetType 0x80
        public byte[] encode() throws Exception {
            byte[] bytes = new byte[20];
            Protocol.writeInt32(bytes, 0, startX);
            Protocol.writeInt32(bytes, 4, startY);
            Protocol.writeInt32(bytes, 8, deltaX);
            Protocol.writeInt32(bytes, 12, deltaY);
            bytes[16] = curDeviceSelId;
            bytes[17] = curDebugSpeed;
            bytes[18] = unknown1;
            bytes[19] = unknown2;
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            startX = Protocol.readInt32(bytes, 0);
            startY = Protocol.readInt32(bytes, 4);
            deltaX = Protocol.readInt32(bytes, 8);
            deltaY = Protocol.readInt32(bytes, 12);
            curDeviceSelId = bytes[16];
            curDebugSpeed = bytes[17];
            // These seem to be sent but not received.
//            unknown1 = bytes[18];
//            unknown2 = bytes[19];
        }
        
        @Override
        public String toString() {
            return String.format("startX %6.2f, startY %6.2f, deltaX %6.2f, deltaY %6.2f, curDeviceSelId %d, curDebugSpeed %d", 
                    startX / 100., startY / 100., deltaX / 100., deltaY / 100., curDeviceSelId, curDebugSpeed);
        }
    }

}
