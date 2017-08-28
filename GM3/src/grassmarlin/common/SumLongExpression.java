package grassmarlin.common;

import javafx.beans.binding.LongBinding;
import javafx.beans.property.LongProperty;

import java.util.LinkedList;
import java.util.List;

public class SumLongExpression extends LongBinding {
    private final List<LongProperty> sources;

    public SumLongExpression() {
        this.sources = new LinkedList<>();
    }

    public void listenTo(final LongProperty property) {
        if(sources.add(property)) {
            this.bind(property);
            invalidate();
        }
    }

    public void stopListeningTo(final LongProperty property) {
        if(sources.remove(property)) {
            this.unbind(property);
            invalidate();
        }
    }

    @Override
    public long computeValue() {
        long sum = 0;
        for(LongProperty value : this.sources) {
            sum += value.get();
        }
        return sum;
    }
}
