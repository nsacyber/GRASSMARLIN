package iadgov.fingerprint.manager.payload;


import iadgov.fingerprint.manager.tree.PayloadItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

import java.util.Arrays;

public interface ParentBox {

    default ObservableList<PayloadItem.OpType> getAvailableOps(){
        return FXCollections.observableArrayList(Arrays.asList(PayloadItem.OpType.values()));
    }

    VBox getChildrenBox();

    void addChild(OpRow child);

    void removeChild(OpRow child);

    /**
     * Call this method when a value has been updated
     */
    void update();

    boolean isLoading();
}
