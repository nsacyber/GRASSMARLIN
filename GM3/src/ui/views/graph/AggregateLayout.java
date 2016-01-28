/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import prefuse.util.GraphicsLib;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualItem;
import static prefuse.visual.VisualItem.POLYGON;

/**
 * Adapted from Jeffrey Heer's AggregateLayout Note, its not safe to use
 * iterators here, its part of the user-mode action, which always draws even
 * during update
 */
public class AggregateLayout extends NoOverlapActionList {

    private final int a_margin = 5;
    private double[] m_pts;
    private final String m_group;
    
    public AggregateLayout(AtomicBoolean lock, String group) {
        super(lock);
        m_group = group;
    }

    public AggregateLayout(String group) {
        this(new AtomicBoolean(false), group);
    }
    
    @Override
    public void runIfSafe(double frac) {
        AggregateTable aggr = (AggregateTable) m_vis.getGroup(m_group);
        if (aggr == null) {
            return;
        }
        int num = aggr.getTupleCount();
        if (num == 0) {
            return;
        }

        int maxsz = 0;
        for (int i = 0; i < num; i++) {
            AggregateItem item = (AggregateItem) aggr.getItem(i);
            maxsz = Math.max(maxsz, 4 * 2 * item.getAggregateSize());
        }

        if (m_pts == null || maxsz > m_pts.length) {
            m_pts = new double[maxsz];
        }

        for (int i = 0; i < num; i++) {
            AggregateItem item = (AggregateItem) aggr.getItem(i);
            
            if (item.getAggregateSize() == 0) {
                item.setVisible(false);
                continue;
            }

            VisualItem vi;
            int idx = 0;

            for (Iterator iter = item.items(); iter.hasNext();) {
                vi = (VisualItem) iter.next();
                if (vi.isVisible()) {
                    addPoint(m_pts, idx, vi, a_margin);
                    idx += 2 * 4;
                }
            }

            if (idx == 0) {
                item.setVisible(false);
                continue; // no visible items in aggregate, so we don't render it
            } else {
                item.setVisible(true);
            }
            
            double[] nhull = GraphicsLib.convexHull(m_pts, idx);

            float[] fhull = (float[]) item.get(VisualItem.POLYGON);

            if (fhull == null || fhull.length < nhull.length) {
                fhull = new float[nhull.length];
            } else if (fhull.length > nhull.length) {
                fhull[nhull.length] = Float.NaN;
            }

            for (int j = 0; j < nhull.length; j++) {
                fhull[j] = (float) nhull[j];
            }

            item.set(POLYGON, fhull);
            item.setValidated(false);
        }

    }

    private void addPoint(double[] pts, int idx, VisualItem item, int margin) {
        Rectangle2D b = item.getBounds();
        double minX = b.getMinX() - margin;
        double maxX = b.getMaxX() + margin;
        double minY = b.getMinY() - margin;
        double maxY = b.getMaxY() + margin;

        pts[idx] = minX;
        pts[idx + 2] = minX;
        pts[idx + 4] = maxX;
        pts[idx + 6] = maxX;

        pts[idx + 1] = minY;
        pts[idx + 3] = maxY;
        pts[idx + 5] = minY;
        pts[idx + 7] = maxY;
    }
}
