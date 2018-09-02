package org.openpnp.machine.chmt36va;

public interface Packet {
    byte[] encode() throws Exception;
    void decode(byte[] bytes) throws Exception;
    public int getTableId();
}