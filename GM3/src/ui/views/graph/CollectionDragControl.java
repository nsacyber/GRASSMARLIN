/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.swing.SwingUtilities;
import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.data.Node;
import prefuse.visual.VisualItem;

/**
 * A drag control for moving groups of visual items together based on a collection
 * that determines their grouping instead of the actual graph.
 * 
 * If none group items are dragged they will be dragged alone.
 */
class CollectionDragControl extends ControlAdapter {
    final static Cursor INTERACTIVE_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    final static Cursor DEFAULT_CURSOR = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    final Map<Node,List<Node>> groups;
    final Function<Node,VisualItem> adapter;
    final Function<VisualItem,VisualItem> aggAdapter;
    
    Point2D down = new Point2D.Double();
    Point2D temp = new Point2D.Double();
    VisualItem selectedItem; // nullable
    boolean dragged;
    
    /**
     * Creates a drag control with a copy of the provided Map. 
     * @param groups Map to create the initial control, may be null.
     */
    public CollectionDragControl(Map<Node,List<Node>> groups, Function<Node,VisualItem> adapter, Function<VisualItem,VisualItem> aggAdapter) {
        this.groups = Collections.unmodifiableMap( groups );
        this.aggAdapter = aggAdapter;
        this.adapter = adapter;
        dragged = false;
        selectedItem = null;
    }
    
    /**
     * Sets the mouse cursor to the cursor provided.
     * @param e MouseEvent that contains the component to set the cursor on.
     * @param interactive True if the {@link CollectionDragControl#INTERACTIVE_CURSOR } should be used, else {@link CollectionDragControl#DEFAULT_CURSOR }.
     */
    private void setCursor(final MouseEvent e, final boolean interactive) {
        Object obj = e.getSource();
        if( obj instanceof Display ) {
            ((Display)e.getSource()).setCursor(interactive ? INTERACTIVE_CURSOR : DEFAULT_CURSOR);
        }
    }
    
    private void setSelection(VisualItem item) {
        this.selectedItem = aggAdapter.apply(item);
    }

    /**
     * Sets all items in the group of the provided item to be fixed by the provided value.
     * @param item Item for which its entire group will call {@link VisualItem#setFixed(boolean) } method on.
     * @param fixed Value to pass to setFixed method.
     */
    void setFixed(VisualItem item, boolean fixed) {
        eachItem(item, i -> i.setFixed(fixed));
    }
    
    void eachItem(VisualItem item, Consumer<VisualItem> cb) {
        eachItem((Node) item.getSourceTuple(), cb);
    }
    
    void eachItem(Node item, Consumer<VisualItem> cb) {
        for( Entry<Node,List<Node>> e : groups.entrySet() ) {
            if( e.getKey().equals(item) || e.getValue().contains(item) ) {
                Stream.concat(
                    Stream.of(adapter.apply(e.getKey())),
                    e.getValue().stream().map(adapter)
                ).forEach(cb);
                break;
            }
        }
    } 
    
    private boolean isGroupItem(VisualItem item) {
        return item.isInteractive() && item.getSourceTuple() instanceof Node;
    }
    
    private void setDragged(boolean dragged) {
        this.dragged = dragged;
    }

    private void copyCoordinate(MouseEvent e, Point2D point) {
        ((Display)e.getSource()).getAbsoluteCoordinate(e.getPoint(), point);
    }
    
    private void moveSingle(VisualItem item, double dx, double dy) {
        if( !item.isVisible() ) {
            return;
        }
        double x = item.getX();
        double y = item.getY();
        item.setStartX(x);
        item.setStartY(y);
        item.setX(x+dx);
        item.setY(y+dy);
        item.setEndX(x+dx);
        item.setEndY(y+dy);
    }
    
    private void moveAll(VisualItem visualItem, double dx, double dy) {
        eachItem(visualItem, item -> moveSingle(item, dx, dy) );
    }
    
    private boolean isWithinGroup(VisualItem item) {
        Node node = (Node) item.getSourceTuple();
        return this.groups.containsKey(node) || this.groups.values().stream().anyMatch(group -> group.contains(node));
    }
    
    @Override
    public void itemEntered(VisualItem item, MouseEvent e) {
        item = aggAdapter.apply(item);
        if( isGroupItem(item) ) {
            setCursor(e, true);
            setSelection(item);
            setFixed(item, true);
        }
    }
    
    @Override
    public void itemExited(VisualItem item, MouseEvent e) {
        item = aggAdapter.apply(item);
        if( isGroupItem(item) ) {
            if( item.equals(selectedItem) ) {
                setSelection(null);
                setFixed(item, false);
            }
            setCursor(e, false);
        }
    }
    
    @Override
    public void itemPressed(VisualItem item, MouseEvent e) {
        if(!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        item = aggAdapter.apply(item);
        if( isGroupItem(item) ) {
            setDragged(false);
            copyCoordinate(e, down);
            setFixed(item, true);
        }
    }
    
    @Override
    public void itemReleased(VisualItem item, MouseEvent e) {
        if(!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        if( dragged ) {
            setSelection(null);
            setFixed(aggAdapter.apply(item), false);
            setDragged(false);
        }
    }
    
    @Override
    public void itemDragged(VisualItem item, MouseEvent e) {
        if(!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        item = aggAdapter.apply(item);
        if( isGroupItem(item) ) {
            setDragged(true);
            copyCoordinate(e, temp);
            double dx = temp.getX()-down.getX();
            double dy = temp.getY()-down.getY();
            if( isWithinGroup(item) ) {
                moveAll(item, dx, dy);
            } else {
                moveSingle(item, dx, dy);
            }
            down.setLocation(temp);
        }
    }

}
