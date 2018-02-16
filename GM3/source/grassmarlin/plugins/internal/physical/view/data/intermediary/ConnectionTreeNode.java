package grassmarlin.plugins.internal.physical.view.data.intermediary;

import grassmarlin.session.HardwareVertex;
import grassmarlin.session.PhysicalConnection;

import java.util.*;

public abstract class ConnectionTreeNode {
    public static class Leaf extends ConnectionTreeNode {
        private final HardwareVertex vertex;

        public Leaf(final HardwareVertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public Collection<HardwareVertex> getAllLeafValues() {
            return Collections.singleton(this.vertex);
        }

        @Override
        public Collection<PhysicalConnection> getAllConnections() {
            if(this.parent == null) {
                return Collections.EMPTY_LIST;
            } else {
                return this.parent.getAllConnections();
            }
        }
    }

    public static class Branch extends ConnectionTreeNode {
        protected ConnectionTreeNode child1;
        protected ConnectionTreeNode child2;
        protected final Set<PhysicalConnection> connections;

        public Branch(final ConnectionTreeNode child1, final ConnectionTreeNode child2) {
            this.child1 = child1;
            this.child2 = child2;
            child1.setParent(this);
            child2.setParent(this);

            this.connections = new HashSet<>();
        }

        public ConnectionTreeNode getChild1() {
            return this.child1;
        }
        public void setChild1(final ConnectionTreeNode child1) {
            this.child1 = child1;
        }

        public ConnectionTreeNode getChild2() {
            return this.child2;
        }
        public void setChild2(final ConnectionTreeNode child2) {
            this.child2 = child2;
        }

        public void replaceChild(final ConnectionTreeNode childOld, final ConnectionTreeNode childNew) {
            if(this.child1 == childOld) {
                this.child1 = childNew;
            } else if(this.child2 == childOld) {
                this.child2 = childNew;
            }
        }
        public ConnectionTreeNode otherChild(final ConnectionTreeNode child) {
            if(this.child1 == child) {
                return this.child2;
            } else {
                return this.child1;
            }
        }

        @Override
        public Collection<HardwareVertex> getAllLeafValues() {
            final HashSet<HardwareVertex> result = new HashSet<>(this.child1.getAllLeafValues());
            result.addAll(this.child2.getAllLeafValues());
            return result;
        }

        @Override
        public Collection<PhysicalConnection> getAllConnections() {
            final ArrayList<PhysicalConnection> result = new ArrayList<>(this.connections);
            if(this.parent != null) {
                result.addAll(this.parent.getAllConnections());
            }
            return result;
        }

        public void addConnection(final PhysicalConnection connection) {
            this.connections.add(connection);
        }
        public boolean removeConnection(final PhysicalConnection connection) {
            if(this.connections.remove(connection)) {
                if(this.connections.isEmpty()) {
                    if(this.parent != null) {
                        //We need to validate connections between child1, child2, and a sibling.
                        //All of the parent's edges associate a member of child1 or child2 with a member of the sibling.
                        //We partition the parent's links into two groups based on which child is linked and can then shift the child associations, or, if one of the sets is empty, cut the branch free.
                        final Collection<HardwareVertex> vertices1 = this.child1.getAllLeafValues();
                        final Collection<HardwareVertex> vertices2 = this.child2.getAllLeafValues();

                        final List<PhysicalConnection> connections1 = new ArrayList<>();
                        final List<PhysicalConnection> connections2 = new ArrayList<>();

                        for(final PhysicalConnection connectionParent : ((Branch)this.parent).connections) {
                            //TODO: Figure out what I meant to do here since this is obviously nonsensical.  Probably meant to be a containsAny where there is a connection with either source or destination, but without a deeper look I'm not certain this is the case.
                            if(connections1.contains(connectionParent.getSource()) || connections1.contains(connectionParent.getDestination())) {
                                connections1.add(connectionParent);
                            } else {
                                connections2.add(connectionParent);
                            }
                        }

                        if(connections1.isEmpty()) {
                            this.child1.setParent(null);
                            this.child2.setParent(this.parent);
                            ((Branch)this.parent).replaceChild(this, this.child2);
                        } else if(connections2.isEmpty()) {
                            this.child1.setParent(this.parent);
                            this.child2.setParent(null);
                            ((Branch)this.parent).replaceChild(this, this.child1);
                        } else {
                            //We have 3 sets of leaf nodes: A, B, and C.
                            //This node used to have A-B associations, but all those are gone.
                            //connections1 contains all A-C associations and connections2 contains all B-C associations.
                            //This node will acquire all the A-C associations and child 2 (B) will be replaced with C.
                            //The parent will retain B-C associations and will have C replaced with B.
                            this.connections.addAll(connections1);
                            ((Branch)this.parent).connections.removeAll(connections1);

                            final ConnectionTreeNode sibling = ((Branch)this.parent).otherChild(this);
                            ((Branch)this.parent).replaceChild(sibling, this.child2);
                            this.child2 = sibling;
                        }
                    } else {
                        //There is no rebalancing to be done.
                        //We mark the children as orphaned and, if fantasy tropes hold, await the awakening of their mystical powers that will save the world.
                        this.child1.setParent(null);
                        this.child2.setParent(null);
                    }
                    //The graph structure changed.
                    return true;
                } else {
                    //The graph structure did not change.
                    return false;
                }
            }
            //The PhysicalConnection wasn't part of this node.  The structure didn't change, but something is wrong.  A rebuild might help?
            return true;
        }
    }


    protected ConnectionTreeNode parent;

    protected ConnectionTreeNode() {
        this.parent = null;
    }

    public ConnectionTreeNode getParent() {
        return this.parent;
    }
    public void setParent(final ConnectionTreeNode parent) {
        this.parent = parent;
    }

    public ConnectionTreeNode getRootAncestor() {
        ConnectionTreeNode result = this;
        while(result.parent != null) {
            result = result.parent;
        }
        return result;
    }

    public ConnectionTreeNode getFirstCommonAncestor(final ConnectionTreeNode other) {
        final List<ConnectionTreeNode> pathThis = new LinkedList<>();
        final Collection<ConnectionTreeNode> pathOther = new HashSet<>();


        for(ConnectionTreeNode cursor = this; cursor != null; cursor = cursor.parent) {
            pathThis.add(cursor);
        }
        for(ConnectionTreeNode cursor = other; cursor != null; cursor = cursor.parent) {
            pathOther.add(cursor);
        }

        for(final ConnectionTreeNode candidate : pathThis) {
            if(pathOther.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public abstract Collection<HardwareVertex> getAllLeafValues();

    public abstract Collection<PhysicalConnection> getAllConnections();
}
