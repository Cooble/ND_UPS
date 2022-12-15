package cz.cooble.ndc.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import static cz.cooble.ndc.net.Net.byteBufferAllocate;

public class Socket {
    DatagramChannel channel;
    Selector selector;
    InetSocketAddress address;

    static class ClientRecord {
        public SocketAddress clientAddress;
        public NetBuffer buffer = new NetBuffer(2048);
    }


    public Socket(int port) {
        try {
            selector = Selector.open();
            address = new InetSocketAddress("0.0.0.0", port);
            channel = DatagramChannel.open();
            channel.socket().bind(new InetSocketAddress(port));

            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ, new ClientRecord());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public int port(){
        return channel.socket().getLocalPort();
    }

    public void close() {
        try {
            selector.close();
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private NetResponseFlags receiveByKey(SelectionKey key, Message m) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        ClientRecord clntRec = (ClientRecord) key.attachment();
        m.buffer.clear();
        clntRec.clientAddress = channel.receive(m.buffer.getInner());
        if (clntRec.clientAddress != null) {  // Did we receive something?
            // Register write with the selector
            //key.interestOps(SelectionKey.OP_WRITE);
            m.buffer.setSize(m.buffer.getInner().position());
            m.address = (InetSocketAddress) clntRec.clientAddress;
            return NetResponseFlags.Success;
        }
        return NetResponseFlags.Error;
    }
    public NetResponseFlags receive(Message m) {
        try {
            if (selector.selectNow() == 0)
                return NetResponseFlags.Error;

            // Get iterator on set of keys with I/O to process
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next(); // Key is bit mask

                // Client socket channel has pending data?
                if (key.isReadable()) {
                    var response = receiveByKey(key, m);
                    keyIter.remove();
                    if (response == NetResponseFlags.Success)
                        return NetResponseFlags.Success;
                }
                keyIter.remove();

                // Client socket channel is available for writing and
                // key is valid (i.e., channel not closed).
                // if (key.isValid() && key.isWritable())
                //    handleWrite(key);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return NetResponseFlags.Error;
    }

    public NetResponseFlags send(Message m) {
        return send(m.address, m.buffer);
    }
    public NetResponseFlags send(InetSocketAddress a, NetBuffer b) {
        try {
            b.flip();
            channel.send(b.getInner(), a);
        } catch (IOException e) {
            e.printStackTrace();
            return NetResponseFlags.Error;
        }
        return NetResponseFlags.Success;
    }
}
