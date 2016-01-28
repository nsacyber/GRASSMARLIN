package core.ui.graph;

import javax.swing.*;

/**
 * GraphPanel is a JPanel specially tuned for use with graphing components,  
 * it is necessary to use a graph panel when using the GraphInteractionKit.
 *
 */
public abstract class GraphPanel extends JPanel implements GraphNodeContainer {

    private DefaultNode clickedNode;
    private IndicatorNode selectedNode;
    private IndicatorNode shiftSelectedNode;

    /**
     * @return Returns the clickedNode.
     */
    protected DefaultNode getClickedNode () {
        return clickedNode;
    }

    /**
     * @param clickedNode The clickedNode to set.
     */
    protected void setClickedNode (DefaultNode clickedNode) {
        this.clickedNode = clickedNode;
    }

    /**
     * @return Returns the selectedNode.
     */
    protected IndicatorNode getSelectedNode () {
        return selectedNode;
    }

    /**
     * @param selectedNode The selectedNode to set.
     */
    protected void setSelectedNode (IndicatorNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    /**
     * @return Returns the shiftSelectedNode.
     */
    protected IndicatorNode getShiftSelectedNode () {
        return shiftSelectedNode;
    }

    /**
     * @param shiftSelectedNode The shiftSelectedNode to set.
     */
    protected void setShiftSelectedNode (IndicatorNode shiftSelectedNode) {
        this.shiftSelectedNode = shiftSelectedNode;
    }
}
