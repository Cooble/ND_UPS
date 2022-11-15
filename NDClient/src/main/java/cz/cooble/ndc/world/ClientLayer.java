package cz.cooble.ndc.world;

import cz.cooble.ndc.net.*;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.input.Event;
import cz.cooble.ndc.input.KeyPressEvent;

import java.net.InetSocketAddress;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;


public class ClientLayer extends Layer {


    private final InetSocketAddress lobbyAddress = new InetSocketAddress("127.0.0.1", 1234);
    private Socket socket;

    @Override
    public void onAttach() {
        System.out.println("client layer started");
        socket = new Socket(0);
        sendInvitationReq();
    }

    private void sendInvitationReq() {
        var header = new EstablishConnectionHeader();
        header.action = Prot.InvitationREQ;
        header.player_name = "karel";

        Message m = new Message(1024);
        m.address = lobbyAddress;
        header.serialize(m.buffer);
        socket.send(m);
    }

    @Override
    public void onUpdate() {
        Message m = new Message(2048);
        while (socket.receive(m) == NetResponseFlags.Success) {
            System.out.println("Incoming datagram from " + m.address);
            var a = new EstablishConnectionHeader();
            a.deserialize(m.buffer);
            if (a.action == Prot.Invitation) {
                a.action = Prot.InvitationACK;
                m.buffer.clear();
                m.address = Net.Address.build(a.server_name);
                a.server_name = "";

                a.serialize(m.buffer);
                socket.send(m);
            } else if (a.action == Prot.SessionCreated) {
                System.out.println("Session id successflylyy obtained: " + a.session_id);
            }
        }
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
}
