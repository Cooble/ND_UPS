package cz.cooble.ndc.net;

public enum Prot {
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
}


