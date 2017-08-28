package grassmarlin.plugins.internal.fingerprint.manager.payload;

import grassmarlin.plugins.internal.fingerprint.manager.tree.PayloadItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.HBox;

import java.io.Serializable;


public class EmptyRow extends OpRow {

    public EmptyRow() {
        super(null);
    }

    @Override
    public HBox getInput() {
        return new HBox();
    }

    @Override
    public Serializable getOperation() {
        return null;
    }

    @Override
     public ObservableList<PayloadItem.OpType> getAvailableOps() {
        return FXCollections.observableArrayList();
    }
}
