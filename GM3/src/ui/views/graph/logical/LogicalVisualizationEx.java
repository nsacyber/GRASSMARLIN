/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.ColorAnimator;
import prefuse.action.animate.PolarLocationAnimator;
import prefuse.action.animate.QualityControlAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.activity.Activity;
import prefuse.activity.SlowInSlowOutPacer;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.PolygonRenderer;
import prefuse.render.Renderer;
import prefuse.visual.*;
import prefuse.visual.expression.InGroupPredicate;
import ui.views.graph.AggregateLayout;
import ui.views.graph.QueueAction;
import ui.views.graph.logical.watch.Watch;
import ui.views.tree.visualnode.PeerVisualNode;
import ui.views.tree.visualnode.VisualNode;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author BESTDOG Updated visualization for GM3 logical graph.
 *         The main Logical visualization has a visual ID of -1. {@link #DEFAULT_VISUAL_ID}
 */
public class LogicalVisualizationEx extends Visualization implements Watch {
    public static final int DEFAULT_VISUAL_ID = -1;

    public final LogicalParameters PARAM;
    public final JLabel counter;

    final Graph graph;
    final AggregateTable aggregateTable;
    final VisualGraph visualGraph;
    final HashSet<VisualItem> prefixSearch;

    /**
     * A queue of actions which help integrate Prefuse interaction with Swing
     */
    final QueueAction queue;
    /**
     *
     */
    private final StyledEdgeRenderer edgeRenderer;
    /**
     * Node with row == 0, nullable
     */
    Node root;
    /**
     * the id of this view, used to distinguish is from other views sharing the
     * same data objects.
     */
    int viewId;
    /**
     * maps tuple rows back to their originating data object
     */
    ConcurrentHashMap<Integer, VisualNode> map;
    /**
     * contains all clear methods which must be run consecutively
     */
    List<Runnable> clearList;
    private boolean networkVisibility;
    private int hostsInNetworkThreshold;
    /**
     * The current prefix being searched for, nullable.
     */
    private String searchString;

    /** main radial layout */
    RadialTreeLayout layout;

    public LogicalVisualizationEx() {
        super();
        setId(DEFAULT_VISUAL_ID);
        hostsInNetworkThreshold = 10;
        networkVisibility = true;
        PARAM = new LogicalParameters();
        map = new ConcurrentHashMap<>(); // contains access to original data
        edgeRenderer = new StyledEdgeRenderer(this::map);
        clearList = new ArrayList<>();
        graph = new Graph();
        queue = new QueueAction();
        counter = new JLabel();
        prefixSearch = new HashSet();

        /**
         * visual item table for drawing the aggregate hulls
         */
        aggregateTable = addAggregates(PARAM.AGGR);
        aggregateTable.addColumn(VisualItem.POLYGON, float[].class);
        aggregateTable.addColumn(PARAM.AGGR_COLOR, int.class, -1);

        clearList.add(queue::clear);
        clearList.add(prefixSearch::clear);
        clearList.add(edgeRenderer::clear);
        clearList.add(aggregateTable::clear);
        clearList.add(graph::clear);
        clearList.add(map::clear);

        visualGraph = addGraph(PARAM.GRAPH, graph);

        initVisualization();
    }

    public void toggleTrafficRatio(Object e) {
        this.edgeRenderer.setRatioVisible(!this.edgeRenderer.isRatioVisible());
    }

    public void checkThresholds() {
        runLater(this::checkThresoldsInternal);
    }

    public void resetThresholds() {
        runLater(this::resetThresholdsInternal);
    }

    public void resetThresholdsInternal() {
        final int threshold = this.getHostThreshold();
        Predicate<VisualNode> shouldExpand = p -> p.getChildCount() < threshold;
        map.values().stream()
                .filter(VisualNode::isNetwork)
                .forEach(node
                                -> setExpanded(node, shouldExpand.test(node))
                );
    }

    private void checkThresoldsInternal() {
        final int threshold = this.getHostThreshold();
        Predicate<VisualNode> shouldExpand = p -> p.getChildCount() >= threshold;
        map.values().stream().filter(VisualNode::isNetwork)
                .filter(shouldExpand)
                .forEach(node
                                -> setExpanded(node, false)
                );
    }

    public int getHostThreshold() {
        return hostsInNetworkThreshold;
    }

    public void setHostThreshold(int hostsInNetworkThreshold) {
        this.hostsInNetworkThreshold = hostsInNetworkThreshold;
        checkThresholds();
    }

    protected void initVisualization() {
        Renderer polyRender = new PolygonRenderer(Constants.POLY_TYPE_CURVE);
        ((PolygonRenderer) polyRender).setCurveSlack(0.05f);
        ColorAction aggregateColorAction = new AggregateColorAction(PARAM.AGGR, PARAM.AGGR_COLOR, PARAM.aggregatePalette);

        MapRenderer mapRenderer = new MapRenderer(this::map);
        mapRenderer.setRenderType(AbstractShapeRenderer.RENDER_TYPE_FILL);
        mapRenderer.setMaxImageDimensions(32, 32);
        mapRenderer.setHorizontalPadding(1);
        mapRenderer.setRoundedCorner(8, 8);
        mapRenderer.setVerticalPadding(1);

        DefaultRendererFactory rendererFactory = new DefaultRendererFactory(mapRenderer);
        rendererFactory.add(new InGroupPredicate(PARAM.AGGR), polyRender);
        rendererFactory.setDefaultEdgeRenderer(edgeRenderer);
        setRendererFactory(rendererFactory);

        LogicalVisibilityCounter counterAction = new LogicalVisibilityCounter(counter::setText, counter::setToolTipText);
        counterAction.setIsNetworkPredicate(this::isNetwork);
        counterAction.setGroup(PARAM.NODES);

        /**
         * Action will colorize text when hovered, searched, or focused
         */
        ColorAction textColorAction = new FontColorAction(PARAM.NODES, VisualItem.TEXTCOLOR, Color.BLACK.getRGB());
        textColorAction.add(PARAM.hover, Color.RED.getRGB());
        textColorAction.add(PARAM.searchGroup, Color.RED.getRGB());
        textColorAction.add(PARAM.focusGroup, Color.RED.getRGB());

        ColorAction nodeColorAction = new MapColorAction(PARAM.NODES, this::map, this::inSearch, "NodeColorAction");
        ColorAction edgeColorAction = new BoundAwareColorAction(PARAM.EDGES, VisualItem.STROKECOLOR, Color.BLACK.getRGB());

        AggregateLayout adjustAggregate = new AggregateLayout(PARAM.AGGR);

        ActionList paint = new ActionList();
        paint.add(queue); // run all queued actions before painting
        paint.add(aggregateColorAction);
        paint.add(nodeColorAction);
        paint.add(textColorAction);
        paint.add(edgeColorAction);
        paint.add(counterAction);

        putAction(PARAM.REPAINT, paint);
        putAction(PARAM.REPAINT_NO, paint);

        ActionList colorAnimator = new ActionList(Activity.INFINITY);
        colorAnimator.add(adjustAggregate);
        colorAnimator.add(new ColorAnimator(PARAM.NODES));
        colorAnimator.add(new ColorAnimator(PARAM.EDGES));
        colorAnimator.add(paint); // required to make entities draw in the correct order
        colorAnimator.add(new RepaintAction()); // required to make entities draw in the correct order
        putAction(PARAM.USERMODE, colorAnimator);

        layout = new RadialTreeLayout(PARAM.GRAPH, 270);
        layout.setAutoScale(true);
//        TreeForceLayout layout = new TreeForceLayout(PARAM.GRAPH, this::streamNodes, this::streamStructureEdges);
//        colorAnimator.add(layout);
        putAction(PARAM.LAYOUT, layout);

        ActionList filter = new ActionList();
        filter.add(queue);
        filter.add(new TreeRootAction(PARAM.GRAPH));
        filter.add(layout); // remove for FDL
        filter.add(paint);
        putAction(PARAM.FILTER, filter);

        ActionList animate = new ActionList(PARAM.FILTER_DURATION);
        animate.add(queue);
        animate.setPacingFunction(new SlowInSlowOutPacer()); // remove for FDL
        animate.add(new PolarLocationAnimator(PARAM.NODES, PARAM.LINEAR)); // remove for FDL
        animate.add(adjustAggregate);
        animate.add(new RepaintAction());
        putAction(PARAM.ANIMATE, animate);

        alwaysRunAfter(PARAM.FILTER, PARAM.ANIMATE);
        alwaysRunAfter(PARAM.ANIMATE, PARAM.USERMODE);

        addFocusGroup(PARAM.LINEAR, new DefaultTupleSet());
        getGroup(Visualization.FOCUS_ITEMS).addTupleSetListener(this::linearFilter);
    }

    /** controls the auto-scaling of the visualization */
    public void setAutoscale(boolean enabled) {
        this.layout.setAutoScale(enabled);
    }

    public boolean isAutoscaleEnabled() {
        return this.layout.getAutoScale();
    }

    private boolean inSearch(VisualItem item) {
        return this.prefixSearch.contains(item);
    }

    private VisualNode map(int row) {
        return map.get(row);
    }

    public VisualNode map(Tuple tuple) {
        return map(tuple.getRow());
    }

    private boolean isNetwork(Tuple tuple) {
        return map(tuple.getRow()).isNetwork();
    }

    private boolean isHost(Tuple tuple) {
        return map(tuple.getRow()).isHost();
    }

    private void linearFilter(TupleSet tuples, Tuple[] add, Tuple[] remaining) {
        TupleSet linear = getGroup(PARAM.LINEAR);
        if (add.length < 1) {
            return;
        }
        linear.clear();
        if (add[0] instanceof Node) {
            for (Node n = (Node) add[0]; n != null; n = n.getParent()) {
                linear.addTuple(n);
            }
        }
    }

    public Tuple getNode(VisualNode visualNode) {
        Tuple tuple = null;
        if (visualNode.isNetwork()) {
            tuple = getNetwork(visualNode);
        } else if (visualNode.isHost()) {
            tuple = getHost(visualNode);
        } else if (visualNode.isRoot()) {
            tuple = getRoot(visualNode);
        }
        return tuple;
    }

    public Stream<VisualNode> getHosts() {
        return map.values().stream().filter(VisualNode::isHost);
    }

    private Tuple getRoot(VisualNode visualNode) {
        Tuple tuple = getRoot();
        put(0, visualNode);
        return tuple;
    }

    private Tuple getNetwork(VisualNode visualNode) {
        Tuple tuple;
        int row = visualNode.getVisualRow(getId());
        if (row == -1) {
            tuple = graph.addNode();
            row = tuple.getRow();
            visualNode.setVisualRow(getId(), row);
            put(row, visualNode);

            /**
             * connect to root and set edge invisible
             */
            addEdge(root, tuple).setVisible(false);

            /**
             * create aggregate
             */
            VisualItem visualItem = getVisualItem(PARAM.NODES, tuple);
            AggregateItem agg = (AggregateItem) aggregateTable.addItem();
            agg.addItem(visualItem);
            visualNode.setVisualAgg(getId(), agg.getRow());

            if (!networksAreVisible()) {
                visualItem.setVisible(false);
            }

            /**
             * update current search if there is one
             */
            checkSearchItem(visualNode, visualItem);

            /**
             * makes networks appear to come from the root
             */
            moveToOtherNode(visualItem, getVisualItem(PARAM.NODES, getRoot()));

        } else {
            tuple = graph.getNode(row);
            VisualItem visualItem = getVisualItem(PARAM.NODES, tuple);
            if (!networksAreVisible()) {
                visualItem.setVisible(false);
            }
        }
        return tuple;
    }

    private void setVisible(Predicate<VisualNode> predicate, boolean visible) {
        runLater(() -> {
            map.values()
                    .stream()
                    .filter(predicate)
                    .map(this::getVisualItem)
                    .forEach(item
                                    -> item.setVisible(visible)
                    );
        });
    }

    private Tuple getHost(VisualNode visualNode) {
        Tuple tuple;
        int row = visualNode.getVisualRow(getId());
        if (row == -1) {
            tuple = graph.addNode();
            row = tuple.getRow();
            visualNode.setVisualRow(getId(), row);
            put(row, visualNode);

            /**
             * connect to parent node and set edge invisible
             */
            VisualNode parentNode = visualNode.getParent();
            Tuple parentTuple = getNode(parentNode);
            VisualItem parentItem = getVisualItem(PARAM.NODES, parentTuple);
            int parentRow = parentTuple.getRow();
            addEdge(parentRow, row).setVisible(false);

            /**
             * locate host within network aggregation
             */
            VisualItem visualItem = getVisualItem(PARAM.NODES, tuple);
            int aggRow = visualNode.getVisualAgg(getId());
            AggregateItem agg = (AggregateItem) aggregateTable.getItem(aggRow);
            agg.addItem(visualItem);


            /**
             * update current search if there is one
             */
            checkSearchItem(visualNode, visualItem);


            if (!agg.isVisible()) {
                /** host enters a network which is hidden, so set its expand state manually and forward it if necessary */
                visualItem.setVisible(false);
                if (!parentNode.isExpanded(getId())) {
                    edgeRenderer.forward(row, parentItem);
                    visualNode.setExpanded(getId(), false);
                }
            } else {
                /* makes host appear to come out of network nodes */
                moveToOtherNode(visualItem, parentItem);
                setExpanded(visualNode, parentNode.isExpanded(getId()));
            }

        } else {
            tuple = graph.getNodeTable().getTuple(row);
        }
        return tuple;
    }

    public void put(int row, VisualNode visualNode) {
        if (row > graph.getTupleCount()) {
            throw new java.lang.IllegalArgumentException("Row does not exist in the current graph");
        }
        this.map.put(row, visualNode);
    }

    /**
     * Safely clears the visualization
     */
    public void clear() {
        runLater(this::clearInternal);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * runs each clear method in the {@link #clearList}
     */
    private void clearInternal() {
        clearList.forEach(Runnable::run);
    }

    /**
     * Adds a Runnable to the {@link #queue}.
     */
    public void runLater(Runnable action) {
        queue.add(action);
    }

    public void pause() {
        queue.add(queue::clear);
        cancel(PARAM.USERMODE);
    }

    public void resume() {
        run(PARAM.USERMODE);
    }

    public void autoLayout() {
        this.cancel(PARAM.USERMODE);
        this.run(PARAM.FILTER);
    }

    public void start() {
        run(PARAM.USERMODE);
    }

    public VisualNode getVisualRoot() {
        return map(0);
    }

    public Node getRoot() {
        if (this.graph.getTupleCount() == 0) {
            root = graph.addNode();
            ((VisualItem) this.visualGraph.getNode(0)).setInteractive(false);
        }
        return root;
    }

    public void setNetworkVisibility(boolean networkVisibility) {
        this.networkVisibility = networkVisibility;
        setVisible(VisualNode::isNetwork, networkVisibility);
    }

    private boolean networksAreVisible() {
        return this.networkVisibility;
    }

    public void validateEdges() {
        runLater(this::validateEdgesInternal);
    }

    private void validateEdgesInternal() {
        map.forEach((row, node) -> {
            if (node.isHost()) {

                Node tuple = graph.getNode(row);
                int expected = node.getChildCount();
                int actual = tuple.getDegree() - 1; // -1 to adjust for invisible edge to network

                if (expected != actual) {
                    node.getChildren().forEach(proxy -> {
                        if (proxy instanceof PeerVisualNode) {
                            VisualNode peer = ((PeerVisualNode) proxy).getOriginal();
                            if (canConnect(node, peer)) {
                                int peerRow = getNode(peer).getRow();

                                if (!edgeExists(row, peerRow)) {
                                    addEdge(row, peerRow);
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    public Graph getGraph() {
        return graph;
    }

    protected boolean canConnect(VisualNode node, VisualNode peer) {
        return true;
    }

    private VisualItem addEdge(Tuple t1, Tuple t2) {
        return addEdge(t1.getRow(), t2.getRow());
    }

    private VisualItem addEdge(int rowA, int rowB) {
        int row = graph.addEdge(rowA, rowB);
        Edge e = graph.getEdge(row);
        VisualItem v = this.getVisualItem(PARAM.EDGES, e);
        v.setInteractive(false);
        return v;
    }

    private boolean edgeExists(int rowA, int rowB) {
        boolean exists = graph.getEdge(rowA, rowB) != -1;
        if (!exists) {
            exists = graph.getEdge(rowB, rowA) != -1;
        }
        return exists;
    }

    private void moveToOtherNode(VisualItem target, VisualItem destination) {
        target.setX(destination.getX());
        target.setY(destination.getY());
    }

    public VisualItem getVisualItem(VisualNode node) {
        return getVisualItem(PARAM.NODES, graph.getNode(node.getVisualRow(getId())));
    }

    /**
     * @param visualNode Sets the expanded state of a single network.
     * @param expanded   True to expand, else false and collapsed.
     */
    public void setExpanded(final VisualNode visualNode, final boolean expanded) {
        final VisualNode node = visualNode.isHost() ? visualNode.getParent() : visualNode;
        runLater(() -> doExpansion(node, expanded));
    }

    /**
     * Sets the expanded state of all current network nodes, this effect is not
     * persistent.
     *
     * @param expanded True to expand all networks, else false and collapsed.
     */
    public void setAllExpanded(final boolean expanded) {
        Predicate<VisualNode> predicate = p -> p.isExpanded(getId()) != expanded;
        List<Runnable> actions = new ArrayList<>();
        getVisualRoot()
                .getChildren()
                .stream()
                .filter(predicate)
                .forEach(network
                                -> actions.add(()
                                        -> doExpansion(network, expanded)
                        )
                );
        runLater(() -> actions.forEach(Runnable::run));
    }

    /**
     * must not call outside of {@link #setExpanded(ui.views.tree.visualnode.VisualNode, boolean)
     * }
     */
    private void doExpansion(final VisualNode node, final boolean expanded) {
        if (inView(node)) {
            final VisualItem parent = this.getVisualItem(node);
            node.setExpanded(getId(), expanded);
            node.getChildren()
                    .stream()
                    .filter(this::inView)
                    .map(this::getVisualItem)
                    .forEach(visualItem -> {
                        visualItem.setVisible(expanded);
                        if (expanded) {
                            edgeRenderer.unforward(visualItem.getRow());
                        } else {
                            edgeRenderer.forward(visualItem.getRow(), parent);
                            parent.setVisible(true);
                        }
                    });
        }
    }

    /**
     * Sets all nodes which are not present in {@link #edgeRenderer}
     *
     * @param visible Sets all items visible if true, else false and hidden.
     */
    public void setAllVisible(boolean visible) {
        final boolean networksVisible = this.networksAreVisible();
        /* for all hosts and networks */
        final Predicate<VisualNode> isValid = p -> p.isNetwork() || p.isHost();
        /* and only networks that are are candidates */
        final Predicate<VisualNode> optionalNetworks = p -> !p.isNetwork() || (p.isNetwork() && networksVisible);
        /* and no nodes which are collapsed (preserves collapse state) */
        final Predicate<VisualNode> notForwarding = p -> !this.edgeRenderer.willForward(p.getVisualRow(getId()));
        /* set the visibility */
        final Consumer<VisualItem> setItem = p -> p.setVisible(visible);
        runLater(() -> {
            map.values()
                    .stream()
                    .filter(isValid)
                    .filter(optionalNetworks)
                    .filter(notForwarding)
                    .map(this::getVisualItem)
                    .forEach(setItem);
        });
    }

    /**
     * Checks if a node has a row set by this visualization.
     *
     * @param node Node to check.
     * @return True if {@link VisualNode#getVisualRow(int) }, return false.
     */
    private boolean inView(VisualNode node) {
        return node.getVisualRow(getId()) != -1;
    }

    /**
     * @return The VisualItem of the node on row 0.
     */
    private VisualItem getVisualRootItem() {
        Tuple t = graph.getNode(0);
        return this.getVisualItem(PARAM.NODES, t);
    }

    /**
     * Sets the visualization to center on the VisualItem provided instead of
     * the root node.
     *
     * @param node Node to center and focus on.
     */
    void setFocus(final VisualItem node) {
        this.runLater(() -> {
            VisualItem visualItem;
            if (node != null) {
                if (edgeRenderer.willForward(node.getRow())) {
                    visualItem = edgeRenderer.get(node.getRow());
                } else {
                    visualItem = node;
                }
            } else {
                visualItem = getVisualRootItem();
            }
            TupleSet focus = getGroup(Visualization.FOCUS_ITEMS);
            focus.clear();
            focus.addTuple(visualItem);
            autoLayout();
        });
    }

    /**
     * Will focus on a node.
     *
     * @param visualNode VisualNode to focus on.
     */
    public void setFocus(final VisualNode visualNode) {
        if (visualNode.getVisualRow(getId()) != -1) {
            setFocus(getVisualItem(visualNode));
        }
    }

    /**
     * Sets a nodes visibility on the {@link #queue} thread.
     *
     * @param node    Item which will be set
     * @param visible True to show item, else false and hidden.
     */
    void setVisible(VisualItem node, boolean visible) {
        runLater(() -> {
            VisualNode visualNode = map(node);
            this.setVisible(visualNode, node, visible);
        });
    }

    private void setVisible(VisualNode visualNode, VisualItem visualItem, boolean visible) {
        if (visible) {
            edgeRenderer.unforward(visualItem.getRow());
        }
        visualItem.setVisible(visible);
        if (visualNode.isNetwork()) {
            Consumer<VisualItem> setVisible = p -> p.setVisible(visible);
            visualNode.getChildren()
                    .stream()
                    .map(this::getVisualItem)
                    .forEach(setVisible);
        }
    }

    /**
     * Sets focus on the AggregateItem the item parameter is contained in.
     *
     * @param item Item within the aggregate.
     * @param b    parameter passed to {@link VisualItem#setHover(boolean) }.
     */
    void hoverAggregate(VisualItem item, boolean b) {
        if (!PARAM.AGGR.equals(item.getGroup())) {
            VisualNode node = map(item);
            if (node != null && (node.isNetwork() || node.isHost())) {
                aggregateTable.getItem(node.getVisualAgg(getId())).setHover(b);
            }
        }
    }

    public boolean isCurveVisible() {
        return this.edgeRenderer.isCurveVisible();
    }

    public void setEdgeCurve(boolean curve) {
        this.edgeRenderer.setCurveVisible(curve);
    }

    @Override
    public int getId() {
        return viewId;
    }

    @Override
    public void setId(int id) {
        viewId = id;
    }

    @Override
    public void update(VisualNode root) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Retrieves the data-tuple for a specific node.
     *
     * @param node VisualNode to get the {@link #graph} tuple of.
     * @return Tuple for the node.
     */
    private Tuple getTupleItem(VisualNode node) {
        return graph.getNode(node.getVisualRow(getId()));
    }

    /**
     * Same as {@link #getTupleItem(VisualNode)} but cast to a {@link prefuse.data.Node}.
     *
     * @param node node VisualNode to get the {@link #graph} tuple of.
     * @return Tuple for the node.
     */
    private Node getNodeItem(VisualNode node) {
        return graph.getNode(node.getVisualRow(getId()));
    }

    /**
     * Should be invoked on {@link #runLater(java.lang.Runnable) } queue.
     *
     * @param string String to search for.
     */
    public void search(String string) {
        searchString = string;
        prefixSearch.clear();
        if (!string.isEmpty()) {
            Predicate<VisualNode> startsWith = p -> p.getName().startsWith(string);
            Predicate<VisualNode> isValid = p -> p.isNetwork() || p.isHost();
            map.values()
                    .stream()
                    .filter(isValid)
                    .filter(startsWith)
                    .map(this::getVisualItem)
                    .forEach(prefixSearch::add);
        }
    }

    /**
     * Checks a single node and adds it to the {@link #prefixSearch} if it meets
     * the search criteria.
     *
     * @param node
     * @param item
     */
    private void checkSearchItem(VisualNode node, VisualItem item) {
        String string = searchString;
        if (string != null && !string.isEmpty()) {
            if (node.getName().startsWith(string)) {
                prefixSearch.add(item);
            }
        }
    }

    /**
     * Restores the layout while refocusing on the root node.
     */
    public void restoreLayout() {
        TupleSet focus = getGroup(Visualization.FOCUS_ITEMS);
        focus.clear();
        focus.addTuple(getVisualRootItem());
        autoLayout();
        Consumer<VisualNode> set = p -> p.setExpanded(getId(), true);
        map.values().stream().forEach(set);
        resetThresholds();
    }


    /**
     * Streams nodes used for experimental TreeForceLayout.
     * @return Stream of node VisualItems.
     */
    private Stream<VisualItem> streamNodes() {
        Stream<VisualItem> stream;
        if (this.graph.getTupleCount() > 0) {
            stream = Stream.concat(Stream.of(this.getVisualRootItem()),
                    this.map.values().stream()
                            .filter(VisualNode::hasDetails)
                            .map(this::getVisualItem)
                    .filter(VisualItem::isVisible)
            );
        } else {
            stream = Stream.empty();
        }
        return stream;
    }

    /**
     * Streams Edges used for experimental TreeForceLayout.
     * stream all edges which make up the tree structure, thats all root -> network and network -> host edges.
     */
    private Stream<EdgeItem> streamStructureEdges() {
        Stream<EdgeItem> stream;
        if (this.graph.getTupleCount() > 0) {
            stream = Stream.concat(streamOutEdges(this.getRoot()),
                    this.map.values().stream()
                            .filter(VisualNode::isNetwork)
                            .map(this::getNodeItem)
                            .flatMap(this::streamOutEdges));
        } else {
            stream = Stream.empty();
        }
        return stream;

    }

    /**
     * Streams Edges used for experimental TreeForceLayout.
     * Streams all out edges as VisualItem (EdgeItems).
     * @param node Node to create stream from.
     * @return All VisualItems for out edges.
     */
    private Stream<EdgeItem> streamOutEdges(Node node) {
        Predicate<EdgeItem> invisible = p -> !p.isVisible();

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(node.outEdges(), Spliterator.NONNULL), false)
                .map(e -> this.getVisualItem(PARAM.EDGES, (Tuple) e))
                .map(e -> (EdgeItem) e)
                .filter(invisible);
    }

}
