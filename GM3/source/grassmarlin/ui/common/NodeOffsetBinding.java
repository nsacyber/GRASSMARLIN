package grassmarlin.ui.common;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public class NodeOffsetBinding extends ObjectBinding<Point2D> {
    protected class XCoordinate extends DoubleBinding {
        public XCoordinate() {
            super.bind(NodeOffsetBinding.this);
        }

        public double computeValue() {
            return NodeOffsetBinding.this.get().getX();
        }
    }
    protected class YCoordinate extends DoubleBinding {
        public YCoordinate() {
            super.bind(NodeOffsetBinding.this);
        }

        public double computeValue() {
            return NodeOffsetBinding.this.get().getY();
        }
    }

    private final List<Node> nodes;

    public NodeOffsetBinding(final Node target, final Node root) {
        nodes = new ArrayList<>();
        Node step = target;
        while(step != root) {
            nodes.add(step);
            step = step.getParent();
        }

        for(final Node node : nodes) {
            super.bind(node.layoutXProperty(), node.layoutYProperty(), node.translateXProperty(), node.translateYProperty(), node.scaleXProperty(), node.scaleYProperty(), node.rotateProperty(), node.rotationAxisProperty());
        }
    }

    public Point2D computeValue() {
        Point2D result = new Point2D(0.0, 0.0);
        for(final Node node : nodes) {
            result = node.localToParent(result);
        }
        return result;
    }

    private XCoordinate x = null;
    public DoubleExpression getX() {
        if(x == null) {
            x = new XCoordinate();
        }
        return x;
    }

    private YCoordinate y = null;
    public DoubleExpression getY() {
        if(y == null) {
            y = new YCoordinate();
        }
        return y;
    }
}
