package iadgov.physical.cisco;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

public class Plugin implements grassmarlin.plugins.IPlugin, grassmarlin.plugins.IPlugin.HasVersionInfo, grassmarlin.plugins.IPlugin.HasImportProcessors {
    private static String VERSION = "0.1";
    private static String NAME = "Physical Graph Cisco Parsers";

    public Plugin(final RuntimeConfiguration config) {

    }

    // == IPlugin.HasImportProcessors
    @Override
    public Collection<ImportProcessorWrapper> getImportProcessors() {
        return Collections.singleton(
                new ImportProcessorWrapper("Cisco Configs", ".log", ".cisco") {
                    @Override
                    public Iterator<Object> getProcessor(final ImportItem item, final Session session) {
                        try(final BufferedReader reader = Files.newBufferedReader(item.getPath())) {
                            final Object result = CiscoParser.parseFile(reader);
                            if(result != null) {
                                return Collections.singleton(result).iterator();
                            } else {
                                Logger.log(Logger.Severity.WARNING, "The selected file does not contain valid Cisco device data. (%s)", item.getPath().getFileName().toString());
                                //If nothing is returned, then the file does not contain a valid device, and this should be an error rather than a completion.
                                //By returning null, the file will not be marked as processed...  I think.  This has been refactored several times and probably will be a few more.
                                return null;
                            }
                        } catch(IOException ex) {
                            return null;
                        }
                    }

                    @Override
                    public boolean itemIsValidTarget(final Path item) {
                        // Always return false--we can rule out a file but confirming it is a cisco is considerably more involved, expecially considering the non-standard means used to get these files in the first place.
                        return false;
                    }
                }
        );
    }

    // We don't provide any pcap parsers, only file parsers.
    @Override
    public HashMap<Integer, FactoryPacketHandler> getPcapHandlerFactories() {
        return null;
    }

    // == IPlugin.HasVersionInfo
    @Override
    public String getVersion() {
        return VERSION;
    }

    // == Core IPlugin
    @Override
    public String getName() {
        return NAME;
    }
    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    private final Image iconLarge = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        return iconLarge;
    }
}
