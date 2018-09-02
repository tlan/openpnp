package org.openpnp.machine.chmt36va;

import java.nio.ByteBuffer;

public class Statuses {

    /**
     * Seems to be sent after every command. Think it might be alarm status or just machine status.
     * Have noticed in dumps that the first byte changed during a job, maybe during an error.
     */
    public static class UnknownStatus1 implements CHMT36VADriver.Packet {
        public int a;
        public int b;
        public int c;
        public int d;
        
        public byte[] encode() throws Exception {
            return null;
        }
        
        public void decode(ByteBuffer bytes) throws Exception {
            a = bytes.get(11) & 0xff;
            b = bytes.get(12) & 0xff;
            c = bytes.get(13) & 0xff;
            d = bytes.get(14) & 0xff;
        }
        
        @Override
        public String toString() {
            return String.format("a %d, b %d, c %d, d %d", 
                    a, b, c, d);
        }
    }

}
