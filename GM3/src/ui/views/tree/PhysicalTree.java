/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import ICSDefines.Category;
import core.topology.Device;
import core.topology.Entities;
import core.topology.Interface;
import core.topology.Ip;
import core.topology.TopologyTree;
import core.types.TriConsumer;
import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import ui.icon.Icons;

/**
 * Physical tree is a three level tree,
 * level 1, Root. There is no data here.
 * level 2, Devices. These items have core.topology.Device entities.
 * level 3, Interface. These items have core.topology.Interface entities.
 * Devices will either have a single interface, and then is consider a NIC. 
 * If a device has multiple interfaces, the device is a Switch, and its interfaces are ports.
 */
public class PhysicalTree extends JTree implements Observer {

    public abstract class StyleItem extends DefaultMutableTreeNode implements Comparable {

        public abstract String getText();

        public abstract Icon getIcon();

        public abstract Entities getEntity();
        
        public List<StyleItem> getChildren() {
            return Collections.list(this.children());
        }
        
        public boolean isDevice() {
            return getEntity() instanceof Device;
        }
        
        @Override
        public int compareTo(Object o) {
            return getText().compareTo(((StyleItem) o).getText());
        }
    }

    private class RootProxy extends StyleItem {

        final Icon icon = Icons.Grassmarlin.getIcon();
        
        @Override
        public boolean isRoot() {
            return true;
        }
        
        @Override
        public String getText() {
            return "Grassmarlin";
        }

        @Override
        public Icon getIcon() {
            return icon;
        }
        
        @Override
        public Entities getEntity() {
            return null;
        }
    }

    private class DeviceProxy extends StyleItem {

        public final Device device;
        public Image image;
        public Icon icon;

        public DeviceProxy(Device device) {
            this.device = device;
            this.image = device.getImage16();
            this.icon = new ImageIcon(this.image);
        }

        @Override
        public String getText() {
            return device.getDisplayText();
        }

        @Override
        public Icon getIcon() {
            if (this.image != device.getImage16()) {
                this.image = device.getImage16();
                this.icon = new ImageIcon(this.image);
            }
            return this.icon;
        }
        
        @Override
        public Entities getEntity() {
            return device;
        }
    }

    private class InterfaceProxy extends StyleItem {

        public final Interface iface;
        Icon icon = Icons.NetworkAdapter.getIcon();

        public InterfaceProxy(Interface iface) {
            this.iface = iface;
        }

        @Override
        public String getText() {
            return iface.getDisplayText();
        }

        @Override
        public Icon getIcon() {
            return this.icon;
        }

        @Override
        public Entities getEntity() {
            return this.iface;
        }
        
    }

    Integer dataSourceHash;
    RootProxy root;
    
    public PhysicalTree() {
        ((DefaultTreeModel) getModel()).setRoot(newRoot());
        this.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                StyleItem item = (StyleItem) value;
                label.setText(item.getText());
                label.setIcon(item.getIcon());
                return label;
            }
        });
    }
    
    private RootProxy newRoot() {
        RootProxy r = new RootProxy();
        this.root = r;
        return r;
    }
    
    public DefaultMutableTreeNode getRoot() {
        return this.root;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof TopologyTree)) {
            return;
        }
        TopologyTree tree = (TopologyTree) arg;

        if (dataSourceHash != null && dataSourceHash.equals(tree.hashCode())) {
            return;
        }

        RootProxy root = newRoot();
        List<Runnable> lookupTasks = new ArrayList<>();

        tree.aggregates.keySet().forEach(physicalNode -> {
            Device device = new Device(physicalNode);
            DeviceProxy deviceProxy = new DeviceProxy(device);

            physicalNode.ports.stream()
                    .map(Interface::new)
                    .forEach(device::addInterface);

            device.streamInterfaces()
                    .map(InterfaceProxy::new)
                    .sorted()
                    .forEach(deviceProxy::add);

            root.add(deviceProxy);
        });

        Stream.concat(tree.eachLeafNode(), tree.eachDisjunctNode()).forEach(node -> {
            Device dev = new Device(node)
                    .setIcon(node.isIncluded() ? Icons.Computer_plus : Icons.Computer_minus)
                    .setCategory(Category.WORKSTATION);

            Interface iface = new Interface(node.getMac(), Ip.MISSING_IP);
            DeviceProxy proxy = new DeviceProxy(dev);
            proxy.add(new InterfaceProxy(iface));
            root.add(proxy);

            if (dev.getVendor().isEmpty() && core.Core.kb.isPresent()) {
                lookupTasks.add(() -> {
                    try {
                        String vendor;
                        vendor = core.Core.kb.get().getOUI(iface.getMac().getOUI()).get();
                        dev.setVendor(vendor);
                        ((DefaultTreeModel) PhysicalTree.this.getModel()).nodeChanged(proxy);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(PhysicalTree.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        Logger.getLogger(PhysicalTree.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            }
        });

        runLater(lookupTasks);
        
        setModel(new DefaultTreeModel(root));
    }

    private void runLater(List<Runnable> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        SwingWorker sw = new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                tasks.forEach(Runnable::run);
                return true;
            }
        };
        sw.execute();
    }

    public void clear() {
        setModel(new DefaultTreeModel(new RootProxy()));
    }
    
    /**
     * If the tree has an Entity equal to the one passed to this method it will be selected.
     * @param obj An Entities instance that is contained within the tree
     */
    public void find(Object obj) {
        if( obj instanceof Entities ) {
            Entities entity = (Entities) obj;
            StyleItem item = getVisualItem(entity);
            if( item != null ) {
                TreePath path = new TreePath(item.getPath());
                scrollPathToVisible(path);
                setSelectionPath(path);
            } else {
                System.err.println("Item not found.");
            }
        }
    }
    
    private StyleItem getVisualItem(Entities entity) {
        StyleItem styleItem = null;
        for( StyleItem item : root.getChildren() ) {
            if( (styleItem = getVisualItem(item, entity)) != null ) {
                break;
            }
        }
        return styleItem;
    }
    
    private StyleItem getVisualItem(StyleItem current, Entities entity) {
        StyleItem styleItem = null;
        if( current.getEntity().getName().equals(entity.getName()) ) {
            styleItem = current;
        } else {
            for( StyleItem item : current.getChildren() ) {
                if( item.getEntity().getName().equals(entity.getName()) ) {
                    styleItem = item;
                    break;
                }
            }
        }
        return styleItem;
    }
    
    public void eachDevice(TriConsumer<Boolean,Device,List<Interface>> cb) {
        root.getChildren().forEach( item -> {
            Device dev = (Device) item.getEntity();
            List<Interface> ifaces = dev.getInterfaces();
            boolean isSwitch = ifaces.size() > 1;
            cb.accept(isSwitch, dev, ifaces);
        });
    }

}
