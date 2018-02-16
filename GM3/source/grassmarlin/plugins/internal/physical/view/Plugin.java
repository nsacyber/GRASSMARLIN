package grassmarlin.plugins.internal.physical.view;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Plugin implements IPhysicalViewApi, IPlugin, IPlugin.SessionEventHooks, IPlugin.SessionSerialization {
    public final static String NAME = "Physical View";

    public final static String PHYSICAL_INTERFACE_RJ45_ACTIVE = "RJ45 Active";
    public final static String PHYSICAL_INTERFACE_RJ45_INACTIVE = "RJ45 Inactive";
    public final static String PHYSICAL_INTERFACE_RJ45_ACTIVE_INVERTED = "RJ45 Active (Inverted)";
    public final static String PHYSICAL_INTERFACE_RJ45_INACTIVE_INVERTED = "RJ45 Inactive (Inverted)";

    private final ImageProperties cloudPhysicalInterfaceProperties;

    private final RuntimeConfiguration config;
    private final Map<Session, SessionState> states;
    private final Map<Serializable, ImageProperties> portImageMappings;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
        this.states = new HashMap<>();
        this.portImageMappings = new LinkedHashMap<>();

        //We use half-pixel margin around each image to prevent bleeding from one image to the next.
        //When the border falls between two pixels, the pixel outside the region is still used for antialiasing rather than clamping to the image edge.
        final Image imageRj45 = new Image(this.getClass().getClassLoader().getResourceAsStream("resources/images/PhysicalPort.png"));
        this.addPortImage(new ImageProperties(imageRj45, new Rectangle2D(0.5, 0.5, 47.0, 47.0)), PHYSICAL_INTERFACE_RJ45_ACTIVE);
        this.addPortImage(new ImageProperties(imageRj45, new Rectangle2D(48.5, 0.5, 47.0, 47.0)), PHYSICAL_INTERFACE_RJ45_INACTIVE);
        this.addPortImage(new ImageProperties(imageRj45, new Rectangle2D(0.5, 48.5, 47.0, 47.0)), PHYSICAL_INTERFACE_RJ45_ACTIVE_INVERTED);
        this.addPortImage(new ImageProperties(imageRj45, new Rectangle2D(48.5, 48.5, 47.0, 47.0)), PHYSICAL_INTERFACE_RJ45_INACTIVE_INVERTED);
        //The cloud image happens to be in the RJ45 image.  This makes the name inaccurate, but I could either decide on a more apt name or I could look at pictures of kittens. >(^.^)<
        this.cloudPhysicalInterfaceProperties = new ImageProperties(imageRj45, new Rectangle2D(96.5, 0.5, 95.0, 95.0));
    }

    public RuntimeConfiguration getConfig() {
        return this.config;
    }

    @Override
    public void sessionCreated(Session session, SessionInterfaceController tabs) {
        final SessionState stateOld = this.states.put(session, new SessionState(this, session, tabs));
        if(stateOld != null) {
            stateOld.close();
        }
    }

    @Override
    public void sessionClosed(Session session) {
        final SessionState stateOld = this.states.remove(session);
        if(stateOld != null) {
            stateOld.close();
        }
    }

    @Override
    public void sessionLoaded(Session session, InputStream stream, GetChildStream fnGetStream) throws IOException {
        //TODO: Implement grassmarlin.plugins.internal.physical.view.Plugin.sessionLoaded
    }

    @Override
    public void sessionSaving(Session session, OutputStream stream, CallbackCreateStream fnCreateStream) throws IOException {
        //TODO: Implement grassmarlin.plugins.internal.physical.view.Plugin.sessionSaving
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    // == IPhysicalViewApi

    @Override
    public void addPortImage(final ImageProperties properties, final Serializable valuePortProperty) {
        if(properties != null && valuePortProperty != null) {
            this.portImageMappings.put(valuePortProperty, properties);
        }
    }

    // == Internal-use side of the PhysicalView API
    public ImageProperties getPortImagePropertiesFor(final Serializable value) {
        return this.portImageMappings.get(value);
    }
    public ImageProperties getCloudImageProperties() {
        return this.cloudPhysicalInterfaceProperties;
    }
}