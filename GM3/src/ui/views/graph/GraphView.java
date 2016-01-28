/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import core.Preferences;
import core.types.InvokeObservable;
import sun.swing.SwingUtilities2;
import ui.GrassMarlin;
import ui.icon.Icons;
import ui.platform.StyledTabbedPaneUI;
import ui.views.graph.logical.LogicalGraphEx;
import ui.views.graph.logical.watch.Watch;
import ui.views.graph.logical.watch.WatchConnections;
import ui.views.graph.logical.watch.WatchNetwork;
import ui.views.tree.TreeView;
import ui.views.tree.visualnode.VisualNode;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
public final class GraphView extends JTabbedPane implements Observer {

    public static boolean DEBUG = false;
    private final int ICON_DIM = 7; // icon is 7 by 7 pixels
    private final InvokeObservable tabChangedObserver;
    private final LogicalGraphEx logicalGraph;
    private final PhysicalGraph physicalGraph;
    private final List<Watch> closeableTabs;
    private final InvokeObservable focusRequest;
    private final InvokeObservable watchRequest;
    private final InvokeObservable subnetChangeRequest;
    private final ConcurrentHashMap<Integer,Rectangle> iconRectangles;

    /** the number of default tabs that will never be removed */
    private final int DEFAULT_TABS = 2;


    public GraphView() {
        super();
        iconRectangles = new ConcurrentHashMap<>();
        focusRequest = new InvokeObservable(this);
        watchRequest = new InvokeObservable(this);
        tabChangedObserver = new InvokeObservable(this);
        subnetChangeRequest = new InvokeObservable(this);

        watchRequest.addObserver(this::observeWatch);

        closeableTabs = new CopyOnWriteArrayList<>();
        logicalGraph = new LogicalGraphEx();
        physicalGraph = new PhysicalGraph();

        setUI(new StyledTabbedPaneUI(iconRectangles::put));
        attachCallbacks(logicalGraph);
        initComponent();
    }

    public InvokeObservable getSubnetChangeRequest() {
        return subnetChangeRequest;
    }

    public InvokeObservable getFocusRequest() {
        return focusRequest;
    }

    public InvokeObservable getWatchRequest() {
        return watchRequest;
    }

    private void attachCallbacks(Watch view) {
        if (view != null) {
            view.setFocusRequest(this.focusRequest)
                    .setSubnetChangeRequest(this.subnetChangeRequest)
                    .setWatchRequest(this.watchRequest);
        }
    }

    void initComponent() {
        addTab("Logical View", logicalGraph, false);
        addTab("Physical View", physicalGraph, false);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                GraphView.this.iconRectangles.forEach((index, rect) -> {
                    if( rect.contains(e.getX(), e.getY()) ) {
                        GraphView.this.removeTabAt(index);
                    }
                });
            }
        });
    }

    @Override
    protected void processEvent(AWTEvent e) {
        super.processEvent(e);
    }

    private void observeWatch(Object nil, Object node) {
        if (node instanceof VisualNode) {
            createWatch((VisualNode) node);
        }
    }

    private void createWatch(VisualNode node) {
        int id = getTabCount() + DEFAULT_TABS;
        Watch lens = null;
        if (node.isNetwork()) {
            lens = new WatchNetwork(id, node);
            addTab(node.getName(), (Component) lens);
            lens.update(logicalGraph.getRoot());
        } else if (node.isHost()) {
            lens = new WatchConnections(id, node);
            addTab(node.getName(), (Component) lens);
            lens.update(logicalGraph.getRoot());
        }
        this.attachCallbacks(lens);
    }

    @Override
    public void removeTabAt(int index) {
        if (index < DEFAULT_TABS) {
            return;
        }

        super.removeTabAt(index);
        super.setSelectedIndex(0);

        this.iconRectangles.clear(); // rectangles will shift, clear all.

        index = index - DEFAULT_TABS;
        if( this.closeableTabs.size() > index ) {
            Watch watch = this.closeableTabs.get(index);
            this.closeableTabs.remove(watch.close());
        }
    }

    /**
     * Adds a tab to the panel.
     * @param title Title of the panel.
     * @param component Component to display when tab is active.
     * @param isClosable If true will add to the {@link #closeableTabs} list, else false and permanent.
     */
    public void addTab(String title, Component component, boolean isClosable) {
        super.addTab(title, component);
        if( isClosable && component instanceof Watch ) {
            closeableTabs.add((Watch) component);
        }
    }

    @Override
    public void addTab(String title, Component component) {
        this.addTab(title, component, true);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof TreeView) {
            VisualNode root = ((TreeView) arg).getRoot();
            logicalGraph.update(root);
            closeableTabs.forEach(view -> view.update(root));
        } else if (arg instanceof VisualNode) {
            VisualNode node = (VisualNode) arg;
            if (DEBUG) {
                System.out.println("Update Graph Item node=" + node.getName());
            }
            logicalGraph.updateNode(node);
        }
    }

    @Override
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        if (index == 0) {
            logicalGraph.vis().resume();
        } else if (index == 1) {
            physicalGraph.requestUpdate();
            logicalGraph.vis().pause();
        }
        tabChangedObserver.setChanged();
        tabChangedObserver.notifyObservers();
    }

    public void clear() {
        logicalGraph.clear();
        physicalGraph.p_vis.clear();
        closeableTabs.clear();
        for (int i = DEFAULT_TABS; i < this.getTabCount(); ++i) {
            removeTabAt(i);
        }
    }

    public void toggleNetworkVisibility(boolean b) {
        logicalGraph.toggleNetworkVisibility(b);
    }

    public GraphView setNetworkCollapseLimit(Integer networkLimit) {
        logicalGraph.setNetworkLimit(networkLimit);
        return this;
    }

    public LogicalGraphEx getLogical() {
        return logicalGraph;
    }

    public PhysicalGraph getPhysical() {
        return physicalGraph;
    }

    public InvokeObservable getTabChangedObserver() {
        return tabChangedObserver;
    }

    public boolean logicalActive() {
        return !getPhysical().isVisible();
    }

    public GraphView setCloudCollapseLimit(Integer cloudLimit) {
        getPhysical().setCloudCollapseLimit(cloudLimit);
        return this;
    }

}
