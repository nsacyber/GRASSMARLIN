/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import core.Core;
import core.topology.TopologyEntity.Type;
import core.topology.TopologyTree;
import core.types.InvokeObservable;
import core.types.LogEmitter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import prefuse.Display;
import prefuse.controls.ControlAdapter;

import prefuse.controls.PanControl;
import prefuse.controls.ZoomControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.io.DataIOException;
import prefuse.data.io.GraphMLWriter;
import prefuse.visual.AggregateItem;
import prefuse.visual.DecoratorItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.ItemSorter;
import ui.dialog.DialogManager;
import ui.icon.Icons;

/**
 *
 */
public class PhysicalGraph extends JPanel implements Observer, GraphController {

    /**
     * Milliseconds spent animating the {@link PhysicalGraph#fit() method}.
     */
    private static final int FIT_DELAY_MS = 500;

    final PhysicalVisualization p_vis;
    final Display display;
    final JPopupMenu popupMenu;
    /** allows data updates to be requested from the graph */
    final InvokeObservable updateRequest, findRequest, viewCamTableRequest, viewHostRequest, viewNICRequest;
    Integer oldHashCode;
    private int cloudCollapseLimit;

    public PhysicalGraph() {
        p_vis = new PhysicalVisualization();
        display = new Display(p_vis);
        display.setHighQuality(true);
        popupMenu = new JPopupMenu();
        findRequest = new InvokeObservable(this);
        updateRequest = new InvokeObservable(this);
        viewCamTableRequest = new InvokeObservable(this);
        viewHostRequest = new InvokeObservable(this);
        viewNICRequest = new InvokeObservable(this);
        cloudCollapseLimit = 10;
        oldHashCode = null;
        initComponent();
    }

    public static final String DEFAULT_SEARCH_KEY = "Search";
    
    private void initComponent() {
        setLayout(new BorderLayout());
        add(display, BorderLayout.CENTER);
        
        javax.swing.JPanel btnContainer;
        javax.swing.JPanel controlPanel;
        javax.swing.JButton fitBtn;
        javax.swing.JButton inBtn;
        javax.swing.JButton outBtn;
        javax.swing.JButton refreshBtn;
        javax.swing.JButton settings;
        JButton toggleAutoscale;
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

        /** disabled buttons */
        JButton showAllBtn = new JButton(Icons.Eye.getIcon());
        showAllBtn.setToolTipText("Show All");
        JButton toggleLineWidths = new JButton(Icons.Graph_edge.getIcon());
        toggleLineWidths.setToolTipText("Toggle edge thickness");
        JButton toggleCurvelines = new JButton(Icons.Draw_curve.getIcon());
        toggleCurvelines.setToolTipText("Toggle curved edges");
        JButton toggleQuality = new JButton(Icons.Quality.getIcon());
        toggleQuality.setToolTipText("Toggle quality");
        toggleAutoscale = new JButton(Icons.Autoscale.getIcon());
        toggleAutoscale.setToolTipText("Toggle Autoscale");

        showAllBtn.setEnabled(false);
        toggleLineWidths.setEnabled(false);
        toggleCurvelines.setEnabled(false);
        toggleQuality.setEnabled(false);
        toggleAutoscale.setEnabled(false);

        setBackground(new java.awt.Color(255, 255, 255));
        setLayout(new java.awt.BorderLayout());

        add(display, java.awt.BorderLayout.CENTER);

        controlPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY));

        refreshBtn.setIcon(Icons.Refresh.getIcon());
//        refreshBtn.addActionListener((e) -> refresh());
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
        settings.addActionListener((e) -> {
            DialogManager.GraphDialog(true);
        });

        Font font = new java.awt.Font("Tahoma", 0, 14);

        JTextField searchField = new JTextField();
//        showAllBtn.addActionListener((e) -> showAll());


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
                )
        );
        btnContainerLayout.setVerticalGroup(
                btnContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, btnContainerLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(btnContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
        
        display.addControlListener(new ZoomControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new CollectionDragControl(p_vis.getGroups(), p_vis::getVisualNode, p_vis::adaptAggregate));

        JMenuItem showNicDetails = new JMenuItem("Show NIC Details");
        JMenuItem centerItem = new JMenuItem("Center Item");
        JMenuItem findItem = new JMenuItem("Find Item in TreeView");
        JMenuItem showHostDetails = new JMenuItem("Show Host Details");
        JMenuItem expand = new JMenuItem();
        JMenuItem viewCamTable = new JMenuItem("View CAM Table");
        JMenuItem refresh = new JMenuItem("Refresh Physical Graph Data");

        String expandText = "Expand this Network Device";
        String CollapseText = "Collapse this Network Device";

        refresh.addActionListener( e -> {
            this.requestUpdate();
        });
        
        expand.addActionListener(e -> 
            PhysicalGraph.this.p_vis.toggleFocusExpansion()
        );

        centerItem.addActionListener(e -> 
            PhysicalGraph.this.p_vis.centerFocus()
        );
        
        showNicDetails.addActionListener(e -> {
            PhysicalGraph self = PhysicalGraph.this;
            self.viewNICRequest.setChanged();
            self.p_vis.withFocusData(self.viewNICRequest::notifyObservers);
        });
        
        showHostDetails.addActionListener(e -> {
            PhysicalGraph self = PhysicalGraph.this;
            self.viewHostRequest.setChanged();
            self.p_vis.withFocusData(self.viewHostRequest::notifyObservers);
        });
        
        viewCamTable.addActionListener(e -> {
            PhysicalGraph self = PhysicalGraph.this;
            self.viewCamTableRequest.setChanged();
            self.p_vis.withFocusData(self.viewCamTableRequest::notifyObservers);
        });
        
        findItem.addActionListener(e -> {
            PhysicalGraph self = PhysicalGraph.this;
            self.findRequest.setChanged();
            self.p_vis.withFocusData(self.findRequest::notifyObservers);
        });

        //<editor-fold defaultstate="collapsed" desc="popup menu controls">
        display.addControlListener(new ControlAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = PhysicalGraph.this.popupMenu;
                    
                    menu.removeAll();
                    menu.add(refresh);
                    
                    if (e.getSource() instanceof Display) {
                        menu.setInvoker((Display) e.getSource());
                        menu.setLocation(e.getLocationOnScreen());
                        menu.setVisible(true);
                    }
                }
            }
            @Override
            public void itemClicked(VisualItem item, MouseEvent e) {
                /* makes the aggregate hulls behave like the switch was clicked */
                item = PhysicalGraph.this.p_vis.adaptAggregate( item );

                /* set focus item used in all right click options */
                PhysicalGraph.this.p_vis.setFocusItem(item);
                
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = PhysicalGraph.this.popupMenu;
                    Type type = PhysicalVisualization.getType(item);
                    boolean expanded = PhysicalGraph.this.p_vis.isExpanded(item);

                    menu.removeAll();

                    menu.add(centerItem);
                    menu.add(findItem);

                    if( Type.HOST.equals(type) ) {
                        menu.add(showHostDetails);
                    }
                    
                    if( Type.SWITCH.equals(type) ) {
                        expand.setText(!expanded ? expandText : CollapseText);
                        menu.add(expand);
                        menu.add(showHostDetails);
                        menu.add(viewCamTable);
                    }
                    
                    if( Type.PORT.equals(type) ) {
                        expand.setText(!expanded ? expandText : CollapseText);
                        menu.add(expand);
                        menu.add(showNicDetails);
                        menu.add(viewCamTable);
                    }

                    if (e.getSource() instanceof Display) {
                        menu.setInvoker((Display) e.getSource());
                        menu.setLocation(e.getLocationOnScreen());
                        menu.setVisible(true);
                    }
                }
            }
        });

        display.setItemSorter(new ItemSorter() {
            @Override
            public int score(VisualItem item) {
                int score = 0;
                if (item instanceof EdgeItem) {
                    score = 1;
                } else if (item instanceof DecoratorItem) {
                    score = 2;
                } else if (item instanceof AggregateItem) {
                    score = -1;
                }
                return score;
            }
        });

//</editor-fold>
    }
    
    @Override
    public String getGroup() {
        return PhysicalVisualization.GRAPH;
    }
    
    @Override
    public Display getDisplay() {
        return display;
    }

    /**
     * Eventually updates the graph with the data in the AFTTable argument.
     *
     * @param tree TopologyTree from which this visualization is populated.
     */
    public void setTree(TopologyTree tree) {
        if( !tree.isEmpty() ) {
            setTree(tree, false);
        }
    }
    
    public void setTree(TopologyTree tree, boolean forceUpdate) {
        /** prevent unnecessary updates, update unconditionally if there are no visual items */
        if( !forceUpdate && !p_vis.isEmpty() && oldHashCode != null ) {
            if( oldHashCode.equals(tree.hashCode()) ) {
                return;
            }
        }
        
        LogEmitter.factory.get().emit(this, Core.ALERT.INFO, "Redrawing Topology.");
        
        oldHashCode = tree.hashCode();
        
        tree.refreshVisualRows();
        
        p_vis.clear();
        
        tree.connectedNodes().forEach(topoNode -> 
            topoNode.edges.forEach(p_vis::addEdge)
        );

        tree.aggregates.entrySet().forEach(e -> {
            p_vis.generateSwitchDiagram(e.getKey(), e.getValue());
        });

        p_vis.validateTree();
        
        vis().animate();
        vis().expandAll(false);
        
        fit();
    }

    PhysicalVisualization vis() {
        return p_vis;
    }

    Graph graph() {
        return p_vis.graph;
    }

    /** provoke this object to fire an resetRequest */
    public void requestUpdate() {
        updateRequest.setChanged();
        updateRequest.notifyObservers();
    }
    
    /**
     * An observable that updates when the physical topology should be updated.
     * @return The InvokeObservable that will be called for this event. 
     */
    public InvokeObservable getUpdateRequest() {
        return updateRequest;
    }
    
    /**
     * An observable that updates when "find in tree view" is called.
     * @return The InvokeObservable that will be called for this event.
     */
    public InvokeObservable getFindRequest() {
        return findRequest;
    }

    /**
     * An observable that updates when "view cam table" is called.
     * @return The InvokeObservable that will be called for this event.
     */
    public InvokeObservable getViewCamTableRequest() {
        return viewCamTableRequest;
    }
    
    /**
     * An observable that updates when "show host details" is called.
     * @return The InvokeObservable that will be called for this event.
     */
    public InvokeObservable getViewHostRequest() {
        return viewHostRequest;
    }
    
    /**
     * An observable that updates when "show NIC details" is called.
     * @return The InvokeObservable that will be called for this event.
     */
    public InvokeObservable getViewNICRequest() {
        return viewNICRequest;
    }
    
    public void fit() {
        SwingUtilities.invokeLater(()
                -> fit(null)
        );
    }
    
    /**
     * Writes the graphs XML content to the provided File.
     * @param xml Non-existing File.
     * @throws DataIOException Thrown if the {@link #getExportGraph() } contains non-supported types.
     * @throws FileNotFoundException The OutputStream cannot be opened.
     */
    public void exportXML(File xml) throws DataIOException, FileNotFoundException {
        FileOutputStream xmlFos = new FileOutputStream(xml);
        GraphMLWriter writer = new GraphMLWriter();
        writer.writeGraph(getExportGraph(), xmlFos);
    }
    
    /**
     * Exports both the IMAGE and XML to a directory.
     * @param file The file should not yet exist on the FS.
     */
    public void exportAll(File file) {
        if( !file.exists() ) {
            file.mkdir();
        }
        if( !file.isDirectory() ) {
            LogEmitter.factory.get().emit(file, Core.ALERT.DANGER, "Export failed. Selected file is not a directory.");
        }
        try {
            File image = new File(file.getCanonicalPath() + File.separator + "physical.png");
            File xml = new File(file.getCanonicalPath() + File.separator + "physical.xml");
            
            exportImage(image);
            exportXML(xml);
            
        } catch (IOException ex) {
            LogEmitter.factory.get().emit(file, Core.ALERT.DANGER, "Export failed.");
            Logger.getLogger(PhysicalGraph.class.getName()).log(Level.SEVERE, null, ex);
        } catch (DataIOException ex) {
            Logger.getLogger(PhysicalGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Makes an exportable copy of the graph.
     * This is necessary because XML cannot handle image type and other
     * data that exists in the visualization but is not necessary or exportable.
     * @return Exportable copy of the graph.
     */
    Graph getExportGraph() {
        /* create a new graph 'g' and the graph to copy from */
        Graph g = new Graph();
        Graph l = p_vis.graph;
        
        /* the field identifiers to copy */
        String TEXT_FIELD  = PhysicalVisualization.TEXT;
        String GROUP_FIELD  = PhysicalVisualization.PARENT_ROW;
        
        /* put the identifiers on each tuple */
        g.addColumn(TEXT_FIELD, String.class);
        g.addColumn(GROUP_FIELD, int.class);
        
        Map<Integer,Integer> nodeMap = new HashMap<>();
        
        /* map the nodes to their adjusted rows */
        l.nodes().forEachRemaining( nodeTuple -> {
            Node node = (Node) nodeTuple;
            Node newNode = g.addNode();
                newNode.setString(TEXT_FIELD, node.getString(TEXT_FIELD));
                newNode.setString(GROUP_FIELD, node.getString(GROUP_FIELD));
            nodeMap.put(node.getRow(), newNode.getRow());
        });
        
        /* connect each edge while referencing their new rows in the graph g */
        l.edges().forEachRemaining( EdgeTuple -> {
            Edge edge = (Edge) EdgeTuple;
            int src = edge.getSourceNode().getRow();
            int dst = edge.getTargetNode().getRow();
            
            int newSrc = nodeMap.get(src);
            int newdst = nodeMap.get(dst);
        
            g.addEdge(newSrc, newdst);
        });
        
        return g;
    }
    
    @Override
    public void update(Observable o, Object arg) {
        if( arg instanceof TopologyTree && isVisible() ) {
            setTree((TopologyTree) arg);
        }
    }

    public void setCloudCollapseLimit(int cloudCollapseLimit) {
        this.cloudCollapseLimit = cloudCollapseLimit;
    }

}
