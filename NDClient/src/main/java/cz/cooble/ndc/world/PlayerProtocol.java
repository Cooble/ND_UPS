package cz.cooble.ndc.world;

import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.SessionProtocol;

import java.util.ArrayList;
import java.util.List;

public class PlayerProtocol extends SessionProtocol {

    public List<BlockModifyEvent> blockModifyEvents = new ArrayList<>();

    @Override
    public void serialize(NetBuffer b) {
        super.serialize(b);
        b.putArray(blockModifyEvents);
    }

    @Override
    public void deserialize(NetBuffer b) {
        super.deserialize(b);
        blockModifyEvents = b.getArray(BlockModifyEvent::new);
    }
}
