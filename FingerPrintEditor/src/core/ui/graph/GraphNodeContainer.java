package core.ui.graph;

/**
 * Created by CC on 7/10/2015.
 */
public interface GraphNodeContainer {

    void removeNode(DefaultNode node);

    void refresh();

    void copy(DefaultNode node);
}
