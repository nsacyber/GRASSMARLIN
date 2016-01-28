/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree.visualnode;

import core.ViewUtils;
import core.types.ByteTreeItem;
import core.types.VisualDetails;
import ui.icon.Icons;

/**
 *
 * @author BESTDOG Network tree nodes exist in "level 1" as a descendant of root
 * and an ancestor of hosts.
 */
public class NetworkVisualNode extends VisualNode {
    
    private final VisualDetails details;
    
    public NetworkVisualNode(VisualNode parent, VisualDetails details) {
        super(parent);
        this.details = details;
        this.details.image.setIcon(Icons.Original_network);
        this.setName(getAddress());
    }
    
    @Override
    public String getAddress() {
        int id = this.getDetails().getNetworkId();
        int cidr = this.getDetails().cidr();
        return ViewUtils.ipString(id, cidr);
    }
    
    @Override
    public VisualDetails getDetails() {
        return details;
    }
    
    @Override
    public String getText() {
        int netmask = getDetails().getNetworkMask();
        String name = getName();
        String ip = getAddress();
        String displayText;
        if( name.equals(ip) ) {
            displayText = ip;
        } else {
            displayText = String.format("%s, Address:%s", name, ip );
        }
        String netmaskText = ViewUtils.ipString(netmask);
        return String.format("Network %s, Subnet:%s", displayText, netmaskText);
    }
    
    @Override
    public int hashCode() {
        return getDetails().getNetworkId();
    }

    @Override
    public int getAddressHash() {
        return getDetails().getNetworkId();
    }
    
    @Override
    public boolean isHost() {
        return false;
    }

    @Override
    public boolean isNetwork() {
        return true;
    }

    @Override
    public String groupName() {
        return VisualNode.NETWORK;
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
    public String getName() {
        return getDetails().getName();
    }

    @Override
    public void setName(String name) {
        getDetails().setName(name);
    }
    
    @Override
    public boolean isExpanded(Integer id) {
        return getDetails().isExpanded(id);
    }

    @Override
    public void setExpanded(Integer id, boolean collapsed) {
        getDetails().setExpanded( id, collapsed );
        this.getChildren().forEach(child->child.setExpanded(id, collapsed));
    }

}
