/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import ui.views.tree.visualnode.VisualNode;
import javax.swing.tree.TreeCellRenderer;

/**
 *
 * @author BESTDOG
 * @param <T> TreeCellRenderer to decorate
 */
@FunctionalInterface
public interface TreeNodeDecorator<T extends TreeCellRenderer> {

    void accept(T label, VisualNode node, boolean selected, Long time);

    default boolean classEquals(Class c) {
        return this.hashCode() == c.hashCode();
    }
    
}
