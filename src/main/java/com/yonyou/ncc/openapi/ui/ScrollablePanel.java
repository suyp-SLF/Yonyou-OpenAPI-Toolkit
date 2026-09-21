package com.yonyou.ncc.openapi.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * 宽度跟随视口、高度按内容的容器，放进 JScrollPane 后窗口再小也能滚到最底部，
 * 不会出现「下面的控件看不到」的情况。
 */
public final class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel() {
        super();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return visibleRect.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}

