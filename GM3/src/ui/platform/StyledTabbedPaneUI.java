package ui.platform;

import sun.swing.SwingUtilities2;
import ui.icon.Icons;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.util.function.BiConsumer;

/**
 * Created by BESTDOG on 12/8/2015.
 *
 */
public class StyledTabbedPaneUI extends BasicTabbedPaneUI {

    private final Color selectedBlue = new Color(242, 249, 252);
    private final Color selectedBlueBottom = new Color(220, 238, 247);
    private final Color selectedBlueBorder = new Color(60, 127, 177);
    private final Color bgGray = new Color(240, 240, 240);
    private final Color bgGrayBottom = new Color(232, 232, 232);
    private final Color bgGrayBorder = new Color(137, 140, 149);
    private final Icon closeIcon = Icons.Cross_tiny.getIcon();
    private final Icon closeIconSelected = Icons.Cross_tiny_selected.getIcon();

    BiConsumer<Integer,Rectangle> rectangleBiConsumer;

    public StyledTabbedPaneUI(BiConsumer<Integer,Rectangle> rectangleBiConsumer) {
        this.rectangleBiConsumer = rectangleBiConsumer;
    }

    @Override
    protected Icon getIconForTab(int tabIndex) {
        Icon icon;
        if (tabIndex > 1) {
            int selectedIndex = tabPane.getSelectedIndex();
            boolean isSelected = selectedIndex == tabIndex;
            if (isSelected) {
                icon = closeIconSelected;
            } else {
                icon = closeIcon;
            }
        } else {
            icon = super.getIconForTab(tabIndex);
        }
        return icon;
    }

    @Override
    protected void paintContentBorderBottomEdge(Graphics g, int tabPlacement, int selectedIndex, int x, int y, int w, int h) {
        g.setColor(bgGrayBorder);
        g.drawLine(x, y - 1, x + w, y - 1);

        g.setColor(selectedBlueBorder);
        Rectangle r = selectedIndex < 0 ? null : getTabBounds(selectedIndex, calcRect);
        g.drawRect(r.x, r.y, r.width, r.height);
    }

    @Override
    protected void paintTab(Graphics g, int tab, Rectangle[] rects, int index, Rectangle iconRect, Rectangle textRect) {
        int selectedIndex = tabPane.getSelectedIndex();
        boolean isSelected = selectedIndex == index;
        Rectangle r = rects[index];
        int half = r.height / 2;
        if (isSelected) {
            g.setColor(selectedBlue);
            g.fillRect(r.x, r.y, r.width, half);
            g.setColor(selectedBlueBottom);
            g.fillRect(r.x, r.y + half, r.width, half + 1);
        } else {
            g.setColor(bgGray);
            g.fillRect(r.x, r.y, r.width, half);
            g.setColor(bgGrayBottom);
            g.fillRect(r.x, r.y + half, r.width, half + 1);
        }

        g.setColor(bgGrayBorder);
        g.drawRect(r.x, r.y, r.width, r.height + 1);

        String title = tabPane.getTitleAt(index);
        Font font = tabPane.getFont();
        FontMetrics metrics = SwingUtilities2.getFontMetrics(tabPane, g, font);
        Icon icon = getIconForTab(index);

        textRect.x = textRect.y = iconRect.x = iconRect.y = 0;

        SwingUtilities.layoutCompoundLabel(tabPane,
                metrics, title, icon,
                SwingUtilities.CENTER,
                SwingUtilities.CENTER,
                SwingUtilities.CENTER,
                SwingUtilities.LEADING,
                r,
                iconRect,
                textRect,
                textIconGap);

        int xNudge = getTabLabelShiftX(0, index, isSelected);
        int yNudge = getTabLabelShiftY(0, index, isSelected);
        iconRect.x += xNudge;
        iconRect.y += yNudge;
        textRect.x += xNudge;
        textRect.y += yNudge;

        String clippedTitle = SwingUtilities2.clipStringIfNecessary(null, metrics, title, textRect.width);

        this.paintText(g, 0, font, metrics, index, clippedTitle, textRect, isSelected);
        if (icon != null) {
            this.paintIcon(g, tab, index, icon, iconRect, isSelected);
            rectangleBiConsumer.accept(index, iconRect);
        }
    }

}
