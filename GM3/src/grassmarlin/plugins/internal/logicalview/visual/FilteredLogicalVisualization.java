package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.FilteredLogicalGraph;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.plugins.internal.logicalview.visual.layouts.CopyFromSource;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FilteredLogicalVisualization extends LogicalVisualization {
    private final List<GraphLogicalVertex> verticesHidden;
    private final MenuItem miUnhideElements;
    private final CopyFromSource layout;

    public FilteredLogicalVisualization(final Plugin plugin, final RuntimeConfiguration config, final FilteredLogicalGraph graph, final LogicalVisualization sourceVisualization, final Plugin.LogicalGraphState state) {
        super(plugin, config, graph, state);

        this.verticesHidden = new ArrayList<>();
        this.layout = new CopyFromSource(this, sourceVisualization);

        this.miUnhideElements = new DynamicSubMenu("Unhide", null, () -> {
            return FilteredLogicalVisualization.this.verticesHidden.stream().map(vertex -> new ActiveMenuItem(vertex.titleProperty().get(), event -> {
                FilteredLogicalVisualization.this.unhideVertex(vertex);
            })).collect(Collectors.toList());
        });

        this.miUnhideElements.setDisable(true);

        this.visualizationMenuItems.add(0, this.miUnhideElements);

        this.getGraph().setPredicate(this::applyFilters);

        this.visualizationMenuItems.add(new ActiveMenuItem("Close Filtered View", event -> {
            state.close(FilteredLogicalVisualization.this);
        }));
    }

    public void hideVertex(final GraphLogicalVertex vertex) {
        if(!verticesHidden.contains(vertex)) {
            this.verticesHidden.add(vertex);
            this.miUnhideElements.setDisable(false);
            //Every time the predicate is set it is reevaluated against all vertices.
            this.getGraph().setPredicate(this::applyFilters);
        }
    }

    public void unhideVertex(final GraphLogicalVertex vertex) {
        if(verticesHidden.contains(vertex)) {
            this.verticesHidden.remove(vertex);
            this.miUnhideElements.setDisable(this.verticesHidden.isEmpty());

            this.getGraph().setPredicate(this::applyFilters);
        }
    }

    @Override
    protected VisualLogicalVertex createVisualLogicalVertexFor(final GraphLogicalVertex vertex, final ImageDirectoryWatcher.MappedImageList images) {
        return new VisualLogicalVertex(this, vertex, images) {
            private final MenuItem miRemoveFromGraph = new ActiveMenuItem("Hide", event -> {
                FilteredLogicalVisualization.this.hideVertex(this.getVertex());
            });
            private final CheckMenuItem miBindToParentLayout = new CheckMenuItem("Enable Automatic Layout");

            {
                this.miBindToParentLayout.setSelected(true);
                this.miBindToParentLayout.setOnAction(event -> {
                    if(miBindToParentLayout.isSelected()) {
                        FilteredLogicalVisualization.this.layout.establishBindingFor(this.getVertex());
                    } else {
                        this.translateXProperty().unbind();
                        this.translateYProperty().unbind();
                    }
                });
            }

            @Override
            public List<MenuItem> getContextMenuItems() {
                final List<MenuItem> result = super.getContextMenuItems();
                result.add(this.miRemoveFromGraph);
                result.add(this.miBindToParentLayout);

                return result;
            }

            @Override
            public void dragTo(final Point2D ptDestination) {
                this.miBindToParentLayout.setSelected(false);
                //We have to manually unbind these
                this.translateXProperty().unbind();
                this.translateYProperty().unbind();

                super.dragTo(ptDestination);
            }
        };
    }

    private boolean applyFilters(final GraphLogicalVertex vertex) {
        return !verticesHidden.contains(vertex);
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        final String terminalTag = source.getLocalName();

        while(source.hasNext()) {
            final int typeNext = source.next();
            final String tag;
            switch (typeNext) {
                case XMLEvent.START_ELEMENT:
                    tag = source.getLocalName();
                    if (tag.equals("Hidden")) {
                        final String key = source.getAttributeValue(null, "key");
                        this.verticesHidden.add(this.getGraph().vertexForKey(key));
                    } else if (tag.equals("Parent")) {
                        super.readFromXml(source);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    tag = source.getLocalName();
                    if (tag.equals(terminalTag)) {
                        //We didn't evaluate the predicate while loading, we evaluate it once now for all vertices.
                        this.getGraph().setPredicate(this::applyFilters);
                        return;
                    }
                    break;
            }
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        for(final GraphLogicalVertex vertex : this.verticesHidden) {
            target.writeStartElement("Hidden");
            target.writeAttribute("key", vertex.getKey());
            target.writeEndElement();
        }

        target.writeStartElement("Parent");
        super.writeToXml(target);
        target.writeEndElement();
    }
}
