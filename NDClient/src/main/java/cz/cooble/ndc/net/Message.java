package cz.cooble.ndc.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import static cz.cooble.ndc.net.Net.byteBufferAllocate;

public class Message {
    public InetSocketAddress address;
    public ByteBuffer buffer;

    public Message() {}

    public Message(int size) {
        buffer = byteBufferAllocate(size);
    }
}
