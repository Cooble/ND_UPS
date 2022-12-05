#include "C:/D/Uni/nd/ND_UPS/NiceDay/NiceDay/CMakeFiles/NiceDay.dir/Debug/cmake_pch.hxx"
#include "TCPTunnel.h"

#include <WinSock2.h>

namespace nd::net
{
void TCPTunnel::receiveFatPacket(Message& m)
{
	SegmentPacket h;
	auto reader = NetReader(m.buffer);
	reader.get(h);

	if (h.packetId < waitingForFirst().packetId)
	{
		// already have
		m_current_ack.ack_ids.push_back(h.packetId);
		return;
	}

	auto offset = h.packetId - waitingForFirst().packetId;

	// too small buffer
	if (offset >= m_in_window.size())
	{
		m_current_ack.nack_ids.push_back(h.packetId);
		return;
	}

	auto& packet = m_in_window[offset];

	// already have then same packet
	if (!packet.isAbsent())
	{
		m_current_ack.ack_ids.push_back(h.packetId);
		return;
	}


	packet.length = m.buffer.size() - reader.pointer;
	packet.memoryIdx = h.memoryIdx;
	packet.packetId = h.packetId;

	bool writtenAll = m_buff_in.write(m.buffer.data() + reader.pointer, packet.memoryIdx, packet.length) == packet.
		length;

	// cannot save message, buffer full
	if (!writtenAll)
	{
		// dont forget to reset packet length to 0, -> no data
		packet.length = 0;
		//send buffer is full
		m_current_ack.nack_ids.push_back(h.packetId);
		return;
	}

	// keep shifting the window until first packet is absent
	Pac first;
	while (!(first = m_in_window[0]).isAbsent())
	{
		m_buff_in.seekp(first.nextMemoryIdx());
		m_in_window.erase(m_in_window.begin());

		Pac last = m_in_window[m_in_window.size() - 1];
		Pac newLast = {last.packetId + 1, last.nextMemoryIdx(), 0, false};
		m_in_window.push_back(newLast);
	}
	// send ack
	m_current_ack.ack_ids.push_back(h.packetId);
}

void TCPTunnel::receiveAckPacket(Message& m)
{
	AckPacket h;
	auto reader = NetReader(m.buffer);
	reader.get(h);

	for (auto ack : h.ack_ids)
	{
		//skip old acknowledgments or weirdly new
		if (ack < m_out_window[0].packetId || ack > m_out_window[m_out_window.size() - 1].packetId)
			continue;

		auto offset = ack - m_out_window[0].packetId;
		auto& packet = m_out_window[offset];
		if (!packet.isAbsent())
			packet.acknowledge();
	}
	while (m_out_window[0].isAck)
	{
		m_buff_out.seekg(m_out_window[0].nextMemoryIdx());
		m_out_window.erase(m_out_window.begin());

		Pac last = m_out_window[m_out_window.size() - 1];
		Pac newLast = {last.packetId + 1, 0, 0, false};
		m_out_window.push_back(newLast);
	}
}

void TCPTunnel::receiveTunnelUDP(Message& m)
{
	SegmentPacket h;
	if (NetReader(m.buffer).get(h))
	{
		if (h.channelId != m_channel_id)
			return;
		receiveFatPacket(m);
		m_address = m.address;
		return;
	}
	AckPacket p;
	if (NetReader(m.buffer).get(p))
	{
		if (p.clientId != m_channel_id)
			return;
		receiveAckPacket(m);
		m_address = m.address;
	}
}

void TCPTunnel::flushTunnelUDP(Message& m)
{
	m.buffer.reserve(MAX_UDP_PACKET_LENGTH);

	// add new packets to window if window is not full and data is available in out buffer
	while (
		m_current_write.packetId <= m_out_window[m_out_window.size() - 1].packetId &&
		m_buff_out.available(m_current_write.memoryIdx))
	{
		auto writer = NetWriter(m.buffer);

		// we dont care about data just the offset of writer pointer
		SegmentPacket fat;
		fat.packetId = m_current_write.packetId;
		fat.memoryIdx = m_current_write.memoryIdx;
		fat.channelId = m_channel_id;

		writer.put(fat);

		// get size, we actually ignore memory copying
		auto written = m_buff_out.read(m.buffer.data() + writer.pointer,
		                               m_current_write.memoryIdx,
		                               m.buffer.capacity() - writer.pointer);

		// update packet in window
		auto& writtenPacket = m_out_window[m_current_write.packetId - m_out_window[0].packetId];
		writtenPacket.memoryIdx = m_current_write.memoryIdx;
		writtenPacket.length = written;
		writtenPacket.timeout = 0; //send immediately

		// shift to next current pointer packet
		m_current_write.packetId++;
		m_current_write.memoryIdx += written;
	}

	flushTimeouts(m);
}

void TCPTunnel::flushTimeouts(Message& m)
{
	// send unacknowledged packets in window that timed out
	m.address = m_address;
	auto now = nowTime();
	for (size_t i = 0; i < m_out_window.size() && m_out_window[i].packetId < m_current_write.packetId; ++i)
	{
		auto& packet = m_out_window[i];
		if (packet.isAck)
			continue;
		if (now - packet.timeout < TCP_TUNNEL_TIMEOUT_MS)
			continue;

		packet.timeout = now;

		SegmentPacket fat;
		fat.packetId = packet.packetId;
		fat.memoryIdx = packet.memoryIdx;
		fat.channelId = m_channel_id;

		auto writer = NetWriter(m.buffer);
		writer.put(fat);

		m_buff_out.read(m.buffer.data() + writer.pointer,
		                packet.memoryIdx,
		                packet.length);
		m.buffer.setSize(packet.length + m.buffer.size());
		send(m_socket, m);
	}
}

void TCPTunnel::flushAck(Message& m)
{
	//nothing to write
	if (m_current_ack.isEmpty())
		return;
	NetWriter(m.buffer).put(m_current_ack);
	m_current_ack.clear();

	m.address = m_address;
	send(m_socket, m);
}

bool TCPTunnel::write(const Message& m)
{
	size_t size;

	auto freeSpace = m_buff_out.freeSpace();

	if (freeSpace < m.buffer.size() + sizeof(size))
		return false;

	//size = htonll(m.buffer.size());
	size = m.buffer.size(); // lets stick with little endian

	m_buff_out.write((const char*)&size, sizeof(size));
	m_buff_out.write(m.buffer.data(), m.buffer.size());

	return true;
}

bool TCPTunnel::read(Message& m)
{
	size_t size;
	if (m_buff_in.available() <= sizeof(size))
		return false;

	// read the size without moving cursor
	m_buff_in.read((char*)&size, m_buff_in.tellg(), sizeof(size));

	//size = ntohll(size);// lets stick with little endian

	// whole message is not sent yet
	if (m_buff_in.available() < sizeof(size) + size)
		return false;

	// move cursor over the size
	m_buff_in.seekg(m_buff_in.tellg() + sizeof(size));
	m.buffer.reserve(size);
	m.buffer.setSize(size);

	// read the sweet data
	m_buff_in.read(m.buffer.data(), m.buffer.size());
	m.address = m_address;
	return true;
}
}
