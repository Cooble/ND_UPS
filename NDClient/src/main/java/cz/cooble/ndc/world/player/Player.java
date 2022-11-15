package cz.cooble.ndc.world.player;

import cz.cooble.ndc.Stats;
import cz.cooble.ndc.graphics.*;
import cz.cooble.ndc.physics.Polygon;
import cz.cooble.ndc.physics.Rect;
import cz.cooble.ndc.world.World;
import cz.cooble.ndc.world.block.BlockRegistry;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Player extends PhysEntity implements BatchRenderable2D {


    public static final float GRAVITY =-9.8f / 60 / 4;
    public static final float MAX_SPEED =40.f / 60;

    Animation m_animation;

    Vector2f lastPos = new Vector2f();
    Vector2f m_facing_direction = new Vector2f();
    boolean m_is_facing_left;
    float m_pose = 0;
    float m_last_pose = 0;
    int m_animation_var = 0;

    static SpriteSheetResource res = new SpriteSheetResource(new Texture(
            new Texture.Info("res/images/player3.png")
                    .filterMode(Texture.FilterMode.NEAREST)
                    .format(Texture.Format.RGBA)), 8, 1);

    public Player() {
        m_is_item_consumer = true;
        m_velocity = new Vector2f();
        m_acceleration = new Vector2f(0.f, GRAVITY);
        m_max_velocity = new Vector2f(MAX_SPEED);


        List<Integer> steps = new ArrayList<>();
        int[] s = new int[]{1, 2, 3, 2, 1, 4, 5, 4};
        for (int i : s)
            steps.add(i);

        m_animation = new Animation(res, steps, false, false, false);
        m_animation.setSpriteFrame(0, 0);
        m_animation.setPosition(new Vector3f(-1, 0, 0));
        m_animation.setSize(new Vector2f(2, 3));
        m_bound = Polygon.toPolygon(Rect.createFromDimensions(-0.75f, 0, 1.5f, 2.9f));
    }

    public void update(World w) {
        var lastPos = new Vector2f(m_pos);
        var lastFacingLeft = m_is_facing_left;
        if (!Stats.move_through_blocks_enable)
            computePhysics(w);
        else {
            float len = m_velocity.length();
            if (len > m_max_velocity.x)
                m_velocity.normalize().mul(m_max_velocity.x);
            m_pos.add(m_velocity);
        }
        if (this.m_pos.x > lastPos.x) {
            m_is_facing_left = false;
            m_animation.setHorizontalFlip(false);
            m_animation_var = 1;
        } else if (this.m_pos.x < lastPos.x) {
            m_animation.setHorizontalFlip(true);
            m_is_facing_left = true;
            m_animation_var = 1;
        } else {
            if (this.m_pos.y != lastPos.y) {
                m_animation_var = 0;
                m_animation.setHorizontalFlip(false);
                m_animation.setSpriteFrame(0, 0);
                m_pose = 0;
                m_last_pose = 0;
            } else {
                if (m_animation_var == 1)
                    m_animation.setSpriteFrame(1, 0);
            }
        }
        if (lastFacingLeft != m_is_facing_left)
            m_animation.updateAfterFlip();

        final float animationBlocksPerFrame = 1;
        m_pose += Math.abs(lastPos.x - m_pos.x);
        if (m_pose - m_last_pose > animationBlocksPerFrame) {
            m_pose = m_last_pose;
            if (!w.isAir((int) m_pos.x, (int) (m_pos.y - 1.f)) || lastFacingLeft != m_is_facing_left || Stats.move_through_blocks_enable)
                //no walking while jumping through air
                m_animation.nextFrame();
        }
    }

    @Override
    EntityType getEntityType() {
        return EntityType.PLAYER;
    }


    @Override
    public void render(BatchRenderer2D renderer) {
        renderer.push(new Matrix4f().translate(m_pos.x, m_pos.y, 0));
        renderer.submit(m_animation);
        renderer.pop();

        if (m_bound.size() != 0 && Stats.show_collisionBox) {
            var rect = m_bound.getBounds();
            var mat = new Matrix4f().translate(new Vector3f(m_pos.x + rect.x0, m_pos.y + rect.y0, 0)).scale(rect.width(), rect.height(), 0);
            renderer.push(mat);
            renderer.submit(Stats.bound_sprite);
            renderer.pop();

            for (int y = -4; y < 4; ++y) {
                for (int x = -4; x < 4; ++x) {
                    var str = Stats.world.getBlock(x + m_pos.x, y + m_pos.y);
                    if (str == null)
                        continue;
                    var b = BlockRegistry.getBlock(str.block_id);
                    if (!b.hasCollisionBox())
                        continue;
                    Polygon polygon = b.getCollisionBox((int) (x + m_pos.x), (int) (y + m_pos.y), str);

                    var mati = new Matrix4f().translate((int) m_pos.x + x, (int) m_pos.y + y, 0);
                    renderer.push(mati);
                    var firstVertex = polygon.getVec(0);
                    for (int i = 1; i < polygon.size() - 1; i++)
                        renderer.submitColorTriangle(firstVertex, polygon.getVec(i), polygon.getVec(i+1), 1,
                                new Vector4f(0, 1, 0, 0.85f));
                    renderer.pop();
                }
            }
        }
    }

    public void setFacingDirection(Vector2f dir) {
        this.m_facing_direction = dir;
    }

    public Vector2f getFacingDirection() {
        return m_facing_direction;
    }
}
