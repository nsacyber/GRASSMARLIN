package grassmarlin.plugins.internal.physical.view.visualization;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;

public interface IPhysicalElement {
    DoubleProperty translateXProperty();
    DoubleProperty translateYProperty();

    BooleanProperty isSubjectToLayoutProperty();
}
