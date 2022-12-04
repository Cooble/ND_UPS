#include "net.h"
#include "ndpch.h"
#include <WinSock2.h>


#ifdef ND_PLATFORM_WINDOWS
#include <WinSock2.h>
#include <WS2tcpip.h>
#include <WS2spi.h>
// need link with Ws2_32.lib
#pragma comment(lib, "Ws2_32.lib")
#else
#include <sys/socket.h>
#endif


namespace nd::net
{
	std::vector<Socket> s_sockets;

	Address::Address(const std::string& ip, int port)
		
	{
		src = {};
		src.sin_family = AF_INET; /* Internet/IP */
		src.sin_addr.s_addr = inet_addr(ip.c_str()); /* Any IP address */
		src.sin_port = htons(port); /* server port */
	}

	Address::Address(unsigned long ip, int port)
		:Address()
	{
		src = {};
		src.sin_family = AF_INET; /* Internet/IP */
		src.sin_addr.s_addr = htonl(ip); /* Any IP address */
		src.sin_port = htons(port); /* server port */
	}

	Address Address::build(const std::string& ipWithPort)
	{
		std::vector<std::string> split;
		SUtil::splitString(ipWithPort, split, ":");
		ASSERT(split.size() == 2, "invalid address");

		return {split[0], std::stoi(split[1])};
	}

	std::string Address::toString() const
	{
		//char str[INET_ADDRSTRLEN];

		// now get it back and print it
		//inet_ntop(AF_INET, &src, str, INET_ADDRSTRLEN);

		return ip() + ":" + std::to_string(port());
	}

	int Address::port() const
	{
		return ntohs(src.sin_port);
	}
	std::string Address::ip() const
	{
		std::stringstream s;
		s << (int)src.sin_addr.S_un.S_un_b.s_b1;
		s << ".";
		s << (int)src.sin_addr.S_un.S_un_b.s_b2;
		s << ".";
		s << (int)src.sin_addr.S_un.S_un_b.s_b3;
		s << ".";
		s << (int)src.sin_addr.S_un.S_un_b.s_b4;
		return s.str();
	}

	NetResponseFlags_ init()
	{
#ifdef ND_PLATFORM_WINDOWS

		WSADATA wsa;
		//Initialise winsock
		ND_TRACE("Initialising Winsock...");
		if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0)
		{
			ND_ERROR("Failed. Error Code :{}", WSAGetLastError());
			return NetResponseFlags_Error;
		}
		ND_TRACE("winsock Initialised.\n");

#endif
		return NetResponseFlags_Success;
	}

	void deinit()
	{
		while (!s_sockets.empty())
			closeSocket(s_sockets[0]);
	}

	int findSocket(const Socket& s)
	{
		for (int i = 0; i < s_sockets.size(); ++i)
			if (s_sockets[i] == s)
				return i;
		return -1;
	}

	NetResponseFlags_ createSocket(Socket& s, const CreateSocketInfo& info)
	{
		/* Create the UDP socket */
		if ((s.m_sock = socket(PF_INET, SOCK_DGRAM, IPPROTO_UDP)) < 0)
		{
			ND_ERROR("Cannot create socket, make sure to call nd::net::init() before creating any socket.");
			return NetResponseFlags_Error;
		}

		/* Construct the server sockaddr_in structure */


		s.m_address = Address(INADDR_ANY, info.port);

		/* Bind the socket */
		if (bind(s.m_sock, (sockaddr*)&s.m_address.src, sizeof(s.m_address)) < 0)
		{
			ND_ERROR("Cannot bind socket");
			return NetResponseFlags_Error;
		}

		//enable async
		u_long enabled = info.async;
		ioctlsocket(s.m_sock, FIONBIO, &enabled);

		ND_INFO("Server started listening on port {}", info.port);

		s_sockets.push_back(s);
		return NetResponseFlags_Success;
	}


	NetResponseFlags_ closeSocket(Socket& s)
	{
		int socketid = findSocket(s);

		if (socketid == -1)
		{
			ND_WARN("Attempt at closing non existing socket");
			return NetResponseFlags_Error;
		}
		s_sockets.erase(s_sockets.begin() + socketid);
		closesocket(s.m_sock);

		return NetResponseFlags_Success;
	}

	NetResponseFlags_ receive(const Socket& socket, Message& message)
	{
		int SenderAddrSize = sizeof(message.address.src);

		int received = recvfrom(socket.m_sock, message.buffer.data(), message.buffer.capacity(), 0,
		                        (sockaddr*)&message.address.src, &SenderAddrSize);
		if (received != SOCKET_ERROR /* || received != ERROR_TIMEOUT*/)
		{
			message.buffer.setSize(received);
			return NetResponseFlags_Success;
		}
		return NetResponseFlags_Error;
	}

	NetResponseFlags_ send(const Socket& s, const Message& m)
	{
		if (sendto(s.m_sock, m.buffer.data(), m.buffer.size(), 0, (sockaddr*)&m.address.src,
			sizeof(m.address.src)) == SOCKET_ERROR) {
			ASSERT(false, "Error during sending to address {}",m.address.toString());
			return NetResponseFlags_Error;
		}
		return NetResponseFlags_Success;
	}
}
