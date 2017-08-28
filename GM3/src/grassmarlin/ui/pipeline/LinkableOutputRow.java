package grassmarlin.ui.pipeline;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.HBox;

public class LinkableOutputRow extends HBox implements Linkable {

    private VisualStage parentStage;
    private String name;

    private BooleanProperty selectedProperty;

    public LinkableOutputRow(int spacing, VisualStage parentStage, String outputName) {
        super(spacing);

        this.parentStage = parentStage;
        this.parentStage.getLinkingPane().getSelected().addListener(this::handleSelectionChange);
        this.name = outputName;

        this.selectedProperty = new SimpleBooleanProperty(false);

    }

    private void handleSelectionChange(ListChangeListener.Change<Linkable> change) {
        while(change.next()) {
            if (change.getAddedSubList().contains(this)) {
                this.selectedProperty.setValue(true);
            } else if (change.getRemoved().contains(this)) {
                this.selectedProperty.setValue(false);
            }
        }
    }

    public VisualStage getParentStage() {
        return this.parentStage;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selectedProperty.setValue(selected);
    }

    @Override
    public boolean isSelected() {
        return this.selectedProperty.get();
    }

    @Override
    public BooleanProperty getSelectedProperty() {
        return this.selectedProperty;
    }

    @Override
    public ObservableNumberValue getLinkXProperty() {
        return null;
    }

    @Override
    public ObservableNumberValue getLinkYProperty() {
        return null;
    }

    @Override
    public ObservableNumberValue getLinkControlXProperty() { return null; }

    @Override
    public ObservableNumberValue getLinkControlYProperty() { return null; }

    @Override
    public ObservableNumberValue getSourceXProperty() {
        return this.layoutXProperty().add(this.parentStage.translateXProperty()).add(this.widthProperty()).subtract(2.0);
    }

    @Override
    public ObservableNumberValue getSourceYProperty() {
        return this.layoutYProperty().add(this.parentStage.translateYProperty()).add(this.heightProperty().divide(2.0));
    }

    @Override
    public ObservableNumberValue getSourceControlXProperty() {
        return this.layoutXProperty().add(this.parentStage.translateXProperty()).add(this.widthProperty()).add(30);
    }

    @Override
    public ObservableNumberValue getSourceControlYProperty() {
        return this.getSourceYProperty();
    }

    @Override
    public LinkingPane getLinkingPane() {
        return this.parentStage.getLinkingPane();
    }
}
