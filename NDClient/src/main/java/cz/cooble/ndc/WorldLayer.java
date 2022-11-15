package cz.cooble.ndc;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.graphics.*;
import cz.cooble.ndc.input.*;
import cz.cooble.ndc.world.*;
import cz.cooble.ndc.world.block.BlockID;
import cz.cooble.ndc.world.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static cz.cooble.ndc.world.WorldRenderManager.BLOCK_PIXEL_SIZE;
import static org.lwjgl.glfw.GLFW.*;


public class WorldLayer extends Layer {

    private World world;
    private WorldRenderManager worldRenderManager;
    private Camera cam = new Camera();
    private Player player;
    private BatchRenderer2D renderer;

    static SpriteSheetResource res = new SpriteSheetResource(new Texture(
            new Texture.Info("res/images/borderBox.png")
                        .filterMode(Texture.FilterMode.NEAREST)
                        .format(Texture.Format.RGBA)), 1, 1);

    @Override
    public void onAttach() {

        Effect.init();
        RegistryLoader.load();
        ChunkMesh.init();

        cam.setPosition(new Vector2f(0,0));

        world = new World();
        world.loadChunk(0, 0);
        world.loadChunk(0, 1);
        world.loadChunk(1, 0);
        world.loadChunk(1, 1);

        worldRenderManager = new WorldRenderManager(cam, world);

        player = new Player();

        renderer = new BatchRenderer2D();

        Stats.bound_sprite = new Sprite(res);
        Stats.bound_sprite.setSpriteIndex(0, 0,false,false,false);
        Stats.bound_sprite.setPosition(new Vector3f(0, 0, 0));
        Stats.bound_sprite.setSize(new Vector2f(1, 1));
        Stats.world = world;
    }


    @Override
    public void onRender() {
        renderer.begin(Renderer.getDefaultFBO());
        worldRenderManager.update();
        worldRenderManager.render(renderer, Renderer.getDefaultFBO());
        renderer.flush();

        //entities
        var entityFbo = worldRenderManager.getEntityFBO();
        entityFbo.bind();
        entityFbo.clear(GCon.BufferBit.COLOR);

        GCon.enableBlend();
        GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
        var worldMatrix = new Matrix4f().translate(new Vector3f(-cam.getPosition().x, -cam.getPosition().y, 0));

        renderer.begin(worldRenderManager.getEntityFBO());
        renderer.push(worldRenderManager.getProjMatrix());
        renderer.push(worldMatrix);

        /*for (auto it = m_world.beginEntities(); it != m_world.endEntities(); ++it)
        {
            WorldEntity* entity = m_world.getLoadedEntity(*it);
            auto ren = dynamic_cast<IBatchRenderable2D*>(entity);
            if (ren)
                ren.render(*renderer);
        }*/
        player.render(renderer);
        renderer.pop(2);
        renderer.flush();
        entityFbo.unbind();

        GCon.enableBlend();
        GCon.enableDepthTest(false);
        GCon.blendFunc(GCon.Blend.SRC_ALPHA, GCon.Blend.ONE_MINUS_SRC_ALPHA);
        Effect.render(worldRenderManager.getEntityFBO().getAttachment(), Renderer.getDefaultFBO());
    }


    @Override
    public void onUpdate() {

            updatePlayer();
        player.update(world);
        cam.setPosition(player.getPosition());
    }

    float CURSOR_X;
    float CURSOR_Y;

    @Override
    public void onEvent(Event e) {

        if (e.getType() == Event.EventType.MouseMove) {
            var ee = (MouseMoveEvent) e;

            CURSOR_X = ee.getLocation().x - App.get().getWindow().getWidth() / 2;
            CURSOR_Y = -ee.getLocation().y + App.get().getWindow().getHeight() / 2;

            CURSOR_X = CURSOR_X / BLOCK_PIXEL_SIZE + cam.getPosition().x;
            CURSOR_Y = CURSOR_Y / BLOCK_PIXEL_SIZE + cam.getPosition().y;

            var b = world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y);


            //System.out.println("Wall : " + b);

        }
        else if (e instanceof WindowResizeEvent)
            worldRenderManager.onScreenResize();

        if (e instanceof MousePressEvent) {
            var pair = ((MousePressEvent) e).getLocation();
            if(!((MousePressEvent) e).isRelease())
                return;


            CURSOR_X = pair.x - App.get().getWindow().getWidth() / 2;
            CURSOR_Y = -pair.y + App.get().getWindow().getHeight() / 2;

            CURSOR_X = CURSOR_X / BLOCK_PIXEL_SIZE + cam.getPosition().x;
            CURSOR_Y = CURSOR_Y / BLOCK_PIXEL_SIZE + cam.getPosition().y;

            //System.out.println("Wall : " + (world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y).isWallFullyOccupied() ? world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y).wallID() : "-1"));



            if (!e.isShiftPressed()) {
                if (((MousePressEvent) e).getButton() == MouseCode.LEFT)
                    world.setBlockWithNotify((int) CURSOR_X, (int) CURSOR_Y, new BlockStruct(0));
                else
                    world.setBlockWithNotify((int) CURSOR_X, (int) CURSOR_Y, new BlockStruct(BlockID.BLOCK_SNOW));
            } else {
                if (((MousePressEvent) e).getButton() == MouseCode.LEFT)
                    world.setWallWithNotify((int) CURSOR_X, (int) CURSOR_Y, BlockID.WALL_DIRT);
                else{
                    world.setWallWithNotify((int) CURSOR_X, (int) CURSOR_Y, 0);
            }}

        }

        if (e instanceof KeyPressEvent) {
            if(!((KeyPressEvent) e).isRelease())
                return;
             if(((KeyPressEvent) e).getFreshKeycode()==GLFW_KEY_J) {

                 Stats.move_through_blocks_enable ^= true;
                 Stats.fly_enable ^= true;
                 System.out.println(Stats.fly_enable);
             }
        }
    }

    private void updatePlayer() {
        //camera movement===================================================================
        Vector2f accel = new Vector2f(0, 0);
        accel.y = Player.GRAVITY;

        Vector2f velocity = player.getVelocity();

        float acc = 0.3f;
        float moveThroughBlockSpeed = 6;

        var istsunderBlock = !world.isAir((int) cam.getPosition().x, (int) (cam.getPosition().y - 1));
        var in = App.get().getInput();

        if (App.get().getWindow().isFocused()) {
            if (Stats.move_through_blocks_enable) {

                velocity.x = 0;
                velocity.y = 0;

                if (in.isKeyPressed(GLFW_KEY_RIGHT))
                    velocity.x = moveThroughBlockSpeed;
                if (in.isKeyPressed(GLFW_KEY_LEFT))
                    velocity.x = -moveThroughBlockSpeed;


                if (in.isKeyPressed(GLFW_KEY_UP))
                    velocity.y = moveThroughBlockSpeed;
                if (in.isKeyPressed(GLFW_KEY_DOWN))
                    velocity.y = -moveThroughBlockSpeed;
            }
            else
            {
                if (in.isKeyPressed(GLFW_KEY_RIGHT))
                    accel.x = acc;
                if (in.isKeyPressed(GLFW_KEY_LEFT))
                    accel.x = -acc;
                if (in.isKeyPressed(GLFW_KEY_UP))
                {
                    if (istsunderBlock || Stats.fly_enable)
                        velocity.y = 1;
                }
                player.setAcceleration(accel);
            }
        }
    }
}
