package grassmarlin.ui.pipeline;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyEvent;

import java.util.List;

public interface LinkingPane<T, G> {
    List<Linkable> getLinkables();

    List<LinkingConnection> getLinks();

    Linkable addLinkable(T linkableData);

    boolean removeLinkable(T linkableData);

    LinkingConnection addLink(G connectionData);

    boolean removeLink(G connectionData);

    boolean select(Linkable selected);

    boolean unSelect(Linkable unselected);

    ObservableList<Linkable> getSelected();

    void handleLinkerSelected(Linker linker);

    void handleKeyPressed(KeyEvent event);

    void handleKeyReleased(KeyEvent event);

    BooleanProperty dirtyProperty();
}
