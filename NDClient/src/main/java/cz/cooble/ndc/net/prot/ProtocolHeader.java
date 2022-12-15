package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.NetSerializable;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ProtocolHeader implements NetSerializable {
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
