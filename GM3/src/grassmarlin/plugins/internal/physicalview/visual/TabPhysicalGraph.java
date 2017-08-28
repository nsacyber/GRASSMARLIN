package grassmarlin.plugins.internal.physicalview.visual;

import grassmarlin.plugins.internal.physicalview.PhysicalGraph;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;

public class TabPhysicalGraph extends Tab {

    private final PhysicalVisualization visualization;

    public TabPhysicalGraph(final PhysicalGraph graph, final ImageDirectoryWatcher<Image> imageMapper) {
        super("Physical Graph");

        this.visualization = new PhysicalVisualization(graph, imageMapper);


        initComponents();
    }

    private void initComponents() {
        this.setContent(this.visualization);
    }
}
