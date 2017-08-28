package grassmarlin.ui.sdi;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.ThreadManagedState;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;

public class DocumentState {
    private final ObjectProperty<Session> session;
    private final StringProperty sessionPath;
    private final StringExpression sessionTitle;
    private final BooleanProperty currentSessionCanChangePipeline;

    private final BooleanProperty isDirty;

    private final ThreadManagedState state = new ThreadManagedState(RuntimeConfiguration.UPDATE_INTERVAL_MS, "DocumentState", Event.PROVIDER_JAVAFX) {
        @Override
        protected void validate() {
            final Session session = DocumentState.this.session.get();
            if(session == null) {
                //EXTEND: Set null-session values here.
                isDirty.set(false);
                sessionPath.set(null);
                return;
            } else {
                isDirty.set(true);
            }

            //EXTEND: Handlers for other/specific invalidations(?)
        }
    };

    public DocumentState(final ObjectProperty<Session> session) {
        this.session = session;
        this.session.addListener(this.handlerSessionChanged);
        this.sessionPath = new SimpleStringProperty();
        this.sessionTitle = new When(this.sessionPath.isNull()).then("New Session").otherwise(this.sessionPath);
        this.currentSessionCanChangePipeline = new SimpleBooleanProperty(false);

        this.isDirty = new SimpleBooleanProperty(false);
    }

    private final ChangeListener<Session> handlerSessionChanged = this::handleSessionChanged;
    private void handleSessionChanged(Observable observable, Session oldValue, Session newValue) {
        if(oldValue != null) {
            //EXTEND: Remove event listeners
            oldValue.onNetworkChange.removeHandler(this.handlerSessionModified);
            oldValue.onEdgeCreated.removeHandler(this.handlerSessionModified);
            oldValue.onLogicalVertexCreated.removeHandler(this.handlerSessionModified);
            oldValue.onPacketReceived.removeHandler(this.handlerSessionModified);
            oldValue.onHardwareVertexCreated.removeHandler(this.handlerSessionModified);
            oldValue.onSessionModified.removeHandler(this.handlerSessionModified);
            this.currentSessionCanChangePipeline.unbind();
            this.currentSessionCanChangePipeline.set(false);
        }
        if(newValue != null) {
            //EXTEND: Add event listeners
            newValue.onNetworkChange.addHandler(this.handlerSessionModified);
            newValue.onEdgeCreated.addHandler(this.handlerSessionModified);
            newValue.onLogicalVertexCreated.addHandler(this.handlerSessionModified);
            newValue.onPacketReceived.addHandler(this.handlerSessionModified);
            newValue.onHardwareVertexCreated.addHandler(this.handlerSessionModified);
            newValue.onSessionModified.addHandler(this.handlerSessionModified);
            this.currentSessionCanChangePipeline.bind(newValue.canSetPipeline());
        }
        //Whenever the session changes, the session is clean.
        this.isDirty.set(false);
    }

    private final Event.EventListener handlerSessionModified = this::handleSessionModified;
    private void handleSessionModified(final Event<Object> event, final Object args) {
        this.state.invalidate();
    }

    public BooleanProperty dirtyProperty() {
        return this.isDirty;
    }
    public StringProperty currentSessionPathProperty() {
        return this.sessionPath;
    }
    public StringExpression currentSessionTitleProperty() {
        return this.sessionTitle;
    }
    public Session getSession() {
        return this.session.get();
    }
    public BooleanExpression currentSessionCanChangePipelineProperty() {
        return this.currentSessionCanChangePipeline;
    }
}
