package grassmarlin.ui.console;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.edit.IActionStack;
import grassmarlin.session.Session;
import grassmarlin.ui.common.SessionInterfaceController;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConsoleSessionInterfaceController implements SessionInterfaceController {
    private final Session session;
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public ConsoleSessionInterfaceController(final RuntimeConfiguration config) {
        this.session = new Session(config);
    }
    public ConsoleSessionInterfaceController(final Session sessionLoaded) {
        this.session = sessionLoaded;
    }

    public Session getSession() {
        return this.session;
    }
    public boolean isDirty() {
        return this.dirty.get();
    }

    @Override
    public Event.IAsyncExecutionProvider getUiExecutionProvider() {
        return Event.PROVIDER_IN_THREAD;
    }

    @Override
    public void createView(View view, boolean visible) {
        //Views are not supported in the console, so we just ignore this command.
    }

    @Override
    public void setViewVisible(View view, boolean visible) {
        //Views are not supported in the console, so we just ignore this command.
    }

    @Override
    public void removeView(View view) {
        //Views are not supported in the console, so we just ignore this command.
    }

    @Override
    public void invalidateSession() {
        this.dirty.set(true);
    }

    @Override
    public IActionStack registerActionStack(IActionStack stackRoot) {
        return stackRoot;
    }
}
