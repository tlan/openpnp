package org.openpnp.machine.chmt36va;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PacketGenerator {
    static int getDataTypeSize(int dataTypeId) {
        // 1 BYTE U8 1
        // 2 unsigned short U16 2
        // 3 short S16 2
        // 4 int S32 4
        // 5 UINT U32 4
        // 6 DOUBLE F16 8
        // 7 String STR 255
        switch (dataTypeId) {
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 2;
            case 4:
                return 4;
            case 5:
                return 4;
            case 6:
                return 8;
            case 7:
                return 255;
        }
        throw new Error("Unknown dataTypeId " + dataTypeId);
    }

    static String getJavaDataType(int dataTypeId) {
        // 1 BYTE U8 1
        // 2 unsigned short U16 2
        // 3 short S16 2
        // 4 int S32 4
        // 5 UINT U32 4
        // 6 DOUBLE F16 8
        // 7 String STR 255
        switch (dataTypeId) {
            case 1:
                return "int";
            case 2:
                return "int";
            case 3:
                return "int";
            case 4:
                return "long";
            case 5:
                return "long";
            case 6:
                return "double";
            case 7:
                return "String";
        }
        throw new Error("Unknown dataTypeId " + dataTypeId);
    }
    
    static String sanitizeSymbol(String s) {
        s = s.replace('/', '_');
        return s;
    }

    static void generateEncode(PrintWriter writer, int dataTypeId) {
        switch (dataTypeId) {
            case 1:
                writer.println(String.format("        bytes[0] = (byte) %d;", dataTypeId));
                writer.println(String.format("        bytes[1] = (byte) (value.value & 0xff);"));
                return;
            case 2:
                writer.println(String.format("        bytes[0] = (byte) %d;", dataTypeId));
                writer.println(String.format("        Protocol.writeUint16(bytes, 1, value.value);"));
                return;
            default:
                throw new Error("Unknown dataTypeId " + dataTypeId);
        }
    }
    
    static void generateDecode(PrintWriter writer, int dataTypeId) {
        switch (dataTypeId) {
            case 1:
                writer.println(String.format("        value = Values.valueFor(bytes[1] & 0xff);"));
                return;
            case 2:
                writer.println(String.format("        value = Values.valueFor(Protocol.readUint16(bytes, 1));"));
                return;
            default:
                throw new Error("Unknown dataTypeId " + dataTypeId);
        }
    }
    
    static void generateEnum(PrintWriter writer, List<CommandParam> params) {
        writer.println(String.format("    public enum Values {"));
        for (CommandParam param : params) {
            writer.println(String.format("        %s(%d),", param.paramSymbol, param.value));
        }
        writer.println(String.format("        ;"));
        writer.println(String.format("        public final int value;"));
        writer.println(String.format("        Values(int value) { this.value = value; }"));
        writer.println(String.format("        public static Values valueFor(int value) {"));
        writer.println(String.format("            switch (value) {"));
        for (CommandParam param : params) {
            writer.println(String.format("                case %d: return %s;", param.value, param.paramSymbol));
        }
        writer.println(String.format("                default: return null;"));
        writer.println(String.format("            }"));
        writer.println(String.format("        }"));
        writer.println(String.format("    }"));
    }

    public static void generateCommand(File dir, String name, List<CommandParam> params) throws Exception {
        File file = new File(dir, name + ".java");
        PrintWriter writer = new PrintWriter(file);
        
        writer.println(String.format("package org.openpnp.machine.chmt36va.packets;"));
        writer.println(String.format(""));
        writer.println(String.format("import org.openpnp.machine.chmt36va.Command;"));
        writer.println(String.format("import org.openpnp.machine.chmt36va.Protocol;"));
        writer.println(String.format(""));
        writer.println(String.format("public class %s implements Command {", name));
        // TODO STOPSHIP handle variableValues
        generateEnum(writer, params);
        writer.println();
        writer.println(String.format("    public Values value = Values.%s;", params.get(0).paramSymbol));
        writer.println();
        writer.println(String.format("    public int getTableId() { return %d; }", params.get(0).tableId));
        writer.println();
        writer.println(String.format("    public int getParamId() { return %d; }", params.get(0).paramId));
        writer.println();
        writer.println(String.format("    public byte[] encode() throws Exception {"));
        writer.println(String.format("        byte[] bytes = new byte[%d];", 1 + getDataTypeSize(params.get(0).dataTypeId)));
        generateEncode(writer, params.get(0).dataTypeId);
        writer.println(String.format("        return bytes;"));
        writer.println(String.format("    }"));
        writer.println();
        writer.println(String.format("    public void decode(byte[] bytes) throws Exception {"));
        generateDecode(writer, params.get(0).dataTypeId);
        writer.println(String.format("    }"));
        writer.println();
        writer.println(String.format("    @Override"));
        writer.println(String.format("    public String toString() {"));
        writer.println(String.format("        return String.format(\"%%s (value = %%s)\", getClass().getSimpleName(), value);"));
        writer.println(String.format("    }"));
        writer.println(String.format("}"));
        writer.println(String.format(""));
        writer.close();
    }
    
    public static void generateNumeric(File dir, String name, List<NumericParam> params) throws Exception {
        File file = new File(dir, name + ".java");
        PrintWriter writer = new PrintWriter(file);
        
        writer.println(String.format("package org.openpnp.machine.chmt36va.packets;"));
        writer.println(String.format(""));
        writer.println(String.format("import org.openpnp.machine.chmt36va.Packet;"));
        writer.println(String.format("import org.openpnp.machine.chmt36va.Protocol;"));
        writer.println(String.format(""));
        writer.println(String.format("public class %s implements Packet {", name));
        for (NumericParam param : params) {
            writer.println(String.format("    public %s %s;", getJavaDataType(param.dataTypeId), param.paramSymbol));
        }
        writer.println();
        writer.println(String.format("    public int getTableId() { return %d; }", params.get(0).tableId));
        writer.println();
        writer.println(String.format("    public byte[] encode() throws Exception {"));
        writer.println(String.format("        byte[] bytes = new byte[%d];", 1 + getDataTypeSize(params.get(0).dataTypeId)));
        // TODO STOPSHIP encode
        writer.println(String.format("        return bytes;"));
        writer.println(String.format("    }"));
        writer.println();
        writer.println(String.format("    public void decode(byte[] bytes) throws Exception {"));
        // TODO STOPSHIP decode
        writer.println(String.format("    }"));
        writer.println();
        writer.println(String.format("    @Override"));
        writer.println(String.format("    public String toString() {"));
        writer.println(String.format("        StringBuffer sb = new StringBuffer();"));
        writer.println(String.format("        sb.append(getClass().getSimpleName());"));
        writer.println(String.format("        sb.append(\" (\");"));
        for (NumericParam param : params) {
            writer.println(String.format("        sb.append(String.format(\"%s = %%s, \", %s));", param.paramSymbol, param.paramSymbol));
        }
        writer.println(String.format("        sb.append(\")\");"));
        writer.println(String.format("        return sb.toString();"));
        writer.println(String.format("    }"));
        writer.println(String.format("}"));
        writer.println(String.format(""));
        writer.close();
    }
    
//  .headers on
//  .mode csv
//  .output Numerics.csv
//  select 0 as type,tableID,paramNo,paramSymbol,dataTypeID,dimension,unitID from paramInfoNumeric order by tableId asc, paramNo asc;
//  .output Statuses.csv
//  select 0 as type,tableID,paramNo,paramSymbol,dataTypeID,status from paramInfoStatus order by tableId asc, paramNo asc;
//  .output Commands.csv
//  select 1 as type,tableID,paramNo,paramSymbol,dataTypeID,value from paramInfoCmd order by tableId asc, paramNo asc;
    public static class CommandParam {
        public int type;
        public int tableId;
        public int paramId;
        public String paramSymbol;
        public int dataTypeId;
        public boolean variableValue;
        public int value;
    }
    
    public static List<String> generateCommands(File file, File dir) throws Exception {
        List<CommandParam> params = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        lines.remove(0);
        for (String line : lines) {
            String[] fields = line.split(",");
            CommandParam param = new CommandParam();
            param.type = Integer.parseInt(fields[0].trim());
            param.tableId = Integer.parseInt(fields[1].trim());
            param.paramId = Integer.parseInt(fields[2].trim());
            param.paramSymbol = fields[3].trim();
            if (param.paramSymbol.equals("/")) {
                continue;
            }
            param.dataTypeId = Integer.parseInt(fields[4].trim());
            if (fields[5].trim().equals("n")) {
                param.variableValue = true;
            }
            else {
                param.value = Integer.parseInt(fields[5].trim());
            }
            params.add(param);
        }
        HashMap<String, List<CommandParam>> partitioned = new HashMap<>();
        for (CommandParam p : params) {
            String name = String.format("Command_%d_%d", p.tableId, p.paramId); 
            List<CommandParam> bucket = partitioned.get(name);
            if (bucket == null) {
                bucket = new ArrayList<>();
                partitioned.put(name, bucket);
            }
            bucket.add(p);
        }
        List<String> names = new ArrayList<>();
        for (String name : partitioned.keySet()) {
            List<CommandParam> bucket = partitioned.get(name);
            if (bucket.size() == 1) {
                name = bucket.get(0).paramSymbol;
            }
            else if (bucket.size() == 2) {
                name = "";
                for (CommandParam p : bucket) {
                    name += p.paramSymbol;
                }
            }
            generateCommand(dir, name, bucket);
            names.add(name);
        }
        return names;
    }
    
    public static class NumericParam {
        public int type;
        public int tableId;
        public int paramId;
        public String paramSymbol;
        public int dataTypeId;
        public int dimension;
        public int unitId;
    }
    
//  .output Numerics.csv
//  select 0 as type,tableID,paramNo,paramSymbol,dataTypeID,dimension,unitID from paramInfoNumeric order by tableId asc, paramNo asc;
    public static List<String> generateNumerics(File file, File dir) throws Exception {
        List<NumericParam> params = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());
        lines.remove(0);
        for (String line : lines) {
            String[] fields = line.split(",");
            NumericParam param = new NumericParam();
            param.type = Integer.parseInt(fields[0].trim());
            param.tableId = Integer.parseInt(fields[1].trim());
            param.paramId = Integer.parseInt(fields[2].trim());
            param.paramSymbol = sanitizeSymbol(fields[3].trim());
            // TODO STOPSHIP need to handle these by looking at the byteNum so we can keep
            // the right offsets, or maybe just use byteNum and remark for the encode/decode.
            if (fields[4].trim().equals("/")) {
                continue;
            }
            param.dataTypeId = Integer.parseInt(fields[4].trim());
            if (fields[5].trim().isEmpty()) {
                continue;
            }
            if (fields[5].trim().equals("/")) {
                continue;
            }
            param.dimension = Integer.parseInt(fields[5].trim());
            if (fields[6].trim().equals("/")) {
                continue;
            }
            param.unitId = Integer.parseInt(fields[6].trim());
            params.add(param);
        }
        HashMap<String, List<NumericParam>> partitioned = new HashMap<>();
        for (NumericParam p : params) {
            String name = String.format("Numeric_%d", p.tableId);
            List<NumericParam> bucket = partitioned.get(name);
            if (bucket == null) {
                bucket = new ArrayList<>();
                partitioned.put(name, bucket);
            }
            bucket.add(p);
        }
        List<String> names = new ArrayList<>();
        for (String name : partitioned.keySet()) {
            generateNumeric(dir, name, partitioned.get(name));
            names.add(name);
        }
        return names;
    }
    
    public static void main(String[] args) throws Exception {
        File dir = new File("/Users/jason/Projects/openpnp/openpnp/src/main/java/org/openpnp/machine/chmt36va/packets/");
        for (File file : dir.listFiles()) {
            file.delete();
        }
        List<String> names = new ArrayList<>();
        names.addAll(generateCommands(new File("/Users/jason/Projects/openpnp/CHMT36VA/Commands.csv"), dir));
        names.addAll(generateNumerics(new File("/Users/jason/Projects/openpnp/CHMT36VA/Numerics.csv"), dir));
        PrintWriter writer = new PrintWriter(new File(dir, "Packets.java"));
        writer.println(String.format("package org.openpnp.machine.chmt36va.packets;"));
        writer.println(String.format(""));
        writer.println(String.format("import org.openpnp.machine.chmt36va.Packet;"));
        writer.println(String.format("import java.util.List;"));
        writer.println(String.format(""));
        writer.println(String.format("public class Packets {"));
        writer.println(String.format("    public static void register(List<Packet> packets) {"));
        for (String name : names) {
            writer.println(String.format("        packets.add(new org.openpnp.machine.chmt36va.packets.%s());", name));
        }
        writer.println(String.format("    }"));
        writer.println(String.format("}"));
        writer.close();
    }
}
