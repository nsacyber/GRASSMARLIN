package core.ui.graph;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by CC on 7/7/2015.
 */
public class RoundedButtonNode extends DefaultNode implements Clickable {
    private String text;
    private Color fontColor;
    private Collection<ClickListener> listeners = new ArrayList<>();
    private boolean hasToolTip = false;
    private String toolTipText = "";

    public RoundedButtonNode(int width, int height, Point topLeft, String text) {
        super(width, height, topLeft);
        this.text = text;
        this.fontColor = Color.WHITE;
    }
    public void setText(String text) {
        this.text = text;
    }

    public void addClickListener(ClickListener listener) {
        this.listeners.add(listener);
    }

    public void setFontColor(Color color) {
        this.fontColor = color;
    }

    @Override
    public synchronized void paintNode(Graphics2D g2d) {
        Shape r = new RoundRectangle2D.Float( this.nodePoints.get(NodePoint.topLeft).x, this.nodePoints.get(NodePoint.topLeft).y, this.width, this.height, 20, 20);
        if(this.useBoarder) {
            g2d.setStroke(new BasicStroke(this.boarderSize));
            g2d.setPaint(this.boarderColor);
            g2d.draw(r);
        }
        if(this.useGradient) {
            g2d.setPaint(new GradientPaint(this.nodePoints.get(NodePoint.topLeft).x,0,this.color, this.nodePoints.get(NodePoint.topRight).x, 0, this.gradColor, false));
        }
        else {
            g2d.setPaint(this.color);
        }

        g2d.fill(r);

        g2d.setFont(new Font("Times New Roman", Font.BOLD, 14));
        g2d.setPaint(this.fontColor);
        g2d.drawString(this.text , this.nodePoints.get(NodePoint.topLeft).x + 5, this.nodePoints.get(NodePoint.topLeft).y + 20 );
    }

    public void onClick() {
        this.listeners.stream().forEach(listener -> listener.performAction());
    }

    public void setToolTipText(String text) {
        this.hasToolTip = true;
        this.toolTipText = text;
    }

    @Override
    public boolean hasToolTip() {
        return this.hasToolTip;
    }

    @Override
    public String getToolTipText() {
        return this.toolTipText;
    }

}
