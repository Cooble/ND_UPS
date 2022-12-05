package cz.cooble.ndc.test;

import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.NetSerializable;

import java.util.List;

public class NetWriter {

    public NetBuffer buffer;
    public int pointer;

    public NetWriter(NetBuffer buffer, boolean append) {
        this.buffer = buffer;
        if (!append) {
            buffer.clear();
            pointer = 0;
        } else
            pointer = buffer.size();
        buffer.getInner().limit(buffer.capacity());
    }

    public NetWriter(NetBuffer buffer) {
        this(buffer, false);
    }

    public void putString(String s) {
        buffer.getInner().position(pointer);
        buffer.getInner().put(s.getBytes());
        buffer.getInner().put((byte) 0);//null terminator
        pointer = buffer.getInner().position();
        buffer.setSize(pointer);
    }

    public <T> void put(T i) {
        if (i instanceof NetSerializable) {
            ((NetSerializable) i).serialize(this);
        } else
            putString("" + i);
    }

    public <T> void putArray(List<T> i) {
        put(i.size());
        for (var a : i)
            put(a);
    }
}
