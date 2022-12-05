package cz.cooble.ndc.net;

import cz.cooble.ndc.test.NetWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ProtocolHeader implements NetSerializable{
    public static final String BANG = "[_ND_]";
    public Prot action;

    @Override
    public void serialize(NetWriter b) {
        b.put(BANG);
        b.put(action.name());
        b.put(action.ordinal());
    }

    @Override
    public void deserialize(NetReader b) {
        String bang = b.getString();
        if(!BANG.equals(bang))
            throw new RuntimeException("invalid protocol header without bang");
        b.getString();//ignore protocol name
        action = Prot.values()[b.getInt()];
    }
}
