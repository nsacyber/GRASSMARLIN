package core.fingerprint;

import core.fingerprint3.ObjectFactory;
import core.ui.graph.*;
import core.ui.graph.DefaultNode.NodePoint;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import sample.Controller;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by CC on 7/7/2015.
 */
public class FpPanel extends GraphPanel {
    private StackPane swingNodeContainer;
    /** reference to node that is clicked, null if none clicked */
    private DefaultNode clickedNode;

    /** References to all Nodes on the Graph **/
    private Collection<DefaultNode> nodes = Collections.synchronizedList(new ArrayList<>());
    private Collection<DefaultNode> nodesToRemove = new ArrayList<>();
//    /** References to all links on the Graph **/
//    private Collection<DefaultNode> buttons = new ArrayList<>();
    /** References to all links on the Graph **/
    private Collection<DefaultLink> links = new ArrayList<>();

    /** reusable tooltipnode */
    private NodeToolTip toolTip = new NodeToolTip();
    private boolean paintToolTip = false;
    private Controller controller;

    public String getUuid() {
        return uuid;
    }

    final private String uuid;

    public FpPanel(StackPane swingNodeContainer) {
        this(swingNodeContainer, java.util.UUID.randomUUID().toString());
    }

    public FpPanel(StackPane swingNodeContainer, String uuid) {
        this.uuid = uuid;
        this.swingNodeContainer = swingNodeContainer;
        this.initComponents();
    }

    private void initComponents() {
        //Click listener
        super.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                super.mousePressed(me);
                FpPanel.this.clickedNode = null;
                FpPanel.this.nodes.stream().filter(node -> node.isMovable() && node.isPointOverNode(me.getPoint())).forEach(node -> {
                    FpPanel.this.clickedNode = node;
                    FpPanel.this.clickedNode.setDragOffset(me.getX() - FpPanel.this.clickedNode.getNodePointValue(NodePoint.topLeft).x,
                            me.getY() - FpPanel.this.clickedNode.getNodePointValue(NodePoint.topLeft).y);
                });

                FpPanel.this.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (FpPanel.this.nodes.stream().anyMatch(nodes -> nodes.mouseRightClicked(e))) {
                        addFilterGroupCopies();
                    }
                } else {
                    FpPanel.this.nodes.stream()
                            .filter(DefaultNode::isCloseable)
                            .forEach(node -> node.maybeClose(e.getPoint()));
                    FpPanel.this.repaint();
                }
            }


            @Override
            public void mouseReleased(MouseEvent me) {
                super.mouseReleased(me);
                if (FpPanel.this.nodes.stream().anyMatch(node -> node.mouseReleased(me, Optional.ofNullable(FpPanel.this.clickedNode)))) {
                    FpPanel.this.clickedNode = null;
                    FpPanel.this.repaint();
                }
            }
        });

        //Hover listener
        super.addMouseMotionListener(new MouseMotionListener() {
            boolean removeToolTip = true;

            private void prepToolTip(Node node, MouseEvent me) {
                removeToolTip = false;
                FpPanel.this.toolTip.setToolTipText(node.getToolTipText());
                Point movedPoint = new Point(me.getPoint().x + 10, me.getPoint().y + 10);
                FpPanel.this.toolTip.moveNode(movedPoint);
                FpPanel.this.paintToolTip = true;
                FpPanel.this.repaint();
            }

            public void mouseMoved(MouseEvent me) {
                removeToolTip = true;
                FpPanel.this.nodes.stream()
                        .filter(node -> node.hasToolTip() && node.isPointOverNode(me.getPoint()))
                        .limit(1)
                        .forEach(node -> prepToolTip(node, me));
                if (removeToolTip && FpPanel.this.paintToolTip) {
                    FpPanel.this.paintToolTip = false;
                    FpPanel.this.repaint();
                }
            }

            public void mouseDragged(MouseEvent me) {
                if (FpPanel.this.clickedNode != null) {
                    FpPanel.this.clickedNode.moveNodeWithOffset(me.getPoint());
                    FpPanel.this.nodes.stream().filter(defaultNode -> defaultNode != FpPanel.this.clickedNode).forEach(node -> node.mouseDragged(me, Optional.ofNullable(FpPanel.this.clickedNode)));
                    FpPanel.this.resize();
                }
                FpPanel.this.repaint();
            }
        });
    }

    private ArrayList<ContainerNode> filterGroupsToCopy = new ArrayList<>();

    private void addFilterGroupCopies() {
        if(this.controller != null) {
            this.filterGroupsToCopy.stream().forEach(node -> {
                ContainerNode newFilterGroup = this.controller.addNewReturnGroup(Controller.FILTER_GROUP_CONTAINER_LABEL_TEXT, Controller.FILTER_GROUP_CONTAINER_IDENTIFIER, this, false);
                node.getElements().stream().filter(eachNode -> eachNode instanceof RoundedTextNode).map(defaultNode -> ((RoundedTextNode)defaultNode))
                        .forEach(nodeToCopy -> {
                            RoundedTextNode newFilter = this.controller.createFilterNode(nodeToCopy.getText(), nodeToCopy.getTextLine2());
                            newFilterGroup.addElement(newFilter);
                        });
            });
            this.filterGroupsToCopy.clear();
        }
    }

    public void copy(DefaultNode node) {
        if(node instanceof ContainerNode) {
            this.filterGroupsToCopy.add((ContainerNode) node);
        }
    }


    /**
     * draws the graph components
     */
    @Override
    public void paint(Graphics g) {
//        boolean minimize = false;
        if(this.nodesToRemove.size() > 0) {
            //minimize = true;
            this.nodesToRemove.stream().forEach(node -> this.nodes.remove(node));
            this.nodesToRemove.clear();
            DefaultNode[] lastNode = {null};
            this.nodes.stream().filter(node->node.getName().equals(Controller.FILTER_GROUP_CONTAINER_IDENTIFIER)).forEach(node -> {
                if (lastNode[0] == null) {
                    node.moveNode(new Point(5, 5));
                } else {
                    int x = lastNode[0].getNodePointValue(DefaultNode.NodePoint.topRight).x+10;
                    node.moveNode(new Point(x,5));
                }
                lastNode[0] = node;
            });
        }
        Graphics2D g2d = (Graphics2D) g;
        //Set up anti alaising makes edges smooth and professional looking
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //creates a blank background also vital to prevent unwanted leftover graphics drag
        GraphToolKit.fillBackgroundColor(g2d, Color.WHITE);
        this.nodes.stream().forEach(node -> node.paintNode(g2d));
        this.links.stream().forEach(link -> link.paintLink(g2d));
        if (this.paintToolTip) {
            this.toolTip.paintNode(g2d);
            resize();
        }
    }

    public void resize() {
        int maxX = 0;
        int maxY = 0;
        int pad = 5;
        try {
            maxX = this.nodes.stream()
                    .map(node -> node.getNodePointValue(NodePoint.topRight).x).max(Integer::compare).get();
            maxY = this.nodes.stream()
                    .map(node -> node.getNodePointValue(NodePoint.bottomLeft).y).max(Integer::compare).get();
            if(this.paintToolTip) {
                if(this.toolTip.getNodePointValue(NodePoint.topRight).x > maxX) {
                    maxX = this.toolTip.getNodePointValue(NodePoint.topRight).x;
                }
                if(this.toolTip.getNodePointValue(NodePoint.bottomLeft).y > maxY) {
                    maxY = this.toolTip.getNodePointValue(NodePoint.bottomLeft).y;
                }
            }
        } catch (NoSuchElementException e) { }

        Dimension newDimension = new Dimension(Math.max(this.getWidth(),(maxX+pad)),Math.max(this.getHeight(),(maxY+pad)));
        this.setSize(newDimension);
        this.swingNodeContainer.setPrefSize(newDimension.getWidth(), newDimension.getHeight());
        this.repaint();
    }

    /**
     * like a resize but reduces the dimension to the smallest possible while fitting the content
     */
    public void minimize() {
        int maxX = 0;
        int maxY = 0;
        int pad = 5;
        try {
            maxX = this.nodes.stream()
                    .map(node -> node.getNodePointValue(NodePoint.topRight).x).max(Integer::compare).get();
            maxY = this.nodes.stream()
                    .map(node -> node.getNodePointValue(NodePoint.bottomLeft).y).max(Integer::compare).get();
        } catch (NoSuchElementException e) { }

        Dimension newDimension = new Dimension((maxX+pad),(maxY+pad));
        this.setSize(newDimension);
        this.swingNodeContainer.setPrefSize(newDimension.getWidth(), newDimension.getHeight());
        this.repaint();
    }

    public void addNode(DefaultNode node) {
        node.updateContainerRef(this);
        this.nodes.add(node);
        if(node instanceof RoundedTextNode) {
            ((RoundedTextNode) node).setToolTipText("Drag Filter to a filter Group\nRight click to edit");
        }
        if(node instanceof ContainerNode) {
            ((ContainerNode) node).setToolTipText("Right click to Copy\nRight click a filter to edit");
        }
    }

    public Collection<DefaultNode> getNodes() {
        return this.nodes;
    }


    @Override
    public void removeNode(DefaultNode node) {
        removeLater(node);
    }

    @Override
    public void refresh() {
        //repaint();
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    private void removeLater(DefaultNode node) {
        this.nodesToRemove.add(node);
    }


}
