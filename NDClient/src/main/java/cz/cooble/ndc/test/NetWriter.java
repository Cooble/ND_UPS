package cz.cooble.ndc.test;

import cz.cooble.ndc.Globals;
import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.NetSerializable;
import org.joml.Vector2f;
import org.joml.Vector2i;

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
        buffer.getInner().put((byte) Globals.TERMINATOR);//null terminator
        pointer = buffer.getInner().position();
        buffer.setSize(pointer);
    }

    public void putVector2f(Vector2f v) {
        put(v.x);
        put(v.y);
    }

    public void putVector2i(Vector2i v) {
        put(v.x);
        put(v.y);
    }

    public <T> void put(T i) {
        if (i instanceof NetSerializable)
            ((NetSerializable) i).serialize(this);
        else if (i instanceof Vector2f)
            putVector2f((Vector2f) i);
        else if (i instanceof Vector2i)
            putVector2i((Vector2i) i);
        else if (i instanceof Boolean)
            putString((boolean) i ? "1" : "0");
        else
            putString("" + i);
    }

    public <T> void putArray(List<T> i) {
        put(i.size());
        for (var a : i)
            put(a);
    }
}
