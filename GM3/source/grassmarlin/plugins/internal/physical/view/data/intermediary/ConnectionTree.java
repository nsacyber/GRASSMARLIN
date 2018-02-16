package grassmarlin.plugins.internal.physical.view.data.intermediary;

import grassmarlin.Event;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.PhysicalConnection;

import java.util.*;
import java.util.stream.Collectors;

public class ConnectionTree {
    private final Map<HardwareVertex, ConnectionTreeNode> leafNodes;
    private final Map<ConnectionTreeNode, Collection<HardwareVertex>> announcedDatasets;

    public Event<ConnectionTreeNode> onTreeCreated;
    public Event<ConnectionTreeNode> onTreeRemoved;
    public Event<ConnectionTreeNode> onTreeMembershipChanged;

    public ConnectionTree() {
        this.leafNodes = new HashMap<>();
        this.announcedDatasets = new HashMap<>();

        this.onTreeCreated = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onTreeRemoved = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onTreeMembershipChanged = new Event<>(Event.PROVIDER_IN_THREAD);
    }

    public void absorb(final ConnectionTree other) {
        this.leafNodes.putAll(other.leafNodes);
        this.announcedDatasets.putAll(other.announcedDatasets);
    }

    public void addVertex(final HardwareVertex vertex) {
        final ConnectionTreeNode nodeNew = new ConnectionTreeNode.Leaf(vertex);
        this.leafNodes.put(vertex, nodeNew);
        this.announcedDatasets.put(nodeNew, nodeNew.getAllLeafValues());
        this.onTreeCreated.call(nodeNew);
    }
    public void removeVertex(final HardwareVertex vertex) {
        final List<PhysicalConnection> connections = this.leafNodes.get(vertex).getAllConnections().stream().filter(connection -> connection.getSource().equals(vertex) || connection.getDestination().equals(vertex)).collect(Collectors.toList());
        for(final PhysicalConnection connection : connections) {
            this.removeConnection(connection);
        }
        final ConnectionTreeNode removed = this.leafNodes.remove(vertex);
        if(removed != null) {
            this.announcedDatasets.remove(removed);
            this.onTreeRemoved.call(removed);
        }
    }

    public void addConnection(final PhysicalConnection connection) {
        final ConnectionTreeNode nodeSource = this.leafNodes.get(connection.getSource());
        final ConnectionTreeNode nodeDestination = this.leafNodes.get(connection.getDestination());

        final ConnectionTreeNode ancestorCommon = nodeSource.getFirstCommonAncestor(nodeDestination);
        if(ancestorCommon == null) {
            final ConnectionTreeNode.Branch rootNew = new ConnectionTreeNode.Branch(nodeSource.getRootAncestor(), nodeDestination.getRootAncestor());
            rootNew.addConnection(connection);

            this.evaluateChanges();
        } else {
            ((ConnectionTreeNode.Branch)ancestorCommon).addConnection(connection);
        }
    }
    public void removeConnection(final PhysicalConnection connection) {
        final ConnectionTreeNode nodeSource = this.leafNodes.get(connection.getSource());
        final ConnectionTreeNode nodeDestination = this.leafNodes.get(connection.getDestination());

        final ConnectionTreeNode nodeCommon = nodeSource.getFirstCommonAncestor(nodeDestination);
        if(((ConnectionTreeNode.Branch)nodeCommon).removeConnection(connection)) {
            this.evaluateChanges();
        }
    }

    private void evaluateChanges() {
        //We call this when there is a sufficiently complicated change to the structure that we're going to forego attempting a graceful, minimalist evaluation and just figure out what changed.
        //Really, there is no need for this, but the overhead is acceptable and the risk of a logic error is considerably less this way.
        final Set<ConnectionTreeNode> nodesCurrent = new HashSet<>();
        for(final ConnectionTreeNode leaf : this.leafNodes.values()) {
            nodesCurrent.add(leaf.getRootAncestor());
        }
        final Set<ConnectionTreeNode> nodesPrevious = new HashSet<>(this.announcedDatasets.keySet());

        final Collection<ConnectionTreeNode> nodesCommon = new LinkedList<>(nodesCurrent);
        nodesCommon.retainAll(nodesPrevious);

        nodesCurrent.removeAll(nodesCommon);
        nodesPrevious.removeAll(nodesCommon);

        //Announce creation, removal, and modification events for everything, as appropriate.
        for(final ConnectionTreeNode nodeRemoved : nodesPrevious) {
            this.announcedDatasets.remove(nodeRemoved);
            this.onTreeRemoved.call(nodeRemoved);
        }
        for(final ConnectionTreeNode nodeAdded : nodesCurrent) {
            this.announcedDatasets.put(nodeAdded, nodeAdded.getAllLeafValues());
            this.onTreeCreated.call(nodeAdded);
        }
        for(final ConnectionTreeNode nodeRetained : nodesCommon) {
            final Collection<HardwareVertex> verticesCurrent = nodeRetained.getAllLeafValues();
            final Collection<HardwareVertex> verticesAnnounced = this.announcedDatasets.get(nodeRetained);

            if(verticesCurrent.size() == verticesAnnounced.size() && verticesCurrent.containsAll(verticesAnnounced)) {
                //They are the same
            } else {
                this.announcedDatasets.put(nodeRetained, verticesCurrent);
                this.onTreeMembershipChanged.call(nodeRetained);
            }
        }
    }

    @Override
    public String toString() {
        final Set<ConnectionTreeNode> nodesCurrent = new HashSet<>();
        for(final ConnectionTreeNode leaf : this.leafNodes.values()) {
            nodesCurrent.add(leaf.getRootAncestor());
        }

        return nodesCurrent.stream().map(root -> root.getAllLeafValues().stream().map(leaf -> leaf.getAddress().toString()).collect(Collectors.joining(", ", "(", ")"))).collect(Collectors.joining(", ", "{", "}"));
    }
}
