package grassmarlin;

import grassmarlin.plugins.internal.AggregatePlugin;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import grassmarlin.plugins.internal.logicalview.visual.VertexColorAssignment;
import grassmarlin.plugins.internal.logicalview.visual.filters.EdgeStyleRule;
import grassmarlin.plugins.internal.logicalview.visual.filters.Style;
import grassmarlin.plugins.internal.physical.view.IPhysicalViewApi;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Session;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.logicaladdresses.RouterLogicalAddress;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * GRASSMARLIN treats itself as a plugin for the purposes of componentized functionality.
 */
public class Plugin extends AggregatePlugin implements ILogicalViewApi, IPhysicalViewApi {
    private final grassmarlin.plugins.internal.logicalview.Plugin pluginLogicalView;
    private final grassmarlin.plugins.internal.physical.view.Plugin pluginPhysicalView;
    public static final String NAME = "Grassmarlin";

    public Plugin(final RuntimeConfiguration config) {
        super(NAME,
                new grassmarlin.plugins.internal.livepcap.Plugin(config),
                new grassmarlin.plugins.internal.offlinepcap.Plugin(config),
                new grassmarlin.plugins.internal.logicalview.Plugin(config),
                new grassmarlin.plugins.internal.graph.Plugin(config),
                new grassmarlin.plugins.internal.metadata.Plugin(config),
                new grassmarlin.plugins.internal.physical.view.Plugin(config),
                new grassmarlin.plugins.internal.physical.deviceimport.Plugin(config)
                );

        this.pluginLogicalView = this.getPlugins(grassmarlin.plugins.internal.logicalview.Plugin.class).stream().findFirst().orElse(null);
        this.pluginPhysicalView  = this.getPlugins(grassmarlin.plugins.internal.physical.view.Plugin.class).stream().findFirst().orElse(null);

        VertexColorAssignment.defineColorMapping(Ipv4.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4BackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4TextProperty()) { });
        VertexColorAssignment.defineColorMapping(Ipv4WithPort.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4WithPortBackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4WithPortTextProperty()) { });
        VertexColorAssignment.defineColorMapping(Ipv4WithPort.Ipv4WithTcpPort.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4TcpBackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4TcpTextProperty()) { });
        VertexColorAssignment.defineColorMapping(Ipv4WithPort.Ipv4WithUdpPort.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4UdpBackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4UdpTextProperty()) { });
        VertexColorAssignment.defineColorMapping(RouterLogicalAddress.class, new VertexColorAssignment.VertexColor(Color.DARKBLUE, Color.WHITE) {});

    }

    // == ILogicalViewApi ==
    @Override
    public void addGroupColorFactory(final String name, final Supplier<ILogicalViewApi.ICalculateColorsForAggregate> factory) {
        this.pluginLogicalView.addGroupColorFactory(name, factory);
    }

    @Override
    public void addVertexContextMenuItem(final BiPredicate<GraphLogicalVertex, LogicalVertex> condition, final String name, final TriConsumer<Session, GraphLogicalVertex, LogicalVertex> action) {
        this.pluginLogicalView.addVertexContextMenuItem(condition, name, action);
    }

    @Override
    public <N extends Node, R extends EdgeStyleRule> void addEdgeStyleRuleUi(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter) {
        this.pluginLogicalView.addEdgeStyleRuleUi(name, uiFactory, getter, setter);
    }
    @Override
    public ObservableList<Style> getEdgeStylesForSession(final Session session) {
        return this.pluginLogicalView.getEdgeStylesForSession(session);
    }
    @Override
    public ObservableList<EdgeStyleRule> getEdgeStyleRulesForSession(final Session session) {
        return this.pluginLogicalView.getEdgeStyleRulesForSession(session);
    }

    @Override
    public void addMappedImage(final String nameProperty, final String valueProperty, final Image image) {
        this.pluginLogicalView.addMappedImage(nameProperty, valueProperty, image);
    }

    // == IPhysicalViewApi ==

    @Override
    public void addPortImage(final ImageProperties properties, final Serializable valuePortProperty) {
        this.pluginPhysicalView.addPortImage(properties, valuePortProperty);
    }
}
