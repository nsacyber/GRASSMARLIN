package grassmarlin.ui.common;

import javafx.geometry.Point2D;

import java.util.List;

public interface IAltClickable {

    default List<Object> getRespondingNodes(double x, double y) {
        return getRespondingNodes(new Point2D(x,y));
    }

    List<Object> getRespondingNodes(Point2D point);
}
