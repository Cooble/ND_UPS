package cz.cooble.ndc.net;

import cz.cooble.ndc.Globals;
import org.joml.Vector2f;
import org.joml.Vector2i;

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
        while (true) {
            char c = (char)buffer.getInner().get(pointer+i);

            while (buffer.getInner().get() != Globals.TERMINATOR)
                i++;
            if (buffer.getInner().limit() <= pointer + i + 1
                    || buffer.getInner().get(pointer + i + 1) != Globals.TERMINATOR) {
                break;
            }
            i++;//skip terminator
            i++;//skip terminator
            buffer.getInner().get();
        }
        byte[] bu = new byte[i];
        buffer.getInner().position(pointer);
        buffer.getInner().get(bu);
        buffer.getInner().get();//read terminator
        pointer = buffer.getInner().position();

        StringBuilder builder = new StringBuilder();
        var a = new String(bu).toCharArray();
        for (int j = 0; j < a.length; j++) {
            char c = a[j];
            if (c == Globals.TERMINATOR)
                j++;
            builder.append(c);
        }
        var out =  builder.toString();
        if(out.equals(" "))
            return "";
        return out;
    }

    public int getInt() {
        return Integer.parseInt(getString());
    }

    public Vector2f getVector2f() {
        return new Vector2f(getFloat(), getFloat());
    }

    public Vector2i getVector2i() {
        return new Vector2i(getInt(), getInt());
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
        if (e instanceof Vector2f)
            return (T) getVector2f();
        if (e instanceof Vector2i)
            return (T) getVector2i();
        return null;
    }

    public <T> List<T> getArray(Supplier<T> constructor) {
        int size = getInt();
        List<T> out = new ArrayList<>();
        for (int j = 0; j < size; j++)
            out.add(get(constructor));
        return out;
    }

    public boolean getBool() {
        return getInt() != 0;
    }
}
