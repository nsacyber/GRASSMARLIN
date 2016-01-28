package core.fingerprint;

import javafx.scene.control.*;
import sample.Controller;

import java.util.Arrays;
import java.util.Optional;

/**
 * Created by CC on 7/21/2015.
 */
public final class XmlElementTreeCell extends TreeCell<XmlElementTreeItem> {
    private Controller controler;
    private TreeView<XmlElementTreeItem> payloadTree;

    private String getString() {
        return getItem() == null ? "" : getItem().toString();
    }

    public XmlElementTreeCell(Controller controler, TreeView<XmlElementTreeItem> payloadTree) {
        super();
        this.controler = controler;
        this.payloadTree = payloadTree;
    }

    private ContextMenu createContextMenu(XmlElementTreeItem element) {
        ContextMenu menu = new ContextMenu();
        Arrays.asList(Instruction.values()).stream().filter(item -> item.isInstructionForParent(element))
                .forEach(instruction -> {
                    MenuItem addMenuItem = new MenuItem("Add " + instruction.getText());
                    menu.getItems().add(addMenuItem);
                    addMenuItem.setOnAction(instruction.getAction(this));
                });
        if (this.getTreeItem().getValue().getInstruction() != Instruction.PAYLOAD) {
            menu.getItems().add(new SeparatorMenuItem());
            if(this.getTreeItem().getValue().getAttributes().size() > 0) {
                MenuItem editMenuItem = new MenuItem("Edit this instruction");
                editMenuItem.setOnAction(event1 -> {
                    XmlElementTreeItem item = this.getTreeItem().getValue().getInstruction().collectInformationFromUser(this.getTreeItem());
                    if (item != null) {
                        this.getTreeItem().setValue(item);
                        this.controler.setNotSaved();
                    }
                });
                menu.getItems().add(editMenuItem);
            }
            MenuItem deleteMenuItem = new MenuItem("Delete this instruction");
            menu.getItems().add(deleteMenuItem);
            deleteMenuItem.setOnAction(event -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("WARNING");
                alert.setHeaderText("You are about to delete this instruction and all sub-instructions");
                alert.setContentText("Proceed with delete?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    setStyle("");
                    this.getTreeItem().getParent().getChildren().remove(this.getTreeItem());
                }

            });
        }
        return menu;
    }

    @Override
    public void updateItem(XmlElementTreeItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            this.controler.setNotSaved();
            setText(getString());
            setGraphic(getTreeItem().getGraphic());
            setContextMenu(createContextMenu(getItem()));
            getTreeItem().setExpanded(true);
            if(Arrays.asList(Instruction.values()).stream().anyMatch(instruction -> instruction.isInstructionForParent(item))) {
                setStyle("-fx-background: #061439");
            }
            else {
                setStyle("-fx-background: #2F4172");
            }
        }
    }
}
