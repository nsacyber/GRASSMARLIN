package ui.views.graph.logical;

import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.tuple.TupleSet;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by BESTDOG on 12/7/2015.
 *
 * A force directed layout which obeys the visibility constraints of the Grassmarlin logical view.
 */
public class TreeForceLayout extends ForceDirectedLayout {

    Supplier<Stream<VisualItem>> nodeStream;
    Supplier<Stream<EdgeItem>> edgeStream;

    public TreeForceLayout(String graph, Supplier<Stream<VisualItem>> nodeStream, Supplier<Stream<EdgeItem>> edgeStream) {
        super(graph);
        this.nodeStream = nodeStream;
        this.edgeStream = edgeStream;
    }

    @Override
    protected float getMassValue(VisualItem item) {
        Tuple t = item.getSourceTuple();
        float mass;
        if( t instanceof Node ) {
            mass = (float)((Node) t).getDepth() + 1.0f;
        } else {
            mass = 1.0f;
        }
        return mass;
    }

    @Override
    protected float getSpringLength(EdgeItem e) {
        float length;
        if( e.getSourceItem().getRow() == 0 || e.getTargetItem().getRow() == 0 ) {
            length = -4.0f;
        } else {
            length = -2.0f;
        }
        return length;
    }

    @Override
    protected float getSpringCoefficient(EdgeItem e) {
        float spring;
        if( e.getSourceItem().getRow() == 0 || e.getTargetItem().getRow() == 0 ) {
            spring = -4.0f;
        } else {
            spring = -2.0f;
        }
        return spring;
    }

    protected void initSimulator(ForceSimulator fsim) {
        // make sure we have force items to work with
        TupleSet ts = m_vis.getGroup(m_nodeGroup);
        if ( ts == null ) return;
        try {
            ts.addColumns(FORCEITEM_SCHEMA);
        } catch ( IllegalArgumentException iae ) { /* ignored */ }

        float startX = (referrer == null ? 0f : (float)referrer.getX());
        float startY = (referrer == null ? 0f : (float)referrer.getY());
        final float x = Float.isNaN(startX) ? 0f : startX;
        final float y = Float.isNaN(startY) ? 0f : startY;

        nodeStream.get().forEach( item -> {
            ForceItem fitem = (ForceItem) item.get(FORCEITEM);
            fitem.mass = getMassValue(item);
            double item_x = item.getEndX();
            double item_y = item.getEndY();
            fitem.location[0] = (Double.isNaN(x) ? x : (float) item_x);
            fitem.location[1] = (Double.isNaN(y) ? y : (float) item_y);
            fsim.addItem(fitem);
        });

        edgeStream.get().forEach(e -> {
            NodeItem n1 = e.getSourceItem();
            ForceItem f1 = (ForceItem) n1.get(FORCEITEM);
            NodeItem n2 = e.getTargetItem();
            ForceItem f2 = (ForceItem) n2.get(FORCEITEM);
            float coeff = getSpringCoefficient(e);
            float slen = getSpringLength(e);
            fsim.addSpring(f1, f2, (coeff >= 0 ? coeff : -1.f), (slen >= 0 ? slen : -1.f));
        });
    }

}
