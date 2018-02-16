package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;

public class AlwaysStyleRule extends EdgeStyleRule {
    public AlwaysStyleRule() {
        //No implementation
    }

    @Override
    public StringExpression descriptionProperty() {
        return new ReadOnlyStringWrapper("Always apply ").concat(this.style);
    }

    @Override
    public boolean applies(final VisualLogicalEdge edge) {
        return true;
    }

    @Override
    public String toString() {
        return String.format("Always apply %s", this.style.get().getName());
    }

}
