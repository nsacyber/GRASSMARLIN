/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.types;

import ICSDefines.Category;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ui.custom.ProxyImage;


/**
 * <pre>
 * Provides the fewest amount of data elements required to support
 * contextual iteration. ("this is network node" or "this is an interface")
 * </pre>
 */
public class VisualDetails extends DataDetails {
    
    private static final String ICON_FIELD = "icon";
    
    private int CIDR = 24;
    private Integer networkId;
    private Integer networkMask;
    public final ProxyImage image;
    Map<Integer,Integer> rows;
    Map<Integer,Integer> groups;
    Map<Integer,Boolean> expanded;

    /**
     * Used to ensure a host doesn't have the same fingerprint run several times.
     * The integers correspond to the Operation Ids in the generated source for Filter.java.
     */
    public Set<Integer> repeatProtection;
    
    public VisualDetails() {
        super();
        rows = new ConcurrentHashMap<>();
        groups = new ConcurrentHashMap<>();
        expanded = new ConcurrentHashMap<>();
        image = new ProxyImage();
        repeatProtection = Collections.synchronizedSet(new HashSet<>());
        networkId = null;
        networkMask = null;
    }
    
    public ProxyImage getImage() {
        return image;
    }
    
    public int cidr() {
        return CIDR;
    }
    
    public void setCidr(int cidr) {
        this.CIDR = cidr;
    }
    
    public void setNetworkHash(int networkHash) {
        this.networkId = networkHash;
    }
    
    public void setNetworkMask(int networkMask) {
        this.networkMask = networkMask;
    }
    
    public int getNetworkMask() {
        return networkMask;
    }
    
    public int getNetworkId() {
        return networkId;
    }
    
    public boolean hasExplicitNetwork() {
        return networkMask != null && networkId != null;
    }
    
    public void resetVisualRows() {
        this.rows.clear();
        this.groups.clear();
        this.expanded.clear();
    }

    @Override
    public void setCategory(Category category) {
        super.setCategory(category);
        this.image.set(category.iconValue);
    }
    
    public void setIcon(String iconName) {
        put(ICON_FIELD, iconName);
        image.set(iconName);
    }

    public boolean isExpanded(Integer id) {
        return expanded.getOrDefault(id, Boolean.TRUE);
    }

    public void setExpanded(Integer id, boolean collapsed) {
        this.expanded.put(id, collapsed);
    }

    public int group(Integer hashCode) {
        return this.groups.getOrDefault(hashCode, -1);
    }

    public int row(Integer hashCode) {
        return this.rows.getOrDefault(hashCode, -1);
    }

    public void setGroup(Integer hashCode, int group) {
        this.groups.put(hashCode, group);
    }

    public void setRow(Integer hashCode, int row) {
        this.rows.put(hashCode, row);
    }

    public void removeVisualIndex(final Integer id) {
        this.groups.remove(id);
        this.rows.remove(id);
    }
}
