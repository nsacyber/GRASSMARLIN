/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree.visualnode;

import core.ViewUtils;
import core.types.ByteTreeItem;
import core.types.VisualDetails;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import ui.views.tree.TreeNodeDecorator;

/**
 *
 * @author BESTDOG
 */
public abstract class VisualNode extends DefaultMutableTreeNode {

    public static final String NETWORK = "networks";
    public static final String HOST = "host";
    public static final String ROOT = "root";
    
    private final Set<TreeNodeDecorator> decorators;
    private final List<VisualNode> children;
    private final VisualNode parent;

    private boolean changed;
    
    protected VisualNode(VisualNode parent) {
        this.parent = parent;
        this.decorators = Collections.synchronizedSet(new HashSet<>());
        this.children = new CopyOnWriteArrayList<>();
        changed = true;
    }

    public abstract int getAddressHash();
    
    public abstract VisualDetails getDetails();
    
    public abstract String getAddress();
    public abstract String getName();
    public abstract String getText();
    public abstract String groupName();
    
    public abstract void setName(String name);

    public abstract boolean isHost();
    public abstract boolean isNetwork();
    public abstract boolean hasDetails();
    public abstract boolean isExpanded(Integer id);
    public abstract void setExpanded(Integer id, boolean collapsed);
    
    public boolean hasChanged() {
        return changed;
    }
    
    public abstract ByteTreeItem getData();
    
    public void setChanged(boolean changed) {
        this.changed = changed;
    }
    
    public String getHardwareOui() {
        String hardwareOui = null;
        if( this.getData() != null ) {
            StringBuilder b = new StringBuilder();
            Byte[] bytes = getData().MAC;
            for( int i = 0; i <3; i++ )
               b.append( String.format("%02X", bytes[i]) );
            hardwareOui = b.toString();
        }
        return hardwareOui;
    }

    public String getSubnet() {
        return ViewUtils.ipString(getDetails().getNetworkMask());
    }

    public boolean hasUserDefinedName() {
        return !getAddress().equals(getName());
    }

    public List<VisualNode> getChildren() {
        return this.children;
    }

    public void addChild(VisualNode node) {
        getChildren().add(node);
    }

    public Set<TreeNodeDecorator> getDecorators() {
        return this.decorators;
    }

    public VisualNode addDecorator(TreeNodeDecorator decorator) {
        getDecorators().add(decorator);
        return this;
    }

    public void decorate(TreeCellRenderer renderer, boolean selected, long time) {
        this.decorators.forEach(dec -> {
            dec.accept(renderer, this, selected, time);
        });
    }

    public void getDecorator(Class decoratorClass, Consumer<TreeNodeDecorator> cb) {
        getDecorators().forEach(dec -> {
            if (dec.classEquals(decoratorClass)) {
                cb.accept(dec);
            }
        });
    }

    public Icon getIcon() {
        return getDetails().image.getScaledIcon();
    }

    public int getVisualAgg(int id) {
        return getDetails().group(id);
    }

    public int getVisualRow(int id) {
        return getDetails().row(id);
    }

    public void setVisualAgg(int id, int group) {
        this.getDetails().setGroup(id, group);
    }

    public void setVisualRow(int id, int row) {
        this.getDetails().setRow(id, row);
    }

    public void removeVisualIndex(final int id) {
        if (this.hasDetails()) {
            this.getDetails().removeVisualIndex(id);
        }
        if (this.children != null) {
            final Consumer<VisualNode> removeVisualIndex = p -> p.removeVisualIndex(id);
            this.children.forEach(removeVisualIndex);
        }
    }

    public void resetVisualRows() {
        if (this.hasDetails()) {
            this.getDetails().resetVisualRows();
        }
        if (this.children != null) {
            this.children.forEach(VisualNode::resetVisualRows);
        }
    }

    public boolean contains(VisualNode node) {
        return false;
    }

    @Override
    public VisualNode getParent() {
        return this.parent;
    }
    
    @Override
    public boolean getAllowsChildren() {
        return true;
    }
    
    @Override
    public boolean isRoot() {
        return false;
    }
    

    @Override
    public int getChildCount() {
        return this.children.size();
    }
    
    @Override
    public TreeNode getChildAt(int index) {
        return this.children.get(index);
    }

    @Override
    public int getIndex(TreeNode aChild) {
        return this.children.indexOf(aChild);
    }
    
    @Override
    public Enumeration children() {
        return Collections.enumeration(children);
    }
    
}
