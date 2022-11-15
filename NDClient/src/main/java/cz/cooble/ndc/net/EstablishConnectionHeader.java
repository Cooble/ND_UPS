package cz.cooble.ndc.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class EstablishConnectionHeader extends ProtocolHeader {

    public static final int STRING_LENGTH = 32;

    public int session_id;
    public String player_name = "";
    public String server_name = "";

    public void serialize(ByteBuffer b) {
        try {
            var o = new ByteArrayOutputStream(4 + 4 + STRING_LENGTH * 2);
            var out = new DataOutputStream(o);
            out.writeInt(action.ordinal());
            out.writeInt(session_id);
            out.writeBytes(player_name);
            for (int i = 0; i < STRING_LENGTH - player_name.length(); i++)
                out.writeByte(0);

            out.writeBytes(server_name);
            for (int i = 0; i < STRING_LENGTH - server_name.length(); i++)
                out.writeByte(0);

            out.close();
            b.put(o.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deserialize(ByteBuffer b) {
        action = Prot.values()[b.getInt()];
        session_id = b.getInt();
        byte[] mess = new byte[STRING_LENGTH + 3];
        mess[STRING_LENGTH + 2] = 'a';
        b.get(mess, 0, STRING_LENGTH);
        player_name = new String(mess).split("\0")[0];
        b.get(mess, 0, STRING_LENGTH);
        server_name = new String(mess).split("\0")[0];
    }
}
