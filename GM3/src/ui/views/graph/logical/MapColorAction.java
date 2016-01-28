/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.awt.Color;
import java.util.function.Function;
import java.util.function.Predicate;
import prefuse.Visualization;
import prefuse.action.assignment.ColorAction;
import prefuse.data.Tuple;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG  <pre>
 * Colors nodes based on three criteria,
 * 1. hover, searched, focused
 * 2. network (transparent) or host
 * 3. if host? get category defined color
 * </pre>
 */
public class MapColorAction extends ColorAction implements MapAction {

    final Function<Tuple, VisualNode> mapFunction;
    final Predicate<VisualItem> selectorFunction;
    
    final int hoverColor;
    final int searchColor;
    final int focusColor;
    final int selectedColor;
    final String name;
    
    public MapColorAction(String group, Function<Tuple, VisualNode> mapFunction, Predicate<VisualItem> selectorFunction, String name) {
        super(group, null);
        this.name = name;
        this.mapFunction = mapFunction;
        this.selectorFunction = selectorFunction;
        this.hoverColor = ColorLib.gray(220, 230);
        this.focusColor = ColorLib.rgb(198, 229, 229);
        this.searchColor = ColorLib.rgb(255, 255, 151);
        this.selectedColor = ColorLib.rgb(33, 218, 103);
    }

    public MapColorAction(String group, Function<Tuple, VisualNode> mapFunction, Predicate<VisualItem> selectorFunction) {
        this(group, mapFunction, selectorFunction, "MapColorAction");
    }

    @Override
    public void process(VisualItem item, double frac) {
        int color;
        if( selectorFunction.test(item) ) {
            color = this.selectedColor;
        } else {
            color = getColor(item);
        }
        item.setInt("_fillColor:start", color);
        item.setInt("_fillColor:end", color);
        item.setInt("_fillColor", color);
    }
    
    private int getColor(VisualNode node) {
        int color;
        if (node == null || node.isNetwork()) {
            color = Color.TRANSLUCENT;
        } else if( node.hasDetails() ) {
            color = node.getDetails().getCategory().rgba;
        } else {
            color = 0;
        }
        return color;
    }

    @Override
    public int getColor(VisualItem item) {
        int color;
        if (item.isHover()) {
            color = this.hoverColor;
        } else if (item.isInGroup(Visualization.SEARCH_ITEMS)) {
            color = this.searchColor;
        } else if (item.isInGroup(Visualization.FOCUS_ITEMS)) {
            color = this.focusColor;
        } else {
            color = getColor(get(item));
        }
        return color;
    }

    @Override
    public VisualNode get(Tuple tuple) {
        return mapFunction.apply(tuple);
    }

}
