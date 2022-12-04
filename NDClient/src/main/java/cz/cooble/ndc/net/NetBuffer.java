package cz.cooble.ndc.net;

import java.io.IOError;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static cz.cooble.ndc.net.Net.byteBufferAllocate;

public class NetBuffer {
    private final ByteBuffer buffer;

    public NetBuffer(int size) {
        buffer = byteBufferAllocate(size);
    }

    public NetBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void putString(String s) {
        buffer.put(s.getBytes());
        buffer.put((byte) 0);//null terminator
    }

    public String getString() {

        var start = buffer.position();
        int i = 0;
        while (buffer.get() != 0)
            i++;
        byte[] bu = new byte[i];
        buffer.position(start);
        buffer.get(bu);
        buffer.get();//read 0
        return new String(bu);
    }

    public int getInt() {
        return Integer.parseInt(getString());
    }

    public byte getByte() {
        return Byte.parseByte(getString());
    }

    public void getBytes(byte[] bytes, int size) {
        for (int i = 0; i < size; i++)
            bytes[i] = getByte();
    }

    public float getFloat() {
        return Float.parseFloat(getString());
    }

    public double getDouble() {
        return Double.parseDouble(getString());
    }

    public <T> void put(T i) {
        if (i instanceof NetSerializable) {
            ((NetSerializable) i).serialize(this);
        } else
            putString("" + i);
    }

    public <T> T get(Supplier<T> constructor) {
        T e = constructor.get();
        if (e instanceof NetSerializable) {
            ((NetSerializable) e).deserialize(this);
            return e;
        }
        if (e instanceof Integer)
            return (T) (Integer) getInt();
        if (e instanceof Byte)
            return (T) (Byte) getByte();
        if (e instanceof String)
            return (T) getString();
        if (e instanceof Float)
            return (T) (Float) getFloat();
        if (e instanceof Double)
            return (T) (Double) getDouble();
        return null;
    }

    public <T> void putArray(List<T> i) {
        put(i.size());
        for (var a : i)
            put(a);
    }

    public <T> List<T> getArray(Supplier<T> constructor) {
        int size = getInt();
        List<T> out = new ArrayList<>();
        for (int j = 0; j < size; j++)
            out.add(get(constructor));
        return out;
    }

    public void clear() {buffer.clear();}

    public ByteBuffer getInnerBuffer() {
        return buffer;
    }
}
