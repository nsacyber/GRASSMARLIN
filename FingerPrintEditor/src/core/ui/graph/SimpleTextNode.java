package core.ui.graph;

import java.awt.*;

/**
 * Created by CC on 7/7/2015.
 */
public class SimpleTextNode extends DefaultNode {

    private String text;
    private Color fontColor;

    public SimpleTextNode() {
        this(0,0);
    }

    public SimpleTextNode(int width, int height) {
        this(width, height, new Point(0,0));
    }

    public SimpleTextNode(int width, int height, Point topLeft) {
        this(width, height, topLeft, new String());
    }

    public SimpleTextNode(int width, int height, Point topLeft, String text) {
        super(width, height, topLeft);
        this.text = text;
        this.fontColor = Color.WHITE;
        //System.out.println("New text node is born: " + text + " " + width + " " + height + " " + topLeft.toString());
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setFontColor(Color color) {
        this.fontColor = color;
    }

    @Override
    public synchronized void paintNode(Graphics2D g2d) {
        g2d.setFont(new Font("Times New Roman", Font.BOLD, 14));
        g2d.setPaint(this.fontColor);
        g2d.drawString(this.text, this.nodePoints.get(NodePoint.topLeft).x + 5, this.nodePoints.get(NodePoint.topLeft).y + 20);
    }

    public String getText() {
        return text;
    }
}
