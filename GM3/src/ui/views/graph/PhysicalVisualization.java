/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import core.Core;
import core.topology.Entities;
import core.topology.PhysicalNode;
import core.topology.TopologyEdge;
import core.topology.TopologyEntity;
import core.topology.TopologyEntity.Type;
import core.topology.TopologyNode;
import core.types.ImmutableMap;
import core.types.LogEmitter;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.layout.CollapsedSubtreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.render.PolygonRenderer;
import prefuse.render.Renderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.HoverPredicate;
import prefuse.visual.expression.InGroupPredicate;
import ui.icon.Icons;
import ui.views.graph.logical.TreeRootAction;

/**
 * Private Class, belongs to and is solely used by PhysicalGraph.
 */
class PhysicalVisualization extends Visualization {

    public final boolean DEFAULT_EXPAND_STATE = true;
    public final Predicate IS_SWITCH_PREDICATE = fieldMatch(ENTITY, Type.SWITCH);
    public final Predicate IS_HOST_PREDICATE = fieldMatch(ENTITY, Type.HOST);
    public final int RADIAL_INCREMENT = 160;

    public final AggregateTable aggregateTable;
    public final VisualGraph visualGraph;
    public final Graph graph;

    final Map<Node, List<Node>> groups;
    final List<String> resumeActions;
    VisualItem focusItem, focusSubItem;

    final static String GRAPH = "graph";
    final static String NODES = "graph.nodes";
    final static String EDGES = "graph.edges";
    final static String AGGR = "aggr";

    final static String HASH = "graph.nodes.hash";
    final static String TEXT = "graph.nodes.text";
    final static String DATA = "graph.nodes.data";
    final static String ICON = "graph.nodes.icon";
    final static String EXPAND = "expand";
    final static String ENTITY = "entity";
    final static String PARENT_ROW = "parent.row";

    final static String LAYOUT = "vis.layout";
    final static String TREE = "vis.layout";
    final static String SUBTREE = "vis.layout";
    final static String REPAINT = "vis.repaint";
    final static String ANIMATE = "vis.animate";
    final static String USER_MODE = "vis.user";

    public PhysicalVisualization() {
        groups = new ImmutableMap<>(ArrayList::new);
        resumeActions = new ArrayList<>();

        this.graph = new Graph();
        this.graph.addColumn(DATA, Entities.class);
        this.graph.addColumn(TEXT, String.class, "");
        this.graph.addColumn(ICON, Image.class, Icons.Cog.get32());
        this.graph.addColumn(HASH, int.class, -1);
        this.graph.addColumn(EXPAND, boolean.class, DEFAULT_EXPAND_STATE);
        this.graph.addColumn(ENTITY, TopologyEntity.Type.class, TopologyEntity.Type.DEFAULT);
        this.graph.addColumn(PARENT_ROW, int.class, -1);
        this.visualGraph = addGraph(GRAPH, graph);

        this.aggregateTable = addAggregates(AGGR);
        this.aggregateTable.addColumn(VisualItem.POLYGON, float[].class);

        //<editor-fold defaultstate="collapsed" desc="setup renderers">
        DefaultRendererFactory drf = new DefaultRendererFactory();
        Renderer polyRender = new PolygonRenderer(Constants.POLY_TYPE_LINE);

        LabelRenderer devRender = new ImageSupportLabelRenderer();
        devRender.setHorizontalImageAlignment(Constants.CENTER);
        devRender.setVerticalImageAlignment(Constants.TOP);
        devRender.setImagePosition(Constants.TOP);

        devRender.setHorizontalTextAlignment(Constants.CENTER);
        devRender.setVerticalTextAlignment(Constants.BOTTOM);

        devRender.setImageField(ICON);
        devRender.setTextField(TEXT);

        devRender.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        devRender.setHorizontalPadding(10);
        devRender.setVerticalPadding(10);

        LabelEdgeRenderer edgeRender = new LabelEdgeRenderer(TEXT)
                .setLineWidth(4.0);

        drf.setDefaultRenderer(devRender);
        drf.setDefaultEdgeRenderer(edgeRender);
        drf.add(new InGroupPredicate(AGGR), polyRender);

        setRendererFactory(drf);

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="setup LAYOUT actions">
        FixedChildLayout affixChildren = new FixedChildLayout(GRAPH, NODES, ICON, EXPAND, groups);
        AggregateLayout aggrLayout = new AggregateLayout(null, AGGR);

        RadialTreeLayout treeLayout = new RadialTreeLayout(GRAPH, RADIAL_INCREMENT);
        treeLayout.setAutoScale(false);
        putAction(TREE, treeLayout);

        CollapsedSubtreeLayout subTreeLayout = new CollapsedSubtreeLayout(GRAPH);
        putAction(SUBTREE, subTreeLayout);

        ActionList layout = new ActionList()
                .add(new TreeRootAction(GRAPH))
                .add(treeLayout)
                .add(subTreeLayout)
                .add(affixChildren);
        putAction(LAYOUT, layout);

        //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="setup COLOR actions">
        Font normal = FontLib.getFont("Tahoma", 14);
        Font large = FontLib.getFont("Tahoma", Font.BOLD, 16);
        int lightBlueOpaque = ColorLib.rgba(153, 217, 234, 200);
        int lightBlueSolid = ColorLib.rgba(153, 217, 234, 255);
        int lightGreenOpaque = ColorLib.rgba(200, 255, 200, 200);
        int lightGreenSolid = ColorLib.rgba(200, 255, 200, 255);

        Predicate isHovered = new HoverPredicate();

        ColorAction textColor = new ColorAction(Visualization.ALL_ITEMS, VisualItem.TEXTCOLOR, Color.BLACK.getRGB());

        /* colors the hosts the light blue color */
        ColorAction hostFill = new ColorAction(NODES, IS_HOST_PREDICATE, VisualItem.FILLCOLOR, lightBlueOpaque)
                .add(isHovered, lightBlueSolid);

        ColorAction switchFill = new ColorAction(AGGR, VisualItem.FILLCOLOR, lightGreenOpaque)
                .add(isHovered, lightGreenSolid);

        ColorAction allEdges = new ColorAction(EDGES, VisualItem.STROKECOLOR) {
            final int weakTopologyColor = ColorLib.rgba(255, 255, 0, 200);
            final int strongTopologyColor = ColorLib.rgba(0, 200, 0, 200);

            @Override
            public int getColor(VisualItem item) {
                int color = 0;
                Type type = PhysicalVisualization.getType(item);
                switch (type) {
                    case CLOUD_HOST:
                    case PORT_CLOUD:
                        color = weakTopologyColor;
                        break;
                    case HOST_PORT:
                    case PORT_PORT:
                        color = strongTopologyColor;
                        break;
                    case DEFAULT:
                    default:
                }
                return color;
            }
        };
        FontAction nodeFont = new FontAction(NODES, normal);
        nodeFont.add(fieldMatch(ENTITY, TopologyEntity.Type.SWITCH), large);
        FontAction edgeFont = new FontAction(EDGES, normal);

        ActionList paintNodes = new ActionList()
                .add(switchFill)
                .add(hostFill)
                .add(nodeFont)
                .add(edgeFont)
                .add(textColor);

        ActionList paintEdges = new ActionList()
                .add(allEdges);

        ActionList repaint = new ActionList()
                .add(paintNodes)
                .add(paintEdges)
                .add(new RepaintAction());
        putAction(REPAINT, repaint);

        ActionList animate = new ActionList()
                .setPacerFunction(new SlowInSlowOutPacer())
                .add(new QualityControlAnimator())
                .add(new VisibilityAnimator(NODES))
                .add(new PolarLocationAnimator(NODES))
                .add(layout);
        putAction(ANIMATE, animate);

        ActionList userMode = new ActionList(ActionList.INFINITY)
                .add(new QualityControlAnimator())
                .add(affixChildren)
                .add(aggrLayout)
                .add(repaint);
        putAction(USER_MODE, userMode);

        alwaysRunAfter(ANIMATE, USER_MODE);

        setInteractive(EDGES, null, false);
        //</editor-fold>
    }

    public AggregateTable aggr() {
        return aggregateTable;
    }

    /**
     * Generates the proxy connections between groups of Ports that make up a
     * switch, populating the map for {@link #getGroups() }.
     *
     * @param physicalNode PhysicalNode of the switch.
     * @param nodes Ports of the PhysicalNode argument.
     * @return The aggregate item that encapsulates all VisualItems of the
     * switch and its switch-ports.
     */
    AggregateItem generateSwitchDiagram(PhysicalNode physicalNode, Set<TopologyNode> nodes) {
        AggregateItem item = (AggregateItem) aggr().addItem();

        Tuple proxyNode = graph.addNode()
                .set(TEXT, physicalNode.GUID)
                .set(ICON, null)
                .set(DATA, physicalNode)
                .set(ENTITY, TopologyEntity.Type.SWITCH);

        item.addItem(getVisualNode(proxyNode));

        List<Node> children = groups.get((Node) proxyNode);

        nodes.stream().sorted( (a,b) -> a.compareTo(b)  )
                .filter(node -> node.type.equals(TopologyEntity.Type.PORT))
                .forEach(node -> {
                    Node n = getNode(node, item);
                    n.setInt(PARENT_ROW, proxyNode.getRow());
                    children.add(n);
                });

        return item;
    }

    /* ensurs that this graph has a tree structure that can be lay */
    void validateTree() {
        Stack<Node> treeChildren = new Stack<>();
        groups.forEach((parent, children) -> {
            treeChildren.clear();

            children.forEach(child -> {
                if (child.getDegree() > 0) {
                    treeChildren.push(child);
                }
            });

            treeChildren.forEach(child -> {
                graph.addEdge(parent, child);

                treeChildren.forEach(xChild -> {

                    if (xChild.getRow() != child.getRow()) {
                        graph.addEdge(xChild, child);
                    }

                });
            });

        });
    }

    /**
     * Creates a "port" node within a switch, setting its ENTITY == ENTITY_PORT
     */
    Node getNode(TopologyNode node, AggregateItem aggregateItem) {
        Node n = getNode(node);
        n.set(ENTITY, node.type);

        if (node.getVisualAgg() == -1) {
            node.setVisualRow(aggregateItem.getRow());
            aggregateItem.addItem(getVisualNode(n));
        }
        return n;
    }

    /**
     * Will create or retrieve an Edge from the visualization by its {@link TopologyEdge#getVisualRow()
     * }. Will create its forward and back Nodes if they do not exist.
     *
     * @param node TopologyEdge to retrieve an Edge for.
     * @return Edge of the TopologyEdge.
     */
    public synchronized Edge addEdge(TopologyEdge edge) {
        Tuple e;
        if (edge.getVisualRow() == -1) {
            Node src = getNode(edge.source);
            Node dst = getNode(edge.destination);
            e = graph.addEdge(src, dst)
                    .set(TEXT, edge.getDisplayText())
                    .set(ENTITY, edge.type);
        } else {
            e = graph.getEdge(edge.getVisualRow());
        }
        return (Edge) e;
    }

    /**
     * Will create or retrieve a Node from the visualization by its
     * {@link TopologyEntity#visualRow}.
     *
     * @param node TopologyEntity to retrieve a Node for.
     * @return Node of the TopologyEntity.
     */
    synchronized Node getNode(TopologyEntity node) {
        Tuple n;
        if (node.getVisualRow() == -1) {
            
            n = graph.addNode()
                    .set(DATA, node)
                    .set(HASH, node.hashCode())
                    .set(ENTITY, node.type)
                    .set(ICON, getIcon(node))
                    .set(TEXT, node.getName());

            if (Type.CLOUD.equals(node.type)) {
                n.set(TEXT, "Transparent Topology");
            }

            node.setVisualRow(n.getRow());
        } else {
            n = graph.getNode(node.getVisualRow());
        }
        return (Node) n;
    }

    /**
     * Retrieve an Image for the {@link TopologyEntity#type}.
     *
     * @param entity TopologyEntity to retrieve an Image for.
     * @return Image for a
     * {@link Type#CLOUD}, {@link Type#HOST}, {@link Type#SWITCH}, else null.
     */
    private Image getIcon(TopologyEntity entity) {
        Type t = entity.type;
        Image image = null;
        if (Type.CLOUD.equals(t)) {
            image = Icons.Original_cloud.get128();
        } else if (Type.HOST.equals(t)) {
            image = Icons.Original_host.get64();
        } else if (Type.SWITCH.equals(t)) {
            image = Icons.Original_port.get64();
        }
        return image;
    }

    /**
     * Temporarily pauses {@link #USER_MODE} and runs the {@link #ANIMATE}
     * action. {@link #USER_MODE} resumes after the layout is finished.
     */
    void animate() {
        if (groups.isEmpty()) {
            return;
        }
        cancel(USER_MODE);
        run(ANIMATE);
    }

    /**
     * Animates the layout, centering the visualization on the {@link #withFocusItem(java.util.function.Consumer)
     * } item.
     */
    public void centerFocus() {
        withFocusItem(item -> {
            Node node = (Node) item.getSourceTuple();
            setFocus(getVisualProxy(node));
            graph.getSpanningTree(node);
            animate();
        });
    }

    /**
     * Toggle on or off the {@link #EXPAND} field of the {@link #withFocusItem(java.util.function.Consumer)
     * } item.
     */
    public void toggleFocusExpansion() {
        withFocusItem(item -> {
            setExpand(item, !isExpanded(item));
        });
    }

    /**
     * Same as {@link #setExpand(prefuse.data.Node, java.lang.Boolean) but accepts a VisualItem.
     *
     * @param node Node within of group or a Key of a group.
     * @param expand True to set the switch-ports of the group to be visible,
     * else false and hidden.
     */
    public void setExpand(VisualItem item, Boolean expand) {
        setExpand((Node) item.getSourceTuple(), expand);
    }

    /**
     * Sets the {@link #EXPAND} field on all nodes from a call to {@link #withGroup(prefuse.data.Node, boolean, java.util.function.Consumer)
     * }.
     *
     * @param node Node within of group or a Key of a group.
     * @param expand True to set the switch-ports of the group to be visible,
     * else false and hidden.
     */
    public void setExpand(Node node, Boolean expand) {
        withGroup(node, false, obj -> {
            ((Node) obj).set(EXPAND, expand);
        });
    }

    /**
     * Same as {@link #withGroup(prefuse.data.Node, boolean, java.util.function.Consumer) but accepts the VisualItem.
     *
     * @param node Node for a Key or in a List within a value of a {@link #getGroups() }.
     * @param visualItems If true will convert each Node to its VisualItem by
     * calling {@link #getVisualNode(prefuse.data.Tuple) }.
     * @param cb Callback to accept each group item including the Key.
     */
    void withGroup(VisualItem item, boolean visualItems, Consumer cb) {
        withGroup((Node) item.getSourceTuple(), visualItems, cb);
    }

    /**
     * Applies the callback to each VisualItem in the group where the Node is a
     * value or key.
     *
     * @param node Node for a Key or in a List within a value of a {@link #getGroups()
     * }.
     * @param visualItems If true will convert each Node to its VisualItem by
     * calling {@link #getVisualNode(prefuse.data.Tuple) }.
     * @param cb Callback to accept each group item including the Key.
     */
    void withGroup(Node node, boolean visualItems, Consumer cb) {
        Consumer callback;
        if (visualItems) {
            callback = (n) -> {
                cb.accept(getVisualNode((Tuple) n));
            };
        } else {
            callback = cb;
        }

        Node proxy = getProxy(node);
        callback.accept(proxy);
        if (groups.containsKey(proxy)) {
            groups.get(proxy).forEach(callback);
        }
    }

    /**
     * Same as {@link #getProxy(prefuse.data.Node) }, but returns the VisualItem
     * of the return value.
     *
     * @param node Node to retrieve the proxy of.
     * @return VisualItem of the proxy if one exists, or the original node if it
     * does not exist as a value in a group.
     */
    VisualItem getVisualProxy(Node node) {
        return getVisualNode(getProxy(node));
    }

    /**
     * Gets the appropriate "owner" or parent node if the argument is in a value
     * of {@link #getGroups() }.
     *
     * @param node Node to retrieve the proxy of.
     * @return Proxy if one exists, or the original node if it does not exist as
     * a value in a group.
     */
    Node getProxy(Node node) {
        Node ret;
        if (isHost(node) || groups.containsKey(node)) {
            ret = node;
        } else {
            int row = node.getInt(PARENT_ROW);
            ret = graph.getNode(row);
        }
        return ret;
    }

    /**
     * checks if the {@link #ENTITY} field is equivalent to {@link Type#PORT}.
     *
     * @param node Node to check.
     * @return True if the fields are equal, else false.
     */
    public static boolean isPort(Node node) {
        return Type.PORT.name().equals(node.getString(ENTITY));
    }

    /**
     * checks if the {@link #ENTITY} field is equivalent to {@link Type#HOST}.
     *
     * @param node Node to check.
     * @return True if the fields are equal, else false.
     */
    public static boolean isHost(Node node) {
        return Type.HOST.name().equals(node.getString(ENTITY));
    }

    /**
     * Sets the {@link #focusItem}, is retrieved on a call to {@link #withFocusItem(java.util.function.Consumer)
     * }.
     *
     * @param focusItem new focusItem to set.
     */
    public void setFocusItem(VisualItem focusItem) {
        this.focusItem = focusItem;
    }

    /**
     * Sets the {@link #focusSubItem} used when finding a port in the tree view.
     *
     * @param focusSubItem new focusSubItem to set.
     */
    public void setFocusSubItem(VisualItem focusSubItem) {
        this.focusSubItem = focusSubItem;
    }

    /**
     * In an atomic-fashion, applies the callback to the last item from the last
     * call to {@link #setFocusItem(prefuse.visual.VisualItem) }.
     *
     * @param cb Callback to accept the item.
     */
    void withFocusItem(Consumer<VisualItem> cb) {
        VisualItem item = this.focusItem;
        if (item != null && PhysicalVisualization.isNodeGroup(item)) {
            cb.accept(item);
        }
    }

    /**
     * Gets the source data for the focus node.
     *
     * @param cb Callback accepts the underlying data for a node if it
     * implements the Entities interface.
     */
    void withFocusData(Consumer<Entities> cb) {
        VisualItem item = this.focusItem;
        if (item != null && PhysicalVisualization.isNodeGroup(item)) {
            Node node = (Node) item.getSourceTuple();
            withData(node, cb);
        }

    }

    /**
     * Gets the source data for a node.
     *
     * @param cb Callback accepts the underlying data for a node if it
     * implements the Entities interface.
     */
    void withData(Node node, Consumer<Entities> cb) {
        Object obj;
        if (node.canGet(DATA, Entities.class) && (obj = node.get(DATA)) != null) {
            cb.accept((Entities) obj);
        } else {
            System.err.println("Cannot locate item.");
            LogEmitter.factory.get().emit(node, Core.ALERT.WARNING, "Cannot locate item.");
        }
    }

    /**
     * Runs the callback on all switch nodes within the aggregate (WILL BE ONLY
     * ONE).
     *
     * @param item Aggregate item to retrieve items from.
     * @param cb Callback to accept any visual items that match
     * {@link #IS_SWITCH_PREDICATE}.
     */
    public void withVisualItem(AggregateItem item, Consumer<VisualItem> cb) {
        Iterator it = item.items(IS_SWITCH_PREDICATE);
        if (it.hasNext()) {
            cb.accept((VisualItem) it.next());
        }
    }

    /**
     * Expands or collapses all switch-ports.
     *
     * @param expand True will cause all switch-ports to display, false will
     * cause them to collapse.
     */
    void expandAll(boolean expand) {
        groups.keySet().forEach(node -> setExpand(node, expand));
    }

    /**
     * Sets the focus item for the graph, this is used as the tree root when the
     * layout is ran.
     *
     * @param item Item to be at the center of the graph.
     */
    void setFocus(VisualItem item) {
        TupleSet focus = getGroup(Visualization.FOCUS_ITEMS);
        focus.clear();
        focus.addTuple(item);
        setFocusItem(item);
    }

    /**
     * Shortcut for {@link #fieldMatch(java.lang.String, java.lang.String, boolean)
     * } for object literals.
     *
     * @param field Field of the right side value.
     * @param literal Literal value to test for equality.
     * @return Equality predicate testing the provided field against the
     * provided literal.
     */
    static Predicate fieldEquals(String field, Object literal) {
        return fieldMatch(field, literal.toString(), false);
    }

    /**
     * Creates a literal or string literal expression and returns the {@link prefuse.data.expression.parser.ExpressionParser#predicate(java.lang.String)
     * } value.
     *
     * @param field Field of the right side value.
     * @param value Value to test for equality.
     * @param isString True if the value should be compared as a String (ie.
     * Enum).
     * @return Equality predicate testing the provided field against the
     * provided value.
     */
    static Predicate fieldMatch(String field, String value, boolean isString) {
        String expression;
        if (isString) {
            expression = String.format("%s == '%s'", field, value);
        } else {
            expression = String.format("%s == %s", field, value);
        }
        return ExpressionParser.predicate(expression);
    }

    /**
     * Enum is compared as string values
     */
    static Predicate fieldMatch(String field, Enum value) {
        return fieldMatch(field, value.name(), true);
    }

    /**
     * Gets the VisualItem for a node.
     *
     * @param n Node to retrieve VisualItem of.
     * @return VisualItem of the node.
     */
    public VisualItem getVisualNode(Tuple n) {
        return getVisualItem(NODES, n);
    }

    /**
     * @param item Item to retrieve the {@link #EXPAND} value of.
     * @return true if expanded, else false. False on failure.
     */
    public boolean isExpanded(VisualItem item) {
        boolean ret = false;
        if (item.canGetBoolean(EXPAND)) {
            ret = item.getBoolean(EXPAND);
        }
        return ret;
    }

    /**
     * @param item Item to retrieve the Type value of.
     * @return Type of the VisualItem or {@link Type#DEFAULT} on failure.
     */
    public static Type getType(VisualItem item) {
        String name = item.getString(PhysicalVisualization.ENTITY);
        Type type;

        try {
            type = TopologyEntity.Type.valueOf(name);
        } catch (Exception ex) {
            Logger.getLogger(PhysicalVisualization.class.getName()).log(Level.SEVERE, "Failed to get TopologyEntity.Type", ex);
            type = Type.DEFAULT;
        }

        return type;
    }

    /**
     * @param item VisualItem to check is {@link prefuse.visual.VisualItem#getGroup()
     * }.
     * @return
     */
    public static boolean isNodeGroup(VisualItem item) {
        return item == null ? false : PhysicalVisualization.NODES.equals(item.getGroup());
    }

    /**
     * Expecting a visualItem, usually an aggregate, will return the switch node
     * which is a parent within the aggregate.
     *
     * @param item VisualItem or aggregate item.
     * @return VisualItem of a switch-Node parent or Host node if the argument
     * if the visualItem of a host node.
     */
    VisualItem adaptAggregate(VisualItem item) {
        if (item instanceof AggregateItem) {
            VisualItem[] proxy = {item};
            withVisualItem((AggregateItem) item, newItem -> {
                proxy[0] = newItem;
            });
            item = proxy[0];
        }
        return item;
    }

    /**
     * Get the {@link #groups} map which maps all ports to the parent switch
     * node.
     *
     * @return
     */
    public Map<Node, List<Node>> getGroups() {
        return groups;
    }

    /**
     * Calls {@link #pause() } then clears all data in the visualization.
     */
    public void clear() {
        pause();
        getGroups().clear();
        aggr().clear();
        graph.getEdgeTable().clear();
        graph.getNodeTable().clear();
    }

    /**
     * @return True if this visualization has no data.
     */
    public boolean isEmpty() {
        return graph.getTupleCount() == 0;
    }

    /**
     * Stops all actions and stores them to be run later on a call to {@link #resume()
     * }.
     */
    void pause() {
        resumeActions.clear();
        Object[] keys = getActions().keys();
        for (Object obj : keys) {
            String key = obj.toString();
            if (getAction(key).isRunning()) {
                resumeActions.add(key);
                cancel(key);
            }
        }
    }

    /**
     * Resumes all paused actions from a call to {@link #pause() }.
     */
    void resume() {
        resumeActions.forEach(this::run);
        resumeActions.clear();
    }

}
