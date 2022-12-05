package cz.cooble.ndc.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NetReader {

    public NetBuffer buffer;
    public int pointer;

    public NetReader(NetBuffer buffer) {
        this.buffer = buffer;
        buffer.getInner().limit(buffer.size());
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

    public String getString() {
        buffer.getInner().position(pointer);
        int i = 0;
        while (buffer.getInner().get() != 0)
            i++;
        byte[] bu = new byte[i];
        buffer.getInner().position(pointer);
        buffer.getInner().get(bu);
        buffer.getInner().get();//read 0
        pointer = buffer.getInner().position();
        return new String(bu);
    }

    public int getInt() {
        return Integer.parseInt(getString());
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

    public <T> List<T> getArray(Supplier<T> constructor) {
        int size = getInt();
        List<T> out = new ArrayList<>();
        for (int j = 0; j < size; j++)
            out.add(get(constructor));
        return out;
    }
}
