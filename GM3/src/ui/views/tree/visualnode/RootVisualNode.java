/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree.visualnode;

import core.types.ByteTreeItem;
import core.types.VisualDetails;


/**
 *
 * @author BESTDOG
 * The root-node in the tree view.
 */
public class RootVisualNode extends VisualNode {
    
    public RootVisualNode() {
        super(null);
    }

    @Override
    public VisualDetails getDetails() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAddress() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getAddressHash() {
        return -1;
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
        return VisualNode.ROOT;
    }
    
    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public boolean hasDetails() {
        return false;
    }
    
    @Override
    public ByteTreeItem getData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isExpanded(Integer id) {
        return true;
    }

    @Override
    public void setExpanded(Integer id, boolean collapsed) {
    }

}