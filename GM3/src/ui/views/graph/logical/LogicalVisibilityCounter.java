/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.data.Tuple;
import prefuse.visual.VisualItem;

/**
 * Counts the amount of total and visible hosts and networks and produces two
 * strings A *Primary* string is a fractional report "Hosts 0/0, Networks 0/0" A
 * *Secondary* string is an English description of the report, "0 of 0 Hosts are
 * visible, 0 of 0 Networks are visible."
 *
 * These should be a {@link javax.swing.JLabel#setText(java.lang.String) } and {@link javax.swing.JLabel#setToolTipText(java.lang.String)
 * } respectively.
 */
public class LogicalVisibilityCounter extends Action {

    Consumer<String> primary, secondary;
    Predicate<Tuple> isNetworkPredicate;

    final int visibleHosts = 0;
    final int totalHosts = 1;
    final int visibleNetworks = 2;
    final int totalNetworks = 3;
    int[] values;
    int[] newValues;

    String group;

    public LogicalVisibilityCounter(Consumer<String> primary, Consumer<String> secondary) {
        this.primary = primary;
        this.secondary = secondary;
        this.values = new int[]{0, 0, 0, 0};
        this.newValues = new int[]{0, 0, 0, 0};
        this.group = Visualization.ALL_ITEMS;
    }

    public void setIsNetworkPredicate(Predicate<Tuple> isNetworkPredicate) {
        this.isNetworkPredicate = isNetworkPredicate;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public void run(double d) {
        Visualization vis = this.getVisualization();

        /* reset all counts*/
        Arrays.setAll(newValues, Int -> 0);
        
        vis.getGroup(group).tuples().forEachRemaining(tuple -> {
            VisualItem item = (VisualItem) tuple;
            if (this.isNetworkPredicate.test(item)) {
                newValues[totalNetworks]++;
                if (item.isVisible()) {
                    newValues[visibleNetworks]++;
                }
            } else {
                newValues[totalHosts]++;
                if (item.isVisible()) {
                    newValues[visibleHosts]++;
                }
            }
        });
        
        /**
         * subtract one from each for the always visible center node.
         */
        newValues[totalHosts]--;
        newValues[visibleHosts]--;
        checkNewValues(newValues);
    }

    private void checkNewValues(int[] newValues) {
        int len = newValues.length;
        for (int i = 0; i < len; i++) {
            if (newValues[i] != values[i]) {
                System.arraycopy(newValues, 0, values, 0, len);
                update(values);
            }
        }
    }

    private void update(int[] v) {
        String secondaryText;
        String primaryText;
        if( v[totalNetworks] == 0 ) {
            primaryText = "Visualization is empty";
            secondaryText = "Import data to populate visualization.";
        } else {
            primaryText = String.format(
                    "Hosts %d/%d, Networks %d/%d",
                    v[visibleHosts],
                    v[totalHosts],
                    v[visibleNetworks],
                    v[totalNetworks]
            );
            secondaryText = String.format(
                    "%d of %d hosts are visible and %d of %d networks are visible.",
                    v[visibleHosts],
                    v[totalHosts],
                    v[visibleNetworks],
                    v[totalNetworks]
            );
        }
        this.primary.accept(primaryText);
        this.secondary.accept(secondaryText);
    }

}
