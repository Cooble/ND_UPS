package cz.cooble.ndc.net;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Net {


    public static ByteBuffer byteBufferAllocate(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    }

    public static class Address {
        public static InetSocketAddress build(String s) {
            String[] split = s.split(":");
           return new InetSocketAddress(split[0], Integer.parseInt(split[1]));
        }
    }


    private static final int PORT = 1234;
    private static final int maxwait = 2000;

    static InetSocketAddress lobbyAddress;


    public static void onReceive(Socket s, Message m) {
        System.out.println("Icoming from " + m.address);
        var a = new EstablishConnectionHeader();
        a.deserialize(m.buffer);
        if (a.action == Prot.Invitation) {
            a.action = Prot.InvitationACK;
            m.buffer.clear();
            m.address = Address.build(a.server_name);
            a.server_name = "";

            a.serialize(m.buffer);
            s.send(m);

        } else if (a.action == Prot.SessionCreated) {
            System.out.println("Session id successflylyy obtained: " + a.session_id);
        }
    }
}
