package grassmarlin;

import grassmarlin.plugins.internal.AggregatePlugin;
import grassmarlin.plugins.internal.logicalview.ColorFactoryMenuItem;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import grassmarlin.plugins.internal.logicalview.visual.VertexColorAssignment;
import grassmarlin.plugins.internal.logicalview.visual.filters.EdgeStyleRule;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import javafx.scene.Node;

import java.util.function.*;

/**
 * GRASSMARLIN treats itself as a plugin for the purposes of componentized functionality.
 */
public class Plugin extends AggregatePlugin implements ILogicalViewApi {
    private final grassmarlin.plugins.internal.logicalview.Plugin pluginLogicalView;
    public static final String NAME = "Grassmarlin";

    public Plugin(final RuntimeConfiguration config) {
        super(NAME,
                new grassmarlin.plugins.internal.livepcap.Plugin(config),
                new grassmarlin.plugins.internal.offlinepcap.Plugin(config),
                new grassmarlin.plugins.internal.logicalview.Plugin(config),
                new grassmarlin.plugins.internal.graph.Plugin(config),
                new grassmarlin.plugins.internal.metadata.Plugin(config),
                new grassmarlin.plugins.internal.physicalview.Plugin(config),
                new grassmarlin.plugins.internal.fingerprint.Plugin(config)
                );

        this.pluginLogicalView = this.getPlugins(grassmarlin.plugins.internal.logicalview.Plugin.class).stream().findFirst().orElse(null);

        VertexColorAssignment.defineColorMapping(Ipv4.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4BackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4TextProperty()) { });
        VertexColorAssignment.defineColorMapping(Ipv4WithPort.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4WithPortBackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4WithPortTextProperty()) { });
        VertexColorAssignment.defineColorMapping(Ipv4WithPort.Ipv4WithTcpPort.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4TcpBackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4TcpTextProperty()) { });
        VertexColorAssignment.defineColorMapping(Ipv4WithPort.Ipv4WithUdpPort.class, new VertexColorAssignment.VertexColor(RuntimeConfiguration.colorGraphElementIpv4UdpBackgroundProperty(), RuntimeConfiguration.colorGraphElementIpv4UdpTextProperty()) { });
    }

    // == ILogicalViewApi ==
    @Override
    public void addGroupColorFactory(final Supplier<ColorFactoryMenuItem.IColorFactory> factory) {
        this.pluginLogicalView.addGroupColorFactory(factory);
    }

    @Override
    public void addVertexContextMenuItem(final Predicate<GraphLogicalVertex> condition, final String name, final Consumer<GraphLogicalVertex> action) {
        this.pluginLogicalView.addVertexContextMenuItem(condition, name, action);
    }

    @Override
    public <N extends Node, R extends EdgeStyleRule> void addEdgeStyleRuleUi(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter) {
        this.pluginLogicalView.addEdgeStyleRuleUi(name, uiFactory, getter, setter);
    }
}
