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

    // Migrating server
    MigrateREQ,
    // (player_data, target_server)
    MigrateWait,
    // (player)
    MigratedACK,
    // (player, server)

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
    WorldUpdate,
    //session, entity_updates[entity_id, type, physics_data] + key_press_history, block updates[x,y,block_data, time?,who]
    PlayerUpdate,
    //session, key_press_history
    Command,
}


