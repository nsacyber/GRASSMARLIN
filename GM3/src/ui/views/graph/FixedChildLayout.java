/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import prefuse.action.layout.Layout;
import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.visual.VisualItem;
import ui.icon.Icons;


    /**
     * Layout that affixes the position of items in an aggregate.
     */
class FixedChildLayout extends Layout {

        Map<Node, List<Node>> groups;
        final String m_graph;
        final String m_icon;
        final String expandField;

        double xOff;
        double xLeft;
        double yOff;
        double yTop;
        double width;
        double height;
        double childCount;
        double childWeight = 1f;
        double rowCapacity;
        double childWidth = 64;
        double childHeight = 64;
        double rows = 2;
        double horizontalPadding = 2;
        double verticalPadding = 2;

        public FixedChildLayout(String graph, String group, String icon, String expand, Map<Node, List<Node>> centroids) {
            super(group);
            this.m_graph = graph;
            this.m_icon = icon;
            this.groups = centroids;
            this.expandField = expand;
        }

        @Override
        public void run(double frac) {

            groups.entrySet().stream().forEach(entry -> {

                Icons icon = Icons.Original_port;
                Node parent = entry.getKey();
                VisualItem center = m_vis.getVisualItem(m_group, parent);
                boolean expanded = center.getBoolean(expandField);

                if (expanded) {

                    childCount = entry.getValue().size();
                    rowCapacity = childCount / rows;
                    width = rowCapacity * childWidth + rowCapacity * horizontalPadding;
                    height = rows * childHeight + (rows + 1) * verticalPadding;

                    xLeft = xOff = center.getX() - width / 2;
                    yTop = yOff = center.getY() - height / 2 - childHeight;

                    Iterator<Node> children = entry.getValue().iterator();
                    while (children.hasNext()) {
                        VisualItem item = toVisualItem(children.next());
                        item.setVisible(true);
                        rowCapacity -= childWeight;

                        if (rowCapacity < 0) {
                            rowCapacity = childCount / rows;
                            xOff = xLeft;
                            yOff += childHeight + verticalPadding;
                            icon = Icons.Original_upsidedown_port;
                        }

                        xOff += horizontalPadding;

                        item.setX(xOff);
                        item.setY(yOff);
                        item.set(m_icon, icon.get64());

                        xOff += childWidth;
                    }

                    center.set(m_icon, null);

                } else {

                    Iterator<Node> children = entry.getValue().iterator();
                    while (children.hasNext()) {
                        VisualItem item = toVisualItem(children.next());
                        item.setVisible(false);
                        item.setX(center.getX());
                        item.setY(center.getY());
                    }

                    center.set(m_icon, Icons.Original_network_device.get64());
                }
            });

        }

        VisualItem toVisualItem(Node node) {
            return m_vis.getVisualItem(m_group, node);
        }

        VisualItem nextAdjacent(Object tuple, Node parent) {
            Edge edge = (Edge) tuple;
            return m_vis.getVisualItem(m_group, edge.getAdjacentNode(parent));
        }

    }
