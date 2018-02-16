package grassmarlin.session.pipeline;

import grassmarlin.Logger;
import grassmarlin.common.fxobservables.FxBooleanProperty;
import javafx.beans.property.BooleanProperty;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class BufferedAggregator implements AutoCloseable {
    private final Consumer<Object> receiver;
    private final List<Iterator<Object>> activeProcessors;
    private final Thread threadProcess;

    private boolean isTerminating = false;
    private final BooleanProperty busy;

    public BufferedAggregator(final Consumer<Object> receiver) {
        this.receiver = receiver;
        this.activeProcessors = new CopyOnWriteArrayList<>();
        this.busy = new FxBooleanProperty(false);

        threadProcess = new Thread(this::Thread_ProcessIterators);
        threadProcess.setName("BufferedAggregator Worker");
        threadProcess.setDaemon(true);
        threadProcess.start();
    }

    @Override
    public void close() {
        isTerminating = true;
        threadProcess.interrupt();
        try {
            threadProcess.join(10);
        } catch(InterruptedException ex) {
            //Ignore it.
        }
    }

    public BooleanProperty busyProperty() {
        return this.busy;
    }

    public void startImport(Iterator<Object> iterator) {
        busy.set(true);
        activeProcessors.add(iterator);
    }

    public Consumer<Object> getReceiver() {
        return this.receiver;
    }

    private void Thread_ProcessIterators() {
        final List<Iterator<Object>> completedIterators = new ArrayList<>(4);
        try {
            while(!isTerminating) {
                if(activeProcessors.isEmpty()) {
                    busy.set(false);
                    Thread.sleep(1);
                } else {
                    boolean hasProcessed = false;
                    for(Iterator<Object> iterator : activeProcessors) {
                        if(iterator.hasNext()) {
                            Object value = iterator.next();
                            if(value != null) {
                                hasProcessed = true;
                                receiver.accept(value);
                            }
                        } else {
                            completedIterators.add(iterator);
                            Logger.log(Logger.Severity.COMPLETION, "Import of %s has completed.", iterator);
                        }
                    }
                    activeProcessors.removeAll(completedIterators);
                    completedIterators.clear();
                    if(!hasProcessed) {
                        Thread.sleep(1);
                    }
                }
            }
        } catch(InterruptedException ex) {
            //terminate thread
        }
    }
}
