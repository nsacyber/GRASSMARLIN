package grassmarlin.ui.pipeline;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.ui.common.BackgroundFromColor;
import grassmarlin.ui.common.IAltClickable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VisualEntry extends HBox implements Linkable, IDraggable, ICanHasContextMenu, IAltClickable {

    private final String name;
    private final PanePipelineEditor container;

    private BooleanProperty selectedProperty;

    private final ObjectProperty<Paint> backgroundProperty;
    private final ObjectProperty<Paint> textColorProperty;
    private final ObjectProperty<Paint> contrastColorProperty;
    private final Text nameLabel;

    public VisualEntry(String name, PanePipelineEditor container, RuntimeConfiguration config) {
        super(8); // Not the best of hotels, but they work

        makeDraggable(true);

        this.name = name;
        this.container = container;
        this.selectedProperty = new SimpleBooleanProperty(false);
        this.container.getSelected().addListener(this::handleSelectionChange);

        this.backgroundProperty = new SimpleObjectProperty<>();
        this.backgroundProperty.bind(config.colorPipelineBackgroundProperty());
        this.textColorProperty = new SimpleObjectProperty<>();
        this.textColorProperty.bind(config.colorPipelineTitleTextProperty());
        this.contrastColorProperty = new SimpleObjectProperty<>();
        this.contrastColorProperty.bind(config.colorPipelineSelectorProperty());

        this.backgroundProperty().bind(new BackgroundFromColor(this.backgroundProperty));

        nameLabel = new Text(this.name);
        nameLabel.fillProperty().bind(this.textColorProperty);
        LinkingTriangle linker = new LinkingTriangle(contrastColorProperty, this, nameLabel.boundsInParentProperty());

        this.getChildren().addAll(nameLabel, linker);
    }

    public Font getFont() {
        return this.nameLabel.getFont();
    }

    private void handleSelectionChange(ListChangeListener.Change<? extends Linkable> change) {
        while(change.next()) {
            if (change.getAddedSubList().contains(this)) {
                this.selectedProperty.setValue(true);
            } else if (change.getRemoved().contains(this)) {
                this.selectedProperty.setValue(false);
            }
        }
    }

    @Override
    public List<Object> getRespondingNodes(Point2D point) {
        if (this.contains(point)) {
            return Arrays.asList(this);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        List<MenuItem> items = new ArrayList<>();

        MenuItem delete = new ActiveMenuItem("Remove Entry", event -> this.container.removeEntry(this));

        items.add(delete);

        return items;
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
    public ObservableNumberValue getLinkControlXProperty() {
        return null;
    }

    @Override
    public ObservableNumberValue getLinkControlYProperty() {
        return null;
    }

    @Override
    public ObservableNumberValue getSourceXProperty() {
        return this.translateXProperty().add(this.widthProperty()).subtract(2.0);
    }

    @Override
    public ObservableNumberValue getSourceYProperty() {
        return this.translateYProperty().add(this.heightProperty().divide(2.0));
    }

    @Override
    public ObservableNumberValue getSourceControlXProperty() {
        return this.translateXProperty().add(this.widthProperty().add(30));
    }

    @Override
    public ObservableNumberValue getSourceControlYProperty() {
        return this.getSourceYProperty();
    }

    @Override
    public LinkingPane getLinkingPane() {
        return this.container;
    }
}
