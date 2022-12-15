package cz.cooble.ndc.world;

import cz.cooble.ndc.world.block.*;
import cz.cooble.ndc.world.wall.WallAir;
import cz.cooble.ndc.world.wall.WallDirt;
import cz.cooble.ndc.world.wall.WallSnow;
import cz.cooble.ndc.world.wall.WallStone;

public class RegistryLoader {


    private static boolean alreadyLoaded = false;
    public static void load(){
        if(alreadyLoaded)
            return;
        alreadyLoaded=true;
        BlockRegistry.registerBlock(new BlockAir());
        BlockRegistry.registerBlock(new BlockStone());
        BlockRegistry.registerBlock(new BlockDirt());
        BlockRegistry.registerBlock(new BlockSnow());
        BlockRegistry.registerBlock(new BlockIce());
        BlockRegistry.registerBlock(new BlockGold());
        BlockRegistry.registerBlock(new BlockSnowBricks());

        BlockRegistry.registerWall(new WallAir());
        BlockRegistry.registerWall(new WallStone());
        BlockRegistry.registerWall(new WallDirt());
        BlockRegistry.registerWall(new WallSnow());

        BlockRegistry.initTextures(new BlockTextureAtlas());
    }
}
