package cz.cooble.ndc.graphics;

import java.util.ArrayList;
import java.util.List;

public class Animation extends Sprite {
    List<Integer> m_indexes = new ArrayList<>();
    int m_current_index = 0;
    boolean m_repeat_saw = false;
    int m_increase_index = 1;

    boolean m_horizontalFlip;
    boolean m_verticalFlip;

    public Animation(SpriteSheetResource r, List<Integer> uvs, boolean repeatSaw, boolean horizontalFlip, boolean verticalFlip) {
        super(r);
        m_repeat_saw = repeatSaw;
        m_horizontalFlip = horizontalFlip;
        m_verticalFlip = verticalFlip;
        m_indexes.addAll(uvs);
    }

    // need call updateAfterFlip() or nextFrame() to refreshImage
    public void setHorizontalFlip(boolean v) {m_horizontalFlip = v;}

    // need call updateAfterFlip() or nextFrame() to refreshImage
    public void setVerticalFlip(boolean v) {m_verticalFlip = v;}

    public void nextFrame() {
        m_current_index += m_increase_index;

        setSpriteIndex(m_indexes.get(m_current_index), 0, m_horizontalFlip, m_verticalFlip, false);

        if (m_current_index == m_indexes.size() - 1) {
            if (m_repeat_saw)
                m_current_index = -1;
            else
                m_increase_index = -1;
        } else if (m_current_index == 0)
            m_increase_index = 1;
    }

    public void setSpriteFrame(int u, int v) {
        setSpriteIndex(u, v, m_horizontalFlip, m_verticalFlip, false);
    }

    // after changing flipping need to call this to refresh image
    public void updateAfterFlip() {
        setSpriteIndex(m_indexes.get(m_current_index), 0, m_horizontalFlip, m_verticalFlip, false);
    }

    public  void reset() {
        m_increase_index = 1;
        m_current_index = -1;
    }

}
