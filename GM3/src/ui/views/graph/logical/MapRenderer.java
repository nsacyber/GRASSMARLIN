/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.awt.Image;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;
import prefuse.data.Tuple;
import prefuse.render.LabelRenderer;
import prefuse.visual.VisualItem;
import ui.icon.Icons;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 */
public class MapRenderer extends LabelRenderer implements MapAction {
    
    final Function<Tuple,VisualNode> mapFunction;
    
    public MapRenderer(Function<Tuple,VisualNode> mapFunction) {
        this.mapFunction = mapFunction;
    }
    
    @Override
    protected String getText(VisualItem item) {
        return getText( get(item) );
    }
    
    private String getText(VisualNode node) {
        String text = "";
        if( node.isHost() ) {
            text = node.getName();
        } else if( node.isNetwork() ) {
            text = node.getName().concat("\n").concat(Integer.toString(node.getChildCount()));
        }
        return text;
    }
    
    @Override
    protected Image getImage(VisualItem item) {
        return getImage( get(item) );
    }
    
    private Image getImage(VisualNode node) {
        Image image = null;
        if( node.isHost() || node.isNetwork() ) {
            image = node.getDetails().image.getDefault();
        } else if( node.isRoot() ) {
            image = Icons.Grassmarlin.get();
        }
        return image;
    }
    
    @Override
    public VisualNode get(Tuple tuple) {
        return mapFunction.apply(tuple);
    }

}
