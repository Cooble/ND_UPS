package cz.cooble.ndc.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public enum Prot {
    // Errors
    ERR_InvalidREQ,

    // establishing connection between client and server
    // using lobby as mediator
    InvitationREQ, // (player)
    Invitation, // (player, invitation_id)
    InvitationACK, // (player, invitation_id)
    SessionCreated, //(player, session_id)

    // Migrating server
    MigrateREQ, // (player_data, target_server)
    MigrateWait, // (player)
    MigratedACK, // (player, server)

    // Normal Ping
    Ping,
    Pong,

    // Cluster Ping
    ClusterPing,
    ClusterPong
}


