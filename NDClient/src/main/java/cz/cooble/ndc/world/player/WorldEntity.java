package cz.cooble.ndc.world.player;

import cz.cooble.ndc.core.NBT;
import cz.cooble.ndc.world.World;
import org.joml.Vector2f;

import static cz.cooble.ndc.core.Utils.BIT;

public abstract class WorldEntity {
    public static final int EFLAG_TEMPORARY = BIT(0);//will be killed on chunk unload
    public static final int EFLAG_CHUNK_LOADER = BIT(1);//will keep chunks around loaded (usually Player)
    public static final int EFLAG_COLLIDER = BIT(2);//will collide with other entities (will be pushed by them)


    boolean m_is_dead = false;
    boolean m_is_item_consumer = false;
    int m_id;// each entity instance has unique id
    long m_flags = 0;
    Vector2f m_pos=new Vector2f();//world pos

    public boolean hasFlag(long flags) {return (m_flags & flags) == flags;}

    public boolean isMarkedDead() {return m_is_dead;}

    public int getID() {return m_id;}

    public void markDead() {m_is_dead = true;}//used for suicide (no one else can call this method)

    abstract EntityType getEntityType();

    public Vector2f getPosition() {return m_pos;}

    public boolean isItemConsumer() {return m_is_item_consumer;}

    public void update(World w) {}

    public void onLoaded(World w) {}

    public void onUnloaded(World w) {}

    public void onSpawned(World w) {}

    public void onKilled(World w) {}


    public void save(NBT src) {
        src.put("entityPos", m_pos);
        src.put("entityTypeID", getEntityType().ordinal());
        src.put("entityID", m_id);
    }

    public void load(NBT src) {
        m_pos = src.getVector2f("entityPos");
        m_id = src.getInteger("entityID");
    }
}
