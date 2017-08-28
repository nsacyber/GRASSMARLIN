package iadgov.visualpipeline;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import javafx.scene.control.MenuItem;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.HasVersionInfo {
    private final Map<Session, VisualTab> tabsFromSession;

    public Plugin(final RuntimeConfiguration config) {
        tabsFromSession = new HashMap<>();
    }

    // == IPlugin.SessionEventHooks ==
    @Override
    public void sessionCreated(final grassmarlin.session.Session session, final grassmarlin.ui.common.TabController tabs) {
        final VisualTab tab = new VisualTab(session.pipelineProperty());
        final VisualTab tabOld = tabsFromSession.put(session, tab);
        if(tabOld != null) {
            tabOld.terminate();
        }
        tabs.addContent(tab, tab.getNavigationRoot(), false);
    }

    @Override
    public void sessionClosed(final grassmarlin.session.Session session) {
        //The tabcontroller and related resources will be cleaned up for us, but we do need to terminate the worker thread used by the tab
        final VisualTab tabOld = tabsFromSession.remove(session);
        if(tabOld != null) {
            tabOld.terminate();
        }
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

    // == IPlugin.HasVersionInfo
    @Override
    public String getVersion() {
        return "1.0";
    }
}
