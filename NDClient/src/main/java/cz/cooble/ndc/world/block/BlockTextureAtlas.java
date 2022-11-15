package cz.cooble.ndc.world.block;

import java.util.HashMap;
import java.util.Map;

import static cz.cooble.ndc.Asserter.ASSERT;
import static cz.cooble.ndc.core.Utils.half_int;

public class BlockTextureAtlas {

    // string, half_int
    private Map<String,Integer> m_subtextures = new HashMap<>();

    
    public BlockTextureAtlas(){
        m_subtextures.put("block/stone",half_int(7*4,1*4));
        m_subtextures.put("block/dirt",half_int(4*4,0*4));
        m_subtextures.put("block/snow",half_int(5*4,0*4));
        m_subtextures.put("block/ice",half_int(2*4,1*4));
        m_subtextures.put("block/gold",half_int(7*4,0*4));
        m_subtextures.put("block/snow_bricks",half_int(3*4,1*4));

        m_subtextures.put("wall/stone",half_int(6*4,1*4));
        m_subtextures.put("wall/dirt",half_int(5*4,1*4));
        m_subtextures.put("wall/gold",half_int(2*4,2*4));
        m_subtextures.put("wall/snow",half_int(5*4,0*4));
    }
    public int getTexture(String fileName) {
       var  o  =  m_subtextures.get(fileName);
       ASSERT(o!=null,"cannot find texture in atlas "+fileName);
       return o;
    }
}
