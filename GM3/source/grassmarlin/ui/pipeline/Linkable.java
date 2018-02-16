package grassmarlin.ui.pipeline;

import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ObservableNumberValue;

public interface Linkable {

    String getName();

    void setSelected(boolean selected);

    boolean isSelected();

    BooleanProperty getSelectedProperty();

    ObservableNumberValue getLinkXProperty();
    ObservableNumberValue getLinkYProperty();
    ObservableNumberValue getLinkControlXProperty();
    ObservableNumberValue getLinkControlYProperty();

    ObservableNumberValue getSourceXProperty();
    ObservableNumberValue getSourceYProperty();
    ObservableNumberValue getSourceControlXProperty();
    ObservableNumberValue getSourceControlYProperty();

    LinkingPane getLinkingPane();
}
