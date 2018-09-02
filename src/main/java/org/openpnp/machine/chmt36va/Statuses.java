package org.openpnp.machine.chmt36va;

public class Statuses {

    /**
     * Seems to be sent after every command. Think it might be alarm status or just machine status.
     * Have noticed in dumps that the first byte changed during a job, maybe during an error.
     */
    public static class UnknownStatus1 implements Packet {
        public byte a; // I think 1 might be idle, and 3 might be machine is moving
        public byte b;
        public byte c;
        public byte d;
        
        public int getTableId() {
            return 32;
        }
        
        public byte[] encode() throws Exception {
            return null;
        }
        
        public void decode(byte[] bytes) throws Exception {
            a = bytes[0];
            b = bytes[1];
            c = bytes[2];
            d = bytes[3];
        }
        
        @Override
        public String toString() {
            return String.format("a %d, b %d, c %d, d %d", 
                    a, b, c, d);
        }
    }

}
