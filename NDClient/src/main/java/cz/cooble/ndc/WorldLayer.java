package cz.cooble.ndc;

import cz.cooble.ndc.core.App;
import cz.cooble.ndc.core.Layer;
import cz.cooble.ndc.core.Pair;
import cz.cooble.ndc.graphics.*;
import cz.cooble.ndc.input.*;
import cz.cooble.ndc.net.Net;
import cz.cooble.ndc.net.Timeout;
import cz.cooble.ndc.net.prot.*;
import cz.cooble.ndc.world.*;
import cz.cooble.ndc.world.block.BlockID;
import cz.cooble.ndc.world.block.BlockRegistry;
import cz.cooble.ndc.world.player.Player;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

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
    private GuiLayer guiLayer;
    private ErrorProtocol currentError;

    float CURSOR_X;
    float CURSOR_Y;

    Timeout.Pocket connectTimeout = new Timeout.Pocket(Globals.CONNECT_TIMEOUT);

    // history of client block placements and player moves
    int blockHistoryEventIdx = 1;
    List<BlockModify> blockHistory = new ArrayList<>();
    int playerHistoryIdx = 1;
    List<PlayerMoves> playerHistory = new ArrayList<>();


    static SpriteSheetResource res = new SpriteSheetResource(new Texture(
            new Texture.Info("res/images/borderBox.png")
                    .filterMode(Texture.FilterMode.NEAREST)
                    .format(Texture.Format.RGBA)), 1, 1);

    public WorldLayer(ClientLayer client, GuiLayer guiLayer) {
        this.client = client;
        this.guiLayer = guiLayer;
    }


    @Override
    public void onAttach() {
        Effect.init();
        RegistryLoader.load();
        ChunkMesh.init();

        currentError=null;
        cam.setPosition(new Vector2f(0, 0));

        // prepare world
        world = new World();
        world.getInfo().chunk_width = 3;
        world.getInfo().chunk_height = 3;


        // graphics
        worldRenderManager = new WorldRenderManager(cam, world);
        renderer = new BatchRenderer2D();
        Stats.bound_sprite = new Sprite(res);
        Stats.bound_sprite.setSpriteIndex(0, 0, false, false, false);
        Stats.bound_sprite.setPosition(new Vector3f(0, 0, 0));
        Stats.bound_sprite.setSize(new Vector2f(1, 1));
        Stats.world = world;

        console = new GuiConsole();
        console.setOnEnter(client::addPendingCommand);


        // callbacks
        client.setOnDisconnect(s -> {
            client.closeSession();
            guiLayer.openInfoScreen("Server disconnected.\nReason:\n" + s, () -> guiLayer.openConnectScreen(this::onIpEntered));
        });
        client.setOnBlockEventsCallback(this::onBlockPacket);
        client.setOnPlayerState(this::onPlayerState);
        client.setOnError(this::onError);
        client.setOnSessionCreatedCallback(this::onSessionCreated);

        // open default gui screen
        guiLayer.openConnectScreen(this::onIpEntered);
    }

    @Override
    public void onRender() {
        if (!client.isSessionCreated() || isLoading)
            return;
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

        for (var p : world.getPlayers().entrySet())
            p.getValue().render(renderer);

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
        renderer.push(new Matrix4f().scale(new Vector3f(2.f / App.get().getWindow().getWidth(), 2.f / App.get().getWindow().getHeight(), 1)));
        console.render(renderer);
        renderer.pop(2);
        renderer.flush();


    }

    private boolean isLoading;

    private void updateConnect() {
        if (!isLoading)
            return;

        // all chunks are downloaded, time to stop loading and begin game
        if (client.isSessionCreated()
                && !client.hasPendingChunks()
                && guiLayer.isEnabled()) {
            guiLayer.setEnabled(false);
            player = world.getPlayerOrNew(client.getPlayerName());
            isLoading = false;
            return;
        }

        // if timeout, reset everything
        if (connectTimeout.isTimeout() || currentError != null) {
            //reattach world, restart everything
            guiLayer.openInfoScreen("Could not connect!\n" + ((currentError != null) ? currentError.reason : "Timeout"), this::onAttach);
            connectTimeout.stop();
            client.closeSession();
            isLoading = false;
            //close gui when connected and downloaded map
            return;
        }
    }

    @Override
    public void onUpdate() {
        if (client.getErrorMessage() != null) {
            guiLayer.openInfoScreen("Fatal Error encountered:\n" + client.getErrorMessage(), () -> System.exit(0));
            return;
        }

        updateConnect();

        // gather new chunks
        if (client.hasPendingChunks()) {
            var newChunk = client.getNewChunk();
            if (newChunk != null) {
                System.out.println("Received whole chunk into world hurra " + newChunk);
                world.setChunk(newChunk);
            }
            return;
        }

        if (isLoading || !client.isSessionCreated())
            return;

        var p = client.getPlayersMoved();
        if (p != null)
            onPlayers(p);

        String s;
        while ((s = client.getNewCommand()) != null)
            console.addLine(s);

        console.update();

        // apply inputs to players
        updatePlayers();
        // move players
        world.onUpdate();

        cam.setPosition(player.getPosition());
    }

    // ========callbacks=================
    private void onPlayerState(PlayerState state) {
        System.out.println("Player state change " + state.type.name() + " valu: " + state.value + " for player " + state.name);
        if (state.type == PlayerState.Type.Fly) {
            var p = world.getPlayerOrNew(state.name);
            p.setFlying(state.value);
        } else if (state.type == PlayerState.Type.Connect) {
            if (state.value) {
                world.getPlayerOrNew(state.name);
            } else
                world.removePlayer(state.name);
        }
    }

    private void onPlayers(PlayersMoved p) {
        float THRESHOLD = 0.001f;
        //System.out.println("Moved Received");
        for (var move : p.moves) {
            if (move.name.equals(client.getPlayerName())) {
                if (playerHistory.isEmpty())//this should never happen
                    continue;
                var offset = move.event_id - playerHistory.get(0).event_id;
                if (offset < 0 || offset >= playerHistory.size())
                    continue;
                playerHistory.subList(0, offset + 1).clear();
                var nextPos = playerHistory.isEmpty() ? player.getPosition() : playerHistory.get(0).pos;
                //System.out.println("Removed events before " + move.event_id);

                //System.out.println("Comparing " + nextPos + " with " + move.targetPos);

                if (nextPos.distanceSquared(move.targetPos) > THRESHOLD * THRESHOLD) {

                    System.out.println("Distance too large, gotta rewind for " + move.event_id);
                    // oh no, server calculated different position from us,
                    // we need to rewind!

                    // change our historic position and rewind back to present
                    player.getPosition().set(move.targetPos);
                    player.getVelocity().set(move.targetVelocity);

                    for (var historicEvent : playerHistory) {
                        //apply speed and position
                        historicEvent.pos.set(player.getPosition());
                        applyPlayerMove(player, historicEvent);
                        // move in the world
                        player.update(world);
                    }
                }
            } else {
                // foreign player which should not even exist
                var pl = world.getPlayer(move.name);
                if (pl == null)
                    continue;

                // we are receiving new input, but we have not applied all previous ones-> set default starting position
                if (pl.getHistory() != null && !pl.getHistory().inputs.isEmpty()) {
                    pl.getPosition().set(pl.getHistory().targetPos);
                    pl.getVelocity().set(pl.getHistory().targetVelocity);

                }
                pl.setHistory(move);
            }
        }
    }

    private void onError(ErrorProtocol e) {
        if (!client.isSessionCreated()) {
            currentError = e;
            System.out.println("Receiving error "+currentError.reason);
        }
    }

    private void onSessionCreated(EstablishConnectionHeader e){
        world.clearWorld();
        guiLayer.openInfoScreen("Downloading map...", null);
        for (int x = 0; x < world.getInfo().chunk_width; x++)
            for (int y = 0; y < world.getInfo().chunk_height; y++)
                client.addPendingChunk(half_int(x, y));
        connectTimeout.stop();
        isLoading = true;
    }

    public void onBlockPacket(ProtocolHeader packet) {
        if (packet instanceof BlockModify) {
            applyBlockModify((BlockModify) packet);
        } else if (packet instanceof BlockAck) {
            if (blockHistory.isEmpty())
                return;
            var offset = ((BlockAck) packet).event_id - blockHistory.get(0).event_id;
            if (offset >= blockHistory.size() || offset < 0)
                return;
            if (!((BlockAck) packet).gut) {
                // undo block modify since server didnt like it
                reverseBlockModify(blockHistory.get(offset));
            }
            // clear all events from before, since they had to be accepted because TCP preservers order
            blockHistory.subList(0, offset + 1).clear();
        }
    }

    public void onIpEntered(Pair<String, String> w) {
        var address = w.getSecond();
        var player = w.getFirst();
        if (player.length() < Globals.MIN_PLAYER_NAME_LENGTH || player.length() > Globals.MAX_PLAYER_NAME_LENGTH) {
            guiLayer.openInfoScreen("Invalid Player Name (3-11 chars)!", () -> guiLayer.openConnectScreen(this::onIpEntered));
            return;
        }

        try {
            InetSocketAddress a = Net.Address.build(address);
            if (a.isUnresolved())
                throw new RuntimeException();
            isLoading = true;
            client.openSession(player, a);
            guiLayer.openInfoScreen("Connecting to server:\n" + address, null);
            connectTimeout.start();
        } catch (Exception e) {
            guiLayer.openInfoScreen("Invalid Address!", () -> guiLayer.openConnectScreen(this::onIpEntered));
        }
    }

    public void applyBlockModify(BlockModify e) {
        if (e.blockOrWall)
            world.setBlockWithNotify(e.x, e.y, new BlockStruct(e.block_id));
        else
            world.setWallWithNotify(e.x, e.y, e.block_id);

        if (e.event_id == 0) {
            e.event_id = blockHistoryEventIdx++;
            blockHistory.add(e);
            client.addPendingBlockModify(e);
        }
    }

    public void applyPlayerMove(Player player, PlayerMoves e) {
        //camera movement===================================================================
        Vector2f accel = new Vector2f(0, 0);
        accel.y = Player.GRAVITY;

        player.getPosition().set(e.pos);

        Vector2f velocity = player.getVelocity();

        float acc = 0.3f;
        float moveThroughBlockSpeed = 6;

        var istsunderBlock = !world.isAir((int) player.getPosition().x, (int) (player.getPosition().y - 1));

        //if (App.get().getWindow().isFocused()) {
        if (player.isFlying()) {
            velocity.x = 0;
            velocity.y = 0;

            if (e.inputs.right)
                velocity.x = moveThroughBlockSpeed;
            if (e.inputs.left)
                velocity.x = -moveThroughBlockSpeed;
            if (e.inputs.up)
                velocity.y = moveThroughBlockSpeed;
            if (e.inputs.down)
                velocity.y = -moveThroughBlockSpeed;
        } else {
            if (e.inputs.right)
                accel.x = acc;
            if (e.inputs.left)
                accel.x = -acc;
            if (e.inputs.up) {
                if (istsunderBlock)
                    velocity.y = 1;
            }
            player.setAcceleration(accel);
        }
        // }

        //even if it is empty we gotta send it otherwise the warui server will cut the line due to timeout
        // zankokuna unmeidayone

        if (client.getPlayerName().equals(player.getName()) && e.event_id == 0) {
            e.event_id = playerHistoryIdx++;
            playerHistory.add(e);
            client.addPendingPlayerMove(e);
        }
    }

    public void reverseBlockModify(BlockModify e) {
        if (e.blockOrWall)
            world.setBlockWithNotify(e.x, e.y, new BlockStruct(e.before_id));
        else
            world.setWallWithNotify(e.x, e.y, e.before_id);
    }

    int primaryBlockId = BlockID.BLOCK_SNOW;
    int secondaryBlockID = BlockID.BLOCK_AIR;

    int primaryWallId = BlockID.WALL_STONE;
    int secondaryWallID = BlockID.WALL_AIR;

    @Override
    public void onEvent(Event e) {
        if (!client.isSessionCreated() || isLoading)
            return;

        if (e.getType() == Event.EventType.MouseMove) {
            var ee = (MouseMoveEvent) e;

            CURSOR_X = ee.getLocation().x - App.get().getWindow().getWidth() / 2;
            CURSOR_Y = ee.getLocation().y - App.get().getWindow().getHeight() / 2;

            CURSOR_X = CURSOR_X / BLOCK_PIXEL_SIZE + cam.getPosition().x;
            CURSOR_Y = CURSOR_Y / BLOCK_PIXEL_SIZE + cam.getPosition().y;

            var b = world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y);

        } else if (e instanceof WindowResizeEvent)
            worldRenderManager.onScreenResize();

        if (console.isEditMode()) {
            console.onEvent(e);
            if (e.handled)
                return;
        }

        if (e instanceof KeyPressEvent) {
            if (((KeyPressEvent) e).isRelease())
                return;
            if (((KeyPressEvent) e).getFreshKeycode() == GLFW_KEY_T) {
                console.openEditMode();
                return;
            }
            if (((KeyPressEvent) e).getFreshKeycode() == GLFW_KEY_ESCAPE) {
                client.closeSession();
                guiLayer.openInfoScreen("Disconnected from server", () -> guiLayer.openConnectScreen(this::onIpEntered));
                onAttach(); //reattach
            }
        }
        if (e instanceof MousePressEvent) {
            var pair = ((MousePressEvent) e).getLocation();
            if (!((MousePressEvent) e).isRelease())
                return;


            CURSOR_X = pair.x - App.get().getWindow().getWidth() / 2;
            CURSOR_Y = pair.y - App.get().getWindow().getHeight() / 2;

            CURSOR_X = CURSOR_X / BLOCK_PIXEL_SIZE + cam.getPosition().x;
            CURSOR_Y = CURSOR_Y / BLOCK_PIXEL_SIZE + cam.getPosition().y;

            //System.out.println("Wall : " + (world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y).isWallFullyOccupied() ? world.getBlockOrAir((int) CURSOR_X, (int) CURSOR_Y).wallID() : "-1"));


            var block = world.getBlock(CURSOR_X, CURSOR_Y);
            if (block == null)
                return;
            var lastWall = block.isWallFullyOccupied() ? block.wallID() : 0;
            var lastBlock = block.block_id;

            if (((MousePressEvent) e).getButton() == MouseCode.MIDDLE) {
                if (!e.isShiftPressed()) {
                    primaryBlockId = lastBlock;
                    System.out.println("Selected " + BlockRegistry.getBlock(primaryBlockId).getStringID());

                } else {
                    primaryWallId = lastWall;
                    System.out.println("Selected " + BlockRegistry.getBlock(primaryWallId).getStringID());
                }
                return;
            }
            if (!e.isShiftPressed()) {
                if (((MousePressEvent) e).getButton() == MouseCode.LEFT) {
                    var event = new BlockModify(lastBlock, primaryBlockId, true, (int) CURSOR_X, (int) CURSOR_Y);
                    applyBlockModify(event);
                } else {
                    var event = new BlockModify(lastBlock, secondaryBlockID, true, (int) CURSOR_X, (int) CURSOR_Y);
                    applyBlockModify(event);
                }
            } else {
                if (((MousePressEvent) e).getButton() == MouseCode.LEFT) {
                    var event = new BlockModify(lastWall, primaryWallId, false, (int) CURSOR_X, (int) CURSOR_Y);
                    applyBlockModify(event);
                } else {
                    var event = new BlockModify(lastWall, secondaryWallID, false, (int) CURSOR_X, (int) CURSOR_Y);
                    applyBlockModify(event);
                }
            }
        }
    }

    private void updatePlayers() {
        var in = App.get().getInput();

        // handle our player
        PlayerMoves e = new PlayerMoves();
        e.inputs.right = in.isKeyPressed(GLFW_KEY_RIGHT);
        e.inputs.left = in.isKeyPressed(GLFW_KEY_LEFT);
        e.inputs.up = in.isKeyPressed(GLFW_KEY_UP);
        e.inputs.down = in.isKeyPressed(GLFW_KEY_DOWN);
        e.pos.set(player.getPosition());
        applyPlayerMove(player, e);

        // handle other players
        for (var p : world.getPlayers().entrySet()) {
            var pl = p.getValue();
            if (pl.getHistory() == null || pl.getName().equals(player.getName()))
                continue;

            if (!pl.getHistory().inputs.isEmpty()) {
                var i = pl.getHistory().inputs.remove(0);
                PlayerMoves m = new PlayerMoves();
                m.inputs = i;
                m.pos.set(pl.getPosition());
                applyPlayerMove(pl, m);

                // It was last input -> apply server pos and velocity, no matter the input from before
                if (pl.getHistory().inputs.isEmpty()) {
                    pl.getPosition().set(pl.getHistory().targetPos);
                    pl.getVelocity().set(pl.getHistory().targetVelocity);
                }
            }
        }
    }
    //todo download map does not timeout
}
