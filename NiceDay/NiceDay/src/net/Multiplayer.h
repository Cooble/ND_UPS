#pragma once
#include "net.h"


enum Action:int
{
	Connect,
	Disconnect,
	Ping,
	Info,
};

typedef int ConnectionID;

struct PacketHeader
{
	Action action;
	ConnectionID connection_id;

	void flipToHost();
	void flipToNet();
};

class Multiplayer
{
	typedef std::function<void(ConnectionID)> CallbackFn;

public:
	enum DisconnectReason
	{
		DisconnectReason_Client,
		DisconnectReason_Timeout
	};

	struct Connection
	{
		ConnectionID connection_id;
		nd::net::Address remote;
		uint64_t last_seen;

		uint64_t age() const { return nd::nowTime() - last_seen; }
		bool exists() const { return connection_id != 0; }
	};

private:
	nd::net::Socket m_socket;
	std::vector<Connection> m_connections;
	int m_connection_count = 0;

	bool isProperConnectionID(ConnectionID id) const { return id >= 0 && id < m_connections.size(); }
	int getFreeID();
	void disconnectConnection(ConnectionID id);


public:
	Multiplayer(int maxConnections, int port);

	void update();

	int getConnectionCount() const { return m_connection_count; }

	void onConnect(Connection& connection);
	void onDisconnect(Connection& connection, DisconnectReason reason);
	void onReceive(Connection& c, nd::net::Message& m);
	Connection* getConnection(ConnectionID id);
	size_t getSize() const { return m_connections.size(); }
};
