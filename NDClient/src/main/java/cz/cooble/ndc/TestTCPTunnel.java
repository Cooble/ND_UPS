package cz.cooble.ndc;

import cz.cooble.ndc.net.*;

import java.nio.ByteBuffer;
import java.util.Random;

public class TestTCPTunnel {


    static void fillBuffer(NetBuffer b, char data, int size) {
        for (int i = 0; i < size; ++i) {
            b.data()[i] = (byte) (data + i);
        }
        b.setSize(size);
    }

    public static int hashCode(int seed) {
        seed = seed & 0xffffff;
        String s = seed+"karel"+seed;
        var value = s.getBytes();
        int h = 0;
        for (byte v : value) {
            h = 31 * h + (v & 0xff);
        }
        return h;
    }
    static int last1 = 5;

    static int nextRandom1(boolean shift) {
        if (shift)
        {
            last1 = hashCode(last1);
        }
        return last1&0x5ff;
    }
    static int last2 = 5;
    static int nextRandom2(boolean shift)
    {
        if (shift)
        {
            last2 = hashCode(last2);
        }
        return last2&0x5ff;
    }

    static Random gen = new Random(0);


    public static void main(String[] a) {
        Runnable f1 = () ->
        {
            Socket s = new Socket(50000);
            Message m = new Message(3000);
            m.address = Net.Address.build("127.0.0.1", 50001);

            TCPTunnel t = new TCPTunnel(s, m.address, 1, 3000);

            while (true) {
                fillBuffer(m.buffer, (char) 0, nextRandom1(false));
                if (t.write(m))
                    nextRandom1(true);

                fillBuffer(m.buffer, (char) 0, nextRandom1(false));
                if (t.write(m))
                    nextRandom1(true);

                // limit output buffer size to force smaller segments
                Message mo = new Message(TCPTunnel.MAX_UDP_PACKET_LENGTH);
                t.flush(mo);

                while (s.receive(m) == NetResponseFlags.Success) {
                    t.receiveTunnelUDP(m);
                }
                //std::this_thread::sleep_for(10000ms);
            }
        };
        Runnable f2 = ()->
        {
            Socket s = new Socket(50001);

            Message m = new Message(3000);
            m.address = Net.Address.build("127.0.0.1", 50000);

            TCPTunnel t = new TCPTunnel(s, m.address, 1, 3000);

            while (true) {
                if (s.receive(m) == NetResponseFlags.Success) {
                    //simulate very heavy packet loss
                    if (gen.nextInt(10) != 0)
                        t.receiveTunnelUDP(m);

                    //simulate packet duplication
                    if (gen.nextInt(31) != 0)
                        t.receiveTunnelUDP(m);
                }
                while (t.read(m)) {
                    boolean isSame = nextRandom2(false) == m.buffer.size();
                    var before = nextRandom2(false);
                    nextRandom2(true);
                    if (!isSame) {
                        System.out.println("shit, it does not work "+before+" is not "+m.buffer.size());
                        System.exit(1);
                    }
                    System.out.println("Gut received: " + m.buffer.size());
                }
                t.flush(m);
            }
        }
        ;

       // var t1 = new Thread(f1);
      //  t1.start();

        var t2 = new Thread(f2);
        t2.start();



        try {
         //   t1.join();
           t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
