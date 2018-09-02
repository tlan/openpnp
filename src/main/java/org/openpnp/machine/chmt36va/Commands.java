package org.openpnp.machine.chmt36va;

public class Commands {
    // all packettype 0x11, not sure if it matters
    public static class CmdDownLamp implements Command {
        public boolean on;

        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 8;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = (byte) 0x01;
            bytes[1] = (byte) (on ? 0x01 : 0x00);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }

    public static class CmdUpLamp implements Command {
        public boolean on;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 24;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = (byte) (on ? 0x01 : 0x00);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }

    public static class CmdToOrigZero implements Command {
        public boolean vision = true;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 21;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = (byte) (vision ? 0x00 : 0x01);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
        }
    }

    public static class CmdReqProcessInfo implements Command {
        public int getTableId() {
            return 51;
        }
        
        public int getParamId() {
            return 6;
        }
        
        public byte[] encode() throws Exception {
            return null;
        }
        
        public void decode(byte[] bytes) throws Exception {
        }
    }
    
    public static class CmdToSetPos implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 23;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = 0x00;
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
        }
    }
}
