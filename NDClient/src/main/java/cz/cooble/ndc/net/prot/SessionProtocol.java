package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.test.NetWriter;

public class SessionProtocol extends ProtocolHeader{

    public int session_id;
    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(session_id);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        session_id = b.getInt();
    }
}
