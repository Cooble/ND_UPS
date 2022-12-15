package cz.cooble.ndc.net.prot;

import cz.cooble.ndc.net.NetReader;
import cz.cooble.ndc.test.NetWriter;

public class ChunkProtocol extends SessionProtocol {
    public int c_x,c_y,piece=-1;

    public static final int CHUNK_PIECE_SIZE = 1040; //52 blocks
    public static final int CHUNK_PIECE_COUNT = 20; //52 blocks
    public static final int LAST_CHUNK_PIECE_IDX = CHUNK_PIECE_COUNT-1; //52 blocks
    public static final int LAST_CHUNK_PIECE_SIZE = 32 * 32 * 20 - (CHUNK_PIECE_COUNT-1) * CHUNK_PIECE_SIZE; //total bytes - 19 pieces

    @Override
    public void serialize(NetWriter b) {
        super.serialize(b);
        b.put(c_x);
        b.put(c_y);
        b.put(piece);
    }

    @Override
    public void deserialize(NetReader b) {
        super.deserialize(b);
        c_x = b.getInt();
        c_y = b.getInt();
        piece = b.getInt();
    }
}
