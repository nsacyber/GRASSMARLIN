package grassmarlin.common.fxobservables;

import javafx.application.Platform;
import javafx.beans.property.SimpleLongProperty;

public class FxLongProperty extends SimpleLongProperty {
    public FxLongProperty(final long value) {
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
