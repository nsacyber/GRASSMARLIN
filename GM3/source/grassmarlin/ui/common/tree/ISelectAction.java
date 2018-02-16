package grassmarlin.ui.common.tree;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public interface ISelectAction {
    EventHandler<ActionEvent> getOnSelected();
    EventHandler<ActionEvent> getOnDeselected();
}
