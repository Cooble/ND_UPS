package cz.cooble.ndc.net;

import java.nio.ByteBuffer;

import static cz.cooble.ndc.Asserter.ASSERT;

public class NetRingBuffer {
    NetBuffer m_buffer;
    int m_consumer;
    int m_producer;

    public NetRingBuffer(int size) {
        reserve(size);
    }

    public void reserve(int size) {
        m_buffer = new NetBuffer((int) size);
        m_buffer.setSize((int) size);
    }

    // bytes available for read
    public int available() {
        return m_producer - m_consumer;
    }

    // bytes available for read from pointer
    public int available(int pointer) {
        if (m_producer < pointer)
            return 0;
        return m_producer - pointer;
    }

    // bytes available to write
    public int freeSpace() {
        return m_buffer.size() - available();
    }

    // bytes available to write starting from fromPointer
    public int freeSpace(int fromPointer) {
        if (fromPointer < m_producer)
            return 0;
        int avail = freeSpace();
        int requiredOffset = fromPointer - m_producer;
        if (avail < requiredOffset)
            return 0;
        return avail - requiredOffset;
    }

    // moves cursor, sending all data before it
    public void seekp(int pointer) {
        ASSERT(pointer >= m_producer, "cannot move pointer back");
        m_producer = pointer;
    }

    // moves cursor, discarding all data before it
    public void seekg(int pointer) {
        ASSERT(pointer >= m_consumer, "cannot move pointer back");
        m_consumer = pointer;
    }

    // writes at specified coordinates
    // !Does not advance writer pointer!
    // Use seekp() to advance the pointer
    public int write(ByteBuffer data, int pointer) {
        int toWrite = Math.min(data.limit(), freeSpace(pointer));
        if (toWrite == 0)
            return 0;

        int startIdx = pointer % m_buffer.size();
        int endIdx = (pointer + toWrite) % m_buffer.size();

        if (startIdx < endIdx) {
            bufferCopy(data, 0, m_buffer.getInner(), startIdx, toWrite);
        } else {
            bufferCopy(data, 0, m_buffer.getInner(), startIdx, m_buffer.size() - startIdx);
            bufferCopy(data, m_buffer.size() - startIdx, m_buffer.getInner(), 0, endIdx);
        }
        return toWrite;
    }

    // automatically advances write pointer
    public int write(ByteBuffer data) {
        var shift = write(data, m_producer);
        m_producer += shift; //wtf
        return shift;
    }

    // copies data properly since bytebuffer can have offset to underlying array
    private void bufferCopy(ByteBuffer src, int sOffset, ByteBuffer dest, int dOffset, int len) {
        System.arraycopy(src.array(), sOffset + src.arrayOffset(), dest.array(), dOffset + dest.arrayOffset(), len);
    }

    // reads from specified coordinates
    // will try to fill buffer to the full capacity
    // !Does not advance reader pointer!
    // Use seekg() to advance the pointer
    public int read(ByteBuffer data, int pointer) {
        int toRead = Math.min(data.capacity(), available(pointer));

        int startIdx = pointer % m_buffer.size();
        int endIdx = (pointer + toRead) % m_buffer.size();

        if (startIdx < endIdx) {
            bufferCopy(m_buffer.getInner(), startIdx, data, 0, toRead);
        } else {
            bufferCopy(m_buffer.getInner(), startIdx, data, 0, m_buffer.size() - startIdx);
            bufferCopy(m_buffer.getInner(), 0, data, m_buffer.size() - startIdx, endIdx);
        }
        return toRead;
    }

    // varmatically advances read pointer
    public int read(ByteBuffer data) {
        var shift = read(data, m_consumer);
        m_consumer += shift;
        return shift;
    }

    public int tellg() {return m_consumer;}

    public int tellp() {return m_producer;}
}
