package cz.cooble.ndc.world;

import cz.cooble.ndc.Globals;
import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Utils;
import cz.cooble.ndc.net.*;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.KeyPressEvent;
import cz.cooble.ndc.net.prot.*;
import cz.cooble.ndc.test.NetWriter;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;

import static cz.cooble.ndc.core.Utils.half_int;
import static cz.cooble.ndc.net.prot.ChunkProtocol.CHUNK_PIECE_COUNT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;


public class ClientLayer extends Layer {

    private InetSocketAddress lobbyAddress;
    private Socket socket;
    private TCPTunnel tunnel;
    private int session_id;
    private int invitation_id;
    private boolean isSessionCreated;
    private InetSocketAddress serverAddress;
    private InetSocketAddress invitationServerAddress;


    private Set<Integer> pendingChunkIds;
    Integer currentPendingChunkID = -1;
    private Chunk chunkBuffer;

    public static final String TM_INVITATION_ACK = "TIMEOUT_INVITATION_ACK";
    public static final String TM_CHUNK_REQ = "TIMEOUT_CHUNK";
    public static final String TM_INV_REQ = "TM_INV_REQ";

    private Timeout t = new Timeout();

    //private Timeout.Pocket serverTimeout = new Timeout.Pocket(2000);
    private Timeout.Pocket serverTimeout = new Timeout.Pocket(Globals.SERVER_TIMEOUT);

    private Set<Integer> pendingPieces;

    private String playerName;


    private void initTimeouts() {
        t = new Timeout();
        t.register(TM_INVITATION_ACK, 250);
        t.register(TM_CHUNK_REQ, 250);
        t.register(TM_INV_REQ, 250);
        serverTimeout.stop();
    }

    @Override
    public void onAttach() {
        System.out.println("Client layer started");
        socket = new Socket(0);
        System.out.println("Socket opened on port " + socket.port());

        lobbyAddress = new InetSocketAddress("127.0.0.1", 1234);
        pendingChunkIds = new HashSet<>();
        pendingPieces = new HashSet<>();
        playerName = null;
        chunkBuffer = null;
        currentPendingChunkID = -1;
        isSessionCreated = false;
        session_id = -1;
        tunnel = null;
        serverAddress = null;

        initTimeouts();
    }

    @Override
    public void onDetach() {
        if (isSessionCreated) {
            System.out.println("Disconnecting from server");
            closeSession();
        }
    }

    public void openSession(String player, InetSocketAddress address) {
        if (isSessionCreated)
            return;

        playerName = player;
        lobbyAddress = address;
        sendInvitationReq();
    }

    public String getPlayerName() {
        return playerName;
    }


    public void closeSession() {
        initTimeouts();
        if (!isSessionCreated)
            return;
        isSessionCreated = false;
        sendClose(new Message(500));
        pendingChunkIds.clear();
        pendingPieces.clear();
        playerMoveEvents.clear();
        playersMoved.clear();
    }

    //=======EVENT=REQUESTS=========================
    private void sendInvitationReq() {
        System.out.println("Sending invitation request");

        var a = new EstablishConnectionHeader();
        a.action = Prot.InvitationREQ;
        a.player_name = playerName;

        Message m = new Message(1024);
        m.address = lobbyAddress;
        a.serialize(new NetWriter(m.buffer));
        socket.send(m);

        t.start(TM_INV_REQ);
    }

    private void sendChunkReq(Message m, int cid, int piece) {
        System.out.println("Sending chunk request for " + Utils.getX(cid) + ", " + Utils.getY(cid) + " piece=" + piece);


        var a = new ChunkProtocol();
        a.action = Prot.ChunkREQ;
        a.session_id = session_id;
        a.c_x = Utils.getX(cid);
        a.c_y = Utils.getY(cid);
        a.piece = piece;
        a.serialize(new NetWriter(m.buffer));

        m.address = serverAddress;
        socket.send(m);
        t.start(TM_CHUNK_REQ);
    }

    private void sendInvitationACK(Message m) {
        System.out.println("Sending invitation ACK");

        var a = new EstablishConnectionHeader();
        a.action = Prot.InvitationACK;
        a.player_name = playerName;
        a.session_id = invitation_id;
        a.serialize(new NetWriter(m.buffer));

        m.address = invitationServerAddress;
        socket.send(m);

        t.start(TM_INVITATION_ACK);
    }

    private void sendMove(Message m) {
        m.address = serverAddress;
        for (var e : playerMoveEvents) {
            e.serialize(new NetWriter(m.buffer));
            //System.out.println("[Sending_Move] eventId:" + e.event_id + " pos:" + e.pos.toString() + " input:" + e.inputs.toString());
            if (!Globals.DISABLE_MOVE)
                socket.send(m);
        }
        playerMoveEvents.clear();
    }

    private void sendCommands(Message m) {
        while (!outcomingCommands.isEmpty()) {
            var a = new CommandProtocol();
            a.action = Prot.Command;
            a.session_id = session_id;
            a.message = outcomingCommands.get(0);
            outcomingCommands.remove(0);
            a.serialize(new NetWriter(m.buffer));
            m.address = serverAddress;
            tunnel.write(m);
        }
    }

    private void sendClose(Message m) {
        System.out.println("Sending closing");

        var a = new ControlProtocol();
        a.action = Prot.Quit;
        a.session_id = session_id;
        a.serialize(new NetWriter(m.buffer));

        m.address = serverAddress;
        socket.send(m);
    }


    //=======EVENT=RESPONSE=========================
    private void onChunkACK(Message m) {
        var a = new ChunkProtocol();
        a.deserialize(new NetReader(m.buffer));

        // new chunk
        if (chunkBuffer == null) {
            pendingPieces.clear();
            for (int i = 0; i < CHUNK_PIECE_COUNT; i++)
                pendingPieces.add(i);

            chunkBuffer = new Chunk(a.c_x, a.c_y);
            chunkBuffer.deserialize(m.buffer.getInner(), a.piece);
            pendingPieces.remove(a.piece);

            //reset timeout for asking explicitly for another piece
            t.start(TM_CHUNK_REQ);
        }
        // chunk already exists, new piece
        else if (chunkBuffer.chunkID() == half_int(a.c_x, a.c_y)) {
            chunkBuffer.deserialize(m.buffer.getInner(), a.piece);
            //System.out.println("Piece " + a.piece + " deserialized");
            pendingPieces.remove(a.piece);

            //reset timeout for asking explicitly for another piece
            t.start(TM_CHUNK_REQ);
        }
        // datagram of foreign chunk
        else {
            //discard
            System.out.println("Received foreign chunk piece, discarding");
        }
        serverTimeout.start();
    }

    private void onInvitation(Message m) {
        var a = new EstablishConnectionHeader();
        a.deserialize(new NetReader(m.buffer));
        invitation_id = a.session_id;
        invitationServerAddress = Net.Address.build(a.server_name);
        sendInvitationACK(m);
        t.stop(TM_INV_REQ);
        System.out.println("Invitation from lobby received");
    }

    private void onPlayerState(Message m) {
        var a = new PlayerState();
        a.deserialize(new NetReader(m.buffer));
        onPlayerState.accept(a);
    }

    Consumer<EstablishConnectionHeader> onSessionCreatedCallback;

    public void setOnSessionCreatedCallback(Consumer<EstablishConnectionHeader> onSessionCreatedCallback) {
        this.onSessionCreatedCallback = onSessionCreatedCallback;
    }

    private void onSessionCreated(Message m) {
        if (invitationServerAddress == null || !invitationServerAddress.equals(m.address))
            return;

        var a = new EstablishConnectionHeader();
        a.deserialize(new NetReader(m.buffer));
        if (isSessionCreated) {
            System.out.println("Closing old session " + serverAddress.toString()
                    + " and preparing for new " + invitationServerAddress.toString());
            closeSession();
        }

        // set new address and session
        serverAddress = invitationServerAddress;
        invitationServerAddress = null;
        session_id = a.session_id;
        invitation_id = -1;
        isSessionCreated = true;
        System.out.println("Session id successfully obtained: " + a.session_id);
        t.stop(TM_INVITATION_ACK);
        tunnel = new TCPTunnel(socket, serverAddress, session_id, 3000);

        onSessionCreatedCallback.accept(a);
    }

    private void onCommandReceived(Message m) {
        var a = new CommandProtocol();
        a.deserialize(new NetReader(m.buffer));
        incomingCommands.add(a.message);
        System.out.println("Received Message from server: " + a.message);
    }

    private Consumer<String> onDisconnect;

    public void setOnDisconnect(Consumer<String> onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    private Consumer<PlayerState> onPlayerState;

    public void setOnPlayerState(Consumer<PlayerState> onPlayerState) {
        this.onPlayerState = onPlayerState;
    }

    private void onQuitReceived(Message m) {
        var a = new ControlProtocol();
        a.deserialize(new NetReader(m.buffer));

        if (onDisconnect != null)
            onDisconnect.accept(a.message);
    }

    private void onPacketReceived(Message m) {
        //System.out.println("Incoming datagram from " + m.address);
        if (TCPTunnel.getClientID(m) != -1) {
            if (tunnel != null) {
                tunnel.receiveTunnelUDP(m);
            }
            return;
        }
        var a = new ProtocolHeader();
        a.deserialize(new NetReader(m.buffer));

        if (a.action == Prot.Invitation) {
            onInvitation(m);
        } else if (a.action == Prot.SessionCreated) {
            onSessionCreated(m);
        } else if (a.action == Prot.ChunkACK) {
            onChunkACK(m);
        } else if (a.action == Prot.Command) {
            onCommandReceived(m);
        } else if (a.action == Prot.Quit) {
            onQuitReceived(m);
        } else if (a.action == Prot.BlockModify || a.action == Prot.BlockAck) {
            onBlockPacket(m);
        } else if (a.action == Prot.PlayersMoved) {
            onMoved(m);
        } else if (a.action == Prot.PlayerState) {
            onPlayerState(m);
        } else if (a.action == Prot.Error) {
            onError(m);
        }

    }

    public void setOnError(Consumer<ErrorProtocol> onError) {
        this.onErrorCallback = onError;
    }

    Consumer<ErrorProtocol> onErrorCallback;
    private void onError(Message m){
        ErrorProtocol e = new ErrorProtocol();
        e.deserialize(new NetReader(m.buffer));
        onErrorCallback.accept(e);
    }

    private void onMoved(Message m) {
        serverTimeout.start();

        PlayersMoved moved = new PlayersMoved();
        moved.deserialize(new NetReader(m.buffer));
        playersMoved.add(moved);
       /* System.out.println("[Receiving_Moves]:");
        for (var e : moved.moves) {
            System.out.println("\t-> eventid:" + e.event_id + " pos:" + e.targetPos);
            for (var o : e.inputs)
                System.out.println("\t\t* " + o.toString());
        }*/
    }

    Consumer<ProtocolHeader> onBlockEvents;

    public void setOnBlockEventsCallback(Consumer<ProtocolHeader> onBlockEvents) {
        this.onBlockEvents = onBlockEvents;
    }

    private void onBlockPacket(Message m) {
        var a = new ProtocolHeader();
        a.deserialize(new NetReader(m.buffer));

        if (a.action == Prot.BlockModify) {
            var e = new BlockModify();
            e.deserialize(new NetReader(m.buffer));
            onBlockEvents.accept(e);
        } else if (a.action == Prot.BlockAck) {
            var e = new BlockAck();
            e.deserialize(new NetReader(m.buffer));
            onBlockEvents.accept(e);
        }
        serverTimeout.start();
    }

    public void sendBlockUpdate(Message m) {
        for (var e : blockModifies) {
            e.serialize(new NetWriter(m.buffer));
            tunnel.write(m);
        }
        blockModifies.clear();
    }

    //===============================================

    String errorMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void onUpdate() {
        Message m = new Message(1500);

        // POLL
        while (socket.receive(m) == NetResponseFlags.Success)
            try {
                onPacketReceived(m);
            } catch (Exception e) {
                e.printStackTrace();
               // errorMessage = "Received invalid UDP, discarding:\n" + new String(m.buffer.getInner().array());
                System.out.println("Received invalid UDP, discarding:\n" + new String(m.buffer.getInner().array()));
              //  errorMessage = e.toString();
            }
        while (tunnel != null && tunnel.read(m)) {
            try {
                onPacketReceived(m);
            } catch (Exception e) {
                e.printStackTrace();
                // errorMessage = "Received invalid UDP, discarding:\n" + new String(m.buffer.getInner().array());
                System.out.println("Received invalid TCP, discarding:\n" + new String(m.buffer.getInner().array()));
                //  errorMessage = e.toString();
            }
        }

        // SEND
        updateBeforeSession(m);
        if (!isSessionCreated)
            return;
        updateAfterSession(m);

    }

    private void updateBeforeSession(Message m) {
        // check if time to resend invitation request
        if (t.isTimeout(TM_INV_REQ)) {
            System.out.println(TM_INV_REQ + " occurred, resending InvitationReq");
            sendInvitationReq();
        }

        // check if time to resend invitation ack
        if (t.isTimeout(TM_INVITATION_ACK)) {
            System.out.println(TM_INVITATION_ACK + " occurred, resending InvitationReq");
            sendInvitationACK(m);
        }
    }

    private void updateAfterSession(Message m) {
        if (isServerTimeout())
            if (onDisconnect != null)
                onDisconnect.accept("Server stopped responding");

        // pick new chunk to ask for
        if (currentPendingChunkID == -1 && !pendingChunkIds.isEmpty()) {
            currentPendingChunkID = pendingChunkIds.stream().findFirst().get();
            t.startWithTimeout(TM_CHUNK_REQ);
        }

        // check if it is time to ask for chunk
        if (t.isTimeout(TM_CHUNK_REQ) && currentPendingChunkID != -1) {
            if (chunkBuffer == null) {// not a single piece arrived yet
                // ask for every piece
                sendChunkReq(m, currentPendingChunkID, -1);
            } else {
                // send requests for missing pieces
                for (var c : pendingPieces)
                    sendChunkReq(m, currentPendingChunkID, c);
            }
        }

        sendMove(m);
        sendBlockUpdate(m);
        sendCommands(m);

        // flush tcp content
        tunnel.flush(m);

    }

    @Override
    public void onEvent(Event e) {
        if (e instanceof KeyPressEvent) {
            if (((KeyPressEvent) e).isRelease())
                return;
            if (((KeyPressEvent) e).getFreshKeycode() == GLFW_KEY_S) {

            }
        }
    }

    public boolean isSessionCreated() {
        return isSessionCreated;
    }

    public boolean hasPendingChunks() {
        return !pendingChunkIds.isEmpty();
    }

    List<BlockModify> blockModifies = new ArrayList<>();
    List<PlayersMoved> playersMoved = new ArrayList<>();
    List<PlayerMoves> playerMoveEvents = new ArrayList<>();
    List<String> outcomingCommands = new ArrayList<>();
    List<String> incomingCommands = new ArrayList<>();


    // ======= ADD PENDING =====================
    public void addPendingBlockModify(BlockModify e) {
        blockModifies.add(e);
    }

    public void addPendingPlayerMove(PlayerMoves playerMoveEvent) {
        playerMoveEvent.session_id = session_id;
        playerMoveEvents.add(playerMoveEvent);
    }

    public void addPendingCommand(String s) {
        outcomingCommands.add(s);
    }

    public void addPendingChunk(int chunkid) {
        pendingChunkIds.add(chunkid);
    }

    // ======= GET ==============================
    public String getNewCommand() {
        if (incomingCommands.isEmpty())
            return null;

        var out = incomingCommands.get(0);
        incomingCommands.remove(0);
        return out;
    }

    public Chunk getNewChunk() {
        if (currentPendingChunkID != -1 && pendingPieces.isEmpty() && chunkBuffer != null) {
            pendingChunkIds.remove(currentPendingChunkID);
            currentPendingChunkID = -1;

            var out = chunkBuffer;
            chunkBuffer = null;
            return out;
        }
        return null;
    }

    public PlayersMoved getPlayersMoved() {
        if (playersMoved.isEmpty())
            return null;

        var out = playersMoved.get(0);
        playersMoved.remove(0);
        return out;
    }

    public boolean isServerTimeout() {
        return serverTimeout.isTimeout();
    }

}
