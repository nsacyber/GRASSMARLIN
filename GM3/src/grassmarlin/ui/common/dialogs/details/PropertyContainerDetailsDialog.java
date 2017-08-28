package grassmarlin.ui.common.dialogs.details;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Property;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertyContainerDetailsDialog extends Dialog<ButtonType> {
    private final ObjectProperty<Map<String, Set<Property<?>>>> container;
    private final TreeItem<String> root;

    public PropertyContainerDetailsDialog() {
        this.container = new SimpleObjectProperty<>(null);
        this.container.addListener(this::handle_containerChanged);

        this.root = new TreeItem<>();

        this.initComponents();
    }

    private void initComponents() {
        RuntimeConfiguration.setIcons(this);
        this.setResizable(true);

        final TreeView<String> tree = new TreeView<>();
        tree.setRoot(this.root);
        tree.setShowRoot(false);

        this.getDialogPane().setContent(tree);

        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private void handle_containerChanged(ObservableValue<? extends Map<String, Set<Property<?>>>> observable, Map<String, Set<Property<?>>> oldValue, Map<String, Set<Property<?>>> newValue) {
        this.root.getChildren().clear();
        if(newValue != null) {
            for(final Map.Entry<String, Set<Property<?>>> entry : newValue.entrySet()) {
                final TreeItem<String> property = new TreeItem<>(entry.getKey());
                property.getChildren().addAll(entry.getValue().stream().map(value -> new TreeItem<>(value.toString())).collect(Collectors.toList()));
                this.root.getChildren().add(property);
            }
        }
    }

    public ObjectProperty<Map<String, Set<Property<?>>>> targetProperty() {
        return this.container;
    }
}
