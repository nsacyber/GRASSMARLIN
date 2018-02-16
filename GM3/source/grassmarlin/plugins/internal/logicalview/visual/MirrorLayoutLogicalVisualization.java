package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.ViewLogical;
import grassmarlin.plugins.internal.logicalview.visual.layouts.Mirror;

public class MirrorLayoutLogicalVisualization extends LogicalVisualization {
    public MirrorLayoutLogicalVisualization(final ViewLogical view, final LogicalVisualization base) {
        super(view);

        this.layoutProperty.set(new Mirror(base));
    }

    @Override
    public void cleanup() {

    }
}
