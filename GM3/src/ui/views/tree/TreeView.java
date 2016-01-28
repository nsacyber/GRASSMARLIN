/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import ICSDefines.PrettyPrintEnum;
import ui.views.tree.visualnode.HostVisualNode;
import ui.views.tree.visualnode.RootVisualNode;
import ui.views.tree.visualnode.PeerVisualNode;
import ui.views.tree.visualnode.NetworkVisualNode;
import ui.views.tree.visualnode.VisualNode;
import core.ViewUtils;
import core.types.ByteTreeItem;
import core.types.VisualDetails;
import core.types.InvokeObservable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import ui.GrassMarlin;
import ui.icon.Icons;
import ui.platform.UIPanel;
import ui.views.viewfilter.ViewFilter;

/**
 * The tree view is the primary GUI polling point for the entire UI. (Logical
 * graph and summary tab.) Since we use a polling model we want to re-use the
 * post-data we extract and process from storage.
 */
public final class TreeView extends UIPanel {

    public static boolean DEBUG = false;

    private static final Color NEW_COLOR = Color.GREEN;
    private static final Color UPDATE_COLOR = Color.CYAN;

    VisualNode root;
    JTree logicalTree;
    PhysicalTree physicalTree;

    Map<Integer, VisualNode> networks, hosts;
    Map<Integer, HashSet<Integer>> connections;
    Map<Integer, Integer> counts;
    Stack<VisualNode> stack;
    DefaultTreeModel model;
    SwingWorker worker;
    ViewFilter<ByteTreeItem> viewFilter;
    TreeFXRenderer fxRenderer;
    VisualNode lastSelection;
    VisualNode lastRightClicked;
    List<Runnable> runnables;
    JPopupMenu options;
    TreeDetailPopup popup;
    Boolean maintenanceUpdate; // update when there is maintenance to do, like a icon changed behind the scenes
    JScrollPane scroll;
    int hoveredRow;
    Set<VisualNode> expandedList;
    /**
     * Fires when new TreeViewNodes are available
     */
    final InvokeObservable pollObserver;
    /**
     * Fires when a node has changed its details, like the display name or icon
     * for example.
     */
    final InvokeObservable nodeUpdateObserver;
    /**
     * Fires when a node should be focused on in other parts of the view
     */
    final InvokeObservable nodeFocusObserver;
    /**
     * Fires when the "Edit Network Subnet Mask" menu item is clicked.
     */
    final InvokeObservable subnetChangeRequest;
    private boolean updatesAreLocked;

    public TreeView(ViewFilter<ByteTreeItem> viewFilter) {
        super("Network Tree Map");
        expandedList = Collections.newSetFromMap(new ConcurrentHashMap<>());
        hoveredRow = 0;
        popup = new TreeDetailPopup();
        connections = Collections.synchronizedMap(new HashMap<>());
        runnables = Collections.synchronizedList(new ArrayList<>());
        networks = Collections.synchronizedMap(new HashMap<>());
        counts = Collections.synchronizedMap(new HashMap<>());
        maintenanceUpdate = false;
        hosts = new HashMap<>();
        stack = new Stack<>();
        options = initPopup();
        this.viewFilter = viewFilter;
        physicalTree = new PhysicalTree();
        root = newRootNode();
        model = new DefaultTreeModel(root);
        logicalTree = new JTree(model) {

            @Override
            public boolean getScrollableTracksViewportWidth() {
                /* avoids swing concancy issue */
                return true;
            }

            @Override
            public void scrollPathToVisible(TreePath path) {
                super.scrollPathToVisible(path);
            }
        };
        logicalTree.setScrollsOnExpand(false);
        logicalTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                TreePath path = logicalTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    VisualNode n = (VisualNode) path.getLastPathComponent();
                    lastRightClicked = n;
                    if (n != null) {
                        hovering(e);
                        if (SwingUtilities.isRightMouseButton(e)) {
                            openOptions(n, e.getLocationOnScreen());
                        }
                    }
                }
            }
        });
        logicalTree.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hovering(null);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                hovering(e);
            }
        });
        fxRenderer = new TreeFXRenderer(this)
                .run();
        pollObserver = new InvokeObservable(this);
        nodeFocusObserver = new InvokeObservable(this);
        nodeUpdateObserver = new InvokeObservable(this);
        subnetChangeRequest = new InvokeObservable(this);
        initComponent();
    }

    public VisualNode newRootNode() {
        return new RootVisualNode()
                .addDecorator(new ColorDecorator(Color.BLUE))
                .addDecorator(new LabelDecorator("Grassmarlin", Icons.Grassmarlin));
    }

    public void hovering(MouseEvent e) {
        SwingUtilities.invokeLater(() -> {
            hoveringInternal(e);
        });
    }

    private void hoveringInternal(MouseEvent e) {
        if (e == null) {
            popup.setVisible(false);
            return;
        }
        Point p = e.getPoint();
        int row = logicalTree.getRowForLocation(p.x, p.y);
        if (hoveredRow == row && popup.isVisible()) {
            return;
        }
        if (row == 0) {
            hovering(null);
        } else {
            this.hoveredRow = row;
            TreePath path = logicalTree.getPathForRow(row);
            if (path == null || this.options.isVisible()) {
                hovering(null);
            } else {
                VisualNode node = (VisualNode) path.getLastPathComponent();
                popup.showDetails(null, e.getLocationOnScreen(), node);
            }
        }
    }

    public JTree getTree() {
        return logicalTree;
    }

    /*
     Find item in View
     Show Details
     Show Connections
     Set Category
     Set Role
     */
    JMenuItem findInViewBtn = new JMenuItem("Find in View");
    JMenuItem showDetailsBtn = new JMenuItem("Show Details");
    JMenuItem showConnectionsBtn = new JMenuItem("Show Connections");
    JMenu setCategoryBtn = new JMenu("Set Category");
    JMenu setRoleBtn = new JMenu("Set Role");
    JMenu sortMenu = new JMenu("Sort");
    JMenuItem collapseChildren = new JMenuItem("Collapse Children");
    JMenuItem expandChildren = new JMenuItem("Expand Children");
    JMenuItem editSubnetMask = new JMenuItem("Edit Network Subnet Mask");

    /**
     * Calls {@link #fireFocusRequest(ui.views.tree.visualnode.VisualNode) } on
     * the hashcode of the current {@link #clearSelection(java.util.function.Consumer)
     * } callback value.
     *
     * @param nil Parameter ignored.
     */
    private void findInViewEvent(Object nil) {
        this.clearSelection(this::fireFocusRequest);
    }

    /**
     * Shows a details dialog for the {@link #clearSelection(java.util.function.Consumer)
     * } callback value;
     *
     * @param nil Parameter ignored.
     */
    private void showDetails(Object nil) {
        this.clearSelection(node -> ViewUtils.showDetailEditor(node, this::fireNodeChange));
    }

    /**
     * Shows a connection dialog for the {@link #clearSelection(java.util.function.Consumer)
     * } callback value;
     *
     * @param nil Parameter ignored.
     */
    private void showConnections(Object nil) {
        this.clearSelection(ViewUtils::showConnectionDialog);
    }

    JPopupMenu initPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(findInViewBtn);
        findInViewBtn.addActionListener(this::findInViewEvent);
        menu.add(showDetailsBtn);
        showDetailsBtn.addActionListener(this::showDetails);
        menu.add(showConnectionsBtn);
        showConnectionsBtn.addActionListener(this::showConnections);
        menu.add(setCategoryBtn);
        setupMenu(setCategoryBtn, ICSDefines.Category.values(), this::setCategory);
        menu.add(setRoleBtn);
        setupMenu(setRoleBtn, ICSDefines.Role.values(), this::setRole);
        menu.add(sortMenu);

        JMenuItem sortByNameBtn = new JMenuItem("Name");
        JMenuItem sortByAddress = new JMenuItem("Address");
        sortMenu.add(sortByNameBtn);
        sortByNameBtn.addActionListener(e -> sort(this::nameComparator));
        sortMenu.add(sortByAddress);
        sortByAddress.addActionListener(e -> sort(this::addressComparator));

        menu.addSeparator();
        menu.add(editSubnetMask);
        editSubnetMask.addActionListener(e -> {
            this.clearSelection(node -> {
                if (node.isHost()) {
                    node = (VisualNode) node.getParent();
                }
                fireSubnetChanged(node);
            });
        });
        menu.addSeparator();
        menu.add(collapseChildren);
        collapseChildren.addActionListener(e -> 
            clearSelection(this::collapseChildren)
        );
        menu.add(expandChildren);
        expandChildren.addActionListener(e -> 
            clearSelection(this::expandChildren)
        );
        return menu;
    }

    public synchronized void sortDefault() {
        VisualNode root = getRoot();
        sort(root, this::addressComparator);
        root.getChildren().listIterator().forEachRemaining(child -> sort(child, this::addressComparator));
    }

    public Integer nameComparator(VisualNode n1, VisualNode n2) {
        int compare;
        boolean b1 = n1.hasUserDefinedName();
        boolean b2 = n2.hasUserDefinedName();
        if (b1 && !b2) {
            compare = 1;
        } else if (!b1 && b2) {
            compare = -1;
        } else if (b1 && b2) {
            compare = n1.getName().compareTo(n2.getName());
        } else {
            compare = addressComparator(n1, n2);
        }
        return compare;
    }

    public Integer addressComparator(VisualNode n1, VisualNode n2) {
        int[] ints1 = ViewUtils.toInts(n1.getAddressHash());
        int[] ints2 = ViewUtils.toInts(n2.getAddressHash());
        int ret = 0;
        int len = Math.min(ints1.length, ints2.length);
        for (int i = 0; i < len && ret == 0; ++i) {
            ret = Integer.compare(ints1[i], ints2[i]);
        }
        return ret;
    }

    private void sort(BiFunction<VisualNode, VisualNode, Integer> compareFunction) {
        clearSelection(node -> sort(node, compareFunction));
    }

    private void sort(VisualNode node, BiFunction<VisualNode, VisualNode, Integer> compareFunction) {
        node.getChildren().sort(compareFunction::apply);
        SwingUtilities.invokeLater(this::refreshLater);
    }

    void refresh() {
        SwingUtilities.invokeLater(() -> {
            model.reload();
            ArrayList<VisualNode> list = new ArrayList<>(this.expandedList);
            list.stream()
                    .map(VisualNode::getPath)
                    .map(TreePath::new)
                    .forEach(logicalTree::expandPath);
        });
    }

    private void refreshLater() {
        JTree tree = this.logicalTree;
        int rows = tree.getRowCount();
        expandedList.clear();
        for (int i = 0; i < rows; ++i) {
            if (tree.isExpanded(i)) {
                TreePath path = logicalTree.getPathForRow(i);
                if (path != null) {
                    Object obj = path.getLastPathComponent();
                    if (obj != null) {
                        VisualNode node = (VisualNode) obj;
                        expandedList.add(node);
                    }
                }
            }
        }
        refresh();
    }

    Integer getHashforRow(int row) {
        Integer hash = 0;
        TreePath path = logicalTree.getPathForRow(row);
        if (path != null) {
            Object obj = path.getLastPathComponent();
            if (obj != null) {
                VisualNode node = (VisualNode) obj;
                hash = node.hashCode();
            }
        }
        return hash;
    }

    private VisualNode clearSelection(Consumer<VisualNode> andThen) {
        VisualNode node = lastRightClicked;
        lastRightClicked = null;
        if (node != null && andThen != null) {
            andThen.accept(node);
        }
        return node;
    }

    private void setupMenu(JMenu menu, PrettyPrintEnum[] values, BiConsumer<VisualNode, Enum> applyFunction) {
        for (int i = 0; i < values.length; ++i) {
            PrettyPrintEnum value = values[i];
            String text = value.getPrettyPrint();
            JMenuItem item = new JMenuItem(text);
            item.addActionListener(e -> {
                VisualNode node = lastRightClicked;
                lastRightClicked = null;
                if (node != null) {
                    applyFunction.accept(node, value.get());
                }
            });
            menu.add(item);
        }
    }

    private void setCategory(VisualNode node, Enum enumValue) {
        if (node.hasDetails()) {
            ICSDefines.Category category = (ICSDefines.Category) enumValue;
            node.getDetails().setCategory(category);
            node.getDetails().image.setIcon(Icons.valueOf(category.iconValue));
            this.fireNodeChange(node);
        }
    }

    private void setRole(VisualNode node, Enum enumValue) {
        node.getDetails().setRole((ICSDefines.Role) enumValue);
        this.fireNodeChange(node);
    }

    public VisualNode dataFromHash(Integer hash) {
        return hosts.containsKey(hash) ? hosts.get(hash) : networks.get(hash);
    }

    void openOptions(VisualNode n, Point p) {
        if (n.isRoot()) {
            JPopupMenu menu = new JPopupMenu();
            menu.add(new JMenuItem("Collapse")).addActionListener(e -> 
                clearSelection(this::collapseChildren)
            );
            menu.add(new JMenuItem("Expand")).addActionListener(e -> 
                clearSelection(this::expandChildren)
            );
            JMenu sort = new JMenu("Sort");
            menu.add(sort);
            sort.add(new JMenuItem("Address")).addActionListener(e -> sort(this::addressComparator));
            sort.add(new JMenuItem("Name")).addActionListener(e -> sort(this::nameComparator));
            menu.setLocation(p);
            menu.setInvoker(logicalTree);
            menu.setVisible(true);
        } else {
            if (n.isHost() || n.isNetwork()) {
                boolean host = n.isHost();
                boolean network = n.isNetwork();
                showDetailsBtn.setEnabled(host || network);
                showConnectionsBtn.setEnabled(host);
                setCategoryBtn.setEnabled(host);
                setRoleBtn.setEnabled(host);
                sortMenu.setEnabled(network);
            }
            options.setLocation(p);
            options.setInvoker(logicalTree);
            options.setVisible(true);
            if (this.popup != null) {
                this.popup.setVisible(false);
            }
        }
    }

    public InvokeObservable getNodeFocusObserver() {
        return nodeFocusObserver;
    }

    public InvokeObservable getPollObserver() {
        return pollObserver;
    }

    void initComponent() {
        scroll = new JScrollPane();
        scroll.setViewportView(logicalTree);
        Border b = BorderFactory.createEmptyBorder();
        setBorder(b);
        scroll.setBorder(b);
        scroll.setViewportBorder(b);
        logicalTree.setCellRenderer(fxRenderer);
        add(scroll, BorderLayout.CENTER);
    }

    public DefaultTreeModel getModel() {
        return model;
    }

    /**
     * Determines if the internal SwingWorker in this component is running.
     *
     * @return
     */
    public boolean busy() {
        return worker != null;
    }

    public boolean update() {
        if (worker == null) {
            worker = new SwingWorker() {
                @Override
                protected Object doInBackground() {
                    try {
                        if (!runnables.isEmpty()) {
                            runnables.forEach(Runnable::run);
                            runnables.clear();
                        } else if (!TreeView.this.updatesAreLocked) {
                            int items = networks.size() + hosts.size();
                            viewFilter.stream().forEach(host -> ViewUtils.networkOf(host, (netIp, netHash) -> walk(host, netIp, netHash)));
                            if (items != networks.size() + hosts.size()) {
                                change();
                            } else if (maintenanceUpdate) {
                                maintenanceUpdate = false;
                                change();
                            }
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(TreeView.class.getName()).log(Level.SEVERE, "Worker stopped unexpectedly.", ex);
                    } finally {
                        worker = null;
                    }
                    return true;
                }
            };
            worker.execute();
        }
        return false;
    }

    private void walk(ByteTreeItem hostData, String networkIp, Integer networkHash) {
        if (hostData.isVisible()) {
            VisualNode network = getNetwork(networkIp, networkHash, hostData);
            VisualNode host = getHost(hostData, network);

            hostData.sampleEdges(counts); // same as a putAll from a complex set
            Set<Integer> allPeers = getConnections(hostData);
            //        if( !hostData.hasSiblings() ) return;

            hostData.forwardEdges.forEach(peerData -> ViewUtils.networkOf(peerData, (ip, hash) -> {
                if (peerData.isVisible()) {
                    VisualNode peerNetwork = getNetwork(ip, hash, peerData);
                    VisualNode peer = getHost(peerData, peerNetwork);

                    Set<Integer> reversePeers = getConnections(peerData);

                    if (allPeers.add(peerData.hash)) {
                        VisualNode leaf = leafHost(peer, host);
                        host.addChild(leaf);
                        update(host);
                    }

                    if (reversePeers.add(hostData.hash)) {
                        VisualNode leaf = leafHost(host, peer);
                        peer.addChild(leaf);
                        update(peer);
                    }
                }
            }));

            hostData.backEdges.forEach(peerData -> ViewUtils.networkOf(peerData, (ip, hash) -> {
                if (peerData.isVisible()) {
                    VisualNode peerNetwork = getNetwork(ip, hash, peerData);
                    VisualNode peer = getHost(peerData, peerNetwork);

                    Set<Integer> reversePeers = getConnections(peerData);

                    if (allPeers.add(peerData.hash)) {
                        VisualNode leaf = leafHost(peer, host);
                        host.addChild(leaf);
                        update(host);
                    }

                    if (reversePeers.add(hostData.hash)) {
                        VisualNode leaf = leafHost(host, peer);
                        peer.addChild(leaf);
                        update(peer);
                    }
                }
            }));
        }
    }

    private void update(VisualNode node) {
        node.getDecorator(ColorDecorator.class, dec -> {
            ((ColorDecorator) dec).fadeFrom(UPDATE_COLOR).fadeTo(Color.WHITE);
        });
    }

    /**
     * Prevents tree from being infinitely recursive by copying the original
     * node data into a leaf-node.
     *
     * @param host Host create a leaf-node copy of.
     * @return New leaf-node representation of a host.
     */
    VisualNode leafHost(VisualNode host, VisualNode parent) {
        return new PeerVisualNode(parent, host)
                .addDecorator(new LabelDecorator(Icons.Original_connection))
                .addDecorator(new ColorDecorator());
    }

    Set<Integer> getConnections(ByteTreeItem host) {
        if (connections.containsKey(host.hash)) {
            return connections.get(host.hash);
        }
        HashSet<Integer> set = new HashSet<>();
        connections.put(host.hash, set);
        return set;
    }

    VisualNode getHost(ByteTreeItem data, VisualNode network) {
        if (hosts.containsKey(data.hash)) {
            return hosts.get(data.hash);
        }

        VisualNode host = new HostVisualNode(network, data)
                .addDecorator(new LabelDecorator())
                .addDecorator(new ColorDecorator(NEW_COLOR));

        GrassMarlin.window.core.withKnowledgebase(kb -> {
            ViewUtils.runGeoIp(kb, host, this::fireNodeChange);
        });

        if (ViewUtils.isRoutableIP(data.hash)) {
            data.details.put(ViewUtils.WARNING_PROPERTY, "ROUTABLE ADDRESS");
        }

        hosts.put(host.hashCode(), host);
        network.addChild(host);
//        fireHostAdded( host, network );
        return host;
    }

    Integer lastNetworkHash;
    VisualNode cachedNetwork;

    VisualNode getNetwork(String ip, Integer hash, ByteTreeItem hostData) {
        if (hash.equals(lastNetworkHash)) {
            return cachedNetwork;
        }
        if (networks.containsKey(hash)) {
            return networks.get(hash);
        }

        VisualDetails detail = new VisualDetails();

        if (hostData.hasNetworkId()) {
            Integer networkId = hostData.getNetworkId();
            Integer mask = hostData.getNetworkMask();
            detail.setNetworkMask(mask);
            detail.setNetworkHash(networkId);
            int cidr = ViewUtils.getCIDR(mask);
            detail.setCidr(cidr);
        } else {
            detail.setNetworkMask(ViewUtils.LARGE_LAN_HASH);
            detail.setNetworkHash(hash);
        }

        VisualNode network = new NetworkVisualNode(root, detail)
                .addDecorator(new LabelDecorator())
                .addDecorator(new ColorDecorator(NEW_COLOR));

        cachedNetwork = network;
        networks.put(network.hashCode(), network);
        root.addChild(network);
        lastNetworkHash = network.hashCode();
//        fireNetworkAdded( network );
        return network;
    }

    public synchronized void clear() {
        networks.clear();
        hosts.clear();
        expandedList.clear();
        counts.clear();
        connections.clear();
        root = newRootNode();
        model = new DefaultTreeModel(root);
        logicalTree.setModel(model);
        physicalTree.clear();
        change();
    }

    void redraw() {
        SwingUtilities.invokeLater(() -> {
            logicalTree.setRowHeight(logicalTree.getRowHeight()); // fixes an issue
            logicalTree.repaint();
        });
    }

    /**
     * Invokes the changes that are sent to observers of the front-end.
     */
    void change() {
        if (this.updatesAreLocked) {
            return;
        }
        sortDefault();
        pollObserver.setChanged();
        pollObserver.notifyObservers(this);
        refresh();
    }

    public int networkCount() {
        return networks.size();
    }

    public int hostCount() {
        return hosts.size();
    }

    public VisualNode getRoot() {
        return root;
    }

    public InvokeObservable getNodeUpdateObserver() {
        return nodeUpdateObserver;
    }

    void fireNodeChange(VisualNode n) {
        SwingUtilities.invokeLater(() -> {
            nodeUpdateObserver.setChanged();
            nodeUpdateObserver.notifyObservers(n);
        });
    }

    public void focusItem(VisualNode node) {
        focusItem(node.hashCode());
    }

    public void focusItem(Integer arg) {
        SwingUtilities.invokeLater(() -> {
            VisualNode n = null;

            if (hosts.containsKey(arg)) {
                n = hosts.get(arg);
                expand(logicalTree, new TreePath(n.getParent().getPath()));
            } else if (networks.containsKey(arg)) {
                n = networks.get(arg);
            }

            if (n == null) {
                return;
            } else if (DEBUG) {
                System.out.println("focusItem arg=" + arg + ", node=" + n.getName());
            }

            n.getDecorator(ColorDecorator.class, dec -> {
                ((ColorDecorator) dec).fadeFrom(Color.RED).fadeTo(Color.WHITE);
            });
        });
    }

    /**
     * Fires the {@link #getNodeFocusObserver() } setting the update argument to
     * the VisualNode parameter.
     *
     * @param focusTarget VisualNode to focus on externally.
     */
    private void fireFocusRequest(VisualNode focusTarget) {
        if (focusTarget.hasDetails()) {
            nodeFocusObserver.setChanged();
            nodeFocusObserver.notifyObservers(focusTarget);
        }
    }

    public void lockUpdates() {
        this.updatesAreLocked = true;
    }

    public void unlockUpdates() {
        this.updatesAreLocked = false;
        change();
    }

    public Map<Integer, HashSet<Integer>> getConnections() {
        return connections;
    }

    public void showLogical() {
        scroll.setViewportView(logicalTree);
    }

    public void showPhysical() {
        scroll.setViewportView(physicalTree);
    }

    public PhysicalTree getPhysicalTree() {
        return physicalTree;
    }

    public void nodechanged(VisualNode node) {
        this.getModel().nodeChanged(node);
        node.setChanged(false);
    }

    public InvokeObservable getSubnetChangeRequest() {
        return this.subnetChangeRequest;
    }

    private void fireSubnetChanged(VisualNode node) {
        subnetChangeRequest.notifyObservers(node);
        subnetChangeRequest.setChanged();
    }

    private void expand(JTree t, TreePath p) {
        VisualNode node = (VisualNode) p.getLastPathComponent();
        if (node.isHost()) {
            return;
        }
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = p.pathByAddingChild(n);
                expand(t, path);
            }
        }
        t.expandPath(p);
    }

    private void expandEx(JTree t, VisualNode node) {
        t.expandPath( new TreePath(node.getPath()) );
        node.getChildren()
                .stream()
                .map(VisualNode::getPath)
                .map(TreePath::new)
                .forEach(t::expandPath);
    }
    
    private void collapseEx(JTree t, VisualNode node) {
        if( node.isRoot() ) {
            node.getChildren()
                    .stream()
                    .map(VisualNode::getPath)
                    .map(TreePath::new)
                    .forEach(t::collapsePath);
            t.expandPath(new TreePath(node.getPath()));
        } else {
            t.collapsePath(new TreePath(node.getPath()));
        }
    }
    
    private void expandChildren(VisualNode node) {
        expandEx(this.logicalTree, node);
    }

    private void collapseChildren(VisualNode node) {
        collapseEx(this.logicalTree, node);
    }

    public Collection<VisualNode> networks() {
        return this.networks.values();
    }

}
