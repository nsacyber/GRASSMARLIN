package iadgov.example.tab;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.Collection;

/**
 * This is a mini-Sokoban clone that serves to demonstrate UI interactions within GrassMarlin.
 *
 * While the Logical and Physical Graph are excellent examples, the amount of functionality
 * they contain coupled with the need to change them in response to bugs, performance concerns,
 * and so on rob them of merit as learning aids.
 *
 * This is a simple Plugin with no ties to anything else that:
 *  - Creates a custom tab
 *  - Provides Undo functionality
 *
 *  As an added bonus, it can help kill time while slogging through a massive import.
 */
public class Plugin implements IPlugin, IPlugin.SessionEventHooks {
    public final static String NAME = "Example: Tab";
    private final Image iconPlugin;

    public Plugin(RuntimeConfiguration config) {
        //The RuntimeConfiguration parameter is necessary but we don't need it for this implementation.
        //You would need to hold on to the config because:
        // - Loading code might interact with classes created by other Plugins
        // - You need to check whether or not Active behaviors are permitted

        this.iconPlugin = new Image(this.getClass().getClassLoader().getResourceAsStream("Box.png"));
    }

    @Override
    public void sessionCreated(Session session, SessionInterfaceController controller) {
        //Since there is only a single View for each Session, this is fairly trivial.
        //If we had multiple views we would most likely just call createView multiple times.
        //If the Plugin can dynamically create new Views, we would have another object to manage that.
        controller.createView(new VisualContainer(controller), false);
    }

    @Override
    public void sessionClosed(Session session) {
        //Cleanup is implicit.
        //If we had resources that create threads to complete tasks, we would ensure they are cleaned up here.
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    @Override
    public Image getImageForSize(int pixels) {
        return iconPlugin;
    }

}
