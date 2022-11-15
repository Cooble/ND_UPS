#include "Prot.h"

#include <WinSock2.h>

#define INT_TO_HOST(e)\
	*(u_long*)&(e) = ntohl(*(u_long*)&(e))

#define INT_TO_NET(e)\
	*(u_long*)&(e) = htonl(*(u_long*)&(e))



