package cz.cooble.ndc.world;

import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.SessionProtocol;
import cz.cooble.ndc.test.NetWriter;

import java.util.ArrayList;
import java.util.List;

public class PlayerProtocol extends SessionProtocol {

    public List<BlockModifyEvent> blockModifyEvents = new ArrayList<>();

    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.putArray(blockModifyEvents);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        blockModifyEvents = b.getArray(BlockModifyEvent::new);
    }
}
