package iadgov.visualpipeline;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.HasVersionInfo {
    private final Map<Session, SessionInterfaceController.View> tabsFromSession;

    public Plugin(final RuntimeConfiguration config) {
        tabsFromSession = new HashMap<>();
    }

    // == IPlugin.SessionEventHooks ==
    @Override
    public void sessionCreated(final grassmarlin.session.Session session, final SessionInterfaceController controller) {
        final VisualPipeline content = new VisualPipeline(session.pipelineProperty());
        final SessionInterfaceController.View viewNew = SessionInterfaceController.View.simple("Visual Pipeline", content);
        tabsFromSession.put(session, viewNew);
        controller.createView(viewNew, true);
    }

    @Override
    public void sessionClosed(final grassmarlin.session.Session session) {
        tabsFromSession.remove(session);
    }

    // == IPlugin ==
    @Override
    public String getName() {
        return "Visual Pipeline";
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    private final Image iconLarge = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        return iconLarge;
    }

    // == IPlugin.HasVersionInfo
    @Override
    public String getVersion() {
        return "1.0";
    }
}
