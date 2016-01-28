package ui.custom;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JProgressBar;

/**
 * <pre>
 * A progress bar to feature of a "100%" percent display and a flat-like progress bar.
 * </pre>
 */
public class FlatProgressBar extends JProgressBar {

    static final Color COLOR_PRIMARY = new Color(0, 161, 231);
    static final Color COLOR_ALT = new Color(54, 82, 120);
    private final Color COLOR_BORDER = new Color(0xDEDCDF);
    boolean highlighted, showOnActivity;
    String displayText;
    private final boolean showBorder;

    public FlatProgressBar() {
        this(false);
    }

    public FlatProgressBar(boolean showBorder) {
        highlighted = false;
        displayText = null;
        showOnActivity = false;
        this.showBorder = showBorder;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
        this.setString(displayText);
        this.setStringPainted(true);
    }

    public void setHighlight(boolean b) {
        highlighted = b;
        if (b) {
            this.setBackground(COLOR_ALT);
        } else {
            this.setBackground(COLOR_PRIMARY);
        }
    }

    /**
     * @param showOnActivity True to only show component when progress is changing, ie, not 0 or 100%.
     */
    public void setOnlyShowOnActivity(boolean showOnActivity) {
        this.showOnActivity = showOnActivity;
    }

    @Override
    public void paint(Graphics g) {
        Graphics componentGraphics = getComponentGraphics(g);
        Graphics co = componentGraphics.create();
        Rectangle clipRect = co.getClipBounds();
        int clipX;
        int clipY;
        int clipW;
        int clipH;
        if (clipRect == null) {
            clipX = clipY = 0;
            clipW = getWidth();
            clipH = getHeight();
        } else {
            clipX = clipRect.x;
            clipY = clipRect.y;
            clipW = clipRect.width;
            clipH = clipRect.height;
        }

        if (clipW > getWidth()) {
            clipW = getWidth();
        }
        if (clipH > getHeight()) {
            clipH = getHeight();
        }

        if (isVisible()) {

            Double percent = this.getPercentComplete();
            Double fraction = percent * 100;
            int intPercent = fraction.intValue();
            Double threshold = percent * clipW;

            g.setColor(highlighted ? Color.LIGHT_GRAY : Color.WHITE);
            g.fillRect(clipX, clipY, clipW, clipH);
            g.setColor(highlighted ? COLOR_ALT : COLOR_PRIMARY);
            g.fillRect(clipX, clipY, threshold.intValue(), clipH);

            if( this.showBorder ) {
                g.setColor(COLOR_BORDER);
                g.drawRect(clipX, clipY, clipW-1, clipH-1);
            }

            g.setColor(highlighted ? Color.WHITE : Color.BLACK);
            String text;
            
            if (displayText == null) {
                text = intPercent + "%";
            } else {
                text = String.format("%s  %d%%", displayText, intPercent);
            }
            
            int textWidth = g.getFontMetrics(getFont()).stringWidth(text);
            int startX = clipW / 2 - ( textWidth / 2 );
            int startY = clipY + 12;
            g.setFont(getFont());
            g.drawString(text, startX, startY);
            
        } else {
            g.clearRect(clipX, clipY, clipW, clipH);
        }

        co.dispose();
    }
    
    @Override
    public boolean isVisible() {
        boolean visible = super.isVisible();
        int val = getValue();
        if( visible && showOnActivity && (val == 0 || val == 100)  ) {
            visible = false;
            this.fireStateChanged();
        }
        return visible;
    }

    @Override
    public int getMaximum() {
        return 100;
    }

    @Override
    public int getMinimum() {
        return 0;
    }

}
