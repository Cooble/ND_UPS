package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

/**
 * Response from server after BlockModifyEvent was sent, if blockEvent was success or failure
 */
public class BlockAck extends ProtocolHeader{

    public int event_id;
    public boolean gut;

    public BlockAck(){action = Prot.BlockAck;}
    @Override
    public void serialize(NetWriter w) {
        super.serialize(w);
        w.put(event_id);
        w.put(gut);

    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        event_id = b.getInt();
        gut = b.getBool();
    }
}
