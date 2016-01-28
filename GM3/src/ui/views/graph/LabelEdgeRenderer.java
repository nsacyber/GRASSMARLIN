/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import prefuse.render.EdgeRenderer;
import prefuse.util.FontLib;
import prefuse.util.GraphicsLib;
import prefuse.visual.VisualItem;

/**
 *
 */
public class LabelEdgeRenderer extends EdgeRenderer {
    float centerX, centerY;
    final Font font = FontLib.getFont("Tahoma", Font.BOLD, 20);
    final String textField;
    
    public LabelEdgeRenderer(String textField) {
        this.textField = textField;
    }
    
    public LabelEdgeRenderer setLineWidth(double w) {
        super.setDefaultLineWidth(w);
        return this;
    }
    
    Font getFont() {
        return font;
    }

    public Color getFontColor() {
        return Color.BLACK;
    }
    
    @Override
    protected void drawShape(Graphics2D g, VisualItem item, Shape shape) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GraphicsLib.paint(g, item, shape, getStroke(item), getRenderType(item));
        String text = item.getString(textField);
        if( !text.isEmpty() ) {
            centerX = (float)(m_line.getX1() + m_line.getX2()) / 2;
            centerY = (float)(m_line.getY1() + m_line.getY2()) / 2;
            centerX -= g.getFontMetrics(getFont()).stringWidth(text) / 2;
            Font lastFont = g.getFont();
            Color lastcolor = g.getColor();
            g.setFont(getFont());
            g.setColor(getFontColor());
            g.drawString(text, centerX, centerY);
            g.setFont(lastFont);
            g.setColor(lastcolor);
        }
    }

}
