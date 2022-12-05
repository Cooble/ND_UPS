package cz.cooble.ndc.world;

import cz.cooble.ndc.core.Utils;
import cz.cooble.ndc.net.*;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.KeyPressEvent;
import cz.cooble.ndc.test.NetWriter;

import java.net.InetSocketAddress;
import java.util.*;

import static cz.cooble.ndc.core.Utils.half_int;
import static cz.cooble.ndc.net.ChunkProtocol.CHUNK_PIECE_COUNT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;


public class ClientLayer extends Layer {


    private final InetSocketAddress lobbyAddress = new InetSocketAddress("127.0.0.1", 1234);
    private Socket socket;
    private int session_id;
    private boolean isSessionCreated;
    private InetSocketAddress serverAddress;


    private final Set<Integer> pendingChunkIds = new HashSet<>();
    Integer currentPendingChunkID = -1;
    private Chunk chunkBuffer;

    public static final String TM_INVITATION_ACK = "TIMEOUT_INVITATION_ACK";
    public static final String TM_CHUNK_REQ = "TIMEOUT_CHUNK";
    public static final String TM_INV_REQ = "TM_INV_REQ";

    private final Timeout t = new Timeout();

    private final Set<Integer> pendingPieces = new HashSet<>();

    private String playerName;


    @Override
    public void onAttach() {
        System.out.println("Client layer started");
        socket = new Socket(0);

        t.register(TM_INVITATION_ACK, 2000);
        t.register(TM_CHUNK_REQ, 3000);
        t.register(TM_INV_REQ, 2000);

    }

    public void openSession(String player) {
        playerName = player;
        sendInvitationReq();
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
        a.session_id = session_id;
        a.serialize(new NetWriter(m.buffer));

        m.address = serverAddress;
        socket.send(m);

        t.start(TM_INVITATION_ACK);
    }

    private void sendBlockModifies(Message m) {
        System.out.println("Sending Player Update");


        var a = new PlayerProtocol();
        a.action = Prot.PlayerUpdate;
        a.session_id = session_id;
        a.blockModifyEvents = blockModifyEvents;
        a.serialize(new NetWriter(m.buffer));

        m.address = serverAddress;
        socket.send(m);
        blockModifyEvents.clear();
    }

    private void sendMessages(Message m) {

        while (!outCommingCommands.isEmpty()) {
            var a = new CommandProtocol();
            a.action = Prot.Command;
            a.session_id = session_id;
            a.message = outCommingCommands.get(0);
            outCommingCommands.remove(0);
            a.serialize(new NetWriter(m.buffer));
            m.address = serverAddress;
            socket.send(m);
        }
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
            System.out.println("Piece " + a.piece + " deserialized");
            pendingPieces.remove(a.piece);

            //reset timeout for asking explicitly for another piece
            t.start(TM_CHUNK_REQ);
        }
        // datagram of foreign chunk
        else {
            //discard
            System.out.println("Received foreign chunk piece, discarding");
        }

    }

    private void onInvitation(Message m) {
        var a = new EstablishConnectionHeader();
        a.deserialize(new NetReader(m.buffer));
        session_id = a.session_id;
        serverAddress = Net.Address.build(a.server_name);
        sendInvitationACK(m);
        t.stop(TM_INV_REQ);
    }

    private void onSessionCreated(Message m) {
        var a = new EstablishConnectionHeader();
        a.deserialize(new NetReader(m.buffer));
        isSessionCreated = true;
        System.out.println("Session id successfully obtained: " + a.session_id);
        t.stop(TM_INVITATION_ACK);
    }

    private void onCommandReceived(Message m){
        var a = new CommandProtocol();
        a.deserialize(new NetReader(m.buffer));
        incommingCommands.add(a.message);
        System.out.println("Received Message from server: "+a.message);
    }

    private void onPacketReceived(Message m) {
        System.out.println("Incoming datagram from " + m.address);
        var a = new ProtocolHeader();
        a.deserialize(new NetReader(m.buffer));

        if (a.action == Prot.Invitation) {
            onInvitation(m);
        } else if (a.action == Prot.SessionCreated) {
            onSessionCreated(m);
        } else if (a.action == Prot.ChunkACK) {
            onChunkACK(m);
        }
        else if (a.action == Prot.Command) {
            onCommandReceived(m);
        }
    }

    //===============================================

    @Override
    public void onUpdate() {
        Message m = new Message(2048);
        while (socket.receive(m) == NetResponseFlags.Success)
            try {
                onPacketReceived(m);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Received invalid datagram, discarding");
            }

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

        if (!blockModifyEvents.isEmpty())
            sendBlockModifies(m);

        if (!outCommingCommands.isEmpty())
            sendMessages(m);
    }


    @Override
    public void onEvent(Event e) {
        if (e instanceof KeyPressEvent) {
            if (((KeyPressEvent) e).isRelease())
                return;

            if (((KeyPressEvent) e).getFreshKeycode() == GLFW_KEY_S) {
                sendInvitationReq();
            }
        }
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

    public void addPendingChunk(int chunkid) {
        pendingChunkIds.add(chunkid);
    }

    public boolean isSessionCreated() {
        return isSessionCreated;
    }

    public boolean hasPendingChunks() {
        return !pendingChunkIds.isEmpty();
    }

    List<BlockModifyEvent> blockModifyEvents = new ArrayList<>();
    public void addPendingBlockModify(BlockModifyEvent e) {
        blockModifyEvents.add(e);
    }

    List<String> outCommingCommands = new ArrayList<>();
    public void addPendingCommand(String s) {
        outCommingCommands.add(s);
    }

    List<String> incommingCommands = new ArrayList<>();

    public String getCommand(){
        if(incommingCommands.isEmpty())
            return null;

        var out = incommingCommands.get(0);
        incommingCommands.remove(0);
        return out;
    }
}
