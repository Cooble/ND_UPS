#pragma once
#include "net/net_iterator.h"
#include "world/block/Block.h"
#include "server_constants.h"

enum class Prot :int
{
	// Errors, not used
	ERR_InvalidREQ,

	//	InvitationREQ
	//	(EstablishConnectionHeader) UDP
	//	[Client/Server ->	Lobby]
	//	[Lobby         ->  Server]
	//
	//	Request sent to lobby to request invitation id, from client or other server
	//
	//	DATA
	//		player = name of the player
	//		<session_id> = id that is forbidden to use, valid only for inter server communication
	//		<server> = name of server to connect to, valid only for inter server communication
	InvitationREQ,

	//	Invitation
	//	(EstablishConnectionHeader) UDP/TCP
	//	[Server		->      Lobby]
	//	[Lobby  ->  Client/Server]
	//
	//	Server sender invitation in response to InvitationREQ
	//
	//	DATA
	//		player = name of the player
	//		session_id = invitation id
	//		server = name of server in case (server to lobby) or address of the server (lobby to client/server)
	Invitation,

	//	InvitationACK
	//	(EstablishConnectionHeader) UDP
	//	[Client		->     Server]
	//
	//	Clients accepts invitation and sends ACK directly to server (not lobby)
	//
	//	DATA
	//		player = name of the player
	//		session_id = invitation id
	InvitationACK,

	//	SessionCreated
	//	(EstablishConnectionHeader) UDP
	//	[Server		->     Client]
	//
	//	Server receives invitation acknowledgement from client,
	//	for server this session is created. (sends SessionCreated to client)
	//
	//	DATA
	//		player = name of the player
	//		session_id = invitation id
	SessionCreated,



	// Not used
	Ping,
	Pong,

	//	ClusterPing
	//	(ProtocolHeader) UDP
	//	[Lobby   ->   Server]
	//
	//	Are ya alive mate?
	ClusterPing,

	//	ClusterPong
	//	(ProtocolHeader) UDP
	//	[Server  ->    Lobby]
	//
	//	Imma alive!
	//
	//	ADDITIONAL DATA
	//		server name = name of the server
	ClusterPong,

	//	ChunkREQ
	//	(ChunkProtocol: SessionProtocol) UDP
	//	[Client	->  Server]
	//
	//	I want chunk!
	//
	//	DATA
	//		c_x,c_y = coordinates of chunk
	//		piece = piece number to send, -1 means send all, interval=(0-19),
	//		session_id
	ChunkREQ,

	//	ChunkACK
	//	(ChunkProtocol: SessionProtocol) UDP
	//	[Server	->  Client]
	//
	//	Here ya go! Have a piece of chunk
	//
	//	DATA
	//		c_x,c_y = coordinates of chunk
	//		piece = piece which was sent (size of piece is 1040B, last one is smaller)
	//		session_id
	//	ADDITIONAL DATA
	//		binary data (Little endian) of the piece (52 blocks):
	//			block_metadata = d.getInt();
	//			wall_id[0] = d.getShort();
	//			wall_id[1] = d.getShort();
	//			wall_id[2] = d.getShort();
	//			wall_id[3] = d.getShort()
	//			wall_corner  = get4bytes();
	//			block_id = d.getShort();
	//			block_corner = d.getByte();
	//			getByte() // last padding byte 
	ChunkACK,

	//	Command
	//	(CommandProtocol: SessionProtocol) TCP
	//	[Client	<->  Server]
	//
	//	Chat messages. Usually sent via TCP
	//
	//	DATA
	//		message = string message, max length 300, if greater, will be silently discarded
	//		session_id (not necessary in tcp)
	Command,

	//	Quit
	//	(ControlProtocol : CommandProtocol) UDP
	//	[Client	<->  Server]
	//
	//	Server or client wants to disconnect.
	//
	//	DATA
	//		message = reason why quit
	//		session_id
	Quit,

	//	BlockModify
	//	(BlockModify) TCP
	//	[Client	->  Server]
	//
	//	Client asks server if it is OK to place a block. 
	//
	//	DATA
	//		block_id = id of block or wall
	//		blockOrWall = true if block, false if wall
	//		x, y = block world position
	//		event_id = client event id (can be anything unique for client, usually increments by one each BlockModify event)
	BlockModify,

	//	BlockAck
	//	(BlockAck) TCP
	//	[Server	->  Client]
	//
	//	Server validates block modification. Is sent after every BlockModify. 
	//
	//	DATA
	//		event_id = id from BlockModify sent by client
	//		gut = true on valid placement
	BlockAck,

	//	PlayersMoved
	//	(PlayersMoved) UDP
	//	[Server	->  Client]
	//
	//	All movements of players in one server update (usually more than 1 tick).
	//
	//	DATA
	//		moved = list of PlayerMoved
	//			PlayerMoved:
	//				name = player
	//				inputs = list of key inputs (4 chars, '0' or '1', up down left right)
	//				targetPos = final pos of player after inputs
	//				targetVelocity = final velocity of player after inputs
	//				int event_id = latest event move id received from client
	PlayersMoved,

	//	PlayerMoves
	//	(PlayerMoves) UDP
	//	[Client	->  Server]
	//
	//	Periodically (every tick) client sends move input information.
	//	tl;dr which arrow keys were pressed. 
	//
	//	DATA
	//		inputs = which arrow key was pressed: 4 chars, '0' or '1', up down left right
	//		<pos> = position of player before input applied (not needed since server is authoritative, good for debug)
	//		event_id = incremental id of move event, unique for player
	PlayerMoves,

	//	PlayerState
	//	(PlayerState) TCP
	//	[Server	->  Client]
	//
	//	Player changes state (connect, disconnect, fly mode on, fly mode off). 
	//	It can happen that server sends Fly without sending Connect first.
	//	In this case client should act as if Connect was sent before anyway.
	// 
	//
	//	DATA
	//		name = player
	//		type = Connect=0 Fly=1
	//		value = bool (Connect/Disconnect, FlyOn,FlyOff)
	PlayerState,

	//	Error
	//	(ErrorProtocol) UDP
	//	[Lobby/Server	->  Client/Server]
	//
	//	Connect error encountered. Such as Cannot connect to server. Server is full. Server does not exist...
	//	Lobby redirects error from server to client.
	//	Client ignores it if session was already created!
	//
	//	DATA
	//		player = name of a receiving client 
	//		reason = What went wrong
	Error,
};

inline const char* ProtStrings[128] = {

	"ERR_InvalidREQ	",
	"InvitationREQ	",
	"Invitation		",
	"InvitationACK	",
	"SessionCreated	",
	"Ping			",
	"Pong			",
	"ClusterPing	",
	"ClusterPong	",
	"ChunkREQ		",
	"ChunkACK		",
	"Command		",
	"Quit			",
	"BlockModify	",
	"BlockAck		",
	"PlayersMoved	",
	"PlayerMoves	",
	"PlayerState	",
	"Error			",

};


constexpr const char* BANG = "[]";

struct ProtocolHeader : nd::net::Serializable
{
	Prot action;

	bool deserialize(nd::net::NetReader& reader)
	{
		std::string bang;
		reader.get(bang, strlen(BANG));
		if (bang != BANG)
			return false;

		//ignore protocol name
		auto s = reader.readStringUntilNull(30);

		return reader.get(*(int*)&action);
	}

	void serialize(nd::net::NetWriter& reader) const
	{
		reader.put(BANG);
		reader.put(std::string(ProtStrings[(int)action]));
		reader.put((int)action);
	}
};

struct SessionProtocol : ProtocolHeader
{
	int session_id;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(session_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(session_id);
	}
};

constexpr int EstablishConnectionProtocol_STRING_LENGTH = 32;

// if action==InvReq -> value inside session_id is guaranteed not to be returned as invitation id
struct EstablishConnectionProtocol : SessionProtocol
{
	std::string player, server;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		if (!reader.get(player, EstablishConnectionProtocol_STRING_LENGTH))
			return false;
		if (!reader.get(server, EstablishConnectionProtocol_STRING_LENGTH))
			return false;

		// forbid longer names
		if (player.size() > 11 && server.size() > 32)
		{
			return false;
		}
		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(player);
		writer.put(server);
	}
};


constexpr int CHUNK_PIECE_SIZE = 1040; //52 blocks
constexpr int CHUNK_PIECE_COUNT = 20; //52 blocks
constexpr int LAST_CHUNK_PIECE_IDX = CHUNK_PIECE_COUNT - 1; //52 blocks
constexpr int LAST_CHUNK_PIECE_SIZE = 32 * 32 * 20 - (CHUNK_PIECE_COUNT - 1) * CHUNK_PIECE_SIZE;
//total bytes - 19 pieces


struct ChunkProtocol : SessionProtocol
{
	int c_x, c_y;
	int piece = -1;

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;
		reader.get(c_x);
		reader.get(c_y);
		reader.get(piece);

		return true;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(c_x);
		writer.put(c_y);
		writer.put(piece);
	}
};

//todo command protocol is not session protocol

struct CommandProtocol : SessionProtocol
{
	std::string message;
	CommandProtocol() { action = Prot::Command; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;

		return reader.get(message) && message.size() < server_const::MAX_COMMAND_MESSAGE_LENGTH;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(message);
	}
};

struct ControlProtocol : CommandProtocol
{
	ControlProtocol() { action = Prot::Quit; }
};

struct BlockAck : ProtocolHeader
{
	int event_id;
	bool gut;

	BlockAck() { action = Prot::BlockAck; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(event_id) && reader.get(gut);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(event_id);
		writer.put(gut);
	}
};

struct BlockModify : ProtocolHeader
{
	int block_id;
	bool blockOrWall;
	int x, y;
	int event_id;

	BlockModify() { action = Prot::BlockModify; }


	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return
			reader.get(block_id) &&
			reader.get(blockOrWall) &&
			reader.get(x) &&
			reader.get(y) &&
			reader.get(event_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(block_id);
		writer.put(blockOrWall);
		writer.put(x);
		writer.put(y);
		writer.put(event_id);
	}
};

struct Inputs : nd::net::Serializable
{
	bool up, down, left, right;

	bool deserialize(nd::net::NetReader& reader)
	{
		std::string s;
		if (!reader.get(s) || s.size() != 4)
			return false;

		up = s[0] == '1';
		down = s[1] == '1';
		left = s[2] == '1';
		right = s[3] == '1';

		return true;
	}

	std::string toString() const
	{
		std::string s;
		s += (char)('0' + up);
		s += (char)('0' + down);
		s += (char)('0' + left);
		s += (char)('0' + right);
		return s;
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		writer.put(toString());
	}
};

struct PlayerMoves : SessionProtocol
{
	Inputs inputs;
	glm::vec2 pos;
	int event_id;


	PlayerMoves() { action = Prot::PlayerMoves; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!SessionProtocol::deserialize(reader))
			return false;

		return
			reader.get(inputs) &&
			reader.get(pos) &&
			reader.get(event_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		SessionProtocol::serialize(writer);
		writer.put(inputs);
		writer.put(pos);
		writer.put(event_id);
	}
};

struct PlayerMoved : nd::net::Serializable
{
	// player
	std::string name;
	// inputs issued by the player in order to get to targetPos
	std::vector<Inputs> inputs;
	// final position that the player got to
	glm::vec2 targetPos;
	// final velocity that the player got to
	glm::vec2 targetVelocity;
	// latest event move id from client
	int event_id = 0;

	bool deserialize(nd::net::NetReader& reader)
	{
		return
			reader.get(name) &&
			reader.get(inputs) &&
			reader.get(targetPos) &&
			reader.get(targetVelocity) &&
			reader.get(event_id);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		writer.put(name);
		writer.put(inputs);
		writer.put(targetPos);
		writer.put(targetVelocity);
		writer.put(event_id);
	}
};

struct PlayersMoved : ProtocolHeader
{
	std::vector<PlayerMoved> moves;

	PlayersMoved() { action = Prot::PlayersMoved; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return reader.get(moves);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(moves);
	}
};

struct PlayerState : ProtocolHeader
{
	std::string name;

	enum:int
	{
		Fly,
		Connect
	} type;

	bool value;
	PlayerState() { action = Prot::PlayerState; }


	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return
			reader.get(name) &&
			reader.get(*(int*)&type) &&
			reader.get(value);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(name);
		writer.put((int)type);
		writer.put(value);
	}
};

struct ErrorProtocol:ProtocolHeader
{
	std::string player;
	std::string reason;

	ErrorProtocol() { action = Prot::Error; }

	bool deserialize(nd::net::NetReader& reader)
	{
		if (!ProtocolHeader::deserialize(reader))
			return false;

		return
			reader.get(player) &&
			reader.get(reason);
	}

	void serialize(nd::net::NetWriter& writer) const
	{
		ProtocolHeader::serialize(writer);
		writer.put(player);
		writer.put(reason);
	}
};
