/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.awt.Color;
import prefuse.Visualization;
import prefuse.action.assignment.ColorAction;
import prefuse.visual.VisualItem;

/**
 *
 * @author BESTDOG
 */
public class FontColorAction extends ColorAction {

    final int RED;
    final int BLACK;
    
    public FontColorAction(String group, String field, int color) {
        super(group, field, color);
        RED = Color.RED.getRGB();
        BLACK = Color.BLACK.getRGB();
    }
    
    @Override
    public void process(VisualItem item, double frac) {
        int color;
        if( item.isHover() || item.isInGroup(Visualization.FOCUS_ITEMS) || item.isInGroup(Visualization.SEARCH_ITEMS) ) {
            color = RED;
        } else {
            color = BLACK;
        }
        item.setInt(m_startField, color);
        item.setInt(m_endField, color);
        item.setInt(m_colorField, color);
    }
    
}
