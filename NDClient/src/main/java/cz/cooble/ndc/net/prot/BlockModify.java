package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.Prot;
import cz.cooble.ndc.test.NetWriter;

public class BlockModify extends ProtocolHeader {

    public int before_id;
    public int block_id;
    public boolean blockOrWall;
    public int x, y;
    public int event_id;

    public BlockModify() {action = Prot.BlockModify;}

    public BlockModify(int before_id, int block_id, boolean blockOrWall, int x, int y) {
        action = Prot.BlockModify;
        this.before_id = before_id;
        this.block_id = block_id;
        this.blockOrWall = blockOrWall;
        this.x = x;
        this.y = y;
    }


    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(block_id);
        b.put(blockOrWall ? 1 : 0);
        b.put(x);
        b.put(y);
        b.put(event_id);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        block_id = b.getInt();
        blockOrWall = b.getInt() == 1;
        x = b.getInt();
        y = b.getInt();
        event_id = b.getInt();
    }
}
