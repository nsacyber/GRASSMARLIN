/*
 * Copyright (C) 2011, 2012
 * This file is part of GRASSMARLIN.
 *
 */
package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentListener;
import javax.swing.BorderFactory;
import javax.swing.Box.Filler;
import static javax.swing.JFrame.EXIT_ON_CLOSE;
import javax.swing.border.Border;
import ui.icon.Icons;
import ui.platform.Footer;
import ui.views.graph.GraphView;
import ui.log.UILog;
import ui.menu.MenuBar;
import ui.menu.SubMenu;
import ui.platform.SplitPane;
import ui.platform.UIPanel;
import ui.dialog.SummaryDialog;
import ui.views.tree.TreeView;

/**
 *
 */
public class UIBuilder {

    GrassMarlin ui;

    int x = 1450; // this should be 1/2 the screen width, or last known size
    int y = 800; // this should be 1/2 the screen height, or last known size
    Dimension windowDim;
    BorderLayout layout;

    SplitPane centerPanelComponent;
    TreeView treeViewComponent;
    SummaryDialog sidebarComponent;
    GraphView graphComponent;
    UILog logComponent;
    Component subMenu;
    Component footer;
    Component menu;

    ComponentListener listener;

    public UIBuilder() {
        windowDim = null;
        layout = null;

        centerPanelComponent = null;
        treeViewComponent = null;
        sidebarComponent = null;
        graphComponent = null;
        subMenu = null;
        footer = null;
        menu = null;
    }

    /**
     *
     * @param listener - ComponentListener to route child component events
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setMouseListener(ComponentListener listener) {
        this.listener = listener;
        return this;
    }

    /**
     *
     * @param sidebar - SummaryDialog component, extends JPanel
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setSideBar(SummaryDialog sidebar) {
        this.sidebarComponent = sidebar;
        return this;
    }

    /**
     *
     * @param log - UILog component, extends UIPanel
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setLog(UILog log) {
        this.logComponent = log;
        return this;
    }

    /**
     *
     * @param footer - Footer component, extends JPanel
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setFooter(Footer footer) {
        this.footer = footer;
        return this;
    }

    /**
     *
     * @param menu - Menu component, extends JMenu
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setMenu(Component menu) {
        this.menu = menu;
        return this;
    }

    /**
     *
     * @param subMenu - Menu component, extends JMenu
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setSubMenu(Component subMenu) {
        this.subMenu = subMenu;
        return this;
    }

    /**
     *
     * @param treeViewComponent - TreeView that extends Component
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setTreeView(TreeView treeViewComponent) {
        this.treeViewComponent = treeViewComponent;
        return this;
    }

    /**
     *
     * @param ui - Reference to a GrassMarlinUI instance to be initialized
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setReference(GrassMarlin ui) {
        this.ui = ui;
        return this;
    }

    /**
     *
     * @param graphComponent - Component that contains the Graph
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setGraph(GraphView graphComponent) {
        this.graphComponent = graphComponent;
        return this;
    }

    /**
     *
     * @param centerPanelComponent - Component that will group the TreeView and
     * Graph
     * @return this - Reference returned so calls can be chained
     */
    public UIBuilder setCenterPanel(SplitPane centerPanelComponent) {
        this.centerPanelComponent = centerPanelComponent;
        return this;
    }

    void applySize(GrassMarlin ui) {
        ui.setPreferredSize(windowDim);
        ui.setSize(windowDim);
    }

    void applyDefaults(GrassMarlin ui) {
        ui.setDefaultCloseOperation(EXIT_ON_CLOSE);
        ui.setLocationByPlatform(true);
        ui.setTitle("Grassmarlin");
        ui.setIconImage(Icons.Grassmarlin.get32());
    }

    void applyLayoutManager(GrassMarlin ui) {
        if (layout == null) {
            layout = new BorderLayout();
        }
        ui.setLayout(layout);
    }

    /**
     * Positions the center two components into something that extends a
     * JSplitPane
     */
    void applyCenterPanel(GrassMarlin ui) {
//        centerPanelComponent.add(sidebarComponent, SplitPane.Position.LEFT);
        if (treeViewComponent != null) {
            centerPanelComponent.add(treeViewComponent, SplitPane.Position.CENTER);
        }
        centerPanelComponent.add(graphComponent, SplitPane.Position.RIGHT);
        centerPanelComponent.add(logComponent, SplitPane.Position.LEFT_BOTTOM);
        Border b = BorderFactory.createMatteBorder(1, 0, 1, 1, Color.GRAY);
        Border b2 = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY);
//
//        sidebarComponent.setBorder(b2);
        treeViewComponent.setBorder(b2);
        graphComponent.setBorder(b);
        logComponent.setBorder(b2);

        centerPanelComponent.setInitialSize(x, y);
        Dimension d = new Dimension(5, 5);
        ui.add(new Filler(d, d, d), BorderLayout.WEST);
        ui.add(new Filler(d, d, d), BorderLayout.EAST);
        ui.add(centerPanelComponent, BorderLayout.CENTER);
    }

    void applyMenuContainer(GrassMarlin ui) {
        UIPanel menus = new UIPanel();
        menus.setLayout(new BorderLayout());

        menus.add(menu, BorderLayout.NORTH);
        menus.add(subMenu, BorderLayout.SOUTH);

        ui.add(menus, BorderLayout.PAGE_START);
    }

    /**
     *
     * @return An instance of GrassMarlinUI
     */
    public GrassMarlin build() {

        if (ui == null) {
            ui = new GrassMarlin();
        }

        if (listener != null) {
            ui.addComponentListener(listener);
        }

        applyDefaults(ui);
        applyLayoutManager(ui);

        if (windowDim == null) {
            windowDim = new Dimension(x, y);
        }

        applySize(ui);

//		if( treeViewComponent == null )
//			treeViewComponent = new TreeView();
        if (graphComponent == null) {
//            GraphAdapter logical = new GraphBuilder().build();
            graphComponent = new GraphView();
        }

//        if (sidebarComponent == null) {
//            sidebarComponent = new SummaryDialog();
//        }

        if (logComponent == null) {
            logComponent = new UILog();
        }

        if (centerPanelComponent == null) {
            centerPanelComponent = new SplitPane();
        }

        applyCenterPanel(ui);

        if (menu == null) {
            menu = new MenuBar();
        }

        if (subMenu == null) {
            subMenu = new SubMenu();
        }

        applyMenuContainer(ui);

        if (footer == null) {
            footer = new Footer();
        }

        ui.add(footer, BorderLayout.SOUTH);

        ui.pack();
        return ui;
    }

}
