package grassmarlin.ui.common;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import grassmarlin.session.ThreadManagedState;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class ImageDirectoryWatcher {
    private static class LazyImage {
        private static final Image IMAGE_ERROR = new Image(LazyImage.class.getClassLoader().getResourceAsStream("resources/images/ErrorUnloadable.png"));
        private Image image;
        private final Path path;

        public LazyImage(final Image image) {
            this.image = image;
            this.path = null;
        }

        public LazyImage(final Path path) {
            this.image = null;
            this.path = path;
        }

        public Image getImage() {
            if(this.image == null) {
                try(final InputStream stream = Files.newInputStream(this.path)) {
                    this.image = new Image(stream);
                } catch(IOException ex) {
                    this.image = IMAGE_ERROR;
                }
            }
            return this.image;
        }

        @Override
        public boolean equals(final Object other) {
            if(other != null && other instanceof LazyImage) {
                if(this.path != null) {
                    return this.path.equals(((LazyImage)other).path);
                } else {
                    return this.image == ((LazyImage)other).image;
                }
            } else {
                return false;
            }
        }
    }

    private class StateUpdate extends ThreadManagedState {
        protected Set<PropertyContainer> modifiedContainers;
        protected boolean imagesModified;

        public StateUpdate() {
            super(500, "ImageDirectoryWatcher", ImageDirectoryWatcher.this.uiProvider);

            this.modifiedContainers = new HashSet<>();
            this.imagesModified = false;
        }

        @Override
        public void validate() {
            synchronized(ImageDirectoryWatcher.this.mappedImagesDefault) {
                synchronized(ImageDirectoryWatcher.this.mappedImages) {
                    if (imagesModified) {
                        for (final PropertyContainer container : ImageDirectoryWatcher.this.imageLists.keySet()) {
                            reassessContainer(container);
                        }
                    } else {
                        for (final PropertyContainer container : modifiedContainers) {
                            //HACK: Use a runlater to avoid a potential deadlock between Fx and ImageDirectoryWatcher worker thread.
                            Platform.runLater(() -> reassessContainer(container));
                        }
                    }
                }
            }

            this.modifiedContainers.clear();
            this.imagesModified = false;
        }

        private void reassessContainer(final PropertyContainer container) {
            final Set<Image> imagesRaw = new HashSet<>();
            for(Map.Entry<String, Set<Property<?>>> entry : container.getProperties().entrySet()) {
                for(final Property<?> value : entry.getValue()) {
                    final Image image = ImageDirectoryWatcher.this.getMappedImage(entry.getKey().toLowerCase(), value);
                    if(image != null) {
                        imagesRaw.add(image);
                    }
                }
            }

            final List<Image> imagesObservable = ImageDirectoryWatcher.this.imageLists.get(container);
            //HACK: We need this check because race conditions make it possible, but it was added to address a problem that occurs after creating a Router Port; The event would fire for the old value despite the hooks being removed--obviously they were not removed but the reason why hasn't been isolated.
            if(imagesObservable == null) {
                return;
            }
            imagesObservable.retainAll(imagesRaw);
            imagesRaw.removeAll(imagesObservable);
            imagesObservable.addAll(imagesRaw);
        }
    }

    final Event.IAsyncExecutionProvider uiProvider;

    private final Map<String, Map<String, LazyImage>> mappedImages;
    private final Map<String, Map<String, LazyImage>> mappedImagesDefault;

    private final Map<PropertyContainer, ObservableList<Image>> imageLists;
    private final StateUpdate state;

    private WatchService ws;
    private Thread threadWatch;

    public ImageDirectoryWatcher(final Event.IAsyncExecutionProvider uiProvider) {
        this.uiProvider = uiProvider;
        this.imageLists = new HashMap<>();

        this.mappedImages = new HashMap<>();
        this.mappedImagesDefault = new HashMap<>();

        this.state = new StateUpdate();

        this.ws = null;
        this.threadWatch = null;
    }

    public void addWatchDirectory(final Path path) {
        this.addImagesFromDirectory(path, true);
    }

    public void addImagesFromDirectory(final Path rootDirectory, final boolean liveUpdates) {
        if(liveUpdates) {
            initializeWatcher();
        }

        try {
            synchronized (this.mappedImages) {
                for (final Path path : Files.newDirectoryStream(rootDirectory)) {
                    final String nameProperty = path.getFileName().toString().toLowerCase();

                    if(liveUpdates && ws != null) {
                        path.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
                    }

                    for(final Path pathFile : Files.newDirectoryStream(path, entry -> entry.getFileName().toString().endsWith(".png") && !Files.isDirectory(entry) && Files.isReadable(entry))) {
                        this.setImage(nameProperty, pathFile);
                    }

                }
            }
        } catch (IOException ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error loading Property Images: %s", ex.getMessage());
        }
    }

    private void initializeWatcher() {
        if(this.ws == null) {
            try {
                this.ws = FileSystems.getDefault().newWatchService();
            } catch(IOException ex) {
                Logger.log(Logger.Severity.WARNING, "Unable to monitory directory: %s", ex.getMessage());
                return;
            }

            this.threadWatch = new Thread(this::threadMonitorDirectories);
            this.threadWatch.setDaemon(true);
            this.threadWatch.start();
        }
    }

    private void threadMonitorDirectories() {
        while(true) {
            try {
                final WatchKey key = this.ws.take();
                final Path pathRoot = (Path)key.watchable();
                final String nameProperty = pathRoot.getFileName().toString().toLowerCase();

                for(WatchEvent<?> event : key.pollEvents()) {
                    final WatchEvent.Kind<?> kind = event.kind();

                    if(kind == OVERFLOW) {
                        continue;
                    } else {
                        final Path filename = ((WatchEvent<Path>)event).context();
                        final Path pathFile = Paths.get(pathRoot.toString(), filename.toString());

                        //During the construction of the ImageDirectoryWatcher, we enumerated the directories
                        // to watch and added the corresponding entries to propertyMaps, so we know the
                        // propertyMaps entry exists--we had to manually register every directory to watch,
                        // and we don't watch that (but, if we did, we could ensure thepropertyMaps collection
                        // was updated in sync)
                        if(kind == ENTRY_CREATE) {
                            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Detected file creation: %s", filename);
                            this.setImage(nameProperty, pathFile);
                        } else if(kind == ENTRY_MODIFY) {
                            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Detected file modification: %s", filename);
                            this.setImage(nameProperty, pathFile);
                        }
                    }
                }

                if(!key.reset()) {
                    //Key is no longer valid, generally resulting from a directory that no longer exists.
                    Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "ImageDirectoryWatcher Key was invalidated");
                    break;
                }
            } catch(InterruptedException ex) {
                ex.printStackTrace();
                return;
            }
        }
    }

    protected void setImage(final String nameProperty, final Path pathFile) {
        final String nameFile = pathFile.getFileName().toString().replaceAll("\\..*$", "");

        Map<String, LazyImage> mapProperty = this.mappedImages.get(nameProperty);
        if (mapProperty == null) {
            mapProperty = new HashMap<>();
            this.mappedImages.put(nameProperty, mapProperty);
        }

        mapProperty.put(nameFile, new LazyImage(pathFile));

        this.state.invalidate(this.state.modifiedContainers, () -> {
            this.state.imagesModified |= !this.imageLists.isEmpty();
            return this.state.imagesModified;
        });
    }

    public void addImage(final String propertyName, final String propertyValue, final Image image) {
        Map<String, LazyImage> valueMappingTemp;
        synchronized(this.mappedImagesDefault) {
            valueMappingTemp = this.mappedImagesDefault.get(propertyName.toLowerCase());
            if(valueMappingTemp == null) {
                valueMappingTemp = new HashMap<>();
                this.mappedImagesDefault.put(propertyName.toLowerCase(), valueMappingTemp);
            }
        }

        final Map<String, LazyImage> valueMapping = valueMappingTemp;
        final LazyImage imageLazy = new LazyImage(image);
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(valueMappingTemp) {
            this.state.invalidate(this.state.modifiedContainers, () -> {
                final boolean result = !imageLazy.equals(valueMapping.put(propertyValue, imageLazy));
                ImageDirectoryWatcher.this.state.imagesModified |= result;
                return result;
            });
        }
    }

    private static String mangleName(final String input) {
        return input.replaceAll("[\\\\/,\\.-]", "_"); // This is dangerously close to an Elder Backslash.  We must be careful not to attract the attention of Codethulu.
    }

    protected Image getMappedImage(final String nameProperty, final Property<?> valueProperty) {
        final String valueText = mangleName(valueProperty.getValue().toString());

        synchronized(this.mappedImages) {
            final Map<String, LazyImage> mappingValues = this.mappedImages.get(nameProperty);
            if (mappingValues != null) {
                final LazyImage result = mappingValues.get(valueText);
                if (result != null) {
                    return result.getImage();
                }
            }
        }

        synchronized(this.mappedImagesDefault) {
            final Map<String, LazyImage> mappingDefaultValues = this.mappedImagesDefault.get(nameProperty);
            if (mappingDefaultValues != null) {
                final LazyImage result = mappingDefaultValues.get(valueText);
                if (result != null) {
                    return result.getImage();
                }
            }
        }
        return null;
    }

    public ObservableList<Image> watchContainer(final PropertyContainer container) {
        this.state.invalidate(state.modifiedContainers, () -> {
            synchronized(ImageDirectoryWatcher.this.imageLists) {
                if (ImageDirectoryWatcher.this.state.modifiedContainers.add(container)) {
                    ImageDirectoryWatcher.this.imageLists.computeIfAbsent(container, k -> new ObservableListWrapper<>(new LinkedList<>()));
                    container.onPropertyChanged.addHandler(this.handlerPropertyChanged);
                    return true;
                } else {
                    return false;
                }
            }
        });

        synchronized(this.imageLists) {
            return this.imageLists.get(container);
        }
    }

    public void unwatchContainer(final PropertyContainer container) {
        this.state.invalidate(state.modifiedContainers, () -> {
            ImageDirectoryWatcher.this.state.modifiedContainers.remove(container);
            ImageDirectoryWatcher.this.imageLists.remove(container);
            container.onPropertyChanged.removeHandler(this.handlerPropertyChanged);
            //We removed the listener and from the collection, so we won't have to reevaluate anything--we just needed to be inside the correct lock.
            return false;
        });
    }
    public void unwatchAll(final Collection<? extends PropertyContainer> containers) {
        this.state.invalidate(state.modifiedContainers, () -> {
            ImageDirectoryWatcher.this.state.modifiedContainers.removeAll(containers);

            for(final PropertyContainer container : containers) {
                ImageDirectoryWatcher.this.imageLists.remove(container);
                container.onPropertyChanged.removeHandler(this.handlerPropertyChanged);
            }

            return false;
        });
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
    private void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        this.state.invalidate(this.state.modifiedContainers, () -> {
            //We only need to invlaidate the list if there is an image for this
            if(ImageDirectoryWatcher.this.getMappedImage(args.getName().toLowerCase(), args.getProperty()) != null) {
                return this.state.modifiedContainers.add(args.getContainer());
            } else {
                //There is no image for the modified property.
                return false;
            }
        });
    }
}
