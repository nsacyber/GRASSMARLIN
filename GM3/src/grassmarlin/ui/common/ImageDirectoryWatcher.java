package grassmarlin.ui.common;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableSet;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

public class ImageDirectoryWatcher<TImage> {
    /**
     * Wraps an image resource as a property.  The PropertyMap will keep the contents synchronized with the disk.
     */
    protected class ImageMapping extends SimpleObjectProperty<TImage> {
        public ImageMapping(final TImage image) {
            super(image);
        }
    }

    /**
     * Manages a set of ImageMapping objects for a directory.  This corresponds to the mappings of all property values for a given key.
     */
    private class PropertyMap {
        private final Map<String, ImageMapping> images;
        private final Path root;

        public PropertyMap(final Path dirRoot) {
            this.images = new HashMap<>();
            this.root = dirRoot;

            // Populate images from disk
            try {
                for(Path pathImage : Files.newDirectoryStream(this.root)) {
                    newFile(pathImage);
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }

        public String getMappedPropertyName() {
            return this.root.getFileName().toString();
        }

        public ImageMapping mappingFor(final String value) {
            ImageMapping result = images.get(value);
            if(result == null) {
                result = new ImageMapping(null);
                images.put(value, result);
            }
            return result;
        }

        public void newFile(final Path pathNewFile) {
            //There is no reason for implementation to differ between these, at least not at this time.
            modifiedFile(pathNewFile);
        }
        public void modifiedFile(final Path pathUpdatedFile) {
            final String nameFile = pathUpdatedFile.getFileName().toString();
            if(nameFile.endsWith(".png")) {
                final String valueToMatch = nameFile.substring(0, nameFile.length() - 4);
                ImageMapping mapping = images.get(valueToMatch);
                if(mapping == null) {
                    mapping = new ImageMapping(null);
                    images.put(valueToMatch, mapping);
                }
                mapping.set(ImageDirectoryWatcher.this.factoryImage.apply(pathUpdatedFile));
            }
        }
    }

    public class MappedImageList extends ObservableListWrapper<ObjectProperty<TImage>> {
        public MappedImageList() {
            super(new ArrayList<>(), param -> new Observable[] {param});
        }

        public List<TImage> getFilteredImageList() {
            return this.stream()
                    .filter(wrapper -> wrapper != null && wrapper.getValue() != null)
                    .distinct()
                    .map(wrapper -> wrapper.get())
                    .collect(Collectors.toList());
        }
    }

    private final Path rootDirectory;
    private final Map<String, PropertyMap> propertyMaps;
    private final WatchService ws;
    private final Thread threadWatch;
    private final Function<Path, TImage> factoryImage;
    private final Event.IAsyncExecutionProvider uiProvider;

    public ImageDirectoryWatcher(final Path rootDirectory, final Event.IAsyncExecutionProvider uiProvider, final Function<Path, TImage> factoryImage) throws IOException {
        this.rootDirectory = rootDirectory;
        this.propertyMaps = new HashMap<>();
        this.factoryImage = factoryImage;
        this.uiProvider = uiProvider;

        this.ws = FileSystems.getDefault().newWatchService();

        for (final Path path : Files.newDirectoryStream(this.rootDirectory)) {
            this.propertyMaps.put(path.getFileName().toString().toLowerCase(), new PropertyMap(path));
            path.register(ws, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        }

        this.threadWatch = new Thread(this::threadMonitorDirectories);
        this.threadWatch.setDaemon(true);
        this.threadWatch.start();
    }

    public void startWatching(final ObservableSet<?> values, final String subpath, final MappedImageList images) {
        if(images != null && imageListForSet.keySet().stream().allMatch(key -> key != values)) {
            imageListForSet.put(values, images);
            subpathForSet.put(values, subpath);
            values.addListener(this.handlerInvalidation);
            this.handleInvalidation(values);
        }
    }

    public void stopWatching(final ObservableSet<?> values) {
        if(imageListForSet.remove(values) != null) {
            values.removeListener(this.handlerInvalidation);
        }
    }

    public MappedImageList startWatching(final PropertyContainer container) {
        if(!imageListsForContainer.containsKey(container)) {
            final MappedImageList images = new MappedImageList();
            imageListsForContainer.put(container, images);
            container.onPropertyChanged.addHandler(this.handlerPropertyChanged);
            //Call the event handler for each existing property so that we match the initial state.
            for(final Map.Entry<String, Set<Property<?>>> entry : container.getProperties().entrySet()) {
                for(final Property<?> property : entry.getValue()) {
                    this.handlerPropertyChanged.handle(null, container.new PropertyEventArgs(entry.getKey(), property, true));
                }
            }
            return images;
        } else {
            return imageListsForContainer.get(container);
        }
    }

    public void stopWatching(final PropertyContainer container, final MappedImageList imageList) {
        if(imageListsForContainer.remove(container) != null) {
            container.onPropertyChanged.removeHandler(this.handlerPropertyChanged);
        }
    }

    /**
     * When calling startWatching on a container, it is added to imageListsForContainer, which tracks this relationship for later use.
     */
    private Map<PropertyContainer, MappedImageList> imageListsForContainer = new IdentityHashMap<>();
    private Map<ObservableSet<?>, MappedImageList> imageListForSet = new IdentityHashMap<>();
    private Map<ObservableSet<?>, String> subpathForSet = new IdentityHashMap<>();

    private static String mangleName(final String input) {
        return input.replaceAll("[\\\\/,\\.-]", "_"); // This is dangerously close to an Elder Backslash
    }

    private final InvalidationListener handlerInvalidation = this::handleInvalidation;
    private void handleInvalidation(Observable observable) {
        final String subpath = subpathForSet.get(observable);
        if(subpath == null) {
            return;
        }
        final PropertyMap map = propertyMaps.get(subpath);
        if(map == null) {
            return;
        }
        if(observable instanceof Collection) {
            final List<ImageMapping> mappingsCurrent = ((Collection<Object>)observable).stream().map(item -> map.mappingFor(mangleName(item.toString()))).collect(Collectors.toList());

            //If the mappings have changed, clear and rebuild the list (because ordering should mirror the order in which they were processed)
            final MappedImageList mappingsOld = imageListForSet.get(observable);
            if(mappingsCurrent.size() != mappingsOld.size() || !mappingsCurrent.containsAll(mappingsOld)) {
                uiProvider.runLater(() -> {
                    mappingsOld.clear();
                    mappingsOld.addAll(mappingsCurrent);
                });
            }
        }
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
    private void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, PropertyContainer.PropertyEventArgs args) {
        if(args.isAdded()) {
            final PropertyMap map = propertyMaps.get(args.getName().toLowerCase());
            if(map != null) {
                final ImageMapping mapping = map.mappingFor(mangleName(args.getValue().toString()));
                if(this.uiProvider.isExecutionThread()) {
                    imageListsForContainer.get(args.getContainer()).add(mapping);
                } else {
                    this.uiProvider.runLater(() -> imageListsForContainer.get(args.getContainer()).add(mapping));
                }
            }
        } else {
            final PropertyMap map = propertyMaps.get(args.getName().toLowerCase());
            if(map != null) {
                final ImageMapping mapping = map.mappingFor(mangleName(args.getValue().toString()));
                if(this.uiProvider.isExecutionThread()) {
                    imageListsForContainer.get(args.getContainer()).remove(mapping);
                } else {
                    this.uiProvider.runLater(() -> imageListsForContainer.get(args.getContainer()).remove(mapping));
                }
            }
        }
    }


    private void threadMonitorDirectories() {
        while(true) {
            try {
                final WatchKey key = this.ws.take();
                final Path pathRoot = (Path)key.watchable();
                final String nameProperty = pathRoot.getFileName().toString();

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
                            propertyMaps.get(nameProperty).newFile(pathFile);
                        } else if(kind == ENTRY_MODIFY) {
                            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Detected file modification: %s", filename);
                            propertyMaps.get(nameProperty).modifiedFile(pathFile);
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
}
