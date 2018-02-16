package grassmarlin.common.fxobservables;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;

public class FxDoubleProperty extends SimpleDoubleProperty {
    public FxDoubleProperty(final double value) {
        super(value);
    }

    @Override
    protected void fireValueChangedEvent() {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(this::fireValueChangedEvent);
            } else {
                super.fireValueChangedEvent();
            }
        } catch(IllegalStateException ex) {
            //Fx not initialized--possibly not an Fx UI mode, so just run in-thread
            super.fireValueChangedEvent();
        }
    }
}
