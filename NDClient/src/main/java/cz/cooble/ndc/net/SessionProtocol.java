package cz.cooble.ndc.net;

public class SessionProtocol extends ProtocolHeader{

    public int session_id;
    @Override
    public void serialize(NetBuffer b) {
        super.serialize(b);
        b.put(session_id);
    }

    @Override
    public void deserialize(NetBuffer b) {
        super.deserialize(b);
        session_id = b.getInt();
    }
}
