/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import core.ViewUtils;
import core.types.InvokeObservable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.controls.FocusControl;
import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.visual.AggregateItem;
import prefuse.visual.VisualItem;
import ui.dialog.DialogManager;
import ui.icon.Icons;
import ui.views.graph.GraphController;
import ui.views.graph.GroupDragControl;
import ui.views.graph.logical.watch.Watch;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 */
public class LogicalGraphEx extends javax.swing.JPanel implements GraphController, Watch {

    public static boolean DEBUG;
    final Display display;
    final JTextField searchField;

    final JLabel visibilityCounter;
    final FocusControl focusControl;
    final LogicalVisualizationEx vis;
    InvokeObservable focusRequest;
    InvokeObservable watchRequest;
    InvokeObservable subnetChangeRequest;

    /**
     * Flag set true when updates should be rejected
     */
    private boolean updatesAreLocked;

    public LogicalGraphEx(LogicalVisualizationEx vis) {
        subnetChangeRequest = new InvokeObservable(this);
        focusRequest = new InvokeObservable(this);
        watchRequest = new InvokeObservable(this);
        this.vis = vis;
        display = new Display(vis);
        display.setHighQuality(true);
        visibilityCounter = vis.counter;

        focusControl = new FocusControl(1);

        display.addControlListener(new PanControl()); // lets us click and drag to pan
        display.addControlListener(new GroupDragControl()); // lets us drag the aggs and reshape them
        display.addControlListener(new ZoomControl()); // zoom with scroll wheel
        display.addControlListener(new ControlAdapter() {
            @Override
            public void itemClicked(VisualItem item, MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    LogicalGraphEx.this.contextMenu(item, e);
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    LogicalGraphEx.this.contextMenu(LogicalGraphEx.this.vis(), e);
                }
            }
            @Override
            public void itemEntered(VisualItem item, MouseEvent e) {
                vis().hoverAggregate(item, true);
            }
            @Override
            public void itemExited(VisualItem item, MouseEvent e) {
                vis().hoverAggregate(item, false);
            }
        });
        display.addControlListener(focusControl);
        searchField = new JTextField();
        initComponents();
    }
    
    public LogicalGraphEx() {
        this(new LogicalVisualizationEx());
    }

    public void setNetworkLimit(Integer networkLimit) {
        vis().setHostThreshold(networkLimit);
    }

    @Override
    public String getGroup() {
        return vis.PARAM.GRAPH;
    }
    
    @Override
    public Display getDisplay() {
        return display;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="gui code">
    private void initComponents() {
        javax.swing.JPanel btnContainer;
        javax.swing.JPanel controlPanel;
        javax.swing.JButton fitBtn;
        javax.swing.JButton inBtn;
        javax.swing.JButton outBtn;
        javax.swing.JButton refreshBtn;
        javax.swing.JButton toggleAutoscale;
        javax.swing.JButton settings;
        javax.swing.JButton showAllBtn;
        javax.swing.JButton toggleLineWidths;
        javax.swing.JButton toggleCurvelines;
        javax.swing.JButton toggleQuality;
        controlPanel = new javax.swing.JPanel();
        btnContainer = new javax.swing.JPanel();
        fitBtn = new javax.swing.JButton();
        fitBtn.setToolTipText("Fit to Screen");
        inBtn = new javax.swing.JButton();
        inBtn.setToolTipText("Zoom In");
        outBtn = new javax.swing.JButton();
        outBtn.setToolTipText("Zoom Out");
        refreshBtn = new javax.swing.JButton();
        refreshBtn.setToolTipText("Refresh");
        settings = new javax.swing.JButton();
        settings.setToolTipText("Options");
        showAllBtn = new javax.swing.JButton();
        showAllBtn.setToolTipText("Show All");
        showAllBtn.setIcon(Icons.Eye.getIcon());
        toggleLineWidths = new JButton();
        toggleLineWidths.setToolTipText("Toggle edge thickness");
        toggleLineWidths.setIcon(Icons.Graph_edge.getIcon());
        toggleCurvelines = new JButton();
        toggleCurvelines.setToolTipText("Toggle curved edges");
        toggleCurvelines.setIcon(Icons.Draw_curve.getIcon());
        toggleQuality = new JButton();
        toggleQuality.setToolTipText("Toggle quality");
        toggleQuality.setIcon(Icons.Quality.getIcon());
        toggleAutoscale = new JButton();
        toggleAutoscale.setToolTipText("Toggle Autoscale");
        toggleAutoscale.setIcon(Icons.Autoscale.getIcon());

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new java.awt.BorderLayout());

        add(display, java.awt.BorderLayout.CENTER);

        controlPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        refreshBtn.setIcon(Icons.Refresh.getIcon());
        refreshBtn.addActionListener(this::refresh);
        refreshBtn.setBackground(Color.WHITE);
        fitBtn.setIcon(Icons.Zoomfit.getIcon()); // NOI18N
        fitBtn.addActionListener(this::fit);
        fitBtn.setBackground(Color.WHITE);
        inBtn.setIcon(Icons.Zoomin.getIcon()); // NOI18N
        inBtn.addActionListener(this::zoomIn);
        inBtn.setBackground(Color.WHITE);
        outBtn.setIcon(Icons.Zoomout.getIcon()); // NOI18N
        outBtn.addActionListener(this::zoomOut);
        outBtn.setBackground(Color.WHITE);
        settings.setIcon(Icons.Cog.getIcon());
        settings.addActionListener((e) -> DialogManager.GraphDialog(true));
        toggleLineWidths.addActionListener(vis::toggleTrafficRatio);
        toggleCurvelines.addActionListener( e -> {
            boolean curve = vis.isCurveVisible();
            if( curve ) {
                toggleCurvelines.setIcon(Icons.Draw_curve.getIcon());
            } else {
                toggleCurvelines.setIcon(Icons.Draw_line.getIcon());
            }
            vis.runLater(() -> vis.setEdgeCurve(!curve) );
        });
        toggleQuality.addActionListener(this::toggleQuality);
        toggleAutoscale.addActionListener( e -> {
            boolean autoscale = vis.isAutoscaleEnabled();
            if( autoscale ) {
                toggleAutoscale.setIcon(Icons.Autoscale_off.getIcon());
            } else {
                toggleAutoscale.setIcon(Icons.Autoscale.getIcon());
            }
            vis.runLater(() -> vis.setAutoscale(!autoscale));
        });

        Font font = new java.awt.Font("Tahoma", 0, 14);

        visibilityCounter.setFont(font);
        visibilityCounter.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        visibilityCounter.setText(vis.PARAM.DEFAULT_VISIBILITY_TEXT);
        visibilityCounter.setToolTipText("");
        visibilityCounter.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));

        showAllBtn.addActionListener(this::showAll);
        searchField.setFont(font); // NOI18N
        searchField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        searchField.setText(vis.PARAM.DEFAULT_SEARCH_KEY);
        searchField.setToolTipText(vis.PARAM.DEFAULT_SEARCH_KEY);
        searchField.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 1, 0, 0, Color.GRAY));
        searchField.setOpaque(false);
        searchField.addActionListener((e) -> {
            search(searchField.getText());
        });
        searchField.setForeground(Color.LIGHT_GRAY);
        searchField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                searchField.setText("");
                searchField.setForeground(Color.BLACK);
                searchField.removeMouseListener(this);
            }
        });

        javax.swing.GroupLayout btnContainerLayout = new javax.swing.GroupLayout(btnContainer);
        btnContainer.setLayout(btnContainerLayout);
        btnContainerLayout.setHorizontalGroup(
                btnContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(btnContainerLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(fitBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(inBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(outBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(refreshBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(toggleAutoscale, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(settings, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(showAllBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(toggleLineWidths, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(toggleCurvelines, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(toggleQuality, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(1, 1, 1)
                        .addComponent(searchField, javax.swing.GroupLayout.DEFAULT_SIZE, 478, Short.MAX_VALUE)
                        .addComponent(visibilityCounter, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE))
        );
        btnContainerLayout.setVerticalGroup(
                btnContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, btnContainerLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(btnContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(visibilityCounter, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(btnContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(fitBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(inBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(outBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(refreshBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(toggleAutoscale, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(settings, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(showAllBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(toggleLineWidths, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(toggleCurvelines, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(toggleQuality)
                                )))
        );
        btnContainer.setBackground(Color.WHITE);
        javax.swing.GroupLayout controlPanelLayout = new javax.swing.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
                controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(btnContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        controlPanelLayout.setVerticalGroup(
                controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, controlPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        controlPanel.setBackground(Color.WHITE);
        add(controlPanel, java.awt.BorderLayout.PAGE_END);
    }
    // </editor-fold>

    private void toggleQuality(Object e) {
        this.getDisplay().setHighQuality(!this.getDisplay().isHighQuality());
    }

    private void restore(Object e) {
        vis.runLater(vis::restoreLayout);
    }
    
    private void refresh(Object e) {
        vis.runLater(vis::autoLayout);
    }

    /**
     * Preforms a search, matching item - prefixes are added to the search
     * group. If new data has arrived since the last time a search was ran, the
     * search tuple set, the set of tuples to search, will be updated.
     *
     * @param string Prefix string to search for, the DISPLAY_TEXT FIELD is
     * searched for.
     */
    private void search(String string) {
        if( !vis().isEmpty() ) {
            vis().runLater(()->
                vis().search( string )
            );
        }
    }

    private VisualNode root;
    public VisualNode getRoot() {
        return root;
    }
    
    /**
     * updates all data drawn by the logical view
     * @param root
     */
    @Override
    public void update(VisualNode root) {
        try {
            if (!updatesAreLocked) {
                this.root = root;
                this.vis.getNode(root);
                this.vis.runLater(this::processData);
                this.vis.start();
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to update graph.", ex);
        }
    }

    /**
     * Introduce new data tuples into the graph.
     */
    protected void processData() {
        VisualNode visualRoot = root;
        if (visualRoot != null) {
            visualRoot.getChildren().forEach(network -> {
                vis.getNode(network);

                if (network.isExpanded(vis.getId()) && network.getChildCount() >= vis.getHostThreshold()) {
                    vis.setExpanded(network, false);
                }

                network.getChildren().forEach(host -> {
                    vis.getNode(host);
                });
            });
            vis.validateEdges();
            vis.checkThresholds();
            vis.autoLayout();
        }
    }

    public void updateNode(VisualNode node) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public LogicalVisualizationEx vis() {
        return vis;
    }

    public void clear() {
        vis.clear();
    }

    public void toggleNetworkVisibility(boolean b) {
        this.vis.setNetworkVisibility(b);
    }

    public void exportAll(File file) {
        if( !file.exists() ) {
            file.mkdir();
        }
        
        if(!file.isDirectory()) {
            danger("Export failed. Selected file is not a directory.");
        } else {
            try {
                String path = file.getCanonicalPath() + File.separator;
                
                File image = new File( path + "logical.png" );
                File xml = new File( path + "logical.xml" );
                
                exportImage(image);
                exportXml(xml);
                
            } catch( Exception ex ) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                danger(ex.toString());
            }
        }
    }
    
    public void exportXml(File file) {
        
    }
    
    /** a callback which is pluggable to an observable */
    public void observeFocus(Object nil, Object node) {
        if (node instanceof VisualNode) {
            VisualNode visualNode = (VisualNode) node;
            vis.setFocus(visualNode);
        }
    }

    /** collapses all networks not already collapsed */
    private void collapseAll(Object nil) {
        vis().setAllExpanded(false);
    }
    /** expands all networks not already expanded */
    private void expandAll(Object nil) {
        vis().setAllExpanded(true);
    }
    /** sets all items visible */
    private void showAll(Object e) {
       vis().setAllVisible(true);
    }
    /**
     * creates and displays a context menu for an node or aggregate at the given
     * events location on screen.
     */
    private void contextMenu(VisualItem item, MouseEvent e) {
        try {
            if (item instanceof AggregateItem) {
                item = (VisualItem) ((AggregateItem) item).items().next();
            }
            VisualNode node = this.vis.map.get(item.getRow());
            if (node != null) {
                contextMenu(vis, node, item, e);
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Unknown visual item", ex);
        }
    }

    /**
     * creates and displays a context menu for an node or aggregate at the given
     * events location on screen.
     */
    private void contextMenu(LogicalVisualizationEx visualization, VisualNode node, VisualItem item, MouseEvent mouseEvent) {
        JPopupMenu menu = new JPopupMenu();
        if (node.isHost()) {
            menu.add(new JMenuItem("Show host details")).addActionListener(e -> {
                ViewUtils.showDetailEditor(node, Objects::nonNull);
            });
            menu.add(new JMenuItem("Center on host")).addActionListener(e -> {
                visualization.setFocus(item);
            });
            JMenu category = new JMenu("Set category");
            ViewUtils.setupCategoryMenu(category, node);
            JMenu role = new JMenu("Set role");
            ViewUtils.setupCategoryMenu(category, node);
            menu.add(category);
            menu.add(role);
            menu.add(new JMenuItem("Find In tree")).addActionListener(e
                    -> getFocusRequest().notifyObservers(node)
            );
            menu.add(new JMenuItem("Show all connections")).addActionListener(e -> {
                ViewUtils.showConnectionDialog(node);
            });
            menu.add(new JMenuItem("Watch connections")).addActionListener(e -> {
                getWatchRequest().notifyObservers(node);
            });
            menu.addSeparator();
            menu.add(new JMenuItem("Hide host")).addActionListener(e -> {
                visualization.setVisible(item, false);
            });
        } else if (node.isNetwork()) {
            menu.add(new JMenuItem("Show network details")).addActionListener(e -> {
                ViewUtils.showDetailEditor(node, Objects::nonNull);
            });
            menu.add(new JMenuItem("Center on network")).addActionListener(e -> {
                visualization.setFocus(item);
            });
            menu.add(new JMenuItem("Find in tree")).addActionListener(e
                    -> getFocusRequest().notifyObservers(node)
            );
            menu.add(new JMenuItem("Watch network")).addActionListener(e -> {
                getWatchRequest().notifyObservers(node);
            });
            menu.add(new JMenuItem("Edit network subnet mask")).addActionListener(e
                    -> getSubnetChangeRequest().notifyObservers(node)
            );
            menu.add(new JMenuItem(node.isExpanded(visualization.getId()) ? "Collapse" : "Expand")).addActionListener(e
                    -> visualization.setExpanded(node, !node.isExpanded(visualization.getId()))
            );
            menu.addSeparator();
            menu.add(new JMenuItem("Hide network")).addActionListener(e -> {
                visualization.setVisible(item, false);
            });
        }
        if (menu.getComponentCount() != 0) {
            menu.setLocation(mouseEvent.getLocationOnScreen());
            menu.setInvoker(getDisplay());
            menu.setVisible(true);
            menu.requestFocus();
        }
    }

    /**
     * creates and displays a context menu for the blank space on the graph.
     */
    private void contextMenu(LogicalVisualizationEx visualization, MouseEvent mouseEvent) {
        JComponent menu = new JPopupMenu();
        JMenuItem restore = new JMenuItem("Restore Logical View");
        restore.addActionListener(this::restore);
        JMenuItem collapse = new JMenuItem("Collapse all");
        collapse.addActionListener(this::collapseAll);
        JMenuItem expand = new JMenuItem("Expand all");
        expand.addActionListener(this::expandAll);
        menu.add(restore);
        menu.add(collapse);
        menu.add(expand);
        
        getVisibilityMenuItems(menu, visualization);
        
        if( menu instanceof JPopupMenu ) {
            JPopupMenu popup = (JPopupMenu) menu;
            popup.setLocation(mouseEvent.getLocationOnScreen());
            popup.setInvoker(getDisplay());
            popup.setVisible(true);
            popup.requestFocus();
        }
    }
    
    /**
     * Fills the 'menu' parameter with the visibility menu items used on this graph.
     * @param menu Menu to add all visibility sub-menus to.
     */
    public void getVisibilityMenuItems(JMenu menu) {
        this.getVisibilityMenuItems(menu, vis());
    }
    
    /**
     * Adds JMenus to a the menu parameter for all collapse, un-collapse, un-hide host, and un-hide network items.
     * @param menu Menu to add all visibility sub-menus to.
     * @param visualization Visualization which will with the actions upon an item selection.
     */
    private void getVisibilityMenuItems(JComponent menu, LogicalVisualizationEx visualization) {
        String collapseName = "Collapse Networks";
        String expandName = "Expand Networks";
        String unhideNetName = "Unhide Networks";
        String unhideHostName = "Unhide Hosts";
        
        JMenu collapse = new JMenu(collapseName);
        JMenu expand = new JMenu(expandName);
        JMenu unhideNetwork = new JMenu(unhideNetName);
        JMenu unhideHosts = new JMenu(unhideHostName);
        
        collapse.setName(collapseName);
        expand.setName(expandName);
        unhideNetwork.setName(unhideNetName);
        unhideHosts.setName(unhideHostName);
        
        String[] names = { collapseName, expandName, unhideNetName, unhideHostName };
        Component[] components = menu instanceof JMenu ? ((JMenu)menu).getMenuComponents() : ((JPopupMenu)menu).getComponents();
        Arrays.asList( components ).forEach( component -> {
            String name = component.getName();
            if( Arrays.asList(names).contains(name) ) {
                menu.remove(component);
            }
        });
        
        Predicate<VisualNode> predicate = p -> p.isHost() || p.isNetwork();
        vis.map.values().stream().filter(predicate).forEach(visualNode
                -> getVisibilityMenuItem(visualNode, visualization, collapse, expand, unhideNetwork, unhideHosts)
        );
        menu.add(collapse);
        menu.add(expand);
        menu.add(unhideNetwork);
        menu.add(unhideHosts);
        if (collapse.getItemCount() == 0) {
            collapse.setEnabled(false);
        }
        if (expand.getItemCount() == 0) {
            expand.setEnabled(false);
        }
        if (unhideNetwork.getItemCount() == 0) {
            unhideNetwork.setEnabled(false);
        }
        if (unhideHosts.getItemCount() == 0) {
            unhideHosts.setEnabled(false);
        }
    }
    /**
     * Processes each possible item which is a valid candidate for inclusion within the visibility menu.
     * @param visualNode Item for which menu items will be  created to control its visibility state.
     * @param visualization Visualization which will with the actions upon an item selection.
     * @param collapse JMenu of items which are expanded and may be collapsed.
     * @param expand JMenu of items which are collapsed and may be expanded.
     * @param unhideNetwork JMenu of items which are not visible and are networks.
     * @param unhideHosts JMenu of items which are not visible and are hosts.
     */
    private void getVisibilityMenuItem(VisualNode visualNode, LogicalVisualizationEx visualization, JMenu collapse, JMenu expand, JMenu unhideNetwork, JMenu unhideHosts) {
        final VisualItem item = visualization.getVisualItem(visualNode);
        final String name = visualNode.getName();
        if (!item.isVisible()) {
            if (visualNode.isHost()) {
                unhideHosts.add(new JMenuItem(name)).addActionListener(e -> {
                    visualization.setVisible(item, true);
                });
            } else {
                unhideNetwork.add(new JMenuItem(name)).addActionListener(e -> {
                    visualization.setVisible(item, true);
                });
            }
        }

        if (visualNode.isNetwork()) {
            if (visualNode.isExpanded(visualization.getId())) {
                collapse.add(new JMenuItem(name)).addActionListener(e -> {
                    visualization.setExpanded(visualNode, false);
                });
            } else {
                expand.add(new JMenuItem(name)).addActionListener(e -> {
                    visualization.setExpanded(visualNode, true);
                });
            }
        }

    }

    @Override
    public int getId() {
        return this.vis().getId();
    }

    @Override
    public void setId(int id) {
        this.vis().setId(id);
    }

    @Override
    public InvokeObservable getWatchRequest() {
        return watchRequest;
    }
    
    @Override
    public InvokeObservable getSubnetChangeRequest() {
        return subnetChangeRequest;
    }

    @Override
    public InvokeObservable getFocusRequest() {
        return focusRequest;
    }

    @Override
    public LogicalGraphEx setWatchRequest(InvokeObservable watchRequest) {
        this.watchRequest = watchRequest;
        return this;
    }
    
    @Override
    public LogicalGraphEx setSubnetChangeRequest(InvokeObservable subnetChangeRequest) {
        this.subnetChangeRequest = subnetChangeRequest;
        return this;
    }

    @Override
    public LogicalGraphEx setFocusRequest(InvokeObservable focusRequest) {
        this.focusRequest = focusRequest;
        return this;
    }
    
    @Override
    public void setVisible(boolean aFlag) {
        if( aFlag ) {
            vis().resume();
        } else {
            vis().pause();
        }
        super.setVisible(aFlag);
    }
    
}
