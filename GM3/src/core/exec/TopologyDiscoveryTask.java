/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exec;

import core.Core;
import core.topology.Mac;
import core.topology.PhysicalNode;
import core.topology.Port;
import core.topology.TopologyNode;
import core.topology.TopologyTree;
import core.types.ImmutableMap;
import core.types.LogEmitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;

/**
 *
 */
public class TopologyDiscoveryTask extends Task {

    public final TopologyTree tree;
    final List<Observer> observers;
    Collection<PhysicalNode> data;
    
    public TopologyDiscoveryTask(TopologyTree tree, List<Observer> observers) {
        this.tree = tree;
        this.observers = observers;
    }

    public TopologyDiscoveryTask(List<Observer> observers) {
        this(new TopologyTree(), observers);
    }
    
    public TopologyDiscoveryTask() {
        this(new TopologyTree(), null);
    }

    @Override
    public void run() {
        copyData();
        tree.setDataSet(getDataSet());

        /* create a set of skeletal paths omitting leaf nodes */
        Map<Port, Set<Port>> skeletalPaths = new ImmutableMap(HashSet::new);

        /* makes sure all port macs are assigned as port macs */
//        checkAllAddresses(tree);
        
        iterator().forEachRemaining(physicalNode -> {
            remotePorts(physicalNode).forEachRemaining(remotePort -> {
                if (physicalNode.macs.contains(remotePort.mac)) {
                    Port localPort = physicalNode.portSees(remotePort.mac);
                    skeletalPaths.get(localPort).add(remotePort);
                    skeletalPaths.get(remotePort).add(localPort);
                }
            });
        });

        if( skeletalPaths.isEmpty() ) {
            /* topology is impossible */
            return;
        }
        
        /* find direct connections between each internal path, these will likely be hub-to-hub connections */
        Map<Port, Set<Port>> externalPaths = expandInternalTopology(tree, skeletalPaths);
        /* make sure all external (non inner) hubs are connected and finds where leaf devices (hosts) are connected */
        Map<Port, Set<Port>> unknownTopology = insertExternalTopology(tree, externalPaths);
        /* finds and connects leaf nodes (hosts) */
        connectLeafNodes(tree, getDataSet());

        if (!unknownTopology.isEmpty()) {
            alertIncompleteTopology();
        }

        /**
         * pre-creates groups for generating the visualization
         */
        iterator().forEachRemaining(pn -> {
            Set<TopologyNode> nodes = tree.aggregates.get(pn);
            pn.ports.forEach(port -> {
                nodes.add(tree.getNode(port));
            });
        });

        complete();
    }

    /**
     * Connects host nodes as edge (leaf) nodes to a network.
     * @param root The TopologyTree "root" data set which is filled with determined topology data.
     * @param nodes The entire source dataset topology is being determined from.
     */
    private void connectLeafNodes(TopologyTree root, Collection<PhysicalNode> nodes) {

        /* count occurences of each address */
        ImmutableMap<Mac, Set<Port>> occurences = new ImmutableMap(HashSet::new);
        nodes.stream().flatMap(node -> node.ports.stream()).forEach(port
                -> port.macs().forEach(mac
                        -> occurences.get(mac).add(port)
                )
        );

        /* remove any port addresses, these are obviously not leaf-nodes */
        nodes.stream().flatMap(node -> node.ports.stream())
                .map(Port::getMac)
                .forEach(occurences::remove);

        // remove addresses that are assigned to switch-ports
        occurences.entrySet().removeIf(e -> e.getKey().isPortMac());

        // ports which connect to incomplete topology portions (clouds)
        Map<Mac,Set<Port>> incompleteTopologyPoints = new ImmutableMap<>(HashSet::new);
        
        // remove ports that have multiple visible macs, these are not directly connected
        occurences.forEach((mac, ports) -> {
            ports.removeIf(port -> {
                boolean removeIf = port.macs().size() > 1;
                if( removeIf ) {
                    incompleteTopologyPoints.get(mac).add(port);
                }
                return removeIf;
            });
        });
        
        Map<Port,List<TopologyNode>> cloudGroups = new ImmutableMap<>(ArrayList::new);

        // there is a one to one connection to make them connect in the tree
        occurences.forEach((mac, ports) -> {
            /*System.out.println( mac + "  " + ports + "  " + ports.size() );*/
            if( !ports.isEmpty() ) {
                
                incompleteTopologyPoints.remove(mac);
                
                Port port = ports.iterator().next();
                TopologyNode hostNode = root.newNode(mac);
                TopologyNode portNode = root.getNode(port);
                root.getEdge(portNode, hostNode);
                root.addLeafNode( hostNode );
                
            } else {
                Port port = getBestPort( mac, incompleteTopologyPoints.get(mac) );
                TopologyNode host = root.newNode(mac);
                root.addDisjunctNode( host );
                if( port != null ) {
                    cloudGroups.get(port).add(host);
                }
                alertIncompleteTopology();
            }
        });
        
        cloudGroups.forEach( (port, remoteNodes) -> {
            TopologyNode cloudNode = root.newCloud();
            TopologyNode portNode = root.getNode(port);
            
            root.getEdge(portNode, cloudNode);
            
            remoteNodes.forEach( node -> 
                root.getEdge(cloudNode, node)
            );
        });
        
//        this.printMap(incompleteTopologyPoints);
    }

    private Port getBestPort( Mac mac, Collection<Port> ports ) {
        Port p = ports.stream().sorted((p0,p1) ->  p0.macs().size() - p1.macs().size() ).findFirst().orElse(null);
        return p;
    }
    
    /**
     * Connects partially internal, or "external", nodes.
     * This will always find switches that are on the edge of a domain boundary.
     * So this is a switch with one or no connections to other switches.
     * @param root The TopologyTree "root" data set which is filled with determined topology data.
     * @param inputMap The map of all "phase 1" processed data.
     * @return A port map of the repainting unconnected address forward tables.
     */
    private Map<Port, Set<Port>> insertExternalTopology(TopologyTree root, Map<Port, Set<Port>> inputMap) {
        HashMap<Port, Set<Port>> returnMap = new HashMap<>(inputMap);

        returnMap.entrySet().removeIf(e -> {
            Port port = e.getKey();
            Set<Port> set = e.getValue();

            if (root.hasNode(port)) {
                set.removeIf(root::hasNode);
            }

            return e.getValue().isEmpty();
        });

        return returnMap;
    }

    /**
     * Connects completely internal nodes.
     * These are always switches that are connected and seen between two switch.
     * @param root The TopologyTree "root" data set which is filled with determined topology data.
     * @param inputMap The map of all "phase 1" processed data.
     * @return A port map of the repainting unconnected address forward tables.
     */
    private Map<Port, Set<Port>> expandInternalTopology(TopologyTree root, Map<Port, Set<Port>> inputMap) {

        int thisSize = 0;
        int nextSize = getChangeHash(inputMap);
        Map<Port, Set<Port>> returnMap = new HashMap<>(inputMap);

        while (nextSize != thisSize) {
            thisSize = nextSize;

            returnMap.entrySet().forEach(e -> {

                Port port = e.getKey();
                Set<Port> set = e.getValue();
                TopologyNode node = root.getNode(port);

                set.removeIf(remotePort -> {
                    TopologyNode remoteNode = root.getNode(remotePort);

                    if (node.edges.isEmpty()) {
                        if (returnMap.containsKey(remotePort) && returnMap.get(remotePort).contains(port)) {
                            root.getEdge(node, remoteNode);
                            returnMap.get(remotePort).remove(port);
                            return true;
                        }
                    }

                    return false;
                });

            });

            returnMap.entrySet().removeIf(e -> e.getValue().isEmpty());

            nextSize = getChangeHash(returnMap);
        }

        return returnMap;
    }

    void alertIncompleteTopology() {
        System.err.println("incomplete topology information");
        LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, "Topology may be incomplete.");
    }
    
    /**
     * Creates a unique hash of a size of a set of sets, this is used to determine if a set has undergone any modification
     * by create a unique key of its key size and value sizes.
     * @param map Map to calculate the unique size hash of.
     * @return A unique size hash.
     */
    public int getChangeHash(Map<Port, Set<Port>> map) {
        return map.values().stream().mapToInt(s->s.size()).sum() * 3 + map.size() * 5;
    }

    /**
     * Excluding the argument's ports creates an iterator over all ports, thus all ports remote to the {@code node}..
     * @param node Node to not iterator ports from.
     * @return Iterator of all remote ports except those which belong to {@code node}.
     */
    private Iterator<Port> remotePorts(PhysicalNode node) {
        return getPipeline().getRawPhysicalData()
                .stream()
                .filter(pn -> !node.equals(pn))
                .map(pn -> pn.ports)
                .flatMap(Set::stream)
                .iterator();
    }

    /**
     * Iterator of {@link #getDataSet() }.
     * @return Iterator to the collection of PhysicalNodes from the {@link #pipeline}.
     */
    private Iterator<PhysicalNode> iterator() {
        return getDataSet().iterator();
    }

    /**
     * Gets the collection of PhysicalNodes from the {@link #pipeline}..
     * @return All parsed physical data in a collection of PhysicalNodes.
     */
    private Collection<PhysicalNode> getDataSet() {
        return this.data;
    }

    /**
     * Copies the dataset so that it wont change during processing.
     */
    private void copyData() {
        this.data = new HashSet(getPipeline().getRawPhysicalData());
    }
    
    /**
     * Pretty prints a map.
     * @param map Map to pretty print.
     */
    private void printMap(Map<Mac,Set<Port>> map) {
        map.forEach((key, set) -> {
            System.out.println(key);
            set.forEach(val -> {
                System.out.println("  " + val);
            });
        });
    }

    /**
     * Pretty prints all PhysicalNode data.
     */
    private void printAll() {
        iterator().forEachRemaining(pn -> {
            System.out.println(pn);
            pn.vlans.forEach(vlan -> {
                System.out.println(pn + "  " + vlan);
                vlan.ports.forEach(port -> {
                    System.out.println(pn + "    " + port);
                    port.macs().forEach(mac -> {
                        System.out.println(pn + "      " + mac);
                    });
                });
            });
        });
    }
    
    @Override
    protected void complete() {
        if( observers != null ) {
            observers.forEach(o -> o.update(null, tree) );
        }
        super.complete();
    }

}
