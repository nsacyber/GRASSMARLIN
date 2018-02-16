package grassmarlin.plugins.internal.logicalview;

import grassmarlin.Logger;
import grassmarlin.plugins.internal.logicalview.visual.LogicalNavigation;
import grassmarlin.plugins.internal.logicalview.visual.SetGroupingMenuItem;
import grassmarlin.plugins.internal.logicalview.visual.filters.EdgeStyleRule;
import grassmarlin.plugins.internal.logicalview.visual.filters.StyleEditor;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.SessionInterfaceController;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.StackPane;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ViewLogical<G extends LogicalGraph, V extends Node & ILogicalVisualization> extends SessionInterfaceController.View implements XmlSerializable {
    protected final StringProperty grouping;
    protected final SessionState sessionState;

    protected final G graph;
    protected final V visualization;
    protected final LogicalNavigation navigation;

    private final MenuItem miSetGrouping;
    private final CheckMenuItem ckUseCurvedEdges;
    private final CheckMenuItem ckUseWeightedEdges;
    private final CheckMenuItem ckUseStyledEdges;
    private final ColorFactoryMenuItem miSetAggregateColorFactory;
    private final MenuItem miEditEdgeStyles;

    protected ViewLogical(final SessionState sessionState, final G graph, final StringExpression title, Function<ViewLogical, V> factoryVisualization, final LogicalNavigation navigation, boolean canClose) {
        super(
                title,
                new StackPane(),
                navigation.getTreeRoot(),
                null, //TODO: Undo Buffer
                new ArrayList<>(),
                canClose,
                canClose    //Logical graph variations are either permanent or temporary--if they can be closed they can be dismissed.
        );

        this.sessionState = sessionState;
        this.grouping = new SimpleStringProperty(null);

        this.graph = graph;

        this.navigation = navigation;

        this.miSetGrouping = new SetGroupingMenuItem(this.graph, this.grouping);
        this.ckUseCurvedEdges = new CheckMenuItem("Use Curved Edges");
        this.ckUseWeightedEdges = new CheckMenuItem("Use Weighted Edges");
        this.ckUseStyledEdges = new CheckMenuItem("Use Edge Style Rules");
        this.ckUseStyledEdges.setSelected(true);
        this.miSetAggregateColorFactory = new ColorFactoryMenuItem(this.sessionState.getPlugin());
        this.miEditEdgeStyles = new ActiveMenuItem("Edit Edge Styles", event -> {
            new StyleEditor(ViewLogical.this.sessionState.getEdgeStyles(), ViewLogical.this.sessionState.getEdgeRules(), ViewLogical.this.sessionState.getPlugin().edgeRuleUiElements).showAndWait();
        });

        this.getViewMenuItems().addAll(Arrays.asList(
                this.miSetGrouping,
                this.ckUseCurvedEdges,
                this.ckUseWeightedEdges,
                this.ckUseStyledEdges,
                this.miSetAggregateColorFactory,
                new SeparatorMenuItem(),    // Items that affect all Logical Views
                this.miEditEdgeStyles
        ));

        this.visualization = factoryVisualization.apply(this);
        ((StackPane)this.getContent()).getChildren().add(this.visualization);

        this.visualization.setContextMenuSources(this::getCommonContextMenuItems, this::getVertexContextMenuItems);
        this.navigation.setContextMenuSources(this::getCommonContextMenuItems, this::getVertexContextMenuItems);

        this.grouping.addListener((observable, oldValue, newValue) -> {
            ViewLogical.this.visualization.setCurrentGrouping(newValue);
            ViewLogical.this.navigation.setCurrentGrouping(newValue);
        });
    }

    protected List<MenuItem> getCommonContextMenuItems() {
        return Collections.singletonList(
                //By returning a new menu item we might avoid whatever condition causes an exception in PopupWindow.java:384
                //The error is that the background region has no scene, but this is managed internally to JavaFx; there is no reason for the null scene, but it happens.  Comments suggest this is still being debugged.
                //Since changing this to return a new instance, the error has not happened again, to my knowledge.
                new SetGroupingMenuItem(this.graph, this.grouping)  //HACK: Should be miSetGrouping, but errors in JavaFx as of this comment make that occasionally produce an exception.
        );
    }

    protected List<MenuItem> getVertexContextMenuItems(final GraphLogicalVertex vertex, final LogicalVertex row) {
        final ArrayList<MenuItem> result = new ArrayList<>();
        final List<GraphLogicalEdge> edges = new ArrayList<>();
        ViewLogical.this.graph.getEdges(edges);

        result.addAll(Arrays.asList(
                new ActiveMenuItem("Center in View", event -> {
                    ViewLogical.this.visualization.zoomToVertex(vertex);
                }),
                new ActiveMenuItem("Show Packet List...", event -> {
                    ViewLogical.this.sessionState.showPacketListDialogFor(ViewLogical.this.graph, vertex);
                }),
                new ActiveMenuItem("Show Details for " + vertex.getRootLogicalAddressMapping() + "...", event -> {
                    ViewLogical.this.sessionState.showDetailsDialogFor(vertex);
                })
        ));
        result.addAll(ViewLogical.this.sessionState.getPluginDefinedMenuItemsFor(vertex, row));
        result.addAll(
                edges.stream()
                        .filter(edge -> edge.getSource() == vertex || edge.getDestination() == vertex)
                        .map(edge -> new ActiveMenuItem(
                                String.format("Details for %s to %s...", edge.getSource().getRootLogicalAddressMapping(), edge.getDestination().getRootLogicalAddressMapping()),
                                event -> {
                                    ViewLogical.this.sessionState.showDetailsDialogFor(edge);
                                }))
                        .collect(Collectors.toList())
        );
        return result;
    }

    public G getGraph() {
        return this.graph;
    }

    public BooleanExpression useCurvedEdgesProperty() {
        return this.ckUseCurvedEdges.selectedProperty();
    }
    public BooleanExpression useWeightedEdgesProperty() {
        return this.ckUseWeightedEdges.selectedProperty();
    }
    public BooleanExpression useStyledEdgesProperty() {
        return this.ckUseStyledEdges.selectedProperty();
    }

    public ImageDirectoryWatcher getImageDirectoryWatcher() {
        return ViewLogical.this.sessionState.getImageDirectoryWatcher();
    }
    public ILogicalViewApi.ICalculateColorsForAggregate getAggregateColorFactory() {
        return this.miSetAggregateColorFactory.getFactory();
    }

    public void markSessionAsModified() {
        ViewLogical.this.sessionState.markSessionAsModified();
    }

    public ObservableList<EdgeStyleRule> getEdgeRules() {
        return this.sessionState.getEdgeRules();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.visualization.cleanup();
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        final String elementTerminal = source.getLocalName();

        while(source.hasNext()) {
            switch (source.nextTag()) {
                case XMLStreamReader.START_ELEMENT:
                    switch(source.getLocalName()) {
                        case "Options":
                            this.grouping.set(source.getAttributeValue(null, "Grouping"));
                            this.ckUseWeightedEdges.selectedProperty().set(Boolean.parseBoolean(source.getAttributeValue(null, "EdgesWeighted")));
                            this.ckUseCurvedEdges.selectedProperty().set(Boolean.parseBoolean(source.getAttributeValue(null, "EdgesCurved")));
                            this.ckUseStyledEdges.selectedProperty().set(Boolean.parseBoolean(source.getAttributeValue(null, "EdgesStyled")));
                            this.miSetAggregateColorFactory.setFactoryByName(source.getAttributeValue(null, "AggregateColorFactory"));
                            break;
                        case "Visualization":
                            this.visualization.readFromXml(source);
                            break;
                        default:
                            //Don't know what to do with it.
                            Logger.log(Logger.Severity.WARNING, "Unexpected element reading Logical View: %s", source.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if(source.getLocalName().equals(elementTerminal)) {
                        return;
                    }
                    break;
                case XMLStreamReader.END_DOCUMENT:
                    return;
            }
        }
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        target.writeStartElement("Options");

        if(this.grouping.get() != null) {
            target.writeAttribute("Grouping", this.grouping.get());
        }
        target.writeAttribute("EdgesWeighted", Boolean.toString(this.ckUseWeightedEdges.selectedProperty().get()));
        target.writeAttribute("EdgesCurved", Boolean.toString(this.ckUseCurvedEdges.selectedProperty().get()));
        target.writeAttribute("EdgesStyled", Boolean.toString(this.ckUseStyledEdges.selectedProperty().get()));
        target.writeAttribute("AggregateColorFactory", this.miSetAggregateColorFactory.getFactoryName());

        target.writeEndElement();

        // Writing the Visualization requires all mutable visual state information--position, expand, color, etc.
        target.writeStartElement("Visualization");
        this.visualization.writeToXml(target);
        target.writeEndElement();
    }
}