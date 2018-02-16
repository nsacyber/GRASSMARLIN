package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.ui.common.MutablePoint;
import javafx.scene.Node;

import java.util.Map;

public interface IPhysicalGraphLayout {
    boolean requiresForcedUpdate();
    <T extends Node & IPhysicalVisualization> Map<IPhysicalElement, MutablePoint> executeLayout(T visualization);
}
