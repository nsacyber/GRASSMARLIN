package core.ui.graph;

import core.fingerprint.FingerprintFilterType;
import core.fingerprint.FpPanel;
import javafx.application.Platform;
import javafx.scene.shape.Ellipse;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;

/**
 * 2007.06.14 - (CC) syncronized all methods touching nodePoints
 *
 */
public class RoundedTextNode extends DefaultNode {
    
    private String text;
    private String textLine2 = "";
    private Color fontColor;
    private boolean hasToolTip = false;
    private String toolTipText = "";
    
    public RoundedTextNode() {
        this(0,0);
    }
    
    public RoundedTextNode(int width, int height) {
        this(width, height, new Point(0,0));
    }
    
    public RoundedTextNode(int width, int height, Point topLeft) {
        this(width, height, topLeft, new String());
    }
    
    public RoundedTextNode(int width, int height, Point topLeft, String text) {
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
        if(!this.textLine2.isEmpty()) {
            g2d.drawString(this.textLine2 , this.nodePoints.get(NodePoint.topLeft).x + 5, this.nodePoints.get(NodePoint.topLeft).y + 35 );
        }
        super.maybePaintCloseButton(g2d);
    }

    public void setToolTipText(String text) { 
        this.hasToolTip = true;
        this.toolTipText = text;
    }

    public boolean mouseRightClicked(MouseEvent me) {
        final boolean[] returnValue = {false};
        if(isPointOverNode(me.getPoint())) {
            //being handled
            returnValue[0] = true;
            Arrays.asList(FingerprintFilterType.values()).stream().forEach(filterType -> {
                if (filterType.getName().equals(getText())) {
                    Platform.runLater(() -> {
                        String newValue = filterType.getValueFromUser();
                        //if new value is empty string then possibly cancel was selected
                        if(newValue != null && !newValue.trim().isEmpty()) {
                            setTextLine2(newValue);
                        }
                        GraphNodeContainer nodeContainer = getNodeContainer();
                        //this block of code scares me a little
                        while (!(nodeContainer instanceof FpPanel)) {
                            nodeContainer = ((DefaultNode)nodeContainer).getNodeContainer();
                        }
                        ((FpPanel)nodeContainer).repaint();
                    });
                }
            });
        }

        return returnValue[0];
    }
    
    @Override
    public boolean hasToolTip() { 
        return this.hasToolTip;
    }
    
    @Override
    public String getToolTipText() { 
        return this.toolTipText;
    }

	public String getText() {
		return text;
	}

    public String getTextLine2() {
        return textLine2;
    }

    public void setTextLine2(String textLine2) {
        this.textLine2 = textLine2;
    }
}
