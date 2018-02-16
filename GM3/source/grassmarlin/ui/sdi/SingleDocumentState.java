package grassmarlin.ui.sdi;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.fxobservables.FxBooleanProperty;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import grassmarlin.ui.common.ActionStackPropertyWrapper;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.binding.When;
import javafx.beans.property.*;

/**
 * In a single-document interface, there is a single session, controller, and pipeline.
 * These may be destroyed and recreated, but there is only a need for one of each at any given point in time.
 *
 */
public class SingleDocumentState {
    private final Session session;
    private final SdiSessionInterfaceController controller;

    private final StringProperty sessionPath;
    private final BooleanProperty isDirty;

    private final StringExpression sessionTitle;    //The title is what is displayed, which will be some form of default, asterisk when dirty, etc.

    public SingleDocumentState(final RuntimeConfiguration config, final ActionStackPropertyWrapper masterActionStack) {
        this.session = new Session(config);
        this.controller = new SdiSessionInterfaceController(masterActionStack, this);

        this.sessionPath = new SimpleStringProperty();
        this.isDirty = new FxBooleanProperty(false);

        this.sessionTitle = new When(this.sessionPath.isNull()).then("New Session").otherwise(new ReadOnlyStringWrapper("[").concat(this.sessionPath).concat(new When(this.isDirty).then("]*").otherwise("]")));

        this.session.onSessionModified.addHandler(this.handlerSessionModified);

        config.enumeratePlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> {
            plugin.sessionCreated(this.session, this.controller);
        });
    }

    private Event.EventListener handlerSessionModified = this::handleSessionModified;
    private void handleSessionModified(final Event<Object> event, final Object args) {
        isDirty.set(true);
    }

    public BooleanExpression dirtyProperty() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(this.isDirty);
    }
    public StringProperty currentSessionPathProperty() {
        return this.sessionPath;
    }
    public StringExpression currentSessionTitleProperty() {
        return this.sessionTitle;
    }

    public Session getSession() {
        return this.session;
    }
    public SdiSessionInterfaceController getController() {
        return this.controller;
    }

    public void invalidate() {
        this.isDirty.set(true);
    }

    public void saved(final String path) {
        this.sessionPath.set(path);
        this.isDirty.set(false);
    }
}
