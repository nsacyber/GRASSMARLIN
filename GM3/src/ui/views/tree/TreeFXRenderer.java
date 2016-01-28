/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import ui.views.tree.visualnode.VisualNode;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * 2015.08.07 - CC - Fixed the tree nodes to refresh and get the correct size
 */
public class TreeFXRenderer extends DefaultTreeCellRenderer {

    List<Runnable> tasks;
    TreeView tree;
    Timer fx;

    public TreeFXRenderer(TreeView tree) {
        super();
        tasks = new ArrayList<>();
        this.tree = tree;
        fx = new Timer(130, (e) -> {
            if (!tasks.isEmpty()) {
                tasks.forEach(Runnable::run);
                tasks.clear();
            }
            SwingUtilities.invokeLater(tree::redraw);
        });
    }

    @Override
    public Component getTreeCellRendererComponent(JTree t, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        VisualNode node = (VisualNode) value;
        node.decorate(this, sel, System.currentTimeMillis());
        if( node.hasChanged() ) {
            this.revalidate();
        }
        return this;
    }

    public TreeFXRenderer run() {
        if (!fx.isRunning()) {
            fx.start();
        }
        return this;
    }

}
