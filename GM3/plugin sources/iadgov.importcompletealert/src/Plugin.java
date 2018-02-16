package iadgov.importcompletealert;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.ui.common.SessionInterfaceController;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import javax.xml.stream.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.SessionSerialization, AutoCloseable {
    public static final String NAME = "Import Complete Alert";

    private final Map<Session, Set<ImportItem>> lookupMonitoredItems;
    private final Map<ObservableBooleanValue, ImportItem> lookupMonitoredStates;
    private final Configuration options;
    private final RuntimeConfiguration config;

    public Plugin(final RuntimeConfiguration config) {
        this.lookupMonitoredItems = new HashMap<>();
        this.lookupMonitoredStates = new HashMap<>();
        this.options = new Configuration();
        this.config = config;
    }

    protected final ListChangeListener<ImportItem> handlerImportItemListChange = this::handleImportItemListChange;
    protected void handleImportItemListChange(final ListChangeListener.Change<? extends ImportItem> change) {
        change.reset();
        // Update hooks on added/removed items--we only have to hook the import Complete handler
        while(change.next()) {
            change.getAddedSubList().forEach(item -> {
                lookupMonitoredStates.put(item.importCompleteProperty(), item);
                item.importCompleteProperty().addListener(this.handlerImportFinished);
                if (item.importCompleteProperty().get()) {
                    this.handleImportFinished(item.importCompleteProperty(), false, item.importCompleteProperty().get());
                }
            });
            change.getRemoved().forEach(item -> {
                item.importCompleteProperty().removeListener(this.handlerImportFinished);
            });
        }
    }

    protected ChangeListener<Boolean> handlerImportFinished = this::handleImportFinished;
    protected void handleImportFinished(final ObservableValue<? extends Boolean> o, Boolean oldValue, Boolean newValue) {
        if(newValue) {

            // If it isn't in the map, then we already processed completion.
            // This sort of thing might happen with very fast imports that complete while this is firing.
            final ImportItem item = lookupMonitoredStates.remove(o);
            if(item != null) {
                item.importCompleteProperty().removeListener(this.handlerImportFinished);
                for(Set<ImportItem> items : lookupMonitoredItems.values()) {
                    items.remove(item);
                }

                if(this.options.getScript() != null) {
                    Logger.log(Logger.Severity.INFORMATION, "Import Complete: Executing \"%s\" \"%s\"", this.options.getScript(), item.toString());
                    try {
                        Runtime.getRuntime().exec(new String[]{this.options.getScript(), item.getPath().toAbsolutePath().normalize().toString()}, null, this.options.getWorkingDirectory() == null ? null : new File(this.options.getWorkingDirectory()));
                    } catch (IOException ex) {
                        Logger.log(Logger.Severity.ERROR, "There was an error calling the script \"%s\" for the import of \"%s\".", this.options.getScript(), item.getPath().toAbsolutePath().normalize().toString());
                    }
                }
            }
        }
    }
        // == AutoCloseable ==
    @Override
    public void close() throws Exception {
        lookupMonitoredItems.keySet().forEach(this::sessionClosed);
    }

    // == IPlugin.SessionEventHooks ==
    @Override
    public void sessionCreated(final grassmarlin.session.Session session, final SessionInterfaceController tabs) {
        if(lookupMonitoredItems.putIfAbsent(session, new HashSet<>()) == null) {
            session.allImportsProperty().addListener(this.handlerImportItemListChange);
        }
    }

    @Override
    public void sessionLoaded(final grassmarlin.session.Session session, final java.io.InputStream stream, final GetChildStream fnGetStream) throws IOException {
        try {
            final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(stream);
            reader.nextTag();
            //If the attributes aren't present, this will set to null, which is the exact desired behavior.
            this.options.setScript(reader.getAttributeValue(null, "Script"));
            this.options.setWorkingDirectory(reader.getAttributeValue(null, "Directory"));
        } catch(XMLStreamException ex) {
            Logger.log(Logger.Severity.WARNING, "There was an error loading the configuration of the %s Plugin: %s", NAME, ex.getMessage());
        }
    }

    @Override
    public void sessionSaving(final grassmarlin.session.Session session, final java.io.OutputStream stream, final CallbackCreateStream fnCreateStream) throws IOException {
        try {
            final XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(stream);
            writer.writeStartElement("Options");
            if(this.options.getScript() != null) {
                writer.writeAttribute("Script", this.options.getScript());
            }
            if(this.options.getWorkingDirectory() != null) {
                writer.writeAttribute("Directory", this.options.getWorkingDirectory());
            }
            writer.writeEndElement();
            writer.close();
        } catch(XMLStreamException ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error while saving: %s", ex.getMessage());
        }
    }
    @Override
    public void sessionClosed(final grassmarlin.session.Session session) {
        final Set<ImportItem> items = lookupMonitoredItems.remove(session);
        if(items != null) {
            session.allImportsProperty().removeListener(this.handlerImportItemListChange);
            for(ImportItem item : items) {
                item.importCompleteProperty().removeListener(this.handlerImportFinished);
                lookupMonitoredStates.remove(item.importCompleteProperty());
            }
        }
    }


    // == IPlugin ==
    @Override
    public Collection<MenuItem> getMenuItems() {
        return Collections.singleton(new ActiveMenuItem("Configure", event -> {
            new PreferenceDialog<>(this.config, this.options).showAndWait();
        }));
    }

    @Override
    public String getName() {
        return NAME;
    }

    private final Image iconLarge = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        return iconLarge;
    }
}
