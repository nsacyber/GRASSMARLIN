package core.ui.graph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * A Genius node for containing other genius nodes
 * 2007.05 - New...
 * 2007.06.14 - (CC) syncronized all methods touching nodePoints
 *
 */
public class ContainerNode extends DefaultNode implements GraphNodeContainer{
    // a list of genius nodes this node contains
    Collection<DefaultNode> elements;
    Collection<DefaultNode> elementsToRemove = new ArrayList<>();
    // default spacing between nodes
    private static final int DEFAULT_SPACING = 5;
    //filter for accepting nodes as elements
    ArrayList<DefaultNodeFilter> nodeFilters = new ArrayList<>();
    private String toolTipText = "";
    private boolean hasToolTip = false;

    /**
     * constructs a new container node given the elements and topleft point
     * @param elements
     * @param topLeft
     */
    public ContainerNode(Collection<DefaultNode> elements, Point topLeft) {
        this.elements = elements;
        this.elements.stream().forEach(defaultNode -> defaultNode.updateContainerRef(this));
        refresh(topLeft);
    }

    public void refresh(Point topLeft) {
        setActualWidthAndHeight();
        generateAllPointsFromTopLeft(topLeft);
    }

    public void refresh() {
        if(getNodeContainer() != null) {
            getNodeContainer().refresh();
        }
        refresh(nodePoints.get(NodePoint.topLeft));
    }

    @Override
    public void copy(DefaultNode node) {
        // do nothing
    }

    /**
     * detrmines the actual height and width by examining its elements
     *
     */
    private void setActualWidthAndHeight () {
        int containerTopLeftY = this.nodePoints.get(NodePoint.topLeft).y;
        int aWidth = 10;
        int aHeight = 10;

        int largestNode = this.elements.stream().map(defaultNode -> defaultNode.getWidth()).max(Integer::compare).get();
        int largestY = this.elements.stream().map(defaultNode -> defaultNode.getNodePointValue(NodePoint.bottomLeft).y).max(Integer::compare).get();

        aWidth += largestNode + DEFAULT_SPACING;
        aHeight += largestY + DEFAULT_SPACING;
        aHeight -= containerTopLeftY;

        this.width = aWidth;
        this.height = aHeight;

        if(this.mouseOverGrow) {
            this.height += 50;
        }
    }
    private boolean mouseOverGrow = false;
    public void setGrowForMouseOver(boolean mouseOverGrow ) {
        this.mouseOverGrow = mouseOverGrow;
    }

    /**
     * paints the node and all elements inside
     */
    public synchronized void paintNode(Graphics2D g2d) {
        elementsToRemove.stream().forEach(element -> this.elements.remove(element));
        elementsToRemove.clear();
        refresh();
        placeSubElements();
        setActualWidthAndHeight();
        super.paintNode(g2d);
        this.elements.stream().forEach(defaultNode -> defaultNode.paintNode(g2d));
        super.maybePaintCloseButton(g2d);
    }

    private void placeSubElements() {
        int containerTopLeftX = this.nodePoints.get(NodePoint.topLeft).x;
        int containerTopLeftY = this.nodePoints.get(NodePoint.topLeft).y;
        Node lastNode = null;
        for(Node node : this.elements) {
            Point nodeTopLeft;
            if(lastNode == null) {
                //this is the first subnode
                nodeTopLeft = new Point(
                        containerTopLeftX + DEFAULT_SPACING,
                        containerTopLeftY + DEFAULT_SPACING);
            }
            else {
                nodeTopLeft = new Point(
                        lastNode.getNodePointValue(NodePoint.bottomLeft).x,
                        lastNode.getNodePointValue(NodePoint.bottomLeft).y + DEFAULT_SPACING);
            }
            node.moveNode(nodeTopLeft);
            lastNode = node;
        }
    }

    public void addElement (DefaultNode node) {
        if(node.getNodeContainer() != null) {
            node.getNodeContainer().removeNode(node);
        }
        node.updateContainerRef(this);
        this.elements.add(node);
    }


    @Override
    public void removeNode(DefaultNode node) {
        removeLater(node);
    }

    private void removeLater(DefaultNode node) {
        elementsToRemove.add(node);
    }


    private synchronized boolean isPointOverCloseButton(Point point) {
        if(super.getCloseButton() == null) {
            return false;
        }
        else {
            return super.getCloseButton().contains(point.x, point.y);
        }
    }

    public void setToolTipText(String text) {
        this.hasToolTip = true;
        this.toolTipText = text;
    }

    @Override
    public synchronized void maybeClose(Point point) {
        if(isPointOverCloseButton(point)) {
            super.maybeClose(point);
        }
        else {
            //check to see if children need to go
            this.elements.stream().forEach(element -> element.maybeClose(point));
        }

    }

    public boolean mouseDragged(MouseEvent me, Optional<DefaultNode> nodeBeingDragged) {
        final boolean[] returnValue = {false};
        nodeBeingDragged.ifPresent(defaultNode -> {
            setGrowForMouseOver(false);
            if(isNodeAddable(defaultNode)) {
                if (isPointOverNode(me.getPoint())) {
                    //event is being handled
                    returnValue[0] = true;
                    if (this.elements.stream().noneMatch(node -> node.mouseDragged(me, nodeBeingDragged))) {
                        //no sub elements are handling this event so we have to handle it
                        setGrowForMouseOver(true);
                    }
                }
            }
        });
        return returnValue[0];
    }

    public boolean mouseReleased(MouseEvent me, Optional<DefaultNode> nodeBeingReleased) {
        setGrowForMouseOver(false);
        final boolean[] returnValue = {false};
        nodeBeingReleased.ifPresent(releasedNode -> {
            //dont want to add ourselves
            if(isNodeAddable(releasedNode)) {
                if (isPointOverNode(me.getPoint())) {
                    //event is being handled
                    returnValue[0] = true;
                    if (this.elements.stream().noneMatch(node -> node.mouseReleased(me, nodeBeingReleased))) {
                        //no sub elements consumed this released node so we consume it
                        addElement(releasedNode);
                        refresh();
                    }
                }
            }
        });
        return returnValue[0];
    }

    public boolean mouseRightClicked(MouseEvent me) {
        final boolean[] returnValue = {false};
        if(isPointOverNode(me.getPoint())) {
            //
            returnValue[0] = true;
            if (this.elements.stream().noneMatch(node -> node.mouseRightClicked(me))) {
                super.getNodeContainer().copy(this);
            }
        }

        return returnValue[0];
    }

    /**
     * TODO: this is a bad way to do this. not very OO make this an externally set-able filter
     * @param releasedNode
     * @return
     */
    private boolean isNodeAddable(DefaultNode releasedNode) {
        //we never add ourselves
        if(releasedNode == this) {
            return false;
        }
        return this.nodeFilters.stream().allMatch(defaultNodeFilter -> defaultNodeFilter.filter(releasedNode));
    }

    private boolean filter(DefaultNode releasedNode) {
        boolean isAddable = false;
        if (releasedNode instanceof RoundedTextNode) {
            isAddable = this.elements.stream().filter(defaultNode -> defaultNode instanceof RoundedTextNode)
                    .map(node -> ((RoundedTextNode) node))
                    .map(RoundedTextNode::getText)
                    .noneMatch(name -> name.equals(((RoundedTextNode) releasedNode).getText()));
        }
        return isAddable;
    }

    public void addDefaultNodeFilter(DefaultNodeFilter filter) {
        this.nodeFilters.add(filter);
    }

    @FunctionalInterface
    public interface DefaultNodeFilter {
        /**
         * returns true if the node matches the filter
         * @param node
         * @return
         */
        public boolean filter(DefaultNode node);
    }

    @Override
    public boolean hasToolTip() {
        return this.hasToolTip;
    }

    @Override
    public String getToolTipText() {
        return this.toolTipText;
    }

    public Collection<DefaultNode> getElements() {
        return elements;
    }
}
