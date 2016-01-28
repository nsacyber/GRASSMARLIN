/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree.visualnode;

import core.types.ByteTreeItem;
import core.types.VisualDetails;

/**
 *
 * @author BESTDOG The leaf nodes (connections) are decendants of
 * {@link HostVisualNode}s and have no children. They proxy another host node,
 * and only overload a {@link Object#toString() } method. All other methods
 * should potentially be things to the original host, except those methods which
 * would cause it to have child nodes in thew tree view.
 */
public class PeerVisualNode extends VisualNode {
    /** the node to proxy methods for */
    private final VisualNode node;

    int in;
    int out;
    
    public PeerVisualNode(VisualNode parent, VisualNode node) {
        super(parent);
        this.node = node;
    }

    public void updateCounts() {
        in = node.getData().in;
        out = node.getData().out;
    }

    public VisualNode getOriginal() {
        return this.node;
    }
    
    public int out() {
        return out;
    }

    public int in() {
        return in;
    }
    
    public int total() {
        return in + out;
    }
    
    @Override
    public VisualDetails getDetails() {
        return node.getDetails();
    }

    @Override
    public String getText() {
        updateCounts();
        return String.format("%s, %d frames, %d in, %d out", node.getName(), total(), in(), out());
    }

    @Override
    public String getName() {
        return node.getName();
    }
    
    @Override
    public void setName(String name) {
        node.setName(name);
    }

    @Override
    public String getAddress() {
        return node.getAddress();
    }

    @Override
    public int getAddressHash() {
        return node.getAddressHash();
    }

    @Override
    public boolean isHost() {
        return false;
    }

    @Override
    public boolean isNetwork() {
        return false;
    }

    @Override
    public String groupName() {
        return "none";
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }
    
    @Override
    public boolean hasDetails() {
        return true;
    }

    @Override
    public ByteTreeItem getData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
    
    @Override
    public boolean isExpanded(Integer id) {
        return false;
    }

    @Override
    public void setExpanded(Integer id, boolean collapsed) {
    }
    
}
