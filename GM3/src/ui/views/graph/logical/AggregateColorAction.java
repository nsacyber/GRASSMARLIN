/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.awt.Color;
import static prefuse.Constants.NOMINAL;
import prefuse.action.assignment.DataColorAction;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import static prefuse.visual.VisualItem.FILLCOLOR;

/**
 *
 * @author BESTDOG
 */
public class AggregateColorAction extends DataColorAction {

    int lim;
    int count;
    int[] palette, darkPalette;
    
    public AggregateColorAction(String group, String field, int[] palette) {
        super(group, field, NOMINAL, FILLCOLOR, palette);
        this.palette = palette;
        this.lim = palette.length;
        this.darkPalette = new int[this.palette.length];
        
        for( int i = 0; i < this.palette.length; ++i ) {
            this.darkPalette[i] = ColorLib.setAlpha(this.palette[i], 120);
        }
        
    }
    
    @Override
    public int getColor(VisualItem item) {
        int color;
        int index;
        String field = getDataField();
        index = item.getInt(field);
        if (index == -1) {
            color = nextIndex();
            item.setInt(field, color);
        } else {
            if( item.isHover() ) {
                color = getDark( index );
            } else {
                color = get( index );
            }
        }
        return color;
    }
    
    private int get( int index ) {
        return this.palette[index];
    }
    
    private int getDark(int index) {
        return this.darkPalette[index];
    }
    
    private int nextIndex() {
        return count++ % lim;
    }

    
}
