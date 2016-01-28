/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.function.Function;
import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.data.Tuple;
import prefuse.visual.VisualItem;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 */
public class MapDragControl extends ControlAdapter implements MapAction {
    private final Cursor INTERACTIVE_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private final Function<Tuple, VisualNode> mapFunction;

    private Point2D down = new Point2D.Double();
    private Point2D temp = new Point2D.Double();
    private VisualItem selectedItem; // nullable
    private boolean dragged;
    
    public MapDragControl( Function<Tuple, VisualNode> mapFunction ) {
        this.mapFunction = mapFunction;
    }
    
    
    /**
     * Sets the mouse cursor to the cursor provided.
     * @param e MouseEvent that contains the component to set the cursor on.
     * @param interactive True if the {@link MapDragControl#INTERACTIVE_CURSOR } should be used, else {@link MapDragControl#DEFAULT_CURSOR }.
     */
    private void setCursor(final MouseEvent e, final boolean interactive) {
        Object obj = e.getSource();
        if( obj instanceof Display ) {
            ((Display)e.getSource()).setCursor(interactive ? INTERACTIVE_CURSOR : DEFAULT_CURSOR);
        }
    }
    
    @Override
    public VisualNode get(Tuple tuple) {
        return mapFunction.apply(tuple);
    }
    
    
}
