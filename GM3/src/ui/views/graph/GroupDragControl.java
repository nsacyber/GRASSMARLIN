/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.views.graph;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;
import javax.swing.SwingUtilities;
import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.visual.AggregateItem;
import prefuse.visual.VisualItem;

/**
 *
 */
public class GroupDragControl extends ControlAdapter {
    final static Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    final static Cursor cursorDefault = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    Point2D down = new Point2D.Double();
    Point2D temp = new Point2D.Double();
    VisualItem active;
    Boolean dragged;
    
    public GroupDragControl() {
        dragged = false;
    }

    @Override
    public void itemEntered(VisualItem item, MouseEvent e) {
        if( item.isInteractive() ) {
            ((Display)e.getSource()).setCursor(cursor);
            active = item;
            setFixed(item,true);
        }
    }
    
    @Override
    public void itemExited(VisualItem item, MouseEvent e) {
        if( item.isInteractive() ) {
            if( item.equals(active) ) {
                active = null;
                setFixed(item,false);
            }
            ((Display)e.getSource()).setCursor(cursorDefault);
        }
    }

    @Override
    public void itemPressed(VisualItem item, MouseEvent e) {
        if(!SwingUtilities.isLeftMouseButton(e)) return;
        dragged = false;
        ((Display)e.getSource()).getAbsoluteCoordinate(e.getPoint(), down);
        if( !(item instanceof AggregateItem) )
            setFixed(item,true);
    }
    
    @Override
    public void itemReleased(VisualItem item, MouseEvent e) {
        if(!SwingUtilities.isLeftMouseButton(e)) return;
        if( dragged ) {
            active = null;
            setFixed(item,false);
            dragged = false;
        }
    }
    
    @Override
    public void itemDragged(VisualItem item, MouseEvent e) {
        if(!SwingUtilities.isLeftMouseButton(e)) return;
        dragged = true;
        ((Display)e.getSource()).getAbsoluteCoordinate(e.getPoint(), temp);
        double dx = temp.getX()-down.getX();
        double dy = temp.getY()-down.getY();
        move(item,dx,dy);
        down.setLocation(temp);
    }
    
    void move(VisualItem item, double dx, double dy) {
        if( item instanceof AggregateItem ) {
            Iterator items = ((AggregateItem)item).items();
            while( items.hasNext() )
                move((VisualItem)items.next(), dx, dy);
        } else {
            double x = item.getX();
            double y = item.getY();
            item.setStartX(x);
            item.setStartY(y);
            item.setX(x+dx);
            item.setY(y+dy);
            item.setEndX(x+dx);
            item.setEndY(y+dy);
        }
    }
    
    void setFixed(VisualItem item, Boolean b) {
        if (item instanceof AggregateItem) {
            Iterator items = ((AggregateItem) item).items();
            while (items.hasNext()) 
                setFixed((VisualItem) items.next(), b);
        } else {
            item.setFixed(b);
        }
    }
}
