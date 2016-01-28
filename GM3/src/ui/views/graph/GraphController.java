/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import core.Core;
import core.types.LogEmitter;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
import prefuse.visual.VisualItem;

/**
 *
 * @author BESTDOG
 * @param <T> The type of visualization for this Graph Controller
 */
public interface GraphController<T extends Visualization> {

    public static final double ZOOM_SCALE_MIN = 0.001; // zoom magic
    public static final double ZOOM_SCALE_MAX = 75.0; // zoom magic
    public static final int FIT_DURATION = 480; // time it takes to do the "fit" animation
    
    Display getDisplay();
    
    String getGroup();
    
    default T getViz() {
        return (T) getDisplay().getVisualization();
    }
    
    default void fit(java.awt.event.ActionEvent evt) {
        if (!getDisplay().isTranformInProgress()) {
            Rectangle2D bounds = getViz().getBounds(getGroup());
            GraphicsLib.expand(bounds, (1 / getDisplay().getScale()));
            DisplayLib.fitViewToBounds(getDisplay(), bounds, FIT_DURATION);
        }
    }
    
    /**
     * Zoom in by 25%, param is nullable
     * @param evt ignored
     */
    default void zoomIn(java.awt.event.ActionEvent evt) {
        zoom(getDisplay(), 1.25, true);
    }
    
    /**
     * Zoom out by 25%, param is nullable
     * @param evt ignored
     */
    default void zoomOut(java.awt.event.ActionEvent evt) {
        zoom(getDisplay(), .75, false);
    }
 
    /**
     * @return Gets the first focus item of this visualization
     */
    default VisualItem getFocusItem() {
        VisualItem item = null;
        try {
            item = (VisualItem) this.getDisplay()
                    .getVisualization()
                    .getFocusGroup(Visualization.FOCUS_ITEMS)
                    .tuples()
                    .next();
        } catch (Exception ex) {
        }
        return item;
    }
    
    /**
     * @param display display to zoom onto.
     * @param zoom Zoom amount 1.00 = no zoom. 1.25 = increased zoom 25%
     * @param abs true if abs zoom.
     */
    default void zoom(Display display, double zoom, boolean abs) {
        if (display.isTranformInProgress()) {
            return;
        }

        double px;
        double py;

        VisualItem i = getFocusItem();
        if (i != null) {
            px = i.getX();
            py = i.getY();
        } else {
            px = display.getX() + display.getWidth() / 2;
            py = display.getY() + display.getHeight() / 2;
        }

        Point.Double p = new Point.Double(px, py);
        double scale = display.getScale();
        double result = scale * zoom;

        if (result < ZOOM_SCALE_MIN) {
            zoom = ZOOM_SCALE_MIN / scale;
        } else if (result > ZOOM_SCALE_MAX) {
            zoom = ZOOM_SCALE_MAX / scale;
        }

        if (abs) {
            display.zoomAbs(p, zoom);
        } else {
            display.zoom(p, zoom);
        }
        display.repaint();
    }
    
    /**
     * Writes the graph png to the provided File.
     * @param file Non-existing File.
     */
    default void exportImage(File file) {
        try ( FileOutputStream fos = new FileOutputStream(file) ) {
            getDisplay().saveImage(fos, "PNG", 1.0);
        } catch( Exception ex ) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            danger(ex.toString());
        }
    }
    
    default void danger(String dangerText) {
        LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, dangerText);
    }
    
}
