package core.ui.graph;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * 
 *  2007.06.14 - (CC) syncronized all methods touching nodePoints
 */
public class NodeToolTip extends DefaultNode {
    public static final Color DEFAULT_COLOR = new Color(250, 246, 170);
    public static final Color DEFAULT_FONT_COLOR = Color.BLACK;
    private String text = "";
    private Color fontColor = DEFAULT_FONT_COLOR;
    private boolean textIsSet = false;
    
    public NodeToolTip() {
        super();
        setBackgroundColor(DEFAULT_COLOR);
        setBorder(2);
    }
    
    public void setToolTipText(String text) { 
        this.text = text;
        this.textIsSet = true;
    }
    
    public void setFontColor(Color fontColor) {
        this.fontColor = fontColor;
    }
    
    public synchronized void paintNode(Graphics2D g2d) {
        findSizeFromText();
        super.paintNode(g2d);
        if(this.textIsSet) {
            g2d.setFont(new Font("Times New Roman", Font.BOLD, 14));
            g2d.setPaint(this.fontColor);
            int i = 1;
            BufferedReader reader = new BufferedReader(new StringReader(this.text));
            try { 
                for (String line = reader.readLine(); line != null; line = reader.readLine()) { 
                    g2d.drawString(line , this.nodePoints.get(NodePoint.topLeft).x + 5, this.nodePoints.get(NodePoint.topLeft).y + (i++ * 22) );
                }
            } catch (IOException e) { 
                throw new Error("Unable to read String!", e);
            }
        }
    }

    private void findSizeFromText () {
        if(this.textIsSet) {
            int longestStringLength = 1;
            int lineCount = 0;
            BufferedReader reader = new BufferedReader(new StringReader(this.text));
            try { 
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    lineCount++;
                    if (line.length() > longestStringLength) { 
                        longestStringLength = line.length();
                    }
                }
            } catch (IOException e) { 
                throw new Error("Unable to read String!", e);
            }
            setWidth((longestStringLength * 10) + 10);            
            setHeight(lineCount * 25);
        }
        
    }
}
