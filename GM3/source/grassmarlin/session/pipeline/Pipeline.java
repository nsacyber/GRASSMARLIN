package grassmarlin.session.pipeline;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.ObservableList;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Pipeline implements AutoCloseable {
    public final static String ENTRY_DEFAULT = "Default";

    // Factory Methods
    public static Pipeline buildPipelineFromTemplate(final RuntimeConfiguration config, final Session session, PipelineTemplate source) {
        if(source == null) {
            source = session.getSessionDefaultTemplate();
        }
        final Pipeline result = new Pipeline();

        Map<IPlugin.PipelineStage, AbstractStage<Session>> instancedStagesMap = new IdentityHashMap<>();
        try {
            for (final IPlugin.PipelineStage stage : source.getStages()) {
                AbstractStage<Session> instanceStage = stage.getStage().getConstructor(RuntimeConfiguration.class, Session.class).newInstance(config, session);

                if (stage.isConfigurable()) {
                    Serializable stageConfig = source.getStageConfiguration(stage.getName());
                    if (stageConfig == null) {
                        stageConfig = ((IPlugin.DefinesPipelineStages)config.pluginFor(stage.getStage())).getDefaultConfiguration(stage);
                    }
                    instanceStage.setConfiguration(stageConfig);
                }

                instancedStagesMap.put(stage, instanceStage);
            }
        } catch(Exception ex) {
            return null;
        }
        result.setStages(instancedStagesMap.values());

        Map<String, List<PipelineStageConnection>> connectionMap = source.getConnections().stream()
                .collect(Collectors.groupingBy(con -> System.identityHashCode(con.getSourceStage()) + "::" + con.getOutput(), Collectors.toList()));

        for (Map.Entry<String, List<PipelineStageConnection>> entry : connectionMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                AbstractStage<Session> stageSource = instancedStagesMap.get(entry.getValue().get(0).getSourceStage());
                String nameOutput = entry.getValue().get(0).getOutput();
                List<AbstractStage<Session>> destinations = entry.getValue().stream()
                        .map(con -> instancedStagesMap.get(con.getDestStage()))
                        .collect(Collectors.toList());
                result.addMulticastLink(config, stageSource, nameOutput, destinations);
            } else if (!entry.getValue().isEmpty()){
                PipelineStageConnection connection = entry.getValue().get(0);
                result.addLink(instancedStagesMap.get(connection.getSourceStage()), connection.getOutput(), instancedStagesMap.get(connection.getDestStage()));
            }
        }

        if (source.getEntryPoints().isEmpty()) {
            result.setEntry(ENTRY_DEFAULT, result.getStages().get(0));
        } else {
            for (Map.Entry<String, List<IPlugin.PipelineStage>> entry : source.getEntryPoints().entrySet()) {
                for (IPlugin.PipelineStage stage : entry.getValue()) {
                    result.setEntry(entry.getKey(), instancedStagesMap.get(stage));
                }
            }
        }

        return result;
    }

    // Data Members
    private final List<Consumer<Object>> stages;

    private final LinkedHashMap<String, BufferedAggregator> entryPoints;

    // Constructor
    protected Pipeline() {
        this.stages = new ArrayList<>();
        //Use a LinkedHashMap for the entry points to preserve order; this is relevant for identifying the default.
        this.entryPoints = new LinkedHashMap<>();
    }

    // BufferedAggregator pass-through methods
    public void startImport(final String entry, Iterator<Object> iterator) {
        if(entryPoints.containsKey(entry)) {
            entryPoints.get(entry).startImport(iterator);
        }
    }

    private final ObservableListWrapper<String> fxEntryPoints = new ObservableListWrapper<>(new ArrayList<>());
    public ObservableList<String> getEntryPoints() {
        return fxEntryPoints;
    }

    // Initialization
    protected void setStages(final Collection<? extends Consumer<Object>> stages) {
        //Terminate any existing stages.
        this.stages.forEach(stage -> {
            if(stage instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) stage).close();
                } catch(Exception ex) {
                    Logger.log(Logger.Severity.WARNING, "Error while terminating stage [%s]: %s", stage.toString(), ex.getMessage());
                }
            }
        });
        this.stages.clear();
        if(stages != null) {
            this.stages.addAll(stages);
        }
    }
    protected void setStages(final Consumer<Object>... stages) {
        this.setStages(Arrays.asList(stages));
    }
    public List<AbstractStage<Session>> getStages() {
        final ArrayList<AbstractStage<Session>> result = new ArrayList<>(this.stages.size());
        for(final Consumer<Object> consumer : this.stages) {
            if(consumer instanceof AbstractStage) {
                result.add((AbstractStage<Session>)consumer);
            }
        }
        return result;
    }
    protected void setEntry(final String label, final Consumer<Object> entry) {
        if(entryPoints.containsKey(label)) {
            //TODO: Check for running imports in that queue.  If anything is running, return, disallowing the edit.
        }
        //Setting to null acts as a remove; this is not a thing that should happen often, but the values in the entryPoints map should be non-null.
        if(entry != null) {
            entryPoints.put(label, new BufferedAggregator(entry));
            fxEntryPoints.add(label);
        } else {
            entryPoints.remove(label);
            fxEntryPoints.remove(label);
        }
    }

    public Consumer<Object> getEntryForLabel(final String label) {
        final BufferedAggregator aggregator = entryPoints.get(label);
        if(aggregator == null) {
            return null;
        } else {
            return aggregator.getReceiver();
        }
    }

    protected void addLink(final AbstractStage<Session> source, final String nameOutput, final AbstractStage<Session> destination) {
        if(stages.contains(source) && stages.contains(destination)) {
            source.connectOutput(nameOutput, destination);
        }
    }

    protected void addMulticastLink(final RuntimeConfiguration config, final AbstractStage<Session> source, final String nameOutput, final Collection<AbstractStage<Session>> destinations) {
        if (stages.contains(source) && stages.containsAll(destinations)) {
            StageMulticast multicast = new StageMulticast(config, source.getContainer());
            this.stages.add(multicast);
            multicast.connectOutput(nameOutput, destinations);
            source.connectOutput(nameOutput, multicast);
        }
    }
    protected void addLink(final int idxOrigin, final String nameOutput, final int idxNext) {
        if(stages.get(idxOrigin) instanceof AbstractStage) {
            ((AbstractStage) stages.get(idxOrigin)).connectOutput(nameOutput, stages.get(idxNext));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void close() {
        setStages();
        for(BufferedAggregator aggregator : entryPoints.values()) {
            aggregator.close();
        }
    }

    // Accessors
    public BooleanExpression busyProperty() {
        BooleanExpression result = null;
        for(final BufferedAggregator aggregator : entryPoints.values()) {
            if(result == null) {
                result = aggregator.busyProperty();
            } else {
                result = result.or(aggregator.busyProperty());
            }
        }
        if(result == null) {
            //If there are no aggregators, we're not busy.
            return new ReadOnlyBooleanWrapper(false);
        } else {
            return result;
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        result.append("Pipeline()\n");
        for(Consumer stage : stages) {
            result.append(' ').append(stage).append('\n');
            if(stage instanceof AbstractStage<?>) {
                final AbstractStage<?> s = (AbstractStage<?>)stage;
                for(String name : s.getOutputs()) {
                    final Consumer<Object> endpoint = s.targetOf(name);
                    if(endpoint != null) {
                        result.append("  ").append(name).append(" -> ").append(endpoint).append('\n');
                    } else {
                        result.append("  ").append(name).append(" -> [null]\n");
                    }
                }
            }
        }

        return result.toString();
    }
}
