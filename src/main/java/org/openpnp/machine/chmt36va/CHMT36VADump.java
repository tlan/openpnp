package org.openpnp.machine.chmt36va;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;

public class CHMT36VADump {
    static final String HEADER = "ebfbebfb";
    static final String FOOTER = "cbdbcbdb";
    
    static HashSet<String> combos = new HashSet<>();
    static Protocol protocol;

    public static String extractPacket(StringBuffer sb) {
        String s = sb.toString();
        if (!s.contains(HEADER) || !s.contains(FOOTER)) {
            return null;
        }
        int start = s.indexOf(HEADER);
        int end = s.indexOf(FOOTER);
        String packet = s.substring(start, end) + FOOTER;
        sb.delete(start, end + FOOTER.length());
        return packet;
    }
    
    public static void dumpPacket(String prefix, byte[] packet) throws Exception {
        Packet p = protocol.decode(packet);
        if (p != null) {
            System.out.println(String.format("%s %s", prefix, p));
        }
        else {
            System.out.println(String.format("%s UNKNOWN %s", prefix, Protocol.bytesToHexString(packet)));
        }
    }
    
    public static void dump(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        reader.readLine();
        StringBuffer readBuffer = new StringBuffer();
        StringBuffer writeBuffer = new StringBuffer();
        while ((line = reader.readLine()) != null) {
            // #;Time;Function;Direction;Status;Data;Data (chars);Data length;Req. length;Port;Comments;
            String[] fields = line.split(";");
            String function = fields[2].trim();
            String direction = fields[3].trim();
            String data = fields[5].trim().replace(" ", "");
            if (!direction.equals("UP")) {
                continue;
            }
            if (function.equals("IRP_MJ_WRITE")) {
                writeBuffer.append(data);
                String packet;
                while ((packet = extractPacket(writeBuffer)) != null) {
                    try {
                        byte[] packetBytes = Protocol.hexStringToBytes(packet);
                        dumpPacket("<<", packetBytes);
                    }
                    catch (Exception e) {
                        System.out.println("<< ERROR: " + e + " : " + packet);
                    }
                }
            }
            else {
                readBuffer.append(data);
                String packet;
                while ((packet = extractPacket(readBuffer)) != null) {
                    try {
                        byte[] packetBytes = Protocol.hexStringToBytes(packet);
                        dumpPacket(">>", packetBytes);
                    }
                    catch (Exception e) {
                        System.out.println(">> ERROR: " + e + " : " + packet);
                    }
                }
            }
        }
        reader.close();    
    }
    
    public static void main(String[] args) throws Exception {
        protocol = new Protocol(new File("/Users/jason/Projects/openpnp/CHMT36VA/Purchase Info/CHMT36VA USB Stick/LICENSE.smt"));
        File dumpsDir = new File("/Users/jason/Dropbox/CHMT36VA/Dumps/");
        for (File file : dumpsDir.listFiles()) {
            if (!file.getName().endsWith(".csv")) {
                continue;
            }
            System.out.println(file.getAbsolutePath());
            dump(file);
            System.out.println();
            System.out.println();
        }
        ArrayList<String> c = new ArrayList<>(combos);
        c.sort(null);
        for (String s : c) {
            System.out.println(s);
        }
    }
}
