package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.ILogicalVisualization;
import grassmarlin.plugins.internal.logicalview.IVisualLogicalVertex;
import grassmarlin.ui.common.MutablePoint;
import javafx.scene.Node;

import java.util.Map;

public interface ILogicalGraphLayout {
    boolean requiresForcedUpdate();
    <T extends Node & ILogicalVisualization> Map<IVisualLogicalVertex, MutablePoint> executeLayout(T visualization);
}
