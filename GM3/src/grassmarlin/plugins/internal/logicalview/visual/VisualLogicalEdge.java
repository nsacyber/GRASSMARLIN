package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.plugins.internal.logicalview.visual.filters.BoundEdgeProperties;
import grassmarlin.ui.common.StrokeWidthFromByteCountBinding;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.When;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;

import java.util.*;
import java.util.stream.Collectors;

public class VisualLogicalEdge extends Group {
    private class EndpointPair {
        private final VisualLogicalVertex.ContentRow ptStart;
        private final VisualLogicalVertex.ContentRow ptEnd;

        public EndpointPair(final VisualLogicalVertex.ContentRow ptStart, final VisualLogicalVertex.ContentRow ptEnd) {
            this.ptStart = ptStart;
            this.ptEnd = ptEnd;
        }

        @Override
        public boolean equals(final Object other) {
            if(other != null && other instanceof EndpointPair) {
                final EndpointPair rhs = (EndpointPair)other;
                return (this.ptStart == rhs.ptStart && this.ptEnd == rhs.ptEnd) || (this.ptStart == rhs.ptEnd && this.ptEnd == rhs.ptStart);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.ptStart.hashCode() ^ this.ptEnd.hashCode();
        }
    }

    private final VisualLogicalVertex vertSource;
    private final VisualLogicalVertex vertDest;

    private final GraphLogicalEdge edge;

    private final LogicalVisualization visualization;

    private final Map<EndpointPair, DirectedCurve> curveFromEndpoint;
    private final Map<GraphLogicalEdge.PacketList, EndpointPair> endpointsFromPacketList;

    private final DoubleProperty multiplierOpacity;
    private final DoubleProperty multiplierWeight;

    public VisualLogicalEdge(final LogicalVisualization visualization, final GraphLogicalEdge edge, final VisualLogicalVertex vertSource, final VisualLogicalVertex vertDest) {
        this.visualization = visualization;
        this.edge = edge;
        this.vertSource = vertSource;
        this.vertDest = vertDest;

        this.multiplierOpacity = new SimpleDoubleProperty(1.0);
        this.multiplierWeight = new SimpleDoubleProperty(1.0);

        this.curveFromEndpoint = new HashMap<>();
        this.endpointsFromPacketList = new HashMap<>();

        this.edge.getPacketLists().addListener(this.handlerPacketListsChanged);
        this.vertSource.getChildren().addListener(this.handlerRecalculateEdges);
        this.vertDest.getChildren().addListener(this.handlerRecalculateEdges);
        this.vertSource.showChildrenProperty().addListener(this.handlerRecalculateEdges);
        this.vertDest.showChildrenProperty().addListener(this.handlerRecalculateEdges);

        this.isEdgeStructureChanging = true;
    }

    private InvalidationListener handlerRecalculateEdges = this::handleRecalculateEdges;
    private void handleRecalculateEdges(Observable observable) {
        isEdgeStructureChanging = true;
        this.requestLayout();
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
    }

    private boolean isEdgeStructureChanging = false;
    private boolean isPerformingLayout = false;

    public void setEdgeProperties(final BoundEdgeProperties properties) {
        this.multiplierOpacity.bind(properties.opacityProperty());
        this.multiplierWeight.bind(properties.weightProperty());
    }

    @Override
    protected void layoutChildren() {
        if(isPerformingLayout) {
            return;
        }
        isPerformingLayout = true;

        if(isEdgeStructureChanging) {
            //If the structure is changing we need to reassess the Maps that cache the edge->endpoint->object mappings and determine which curves to delete, which to modify, and which to create.
            final HashMap<GraphLogicalEdge.PacketList, EndpointPair> currentMappings = new HashMap<>();
            final List<EndpointPair> changes = new LinkedList<>();

            for(final GraphLogicalEdge.PacketList packets : this.edge.getPacketLists()) {
                final boolean isDirectionMatched = this.getSourceVertex().getVertex().getRootLogicalAddressMapping().contains(packets.getSourceAddress());
                final VisualLogicalVertex.ContentRow rowSource;
                final VisualLogicalVertex.ContentRow rowDestination;
                if(isDirectionMatched) {
                    rowSource = this.vertSource.rowFor(packets.getSourceAddress());
                    rowDestination = this.vertDest.rowFor(packets.getDestinationAddress());
                } else {
                    rowSource = this.vertSource.rowFor(packets.getDestinationAddress());
                    rowDestination = this.vertDest.rowFor(packets.getSourceAddress());
                }

                final EndpointPair endpoints = new EndpointPair(rowSource, rowDestination);
                currentMappings.put(packets, endpoints);
            }

            final Collection<EndpointPair> endpointsNew = new HashSet<>(currentMappings.values());
            endpointsNew.removeAll(this.endpointsFromPacketList.values());
            final Collection<EndpointPair> endpointsRemoved = new HashSet<>(this.endpointsFromPacketList.values());
            endpointsRemoved.removeAll(currentMappings.values());

            final List<DirectedCurve> curvesToRemove = new LinkedList<>();
            final List<DirectedCurve> curvesToAdd = new LinkedList<>();
            for(final EndpointPair pair : endpointsRemoved) {
                final DirectedCurve curve = curveFromEndpoint.remove(pair);
                curvesToRemove.add(curve);
            }
            for(final EndpointPair pair : endpointsNew) {
                final DirectedCurve curve = new DirectedCurve(this.visualization.getPlugin(), pair.ptStart, pair.ptEnd, this.multiplierOpacity, this.multiplierWeight);

                //We don't set the other visual properties here since they need to be re-evaluated every time layout is called.
                curveFromEndpoint.put(pair, curve);
                curvesToAdd.add(curve);
            }

            this.endpointsFromPacketList.clear();
            this.endpointsFromPacketList.putAll(currentMappings);

            this.getChildren().removeAll(curvesToRemove);
            this.getChildren().addAll(curvesToAdd);

            //Whenever the structure changes, we will rebind the listeners in case the groupings have changed--tracking and checking has a similar (or worse) cost in testing.
            for(Map.Entry<EndpointPair, DirectedCurve> pair : this.curveFromEndpoint.entrySet()) {
                final List<GraphLogicalEdge.PacketList> edgesForCurve = this.endpointsFromPacketList.entrySet().stream().filter(entry -> entry.getValue().equals(pair.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList());

                NumberExpression totalPacketSize = null;
                for(final GraphLogicalEdge.PacketList packetList : edgesForCurve) {
                    if(totalPacketSize == null) {
                        totalPacketSize = packetList.totalPacketSizeProperty();
                    } else {
                        totalPacketSize = totalPacketSize.add(packetList.totalPacketSizeProperty());
                    }
                }
                pair.getValue().strokeWidthProperty().bind(
                        new When(this.visualization.useWeightedEdgesBinding())
                                .then(new StrokeWidthFromByteCountBinding(totalPacketSize))
                                .otherwise(1.0));

                BooleanExpression sourceVisible = null;
                BooleanExpression destinationVisible = null;
                for(final GraphLogicalEdge.PacketList packetList : edgesForCurve) {
                    if(sourceVisible == null) {
                        sourceVisible = this.edge.getHasEndpointReceivedData(packetList.getSourceAddress());
                        destinationVisible = this.edge.getHasEndpointReceivedData(packetList.getDestinationAddress());
                    } else {
                        sourceVisible = sourceVisible.or(this.edge.getHasEndpointReceivedData(packetList.getSourceAddress()));
                        destinationVisible = destinationVisible.or(this.edge.getHasEndpointReceivedData(packetList.getDestinationAddress()));
                    }
                }
                pair.getValue().isSourceArrowVisibleProperty().bind(sourceVisible);
                pair.getValue().isDestinationArrowVisibleProperty().bind(destinationVisible);
            }

            isEdgeStructureChanging = false;
        }

        //The existing invalidation should be enough to update the child node endpoints.  Everything else should be bound.

        isPerformingLayout = false;
    }

    private ListChangeListener<GraphLogicalEdge.PacketList> handlerPacketListsChanged = this::handlePacketListsChanged;
    private void handlePacketListsChanged(final ListChangeListener.Change<? extends GraphLogicalEdge.PacketList> change) {
        isEdgeStructureChanging = true;
        this.requestLayout();
    }

    public VisualLogicalVertex getSourceVertex() {
        return this.vertSource;
    }
    public VisualLogicalVertex getDestinationVertex() {
        return this.vertDest;
    }
    public GraphLogicalEdge getEdgeData() {
        return this.edge;
    }
}
