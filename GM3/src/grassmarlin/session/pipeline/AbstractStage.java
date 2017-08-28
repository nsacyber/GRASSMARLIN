package grassmarlin.session.pipeline;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * This is the base class for all data processing Stages. To create a processing stage for your plugin that can be added
 * to the data pipeline you must create an implementation of this class
 * @param <TContainer> The Object that this stage modifies (Always the current Session). If this stage does not modify
 *                    the Session then declare its container type as Object
 */
public abstract class AbstractStage<TContainer> implements Consumer<Object>, AutoCloseable {
    public static final String DEFAULT_OUTPUT = "Default";

    private final TContainer container;
    private final Thread threadProcessing;
    private final BlockingQueue<Object> backlogAccepted;

    //Stat trackers
    private final AtomicLong cntInputsAccepted;
    private final AtomicLong cntInputsProcessed;
    private final AtomicLong cntOutputsProcessed;
    private final AtomicLong cntOutputsDropped;

    private final AtomicLong msAcceptDelay;

    private boolean isTerminating = false;

    /**
     * PassiveMode processes the inputs but also passes them through to the output as part of the acceptance.  This allows the process method to return only value-added objects, and those objects won't be added to default output.
     */
    private boolean passiveMode = false;

    private final Set<Class<?>> typesAccepted;
    private final Map<String, Set<Class<?>>> outputsWhitelist;
    private final Map<String, Set<Class<?>>> outputsBlacklist;
    private final Map<String, Consumer<Object>> connections;

    protected IPlugin plugin;

    protected AbstractStage(final RuntimeConfiguration config, final TContainer container, final BlockingQueue<Object> backlog, final Class<?>... inputs) {
        this(config, container, backlog, Arrays.asList(inputs));
    }
    protected AbstractStage(final RuntimeConfiguration config, final TContainer container, final BlockingQueue<Object> backlog, final Collection<Class<?>> inputs) {
        this.container = container;
        this.backlogAccepted = backlog;
        this.plugin = config.pluginFor(this.getClass());

        this.typesAccepted = new HashSet<>();
        if(inputs != null) {
            this.typesAccepted.addAll(inputs);
        }

        this.outputsWhitelist = new HashMap<>();
        this.outputsBlacklist = new HashMap<>();
        this.connections = new HashMap<>();

        this.cntInputsAccepted = new AtomicLong(0);
        this.cntInputsProcessed = new AtomicLong(0);
        this.cntOutputsProcessed = new AtomicLong(0);
        this.cntOutputsDropped = new AtomicLong(0);

        this.msAcceptDelay = new AtomicLong(0);

        //TODO: Facilities for multithreaded processing.
        if(backlog == null && this.typesAccepted.isEmpty()) {
            this.threadProcessing = null;
            this.isTerminating = true;
        } else {
            this.threadProcessing = new Thread(this::Thread_Process);
            this.threadProcessing.setDaemon(true);
            this.threadProcessing.setName(String.format("Worker [%s]", this.getName()));
            this.threadProcessing.start();
        }

        this.defineOutput(DEFAULT_OUTPUT);
    }

    protected AbstractStage(final RuntimeConfiguration config, final TContainer container, final Collection<Class<?>> inputs) {
        this(config, container, new ArrayBlockingQueue<>(100), inputs);
    }
    protected AbstractStage(final RuntimeConfiguration config, final TContainer container, final Class<?>... inputs) {
        this(config, container, Arrays.asList(inputs));
    }

    protected void setPassiveMode(final boolean passive) {
        this.passiveMode = passive;
    }

    // == Shutdown routines ==
    @Override
    public void close() throws Exception {
        this.terminate(0);
    }
    public void terminate(final long msWait) throws InterruptedException {
        isTerminating = true;
        backlogAccepted.clear();
        threadProcessing.interrupt();
        if(msWait > 0) {
            threadProcessing.join(msWait);
        }
    }

    // == Connection ==
    protected void defineOutput(final String name, final Collection<Class<?>> classesAllowed, final Collection<Class<?>> classesRejected) {
        final HashSet<Class<?>> whitelist = new HashSet<>();
        if(classesAllowed != null) {
            whitelist.addAll(classesAllowed);
        }
        this.outputsWhitelist.put(name, whitelist);

        final HashSet<Class<?>> blacklist = new HashSet<>();
        if(classesRejected != null) {
            blacklist.addAll(classesRejected);
        }
        this.outputsBlacklist.put(name, blacklist);
    }
    protected void defineOutput(final String name, final Class<?>... outputs) {
        this.defineOutput(name, Arrays.asList(outputs), null);
    }
    protected void disallowOutputClasses(final String name, final Class<?>... outputs) {
        this.outputsBlacklist.get(name).addAll(Arrays.asList(outputs));
    }
    public void connectOutput(final String name, final Consumer<Object> next) {
        if(!outputsWhitelist.containsKey(name)) {
            //If there isn't an entry in the whitelisted classes, then this isn't a valid output queue.
            return;
        }
        if(next != null) {
            connections.put(name, next);
        } else {
            connections.remove(name);
        }
    }
    public Set<String> getOutputs() {
        return connections.keySet();
    }
    public Consumer<Object> targetOf(final String name) {
        return connections.get(name);
    }

    // == Data flow ==
    protected void processOutput(final Object output) {
        if(output == null) {
            //Ignore nulls.
            return;
        } else if(output instanceof Collection<?>) {
            //Process each element of a collection independently.
            for(Object element : (Collection<?>)output) {
                processOutput(element);
            }
        } else {
            this.cntOutputsProcessed.incrementAndGet();

            boolean processedOutput = false;

            for(Map.Entry<String, Consumer<Object>> entry : this.connections.entrySet()) {
                // Skip anything that isn't attached to another stage.
                if(entry.getValue() == null) {
                    continue;
                }

                boolean isAllowed = false;

                final Set<Class<?>> whitelist = outputsWhitelist.get(entry.getKey());
                final Set<Class<?>> blacklist = outputsBlacklist.get(entry.getKey());

                // If the whitelist is blank, then allow the object.
                // If it isn't blank, only allow it if it appears in the list.
                if(whitelist.isEmpty()) {
                    isAllowed = true;
                } else {
                    for (Class<?> clazz : whitelist) {
                        if(clazz.isInstance(output)) {
                            isAllowed = true;
                            break;
                        }
                    }
                }

                if(isAllowed) {
                    // If the blacklist is blank, then fall through to allowing it.
                    // Otherwise, if there is a match, disallow it.
                    if (!blacklist.isEmpty()) {
                        for (Class<?> clazz : blacklist) {
                            if (clazz.isInstance(output)) {
                                isAllowed = false;
                                break;
                            }
                        }
                    }

                    // Re-check isAllowed in case the blacklist changed it.
                    processedOutput |= isAllowed;
                    if(isAllowed) {
                        entry.getValue().accept(output);
                    }
                }
            }

            if(!processedOutput) {
                this.cntOutputsDropped.incrementAndGet();
            }
        }
    }

    public void accept(Object o) {
        this.cntInputsAccepted.incrementAndGet();
        try {
            for(Class<?> clazz : typesAccepted) {
                if(clazz.isInstance(o)) {
                    while(!backlogAccepted.offer(o)) {
                        this.msAcceptDelay.incrementAndGet();
                        Thread.sleep(1);
                    }
                    //In passive mode, we pass everything through to output in addition to copying it to the queue.
                    if(passiveMode) {
                        processOutput(o);
                    }
                    return;
                }
            }
            //It was not accepted; process it as output.
            processOutput(o);
        } catch(InterruptedException ex) {
            //If interrupted we're probably shutting down, so losing an element is harmless.
        }
    }

    protected void Thread_Process() {
        while(!isTerminating) {
            try {
                Object item = backlogAccepted.take();
                processOutput(this.process(item));
                this.cntInputsProcessed.incrementAndGet();
            } catch(InterruptedException ex) {
                if(isTerminating) {
                    //Ignore it.
                } else {
                    ex.printStackTrace();
                }
            }
        }
    }

    // Configuration

    /**
     * This method is called when the stage is created if the PipelineStage is configurable, passing it the
     * configuration associated with that PipelineStage. The default implementation in <code>AbstractStage</code>
     * does nothing.
     *
     * @param configuration an object that contains all the required configuration information for a stage
     */
    public void setConfiguration(Serializable configuration) {
        // The default implementation does nothing
    }

    /**
     * Any object received by the stage that matches one of the types in typesAccepted will be enqueued to be passed to the process method.
     * The results of the process method will be mapped to the named connections per the class associations set in outputsWhitelist.
     * If the result is a Collection, each element of the collection will be processed.
     * If the result is null, it will be discarded.
     * Strictly speaking, this can be protected, but by being defined public it can be manually invoked in special cases.
     * @param obj
     * @return
     */
    public abstract Object process(final Object obj);
    public abstract String getName();

    // == Accessors ==
    public TContainer getContainer() {
        return this.container;
    }

    // == Accessors (for use with VisualAbstractStage in the visualpipeline plugin)
    public BlockingQueue<Object> getQueue() {
        return this.backlogAccepted;
    }

    public AtomicLong getInputsAccepted() {
        return cntInputsAccepted;
    }
    public AtomicLong getInputsProcessed() {
        return this.cntInputsProcessed;
    }
    public AtomicLong getOutputsProcessed() {
        return this.cntOutputsProcessed;
    }
    public AtomicLong getOutputsDropped() {
        return this.cntOutputsDropped;
    }

    public AtomicLong getAcceptDelay() {
        return this.msAcceptDelay;
    }
}
