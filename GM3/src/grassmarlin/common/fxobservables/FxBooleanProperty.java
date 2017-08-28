package grassmarlin.common.fxobservables;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;

public class FxBooleanProperty extends SimpleBooleanProperty {
    public FxBooleanProperty(final boolean state) {
        super(state);
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
