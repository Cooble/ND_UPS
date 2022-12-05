package cz.cooble.ndc.world;

import cz.cooble.ndc.net.NetBuffer;
import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.net.NetSerializable;
import cz.cooble.ndc.test.NetWriter;

public class BlockModifyEvent implements NetSerializable {

    public int block_id;
    public boolean blockOrWall;
    public int x,y;

    public BlockModifyEvent(){}
    public BlockModifyEvent(int block_id,boolean blockOrWall,int x,int y){
        this.block_id = block_id;
        this.blockOrWall = blockOrWall;
        this.x = x;
        this.y = y;
    }


    @Override
    public void serialize(NetWriter b) {
        b.put(block_id);
        b.put(blockOrWall?1:0);
        b.put(x);
        b.put(y);
    }

    @Override
    public void deserialize(NetReader b) {
        block_id = b.getInt();
        blockOrWall = b.getInt()==1;
        x = b.getInt();
        y = b.getInt();

    }
}
