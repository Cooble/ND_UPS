package cz.cooble.ndc.net;

public enum Prot {
    // Errors
    ERR_InvalidREQ,

    // establishing connection between client and server
    // using lobby as mediator
    InvitationREQ,
    // (player)
    Invitation,
    // (player, invitation_id)
    InvitationACK,
    // (player, invitation_id)
    SessionCreated,
    //(player, session_id)

    // Normal Ping
    Ping,
    Pong,

    // Cluster Ping
    ClusterPing,
    ClusterPong,

    //World
    ChunkREQ,
    // session, c_x, c_y, [pieces]
    ChunkACK,
    // session, c_x,c_y, piece
    Command,
    // disconnect
    Quit,

    BlockModify,
    BlockAck,
    PlayersMoved,
    PlayerMoves,

    PlayerState,
    Error;
}


