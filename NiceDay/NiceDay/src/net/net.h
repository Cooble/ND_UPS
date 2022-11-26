#pragma once
#include "ndpch.h"
#include <ws2def.h>


enum NetResponseFlags_
{
	NetResponseFlags_Success,
	NetResponseFlags_Error,
	NetResponseFlags_Timeout,
};

namespace nd::net
{
	typedef int SocketID;

	struct CreateSocketInfo
	{
		uint32_t port;
		bool async;
	};

	struct Address
	{
		sockaddr_in src;
		Address() = default;
		Address(const std::string& ip, int port);
		Address(unsigned long ip, int port);
		static Address build(const std::string& ipWithPort);
		std::string toString() const;
		int port() const;
		std::string ip() const;
	};

	struct Socket
	{
		SocketID m_sock;
		Address m_address;

		bool operator==(std::vector<Socket>::const_reference socket) const
		{
			// todo make proper equal
			return m_sock == socket.m_sock;
		}
	};


	struct Buffer
	{
		std::vector<char> buff;
		int siz = 0;

		void reserve(size_t size)
		{
			buff.resize(size);
			siz = 0;
		}

		void setSize(int size)
		{
			ASSERT(size <= capacity(), "invalid size");
			siz = size;
		}

		auto data() { return buff.data(); }
		auto data() const { return buff.data(); }
		int size() const { return siz; }
		int capacity() const { return (int)buff.capacity(); }
	};

	struct BufferWriter
	{
		Buffer& b;
		int pointer;
		bool expand;

		BufferWriter(Buffer& b, bool append = true, bool expand = true): b(b), pointer(b.size()), expand(expand)
		{
			if (!append)
			{
				b.setSize(0);
				pointer = 0;
			}
		}

		template <typename T>
		void write(const T* t, int count)
		{
			const int sizeOfT = count * sizeof(T);

			ASSERT(expand || pointer + sizeOfT <= b.capacity(), "not enough space");
			if (expand && pointer + sizeOfT > b.capacity())
				b.reserve((pointer + sizeOfT) * 2);

			auto p = b.data() + pointer;

			memcpy(p, t, sizeOfT);

			pointer += sizeOfT;
			b.setSize(pointer);
		}

		template <typename T>
		void write(const T& t)
		{
			if constexpr (std::is_same_v<std::remove_const_t<T>, char*>)
			{
				write(std::to_string(t));
				return;
			}
			ASSERT(expand || pointer + sizeof(T) <= b.capacity(), "not enough space");
			if (expand && pointer + sizeof(T) > b.capacity())
				b.reserve((pointer + sizeof(T)) * 2);

			auto p = b.data() + pointer;

			memcpy(p, &t, sizeof(T));

			pointer += sizeof(T);
			b.setSize(pointer);
		}

		template <>
		void write<std::string>(const std::string& t)
		{
			ASSERT(expand || pointer + t.size() <= b.capacity(), "not enough space");
			if (expand && pointer + t.size() > b.capacity())
				b.reserve((pointer + t.size()) * 2);

			auto p = b.data() + pointer;

			memcpy(p, t.c_str(), t.size());
			pointer += t.size();

			if (pointer < b.capacity())
				b.data()[pointer++] = '\0';

			b.setSize(pointer);
		}

		template <>
		void write<const char*>(const char* const& t)
		{
			write(std::string(t));
		}
	};

	struct BufferReader
	{
		Buffer& b;
		int pointer;

		BufferReader(Buffer& b) : b(b), pointer(0)
		{
		}

		template <typename T>
		void read(T& t)
		{
			ASSERT(pointer + sizeof(T) <= b.size(), "not enough space");
			memcpy(&t, b.data() + pointer, sizeof(T));

			pointer += sizeof(T);
		}

		template <typename T>
		T* tryRead()
		{
			if (pointer + sizeof(T) > b.size())
				return nullptr;

			auto out = reinterpret_cast<T*>(b.data() + pointer);
			pointer += sizeof(T);
			return out;
		}

		std::string readStringUntilNull(int maxSize)
		{
			if (maxSize == -1)
				maxSize = std::numeric_limits<int>::max();

			auto remaingSize = std::min(maxSize, b.size() - pointer);

			if (remaingSize <= 0)
				return "";

			auto out = b.data() + pointer;

			if (*out == '\0')
			{
				pointer++;
				return "";
			}
			auto length = strnlen_s(b.data() + pointer, remaingSize);

			pointer += length;

			//add one to discard null character as well
			pointer += pointer < b.size() && *(b.data() + pointer) == '\0';

			return std::string(out, length);
		}
	};

	struct Message
	{
		Address address;
		Buffer buffer;
	};

	NetResponseFlags_ init();
	void deinit();
	NetResponseFlags_ createSocket(Socket&, const CreateSocketInfo&);
	NetResponseFlags_ closeSocket(Socket&);
	NetResponseFlags_ receive(const Socket&, Message&);
	NetResponseFlags_ send(const Socket&, const Message&);
};