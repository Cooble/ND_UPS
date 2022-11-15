package cz.cooble.ndc.net;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ProtocolHeader {
    public Prot action;

    public void serialize(ByteBuffer b) {
        try {
            var o = new ByteArrayOutputStream(4);
            var out = new DataOutputStream(o);
            out.writeInt(action.ordinal());
            out.close();
            b.put(o.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
