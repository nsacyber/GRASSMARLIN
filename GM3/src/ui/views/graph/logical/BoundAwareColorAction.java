/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import prefuse.action.assignment.ColorAction;
import prefuse.visual.VisualItem;

/**
 *
 * @author BESTDOG Hashes all {@link VisualItem#getBounds() } and only draws
 * unique shapes that do not exist entirely within the same space.
 */
public class BoundAwareColorAction extends ColorAction {

    ArrayList<VisualItem> items;
    HashSet<Integer> hash;

    public BoundAwareColorAction(String group, String field, int color) {
        super(group, field, color);
        items = new ArrayList<>();
        hash = new HashSet<>();
    }

    @Override
    public void run(double frac) {
        Iterator it = getVisualization().items(m_group, m_predicate);

        while (it.hasNext()) {
            VisualItem item = (VisualItem) it.next();
            if (item != null && item.getRow() != -1) {
                if (hash.add(item.getBounds().hashCode())) {
                    items.add(item);
                }
            }
        }

        items.forEach(item -> process(item, frac));
        items.clear();
        hash.clear();
    }

}
