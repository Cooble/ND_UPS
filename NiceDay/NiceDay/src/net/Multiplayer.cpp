#include "Multiplayer.h"

#include <WinSock2.h>

using namespace nd::net;

//constexpr uint64_t TIMEOUT = 10000;
constexpr uint64_t TIMEOUT = 0;

void PacketHeader::flipToHost()
{
	*(u_long*)&action = ntohl(action);
	*(u_long*)&connection_id = ntohl(connection_id);
}

void PacketHeader::flipToNet()
{
	*(u_long*)&action = htonl(action);
	*(u_long*)&connection_id = htonl(connection_id);
}

int Multiplayer::getFreeID()
{
	for (int i = 1; i < m_connections.size(); ++i)
	{
		if (m_connections[i].connection_id == 0)
			return i;
	}
	return -1;
}

Multiplayer::Connection* Multiplayer::getConnection(ConnectionID id)
{
	if (!isProperConnectionID(id))
		return nullptr;
	if (m_connections[id].connection_id == 0)
		return nullptr;
	return &m_connections[id];
}

void Multiplayer::disconnectConnection(ConnectionID id)
{
	ASSERT(isProperConnectionID(id), "unexisting conenction");
	m_connections[id].connection_id = 0;
	ND_INFO("Closing connection {}", id);
}

Multiplayer::Multiplayer(int maxConnections,int port)
{
	m_connections.resize(maxConnections);
	ZeroMemory(m_connections.data(), maxConnections * sizeof(Connection));

	CreateSocketInfo info = {};
	info.async = true;
	info.port = port;
	createSocket(m_socket, info);
}

void Multiplayer::update()
{
	Message m;
	m.buffer.reserve(256);

	while (receive(m_socket, m) == NetResponseFlags_Success)
	{
		if (m.buffer.size() >= sizeof(PacketHeader))
		{
			auto& header = *(PacketHeader*)m.buffer.data();
			header.flipToHost();
			auto connectionPtr = getConnection(header.connection_id);

			// update address
			if (connectionPtr)
			{
				connectionPtr->last_seen = nd::nowTime();
				connectionPtr->remote = m.address;
			}


			switch (header.action)
			{
			case Connect:
				if (!connectionPtr)
				{
					auto newId = getFreeID();
					m_connections[newId].connection_id = newId;
					m_connections[newId].last_seen = nd::nowTime();
					m_connections[newId].remote = m.address;
					ND_INFO("Creating new connection {}", newId);
					onConnect(m_connections[newId]);
				}
				break;
			case Disconnect:
				if (connectionPtr)
				{
					onDisconnect(*connectionPtr, DisconnectReason_Client);
					disconnectConnection(header.connection_id);
				}
				break;
			case Ping:
				ND_INFO("Ponging Back to {}", header.connection_id);
				header.flipToNet();
				send(m_socket, m);
				break;
			default:
				if (connectionPtr)
					onReceive(*connectionPtr, m);
			}
		}
	}

	for (auto& c : m_connections)
		if (c.exists() && c.age() > TIMEOUT && TIMEOUT != 0)
		{
			onDisconnect(c, DisconnectReason_Timeout);
			disconnectConnection(c.connection_id);
		}
}

void Multiplayer::onConnect(Connection& connection)
{
	ND_INFO("client connected {}", connection.connection_id);
}

void Multiplayer::onDisconnect(Connection& connection, DisconnectReason reason)
{
	ND_INFO("client disconnected {}", connection.connection_id);
}

void Multiplayer::onReceive(Connection& c, nd::net::Message& m)
{
	ND_INFO("received from client {}", c.connection_id);
}
