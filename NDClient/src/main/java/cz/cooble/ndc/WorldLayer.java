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

import static cz.cooble.ndc.core.Utils.half_int;
import static cz.cooble.ndc.world.WorldRenderManager.BLOCK_PIXEL_SIZE;
import static org.lwjgl.glfw.GLFW.*;


public class WorldLayer extends Layer {

    private final ClientLayer client;
    private World world;
    private WorldRenderManager worldRenderManager;
    private Camera cam = new Camera();
    private Player player;
    private BatchRenderer2D renderer;
    private GuiConsole console;


    static SpriteSheetResource res = new SpriteSheetResource(new Texture(
            new Texture.Info("res/images/borderBox.png")
                    .filterMode(Texture.FilterMode.NEAREST)
                    .format(Texture.Format.RGBA)), 1, 1);

    public WorldLayer(ClientLayer client) {
        this.client = client;
    }

    @Override
    public void onAttach() {

        Effect.init();
        RegistryLoader.load();
        ChunkMesh.init();

        cam.setPosition(new Vector2f(0, 0));

        client.openSession("karel");


        world = new World();
        world.getInfo().chunk_width = 3;
        world.getInfo().chunk_height = 3;

        for (int x = 0; x < world.getInfo().chunk_width; x++)
            for (int y = 0; y < world.getInfo().chunk_height; y++)
                client.addPendingChunk(half_int(x, y));


        worldRenderManager = new WorldRenderManager(cam, world);

        player = new Player();

        renderer = new BatchRenderer2D();

        Stats.bound_sprite = new Sprite(res);
        Stats.bound_sprite.setSpriteIndex(0, 0, false, false, false);
        Stats.bound_sprite.setPosition(new Vector3f(0, 0, 0));
        Stats.bound_sprite.setSize(new Vector2f(1, 1));
        Stats.world = world;

        console = new GuiConsole();
        console.setOnEnter(client::addPendingCommand);
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

        renderer.begin(Renderer.getDefaultFBO());

        renderer.push(new Matrix4f().translate(new Vector3f(-1, -1, 0)));
        renderer.push(new Matrix4f().scale(new Vector3f(2.f / App.get().getWindow().getWidth(), 2.f / App.get().getWindow().getHeight(), 1 )));
        console.render(renderer);
        renderer.pop(2);
        renderer.flush();
    }


    @Override
    public void onUpdate() {

        String s;
        while ((s = client.getCommand())!=null)
            console.addLine(s);

        console.update();

        if (client.hasPendingChunks()) {
            var newChunk = client.getNewChunk();
            if (newChunk != null) {
                System.out.println("Received whoel chunk into world hurra " + newChunk);
                world.setChunk(newChunk);
            }
            return;
        }

        updatePlayer();
        player.update(world);
        cam.setPosition(player.getPosition());
    }

    float CURSOR_X;
    float CURSOR_Y;

    @Override
    public void onEvent(Event e) {
        if(!client.isSessionCreated())
            return;

        if (e.getType() == Event.EventType.MouseMove) {
            var ee = (MouseMoveEvent) e;

            CURSOR_X = ee.getLocation().x - App.get().getWindow().getWidth() / 2;
            CURSOR_Y = -ee.getLocation().y + App.get().getWindow().getHeight() / 2;

            CURSOR_X = CURSOR_X / BLOCK_PIXEL_SIZE + cam.getPosition().x;
            CURSOR_Y = CURSOR_Y / BLOCK_PIXEL_SIZE + cam.getPosition().y;

            var b = world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y);


            //System.out.println("Wall : " + b);

        } else if (e instanceof WindowResizeEvent)
            worldRenderManager.onScreenResize();

        if(console.isEditMode()){
            console.onEvent(e);
            if(e.handled)
                return;
        }
        if (e instanceof KeyPressEvent) {
            if (((KeyPressEvent) e).isRelease())
                return;
            if (((KeyPressEvent) e).getFreshKeycode() == GLFW_KEY_T) {
                console.openEditMode();
                return;
            }
            if (((KeyPressEvent) e).getFreshKeycode() == GLFW_KEY_J) {

                Stats.move_through_blocks_enable ^= true;
                Stats.fly_enable ^= true;
                System.out.println(Stats.fly_enable);
            }

        }
        if (e instanceof MousePressEvent) {
            var pair = ((MousePressEvent) e).getLocation();
            if (!((MousePressEvent) e).isRelease())
                return;




            CURSOR_X = pair.x - App.get().getWindow().getWidth() / 2;
            CURSOR_Y = -pair.y + App.get().getWindow().getHeight() / 2;

            CURSOR_X = CURSOR_X / BLOCK_PIXEL_SIZE + cam.getPosition().x;
            CURSOR_Y = CURSOR_Y / BLOCK_PIXEL_SIZE + cam.getPosition().y;

            //System.out.println("Wall : " + (world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y).isWallFullyOccupied() ? world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y).wallID() : "-1"));


            int primaryBlockId = BlockID.BLOCK_AIR;
            int secondaryBlockID = BlockID.BLOCK_SNOW;

            int primaryWallId = BlockID.WALL_AIR;
            int secondaryWallID = BlockID.WALL_DIRT;

            if (!e.isShiftPressed()) {
                if (((MousePressEvent) e).getButton() == MouseCode.LEFT) {
                    world.setBlockWithNotify((int) CURSOR_X, (int) CURSOR_Y, new BlockStruct(primaryBlockId));
                    client.addPendingBlockModify(new BlockModifyEvent(primaryBlockId, true, (int) CURSOR_X, (int) CURSOR_Y));
                } else {
                    world.setBlockWithNotify((int) CURSOR_X, (int) CURSOR_Y, new BlockStruct(secondaryBlockID));
                    client.addPendingBlockModify(new BlockModifyEvent(secondaryBlockID, true, (int) CURSOR_X, (int) CURSOR_Y));
                }
            } else {
                if (((MousePressEvent) e).getButton() == MouseCode.LEFT) {
                    world.setWallWithNotify((int) CURSOR_X, (int) CURSOR_Y, primaryWallId);
                    client.addPendingBlockModify(new BlockModifyEvent(primaryWallId, false, (int) CURSOR_X, (int) CURSOR_Y));
                } else {
                    world.setWallWithNotify((int) CURSOR_X, (int) CURSOR_Y, secondaryWallID);
                    client.addPendingBlockModify(new BlockModifyEvent(secondaryWallID, false, (int) CURSOR_X, (int) CURSOR_Y));
                }
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
            } else {
                if (in.isKeyPressed(GLFW_KEY_RIGHT))
                    accel.x = acc;
                if (in.isKeyPressed(GLFW_KEY_LEFT))
                    accel.x = -acc;
                if (in.isKeyPressed(GLFW_KEY_UP)) {
                    if (istsunderBlock || Stats.fly_enable)
                        velocity.y = 1;
                }
                player.setAcceleration(accel);
            }
        }
    }
}
