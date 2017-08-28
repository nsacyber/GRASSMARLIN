package grassmarlin.common.fxobservables;

import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;

public class FxStringProperty extends SimpleStringProperty {
    public FxStringProperty(final String text) {
        super(text);
    }
    public FxStringProperty(final StringBinding text) {
        super();

        this.bind(text);
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
