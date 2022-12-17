package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

public class ErrorProtocol extends ProtocolHeader{
    public String player, reason;

    public ErrorProtocol(){action= Prot.Error;}


    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(player);
        b.put(reason);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        player = b.getString();
        reason = b.getString();
    }
}
