package cz.cooble.ndc.world.player;

import cz.cooble.ndc.core.NBT;
import cz.cooble.ndc.core.Utils;
import cz.cooble.ndc.physics.Polygon;
import cz.cooble.ndc.physics.Rect;
import cz.cooble.ndc.world.World;
import cz.cooble.ndc.world.block.BlockRegistry;
import org.joml.Vector2f;

import java.awt.*;

import static cz.cooble.ndc.physics.Polygon.*;
import static cz.cooble.ndc.world.block.BlockID.BLOCK_AIR;
import static cz.cooble.ndc.world.block.BlockID.BLOCK_ICE;
import static cz.cooble.ndc.world.player.PhysEntity.Blockage.*;
import static org.joml.Math.*;

public abstract class PhysEntity extends WorldEntity {
    enum Blockage {
        LEFT,
        RIGHT,
        STUCK,
        NONE,
    }

    boolean m_is_on_floor;
    Blockage m_blockage;

    Vector2f m_velocity = new Vector2f();
    Vector2f m_max_velocity = new Vector2f(40.f / 60);
    Vector2f m_acceleration = new Vector2f();
    Polygon m_bound = Polygon.toPolygon(new Rect(0, 0, 2, 3));
    boolean m_can_walk = true;

    static boolean findBlockIntersection(World w, Vector2f entityPos, Polygon entityBound) {
        var entityRectangle = entityBound.getBounds();
        for (int x = (int) (entityRectangle.x0 - 1); x < Math.ceil(entityRectangle.x1) + 1; ++x) {
            for (int y = (int) (entityRectangle.y0 - 1); y < Math.ceil(entityRectangle.y1) + 1; ++y) {
                Vector2f blockPos = new Vector2f(x, y);
                blockPos.add(entityPos);

                var stru = w.getBlock((int) blockPos.x, (int) blockPos.y);
                if (stru == null || stru.block_id == BLOCK_AIR)
                    continue;
                var block = BlockRegistry.getBlock(stru.block_id);
                if (!block.hasCollisionBox())
                    continue;
                Polygon blockBounds = block.getCollisionBox((int) blockPos.x, (int) blockPos.y, stru).copy();

                blockBounds.plus(new Vector2f(x - (entityPos.x - (int) entityPos.x), y - (entityPos.y - (int) entityPos.y)));
                if (isIntersects(blockBounds, entityBound))
                    return true;
            }
        }
        return false;
    }

    public static float getFloorHeight(World w, Vector2f entityPos, Rect entityRectangle) {
        float lineY = -Float.MAX_VALUE;
        for (int i = 0; i < 2; ++i) {
            for (int yy = 0; yy < 2; ++yy) {
                float x = entityRectangle.x0 + entityPos.x + i * entityRectangle.width();
                float y = entityRectangle.y0 + entityPos.y + yy;
                Vector2f pointDown0 = new Vector2f(x, y);
                Vector2f pointDown1 = new Vector2f(x, y - 10000);

                var stru = w.getBlock((int) pointDown0.x, (int) pointDown0.y);
                if (stru == null || stru.block_id == BLOCK_AIR)
                    continue;
                var block = BlockRegistry.getBlock(stru.block_id);
                if (!block.hasCollisionBox())
                    continue;

                Polygon blockBounds = block.getCollisionBox((int) pointDown0.x, (int) pointDown0.y, stru).copy();
                Vector2f blockPos = new Vector2f((int) pointDown0.x, (int) pointDown0.y);
                for (int j = 0; j < blockBounds.size(); ++j) {
                    Vector2f v0 = blockBounds.getVec(j).add(blockPos);
                    Vector2f v1 = blockBounds.getVec((j + 1) % blockBounds.size()).add(blockPos);

                    if (i == 0) //left
                    {
                        if (v0.x > pointDown0.x)
                            lineY = Math.max(lineY, v0.y);
                    } else if (v0.x < pointDown0.x) //right
                        lineY = Math.max(lineY, v0.y);

                    Vector2f v = intersectLines(v0, v1, pointDown0, pointDown1);
                    if (isValid(v) && v.y < entityRectangle.y1 + entityPos.y)
                        lineY = Math.max(lineY, v.y);
                }
            }
        }
        return lineY;
    }

    static final float maxDistancePerStep = 0.4f;

    static float sgn(float f) {
        if (f == 0)
            return 0;
        return abs(f) / f;
    }

    public Vector2f getVelocity() {
        return m_velocity;
    }

    public Vector2f getAcceleration() {
        return m_acceleration;
    }

    public void setAcceleration(Vector2f m_acceleration) {
        this.m_acceleration = m_acceleration;
    }

    public void computePhysics(World w) {
        //todo make entity flags to dtermine whether this force oughta be applied
        float xRepelForce = 0;

        float lastXAcc = m_acceleration.x;
        if (xRepelForce != 0) {
            m_acceleration.x += clamp(xRepelForce, -1.f, 1.f) * 0.15f;
        }

        computeVelocity(w);

        m_acceleration.x = lastXAcc;

        computeWindResistance(w);

        float lengthCab = m_velocity.length();

        if (lengthCab > maxDistancePerStep) {
            float dividor = ceil(lengthCab / maxDistancePerStep);
            for (int i = 0; i < dividor; ++i) {
                if (moveOrCollide(w, 1.0f / dividor))
                    break;
            }
        } else {
            boolean b = moveOrCollide(w, 1);
        }
    }

    private static final boolean IGNORE_FAULTY_FLOOR_HEIGHT = true;

    boolean moveOrCollide(World w, float dt) {
        m_blockage = NONE;
        m_is_on_floor = false;

        var underBlock = w.getBlock(m_pos.x, m_pos.y - 1);
        var currentBlock = w.getBlock(m_pos.x, m_pos.y);
        boolean isSlippery = (underBlock != null && underBlock.block_id ==
                BLOCK_ICE)
                || (currentBlock != null && currentBlock.block_id == BLOCK_ICE);
        float floorResistance = isSlippery ? 0.005f : 0.2f; //negative numbers mean conveyor belt Yeah!


        //collision detection============================
        var possibleEntityPos = new Vector2f(m_pos).add(new Vector2f(m_velocity).mul(dt));
        //check if we can procceed in x, y direction
        if (findBlockIntersection(w, possibleEntityPos, m_bound)) {
            possibleEntityPos.set(m_pos);
            possibleEntityPos.x += m_velocity.x * dt;
            //check if we can procceed at least in x direction
            if (findBlockIntersection(w, possibleEntityPos, m_bound)) {
                //walk on floor check
                if (m_can_walk && m_bound.isRectangle) {
                    float y0 = m_bound.getBounds().y0 + m_pos.y;
                    float floorHeight = getFloorHeight(w, possibleEntityPos, m_bound.getBounds());

                    float difference = floorHeight - y0;

                    final float maxHeightToWalk = 0.99f;
                    if (floorHeight < (m_bound.getBounds().y1 + m_pos.y) &&
                            difference > -0.06f &&
                            difference < maxHeightToWalk) {
                        possibleEntityPos.y = floorHeight + 0.05f + m_bound.getBounds().y0;
                        if (!findBlockIntersection(w, possibleEntityPos, m_bound)) {
                            m_is_on_floor = true;
                            m_pos = new Vector2f(possibleEntityPos);
                            m_velocity.y = 0;

                            if (m_velocity.x > 0) //apply resistance
                            {
                                m_velocity.x -= floorResistance / 2;
                                if (m_velocity.x < 0)
                                    m_velocity.x = 0;
                            } else if (m_velocity.x < 0) {
                                m_velocity.x += floorResistance / 2;
                                if (m_velocity.x > 0)
                                    m_velocity.x = 0;
                            }

                            return false;
                        }
                    }
                }

                possibleEntityPos.set(m_pos);
                possibleEntityPos.y += m_velocity.y * dt;
                //check if we can procceed at least in y direction
                if (findBlockIntersection(w, possibleEntityPos, m_bound)) {
                    //we have nowhere to go
                    m_blockage = m_velocity.x > 0 ? RIGHT : LEFT;
                    if (m_velocity.y <= 0) {
                        m_is_on_floor = true;
                        m_pos.y = (int) m_pos.y + 0.01f;
                    }
                    possibleEntityPos.set(m_pos);
                    m_velocity.set(0);
                } else {
                    //we can proceed in y direction
                    m_velocity.x = 0;
                }
            } else //we can procceed in x direction
            {
                if (m_velocity.y <= 0) {
                    if (m_bound.isRectangle) {
                        float floorHeight = getFloorHeight(w, possibleEntityPos, m_bound.getBounds());
                        if (isValidFloat(floorHeight) && w.isBlockValid((int) possibleEntityPos.x, (int) floorHeight) && abs(
                                floorHeight - m_pos.y) < 0.5)
                            possibleEntityPos.y = floorHeight + 0.01f;
                    }
                    m_is_on_floor = true;
                }
                m_velocity.y = 0;


                if (m_velocity.x > 0) {
                    m_velocity.x -= floorResistance;
                    if (m_velocity.x < 0)
                        m_velocity.x = 0;
                } else if (m_velocity.x < 0) {
                    m_velocity.x += floorResistance;
                    if (m_velocity.x > 0)
                        m_velocity.x = 0;
                }
            }
        }
        m_pos.set(possibleEntityPos);
        return false;
    }


    boolean moveOrCollideOnlyBlocksNoBounds(World w) {
        var possiblePos = new Vector2f(m_pos).add(m_velocity);
        var b = w.getBlock(possiblePos.x, possiblePos.y);
        if (b != null) {
            var block = BlockRegistry.getBlock(b.block_id);
            if (block.hasCollisionBox()) {
                var blockBounds = block.getCollisionBox((int) possiblePos.x, (int) possiblePos.y, b);
                if (contains(blockBounds,
                        new Vector2f((possiblePos.x - (int) possiblePos.x), (possiblePos.y - (int) possiblePos.y))))
                    return false;
            }
        }
        m_pos = new Vector2f(possiblePos);
        return true;
    }

    boolean collideLine(World w, Vector2f from, Vector2f to) {
        float DIVIDER = 1 / 2.f;
        var vector = new Vector2f(to).sub(from);
        float length = vector.length();

        var slider = new Vector2f(vector).mul(DIVIDER / length);


        for (int i = 0; i < (int) (length / DIVIDER); ++i) {
            var pos = new Vector2f(from).add(new Vector2f(slider).mul(i)).add(new Vector2f(0, 1));
            var bb = w.getBlockOrAir((int) pos.x, (int) pos.y);

            var block = BlockRegistry.getBlock(bb.block_id);
            if (block.hasCollisionBox()) {
                var blockBounds = block.getCollisionBox((int) pos.x, (int) pos.y, bb);
                if (contains(blockBounds,
                        new Vector2f((pos.x - (int) pos.x), (pos.y - (int) pos.y))))
                    return false;
            }
        }
        return true;


    }

    void computeVelocity(World w) {
        //motion=========================================
        m_velocity.add(m_acceleration);
        m_velocity = Utils.clamp(m_velocity, new Vector2f(m_max_velocity).mul(-1), m_max_velocity);
    }

    void computeWindResistance(World w) {
        final float windResistance = 0.01f;
        //wind resistance================================
        //expr float windResistance = 0.01;
        final float windVelocityDepenceFactor = 1000000; //we dont need higher air resistance the bigger the velocity

        if (m_velocity.x > 0) {
            m_velocity.x -= windResistance + m_velocity.x / windVelocityDepenceFactor;
            if (m_velocity.x < 0)
                m_velocity.x = 0;
        } else if (m_velocity.x < 0) {
            m_velocity.x += windResistance - m_velocity.x / windVelocityDepenceFactor;
            if (m_velocity.x > 0)
                m_velocity.x = 0;
        }
    }

    public void save(NBT src) {
        super.save(src);
        src.put("velocity", m_velocity);
        src.put("acceleration", m_acceleration);
    }

    public void load(NBT src) {
        super.load(src);
        m_velocity = src.getVector2f("velocity", new Vector2f(0, 0));
        m_acceleration = src.getVector2f("acceleration", new Vector2f(0, -9.8f / 60));
    }
}
