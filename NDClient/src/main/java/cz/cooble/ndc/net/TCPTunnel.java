package cz.cooble.ndc.net;

import cz.cooble.ndc.test.NetWriter;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static cz.cooble.ndc.core.App.nowTime;
import static cz.cooble.ndc.net.Net.byteBufferAllocate;

public class TCPTunnel {

    public static final int TCP_PACKET_FAT_ID = -1;
    public static final int TCP_PACKET_ACK_ACTION = -2;

    static class SegmentPacket implements NetSerializable {
        int channelId;
        int packetId;
        int memoryIdx;

        @Override
        public void deserialize(NetReader b) {
            if (b.getInt() != TCP_PACKET_FAT_ID)
                throw new RuntimeException("Wrong data");
            if (!b.getString().equals("TCP"))
                throw new RuntimeException("Wrong data");
            channelId = b.getInt();
            packetId = b.getInt();
            memoryIdx = b.getInt();
        }

        @Override
        public void serialize(NetWriter b) {
            b.put(TCP_PACKET_FAT_ID);
            b.put("TCP");
            b.put(channelId);
            b.put(packetId);
            b.put(memoryIdx);
        }
    }

    static class AckPacket implements NetSerializable {
        public int channelId;
        public List<Integer> ack_ids = new ArrayList<>();
        public List<Integer> nack_ids = new ArrayList<>();

        public boolean isEmpty() {
            return ack_ids.isEmpty() && nack_ids.isEmpty();
        }

        public void clear() {
            ack_ids.clear();
            nack_ids.clear();
        }

        @Override
        public void deserialize(NetReader b) {
            if (b.getInt() != TCP_PACKET_ACK_ACTION)
                throw new RuntimeException("Invalid data");
            if (!b.getString().equals("TCP_ACK"))
                throw new RuntimeException("Invalid data");

            channelId = b.getInt();
            ack_ids = b.getArray(() -> 1);
            nack_ids = b.getArray(() -> 1);
        }

        @Override
        public void serialize(NetWriter b) {
            b.put(TCP_PACKET_ACK_ACTION);
            b.put("TCP_ACK");
            b.put(channelId);
            b.putArray(ack_ids);
            b.putArray(nack_ids);
        }
    }

    static class Pac {
        public int packetId;
        public int memoryIdx;
        public int length;
        public long timeout;

        public boolean isAbsent() {return length == 0;}

        public boolean isAck;

        public int nextMemoryIdx() {return length + memoryIdx;}

        public void acknowledge() {isAck = true;}
    }

    public static final int TCP_TUNNEL_TIMEOUT_MS = 100;
    public static final int PACKETS_IN_WINDOW = 50;
    //static constexpr size_t MAX_UDP_PACKET_LENGTH = 1024 + 100; //dont forget to include sizeof(FatPacket)
    public static final int MAX_UDP_PACKET_LENGTH = 1000; //dont forget to include sizeof(FatPacket)


    Socket m_socket;
    int m_channel_id;
    NetRingBuffer m_buff_in, m_buff_out;
    List<Pac> m_in_window = new ArrayList<>();
    List<Pac> m_out_window = new ArrayList<>();

    Pac m_current_write = new Pac();
    AckPacket m_current_ack = new AckPacket();
    InetSocketAddress m_address;
    //Buffer m_out_message_buf;

    public TCPTunnel(Socket socket, InetSocketAddress a, int id, int buffSize) {
        m_socket = socket;
        m_address = a;
        m_channel_id = id;

        m_buff_in = new NetRingBuffer(buffSize);
        m_buff_out = new NetRingBuffer(buffSize);
        m_current_ack.channelId = m_channel_id;

        for (int i = 0; i < PACKETS_IN_WINDOW; ++i) {
            Pac p = new Pac();
            p.packetId = i;
            m_in_window.add(p);

            p = new Pac();
            p.packetId = i;
            m_out_window.add(p);
        }
    }

    // returns client id of a message or -1 if message is not tcp
    public static int getClientID(Message m) {
        var h = new SegmentPacket();
        try {
            h.deserialize(new NetReader(m.buffer));
            return h.channelId;
        } catch (Exception e) {
            var p = new AckPacket();
            try {
                p.deserialize(new NetReader(m.buffer));
                return p.channelId;
            } catch (Exception ee) {
                return -1;
            }
        }
    }

    // receive udp packet
    // before calling you have to check that getClientID() returns id of this tunnel
    public void receiveTunnelUDP(Message m) {
        var h = new SegmentPacket();
        try {
            h.deserialize(new NetReader(m.buffer));
            if (h.channelId != m_channel_id)
                return;
            receiveFatPacket(m);
            m_address = m.address;
        } catch (Exception e) {
            var p = new AckPacket();
            try {
                p.deserialize(new NetReader(m.buffer));
                if (p.channelId != m_channel_id)
                    return;
                receiveAckPacket(m);
                m_address = m.address;
            } catch (Exception ee) {
            }
        }
    }

    // sends buffered udp packets
    // content of message is not relevant
    public void flushTunnelUDP(Message m) {
        m.buffer.reserve(MAX_UDP_PACKET_LENGTH);
        // add new packets to window if window is not full and data is available in out buffer
        while (
                m_current_write.packetId <= m_out_window.get(m_out_window.size() - 1).packetId &&
                        m_buff_out.available(m_current_write.memoryIdx) != 0) {

            // we dont care about data just the offset of writer pointer
            SegmentPacket fat = new SegmentPacket();
            fat.packetId = m_current_write.packetId;
            fat.memoryIdx = m_current_write.memoryIdx;
            fat.channelId = m_channel_id;

            var writer = new NetWriter(m.buffer);
            writer.put(fat);

            // get size, we actually ignore memory copying
            var written = m_buff_out.read(
                    m.buffer.slice(writer.pointer, m.buffer.capacity() - writer.pointer),
                    m_current_write.memoryIdx);

            // update packet in window
            var writtenPacket = m_out_window.get(m_current_write.packetId - m_out_window.get(0).packetId);
            writtenPacket.memoryIdx = m_current_write.memoryIdx;
            writtenPacket.length = written;
            writtenPacket.timeout = 0; //send immediately

            // shift to next current pointer packet
            m_current_write.packetId++;
            m_current_write.memoryIdx += written;
        }

        flushTimeouts(m);
    }

    // flush current ack packet
    public void flushAck(Message m) {
        //nothing to write
        if (m_current_ack.isEmpty())
            return;

        m_current_ack.serialize(new NetWriter(m.buffer));
        m_current_ack.clear();

        m.address = m_address;
        m_socket.send(m);
    }

    //flush everything
    public void flush(Message m) {
        flushAck(m);
        flushTunnelUDP(m);
    }

    private ByteBuffer G = byteBufferAllocate(8);

    public boolean write(Message m) {
        int size;

        long freeSpace = m_buff_out.freeSpace();

        if (freeSpace < m.buffer.size() + 8)
            return false;

        size = m.buffer.size();
        G.putLong(0, size);
        m_buff_out.write(G);
        m.buffer.flip();
        m_buff_out.write(m.buffer.getInner());
        return true;
    }

    public boolean read(Message m) {
        long size;
        if (m_buff_in.available() <= 8)
            return false;

        // read the size without moving cursor
        m_buff_in.read(G, m_buff_in.tellg());
        size = G.getLong(0);
        //size = ntohll(size);// lets stick with little endian

        // whole message is not sent yet
        if (m_buff_in.available() < 8 + size)
            return false;

        // move cursor over the size
        m_buff_in.seekg(m_buff_in.tellg() + 8);
        m.buffer.reserve((int) size);
        m.buffer.setSize((int) size);

        // read the sweet data
        m_buff_in.read(m.buffer.slice(0, (int) size));
        m.address = m_address;
        return true;
    }

    private void flushTimeouts(Message m) {
        // send unacknowledged packets in window that timed out
        m.address = m_address;
        var now = nowTime();
        for (int i = 0; i < m_out_window.size() && m_out_window.get(i).packetId < m_current_write.packetId; ++i) {
            var packet = m_out_window.get(i);
            if (packet.isAck)
                continue;
            if (now - packet.timeout < TCP_TUNNEL_TIMEOUT_MS)
                continue;

            packet.timeout = now;

            SegmentPacket fat = new SegmentPacket();
            fat.packetId = packet.packetId;
            fat.memoryIdx = packet.memoryIdx;
            fat.channelId = m_channel_id;

            var writer = new NetWriter(m.buffer);
            fat.serialize(writer);

            m_buff_out.read(m.buffer.slice(writer.pointer, packet.length), packet.memoryIdx);
            m.buffer.setSize(packet.length + writer.pointer);
            m_socket.send(m);
        }
    }

    private void receiveAckPacket(Message m) {
        AckPacket h = new AckPacket();
        h.deserialize(new NetReader(m.buffer));

        for (var ack : h.ack_ids) {
            //skip old acknowledgments or weirdly new
            if (ack < m_out_window.get(0).packetId || ack > m_out_window.get(m_out_window.size() - 1).packetId)
                continue;

            var offset = ack - m_out_window.get(0).packetId;
            var packet = m_out_window.get(offset);
            if (!packet.isAbsent())
                packet.acknowledge();
        }
        while (m_out_window.get(0).isAck) {
            m_buff_out.seekg(m_out_window.get(0).nextMemoryIdx());
            m_out_window.remove(0);

            Pac last = m_out_window.get(m_out_window.size() - 1);
            Pac newLast = new Pac();
            newLast.packetId = last.packetId + 1;
            m_out_window.add(newLast);
        }
    }

    private void receiveFatPacket(Message m) {
        SegmentPacket h = new SegmentPacket();
        var reader = new NetReader(m.buffer);
        h.deserialize(reader);

        if (h.packetId < m_in_window.get(0).packetId) {
            // already have
            m_current_ack.ack_ids.add(h.packetId);
            return;
        }

        var offset = h.packetId - m_in_window.get(0).packetId;

        // too small buffer
        if (offset >= m_in_window.size()) {
            m_current_ack.nack_ids.add(h.packetId);
            return;
        }

        var packet = m_in_window.get(offset);

        // already have then same packet
        if (!packet.isAbsent()) {
            m_current_ack.ack_ids.add(h.packetId);
            return;
        }


        packet.length = m.buffer.size() - reader.pointer;
        packet.memoryIdx = h.memoryIdx;
        packet.packetId = h.packetId;

        boolean writtenAll = m_buff_in.write(m.buffer.slice(reader.pointer, packet.length), packet.memoryIdx) == packet.
                length;

        // cannot save message, buffer full
        if (!writtenAll) {
            // dont forget to reset packet length to 0, -> no data
            packet.length = 0;
            //send buffer is full
            m_current_ack.nack_ids.add(h.packetId);
            return;
        }

        // keep shifting the window until first packet is absent
        Pac first;
        while (!(first = m_in_window.get(0)).isAbsent()) {
            m_buff_in.seekp(first.nextMemoryIdx());
            m_in_window.remove(0);

            Pac last = m_in_window.get(m_in_window.size() - 1);
            Pac newLast = new Pac();
            newLast.packetId = last.packetId + 1;
            newLast.memoryIdx = last.nextMemoryIdx();
            m_in_window.add(newLast);
        }
        // send ack
        m_current_ack.ack_ids.add(h.packetId);
    }
}
