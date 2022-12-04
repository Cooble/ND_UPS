#include "TestTCPTunnel.h"

#include <random>

#include "net/net.h"
#include "net/TCPTunnel.h"

void fillBuffer(nd::net::Buffer& b, char data, int size)
{
	for (int i = 0; i < size; ++i)
	{
		b.data()[i] = data + i;
	}
	b.setSize(size);
}

void generateData(nd::net::Buffer& b, int index)
{
	fillBuffer(b, 0, (index) % 200 + ((bool)(index & 1)) * 100);
}

std::default_random_engine gen1(0);
std::default_random_engine gen2(0);
std::uniform_int_distribution d1(10, 2000);
std::uniform_int_distribution d2(10, 2000);


int nextRandom1(bool shift)
{
	static int last = 10;
	if (shift) {
		last = d1(gen1);
	}
	return last;
}

int nextRandom2(bool shift)
{
	static int last = 10;
	if (shift) {
		last = d2(gen2);
	}
	return last;
}


void TestTCPTunnel::test()
{
	using namespace nd;
	using namespace nd::net;


	nd::net::init();

	auto f1 = []
	{
		CreateSocketInfo info;
		info.async = true;
		info.port = 50000;
		Socket s;
		createSocket(s, info);

		Message m;
		m.address = Address("127.0.0.1", 50001);
		m.buffer.reserve(3000);

		TCPTunnel t(s, m.address, 1, 3000);

		while (true)
		{
			fillBuffer(m.buffer,0, nextRandom1(false));
			if (t.write(m))
				nextRandom1(true);

			fillBuffer(m.buffer, 0, nextRandom1(false));
			if (t.write(m))
				nextRandom1(true);

			// limit output buffer size to force smaller segments
			Message mo;
			mo.buffer = Buffer();
			mo.buffer.reserve(TCPTunnel::MAX_UDP_PACKET_LENGTH);
			t.flush(mo);

			while (receive(s, m) == NetResponseFlags_Success)
			{
				t.receiveTunnelUDP(m);
			}
			using namespace std::chrono_literals;
			//std::this_thread::sleep_for(10000ms);
		}
	};
	auto f2 = []
	{
		CreateSocketInfo info;
		info.async = true;
		info.port = 50001;
		Socket s;
		createSocket(s, info);

		Message m;
		m.address = Address("127.0.0.1", 50000);
		m.buffer.reserve(3000);

		TCPTunnel t(s, m.address, 1, 3000);

		while (true)
		{
			if (receive(s, m) == NetResponseFlags_Success)
			{
				//simulate very heavy packet loss
				if (std::rand()%10)
					t.receiveTunnelUDP(m);

				//simulate packet duplication
				if(std::rand()%30==0)
					t.receiveTunnelUDP(m);
			}
			while (t.read(m))
			{
				bool isSame = nextRandom2(false) == m.buffer.size();
				nextRandom2(true);
				if (!isSame) {
					std::cout << "shit, it does not work\n";
					exit(1);
				}
				std::cout << "Gut received: " << m.buffer.size() << std::endl;
			}
			t.flush(m);
		}
	};

	std::thread t1(f1);
	std::thread t2(f2);

	t1.join();
	t2.join();
}
