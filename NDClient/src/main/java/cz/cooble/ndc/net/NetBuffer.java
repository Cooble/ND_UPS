package cz.cooble.ndc.net;

import java.nio.ByteBuffer;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.net.Net.byteBufferAllocate;

public class NetBuffer {

    private ByteBuffer buffer;
    private int siz;

    public NetBuffer(int size) {
        reserve(size);
        buffer.position(0);
    }

    public NetBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public byte[] data() {return buffer.array();}
    public int dataOffset(){return buffer.arrayOffset();}

    public ByteBuffer slice(int offset, int len) {
        var lastPos = buffer.position();
        buffer.position(offset);
        var lastLimit = buffer.limit();
        var out = buffer.slice();
        buffer.position(lastPos);
        buffer.limit(lastLimit);
        return out;
    }

    public void reserve(int size) {
        if (buffer==null || size > capacity())
            buffer = byteBufferAllocate(size);
        this.siz = 0;
        buffer.clear();
    }

    public int capacity() {return buffer.capacity();}

    public int size() {
        return siz;
    }

    public void setSize(int size) {
        ASSERT(size <= capacity(), "buffer to small, cannot resize from " + siz + " to " + size+" with capacity "+ capacity());
        this.siz = size;
    }

    public void clear() {
        buffer.clear();
        siz = 0;
    }

    public ByteBuffer getInner() {
        return buffer;
    }

    // call before passing inner buffer to some foreign code
    public void flip(){
        buffer.flip();
        buffer.limit(siz);
    }

}
