package org.openpnp.machine.chmt36va;
import java.io.File;

import org.openpnp.machine.chmt36va.Commands.CmdToSetPos;
import org.openpnp.machine.chmt36va.Numerics.PositionReport;

import jssc.SerialPort;

public class CHMT36VADriverTest implements Runnable {
    static SerialPort port;
    static Protocol protocol;
    
    public static void main(String[] args) throws Exception {
        protocol = new Protocol(new File("/Users/jason/Projects/openpnp/CHMT36VA/Purchase Info/CHMT36VA USB Stick/LICENSE.smt"));
        
        // connect to serial port
        port = new SerialPort("/dev/tty.usbserial-FTAMPCBF");
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        port.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE, false, true);
        port.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
        
        // start reader thread
        new Thread(new CHMT36VADriverTest()).start();
        
//        CmdToOrigZeroVCmdToOrigZeroNoV cmd = new CmdToOrigZeroVCmdToOrigZeroNoV();
//        send(cmd);
//        Thread.sleep(15 * 1000);
        
//        moveTo(200, 200);
//        Thread.sleep(2000);

        /**
         * curDeviceSelId:
         * 3 = head camera
         * 2 = right nozzle
         * 1 = left nozzle
         * 0 = nothing?
         */
        byte unknown1 = (byte) 0;
        byte unknown2 = (byte) 0;
        while (true) {
            moveTo(200, 200, (byte) 2, (byte) 1, unknown1, unknown2);
//            unknown1 += 10;
//            unknown2 += 10;
            Thread.sleep(500);
        }
        
//        Thread.sleep(2000);
//        
//        for (int i = 0; i < 5; i++) {
//            moveTo(100, 200);
//            Thread.sleep(500);
//            moveTo(200, 200);
//            Thread.sleep(500);
//            moveTo(200, 100);
//            Thread.sleep(500);
//            moveTo(100, 100);
//            Thread.sleep(500);
//        }
    }
    
    public static void moveTo(double x, double y, byte curDeviceSelId, byte debugSpeed, byte unknown1, byte unknown2) throws Exception {
        PositionReport p = new PositionReport();
        p.startX = (int) (x * 100.);
        p.startY = (int) (y * 100.);
        p.deltaX = 0;
        p.deltaY = 0;
        p.curDeviceSelId = curDeviceSelId;
        p.curDebugSpeed = debugSpeed;
        p.unknown1 = unknown1;
        p.unknown2 = unknown2;
        send(p);
        CmdToSetPos p1 = new CmdToSetPos();
        send(p1);
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
        byte[] b = protocol.encode(p);
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
                    int footerIndex = indexOf(buffer, 0, Protocol.FOOTER);
                    if (footerIndex == -1) {
                        break;
                    }
                    
                    // If a footer is found, search for it's nearest header
                    int headerIndex = lastIndexOf(buffer, footerIndex, Protocol.HEADER);
                    if (headerIndex == -1) {
                        // If there is a footer in the buffer but no header we've received an incomplete
                        // packet, so dump everything up to and including the footer by moving all the
                        // bytes past the footer down to the beginning of the buffer.
                        offset -= deleteUpTo(buffer, footerIndex + 4);
                        break;
                    }
                    
                    // Extract the packet
                    byte[] packet = new byte[footerIndex - headerIndex + 4];
                    System.arraycopy(buffer, headerIndex, packet, 0, packet.length);
                    offset -= deleteUpTo(buffer, footerIndex + 4);
                    
                    // Decode the packet
                    Packet p = protocol.decode(packet);
                    if (p != null) {
                        System.out.println("<< " + p.getClass().getSimpleName() + ": " + p);
                    }
                    else {
                        System.out.println("<< Unknown Packet " + Protocol.bytesToHexString(packet));
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
}
