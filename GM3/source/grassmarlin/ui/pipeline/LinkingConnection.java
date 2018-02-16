package grassmarlin.ui.pipeline;

import javafx.scene.shape.CubicCurve;

public interface LinkingConnection<G, T> {
    CubicCurve getConLine();

    G getSource();

    T getDest();
}
