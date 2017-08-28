package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;

public class AlwaysStyleRule extends EdgeStyleRule {
    public AlwaysStyleRule() {
        //No implementation
    }

    @Override
    public boolean applies(final VisualLogicalEdge edge) {
        return true;
    }

    @Override
    public String toString() {
        return String.format("Always apply %s", this.styleNameProperty.get());
    }

}
