package grassmarlin.ui.common.dialogs.pluginconflict;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.When;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginConflictDialog extends Dialog<ButtonType> {
    private final ObservableList<PluginConflict> conflicts;

    public PluginConflictDialog() {
        this.conflicts = new ObservableListWrapper<>(new ArrayList<>());

        initComponents();
    }

    private void initComponents() {
        RuntimeConfiguration.setIcons(this);

        final TableView<PluginConflict> tblConflicts = new TableView<>();
        tblConflicts.setItems(this.conflicts);
        tblConflicts.setPrefWidth(800.0);
        tblConflicts.setRowFactory(view -> {
            final TableRow<PluginConflict> row = new TableRow<>();
            final ContextMenu menuRow = new ContextMenu();
            menuRow.getItems().addAll(
                    new DynamicSubMenu("Resolve", null, () ->
                        row.getItem().conflictProperty().get().getAllowedActions().stream().map(action -> new ActiveMenuItem(action.toString(), event -> row.getItem().actionProperty().set(action))).collect(Collectors.toList())
                    )
            );
            row.contextMenuProperty().bind(new When(row.emptyProperty()).then((ContextMenu)null).otherwise(menuRow));
            return row;
        });
        // == Columns ==
        final TableColumn<PluginConflict, String> colPlugin = new TableColumn<>("Plugin");
        colPlugin.setCellValueFactory(new PropertyValueFactory<>("plugin"));
        final TableColumn<PluginConflict, PluginConflict.Conflict> colConflict = new TableColumn<>("Conflict");
        colConflict.setCellValueFactory(new PropertyValueFactory<>("conflict"));
        final TableColumn<PluginConflict, PluginConflict.Action> colAction = new TableColumn<>("Action");
        colAction.setCellValueFactory(new PropertyValueFactory<>("action"));
        tblConflicts.getColumns().addAll(colPlugin, colConflict, colAction);

        this.setResizable(true);
        this.getDialogPane().setContent(tblConflicts);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        this.getDialogPane().lookupButton(ButtonType.OK).disableProperty().set(true);
    }

    public Optional<ButtonType> showAndWait(Collection<PluginConflict> conflicts) {
        this.conflicts.clear();
        this.conflicts.addAll(conflicts);
        //TODO: Unbind OK button
        if(this.getDialogPane().lookupButton(ButtonType.OK).disableProperty().isBound()) {
            this.getDialogPane().lookupButton(ButtonType.OK).disableProperty().unbind();
        }

        BooleanBinding allConflictsResolved = null;
        for(PluginConflict conflict : this.conflicts) {
            if(allConflictsResolved == null) {
                allConflictsResolved = conflict.isResolvedProperty();
            } else {
                allConflictsResolved = allConflictsResolved.and(conflict.isResolvedProperty());
            }
        }

        if(allConflictsResolved == null) {
            //No conflicts, just skip.
            return Optional.of(ButtonType.OK);
        } else {
            this.getDialogPane().lookupButton(ButtonType.OK).disableProperty().bind(allConflictsResolved.not());
        }
        return super.showAndWait();
    }
}
