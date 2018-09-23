package org.openpnp.machine.chmt36va;

public class Commands {
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
    
    public static class CmdMachineReset implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 1;
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
    
    public static class CmdSelectTopCamera implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 42;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = 0x01;
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }
    
    public static class CmdSelectBottomCamera implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 42;
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
    
    public static class CmdPin implements Command {
        public boolean down = false;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 12;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = (byte) (down ? 0x01 : 0x00);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }
    
    public static class CmdPump implements Command {
        public boolean on = false;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 4;
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
    
    public static class CmdFilmPull implements Command {
        public boolean on = false;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 11;
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
    
    public static class CmdAlarm implements Command {
        public boolean on = false;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 10;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = (byte) (on ? 0x00 : 0x01);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }
    
    public static class CmdNozzle1Vacuum implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 17;
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
    
    public static class CmdNozzle2Vacuum implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 18;
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
    
    public static class CmdNozzle1Blow implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 19;
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
    
    public static class CmdNozzle2Blow implements Command {
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 20;
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
    
    public static class CmdNozzle1Down implements Command {
        public boolean down = false;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 13;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = (byte) (down ? 0x00 : 0x01);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }
    
    public static class CmdNozzle2Down implements Command {
        public boolean down = false;
        
        public int getTableId() {
            return 50;
        }
        
        public int getParamId() {
            return 14;
        }

        public byte[] encode() throws Exception {
            byte[] bytes = new byte[2];
            bytes[0] = 0x01;
            bytes[1] = (byte) (down ? 0x00 : 0x01);
            return bytes;
        }
        
        public void decode(byte[] bytes) throws Exception {
            
        }
    }
}
