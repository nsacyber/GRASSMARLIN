/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree.visualnode;

import ICSDefines.Category;
import ICSDefines.Role;
import core.ViewUtils;
import core.types.ByteTreeItem;
import core.types.VisualDetails;

/**
 *
 * @author BESTDOG
 */
public class HostVisualNode extends VisualNode {

    private final ByteTreeItem data;
    final String[] textArray;

    public HostVisualNode(VisualNode parent, ByteTreeItem data) {
        super(parent);
        this.data = data;
        this.textArray = new String[6];
        setName( getAddress() );
    }

    @Override
    public VisualDetails getDetails() {
        return data.details;
    }

    int nameIndex = 0;
    int addressIndex = 1;
    int categoryIndex = 2;
    int roleIndex = 3;
    int confidenceIndex = 4;
    int ouiIndex = 5;

    private static final String[] titles = {
        "Name",
        "Address",
        "Category",
        "Role",
        "Confidence",
        "OUI"
    };

    @Override
    public String getText() {
        String name = getName();
        String address = getAddress();
        if( name.equals(address) ) {
            name = "";
        }
        textArray[categoryIndex] = getCategoryText();
        textArray[addressIndex] = address;
        textArray[roleIndex] = getRoleText();
        textArray[confidenceIndex] = getConfidenceText();
        textArray[nameIndex] = name;
        textArray[ouiIndex] = getDetails().getHardwareVendor();
        StringBuilder sb = new StringBuilder();
        for (int i = nameIndex, parts = 0; i < ouiIndex; ++i) {
            if (!textArray[i].isEmpty()) {
                if (parts > 0) {
                    sb.append(", ");
                }
                sb.append(titles[i])
                        .append(": ")
                        .append(textArray[i]);
                parts++;
            }
        }
        return sb.toString();
    }

    public String getCategoryText() {
        return Category.UNKNOWN.equals(data.details.getCategory()) ? "" : data.details.getCategory().getPrettyPrint();
 	}
 	
 	public String getRoleText() {
        return Role.UNKNOWN.equals(data.details.getRole()) ? "" : data.details.getRole().getPrettyPrint();
 	}
 	
 	public String getConfidenceText() {
        return data.details.getConfidence() == 0 ? "" : data.details.getConfidence().toString();
 	}

    public boolean contains(VisualNode node) {
        return this.isHost() && node.isHost() ? this.getData().hasForwadEdge(node.getData()) : false;
    }

    @Override
    public String getAddress() {
        return ViewUtils.ipString(data.hash);
    }

    @Override
    public int getAddressHash() {
        return data.hash;
    }

    @Override
    public boolean isHost() {
        return true;
    }

    @Override
    public boolean isNetwork() {
        return false;
    }

    @Override
    public ByteTreeItem getData() {
        return this.data;
    }

    @Override
    public String groupName() {
        return VisualNode.HOST;
    }

    @Override
    public boolean hasDetails() {
        return true;
    }
    
    @Override
    public int hashCode() {
        return data.hash;
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
    }

    @Override
    public int getVisualAgg(int id) {
        return this.getParent().getVisualAgg(id);
    }
}
