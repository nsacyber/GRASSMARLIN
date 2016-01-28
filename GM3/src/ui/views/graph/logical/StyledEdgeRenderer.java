package ui.views.graph.logical;

import prefuse.Constants;
import prefuse.data.Tuple;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import ui.views.tree.visualnode.VisualNode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.function.Function;

/**
 * Created by BESTDOG on 12/7/2015.
 *
 * Edge renderer with has a toggleable feature set.
 */
public class StyledEdgeRenderer extends ForwardingEdgeRenderer implements MapAction {

    private final double MAX_EDGE_WIDTH = 5.0;

    private boolean visible;
    private final Function<Tuple, VisualNode> mapFunction;
    private Function<VisualItem,Double> widthRenderFunction;

    private int max;
    private int maxHash;

    public StyledEdgeRenderer(Function<Tuple, VisualNode> mapFunction) {
        super();
        this.max = 1;
        this.maxHash = -1;
        this.mapFunction = mapFunction;
        this.setRatioVisible(false);
        this.setCurveVisible(false);
    }

    public boolean isRatioVisible() {
        return visible;
    }

    /**
     * @param visible If true the weighted ratio of the line with my visualized as the width of the edge between two hosts.
     */
    public void setRatioVisible(boolean visible) {
        this.visible = visible;
        if( this.visible ) {
            this.widthRenderFunction = this::calculateGetLineWidth;
        } else {
            this.widthRenderFunction = this::defaultGetLineWidth;
        }
    }

    private int getUpdatedMax(int degree, VisualNode node) {
        if( degree >= this.max ) {
            this.max = degree;
            this.maxHash = node.getAddressHash();
        } else if(node.getAddressHash() == this.maxHash) {
            this.max = 1;
        }
        return this.max;
    }

    private Double calculateGetLineWidth(VisualItem item) {
        VisualNode node = get(item);
        double width;
        if( node.isHost() ) {

            int degree = node.getData().in + node.getData().out;
            double degree_d = (double)degree;
            double max = this.getUpdatedMax(degree, node);
            double ratio = degree_d / max * MAX_EDGE_WIDTH;

            ratio = Math.min(ratio, MAX_EDGE_WIDTH);
            width = ratio;

        } else {
            width = super.getLineWidth(item);
        }
        return width;
    }

    private Double defaultGetLineWidth(VisualItem item) {
        return super.getLineWidth(item);
    }

    /**
     * SHOULD be invoked in on a prefuse thread.
     * @param curve Curved lines will display if true, else false and strait.
     */
    public void setCurveVisible(boolean curve) {
        if( curve ) {
            this.setEdgeType(Constants.EDGE_TYPE_CURVE);
        } else {
            this.setEdgeType(Constants.EDGE_TYPE_LINE);
        }
    }

    /**
     * @return True if this renderer is showing curved edges, else false and strait.
     */
    public boolean isCurveVisible() {
        return Constants.EDGE_TYPE_CURVE == this.getEdgeType();
    }

    @Override
    public Shape getRawShape(VisualItem item) {
        Object shape;
        EdgeItem edge;
        VisualItem item1;
        VisualItem item2;

        edge = (EdgeItem)item;
        item1 = this.getSource(edge);
        item2 = this.getTarget(edge);

        if(super.shouldSkip(item1, item2)) {
            shape = null;
        } else {
            getAlignedPoint(this.m_tmpPoints[0], item1.getBounds(), this.m_xAlign1, this.m_yAlign1);
            getAlignedPoint(this.m_tmpPoints[1], item2.getBounds(), this.m_xAlign2, this.m_yAlign2);
            this.m_curWidth = (float)this.getLineWidth(item1);

            Point2D start = this.m_tmpPoints[0];
            Point2D end = this.m_tmpPoints[1];

            double n1x1 = start.getX();
            double n1y1 = start.getY();
            double n2x1 = end.getX();
            double n2y1 = end.getY();

            if( this.m_edgeType == Constants.EDGE_TYPE_LINE ) {
                this.m_line.setLine(n1x1, n1y1, n2x1, n2y1);
                shape = this.m_line;
            } else {
                this.getCurveControlPoints(edge, this.m_ctrlPoints, n1x1, n1y1, n2x1, n2y1);
                this.m_cubic.setCurve(
                        n1x1,
                        n1y1,
                        this.m_ctrlPoints[0].getX(),
                        this.m_ctrlPoints[0].getY(),
                        this.m_ctrlPoints[1].getX(),
                        this.m_ctrlPoints[1].getY(),
                        n2x1,
                        n2y1
                );
                shape = this.m_cubic;
            }
        }
        return (Shape) shape;
    }

    public void render(Graphics2D g, VisualItem item) {
        Shape shape = this.getShape(item);
        if(shape != null) {
            this.drawShape(g, item, shape);
        }
    }

    @Override
    protected double getLineWidth(VisualItem item) {
        return this.widthRenderFunction.apply(item);
    }

    @Override
    public VisualNode get(Tuple tuple) {
        return mapFunction.apply(tuple);
    }

    @Override
    public void clear() {
        super.clear();
        this.max = 1;
        this.maxHash = -1;
    }
}
