package grassmarlin.common;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.pipeline.PipelineTemplate;
import javafx.collections.ObservableList;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class PipelineWatcher {
    private final ObservableList<PipelineTemplate> pipelineTemplates;
    private WatchService watcher;

    private final RuntimeConfiguration config;

    public PipelineWatcher(final RuntimeConfiguration config) {
        this.config = config;

        this.pipelineTemplates = new ObservableListWrapper<>(new CopyOnWriteArrayList<>());
        try {
            this.pipelineTemplates.add(config.getDefaultPipelineTemplate());
        } catch (Exception ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error loading the default pipeline template: %s", ex.getMessage());
        }
    }

    private void loadPipeline(Path file) {
        this.config.getUiEventProvider().runLater(() -> {
            try (InputStream in = Files.newInputStream(file)){
                this.pipelineTemplates.add(PipelineTemplate.loadTemplate(this.config, in));
            } catch (XMLStreamException xse) {
                Logger.log(Logger.Severity.WARNING, "Unable to load malformed pipeline: " + file.getFileName());
            } catch (ClassNotFoundException cne) {
                Logger.log(Logger.Severity.WARNING, "Plugin required by pipeline " + file.getFileName() + " not loaded");
            } catch (IOException ioe) {
                Logger.log(Logger.Severity.WARNING, "Unable to read pipeline file " + file.getFileName());
            }
        });
    }

    public void startPipelineWatcher() {
        Path pipelinePath = Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_PIPELINES));
        try {
            if (Files.exists(pipelinePath) && Files.isDirectory(pipelinePath)) {
                Files.newDirectoryStream(pipelinePath, path -> path.toString().endsWith(".pt")).forEach(this::loadPipeline);

                // start a directory watcher to auto update available pipelineTemplates
                try {
                    this.watcher = FileSystems.getDefault().newWatchService();
                    pipelinePath.register(this.watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);

                    Thread t = new Thread(this::pipelineWatchThread);
                    t.setName("Pipeline Watcher");
                    t.setDaemon(true);
                    t.start();
                } catch (IOException ioe) {
                    Logger.log(Logger.Severity.WARNING, "Unable to start pipeline auto-update service, new Pipeline Templates will not be available");
                }
            } else {
                //BUGFIX#42 Application no longer makes possibly faulty assumptions about the user's parentage
                Logger.log(Logger.Severity.ERROR, "Pipeline directory (" + pipelinePath + ") does not exist, non-default pipelineTemplates not available");
            }
        } catch (IOException ioe) {
            Logger.log(Logger.Severity.ERROR, "Unable to read pipeline directory (" + pipelinePath + "), non-default pipelineTemplates not available");
        }
    }

    final void pipelineWatchThread() {
        while(true) {
            try {
                WatchKey key = this.watcher.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    Path fileName = ((WatchEvent<Path>) event).context();
                    Path file = Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_PIPELINES)).resolve(fileName);

                    // we only care if it is a .pt file
                    if (file.toString().endsWith(".pt")) {
                        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            loadPipeline(file);
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            this.config.getUiEventProvider().runLater(() -> {
                                Optional<PipelineTemplate> template = pipelineTemplates.stream()
                                        .filter(pl -> pl.getName().equals(file.getFileName().toString()))
                                        .findFirst();
                                if (template.isPresent()) {
                                    this.pipelineTemplates.remove(template.get());
                                }
                            });
                        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            this.config.getUiEventProvider().runLater(() -> {
                                Optional<PipelineTemplate> template = pipelineTemplates.stream()
                                        .filter(pl -> pl.getName().equals(file.getFileName().toString()))
                                        .findFirst();
                                if (template.isPresent()) {
                                    this.pipelineTemplates.remove(template.get());
                                    loadPipeline(file);
                                }
                            });
                        }
                    } else {
                        continue;
                    }
                }

                if (!key.reset()) {
                    break;
                }
            } catch (InterruptedException ie) {
                return;
            }
        }
    }

    public ObservableList<PipelineTemplate> getPipelineTemplates() {
        return this.pipelineTemplates;
    }
}
