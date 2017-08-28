package iadgov.bro2connparser;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import javafx.scene.control.MenuItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 *  Plugin to import bro conn.log files
 *  rewritten for v3.3 instead of ported from v3.2
 *
 *  This plugin was developed by an intern with caffeine... (caffeine-to-child ratio < 1:1000000)
 *
 */

public class Plugin implements IPlugin, IPlugin.HasImportProcessors, IPlugin.HasVersionInfo {
    private final String VERSION = "1.0";

    private final ImportProcessorWrapper wrapperBro2Conn;
    private final ImportProcessorWrapper wrapperBro2ConnJson;

    public Plugin(final RuntimeConfiguration config) {
        this.wrapperBro2Conn = new ImportProcessorWrapper("Bro Conn Log", ".bro2") {
            @Override
            public Iterator<Object> getProcessor(final ImportItem item, final Session session) {
                try(final BufferedReader reader = Files.newBufferedReader(item.getPath())) {
                    final Bro2ConnImport broImport = new Bro2ConnImport(config, item, new Bro2ConnLogParser(config, item));
                    return broImport.getIterator();
                } catch(IOException ex) {
                    return null;
                }
            }
            @Override
            public boolean itemIsValidTarget(final ImportItem item) {
                return true;
            }

        };
        this.wrapperBro2ConnJson = new ImportProcessorWrapper("Bro Conn Json", ".json") {
            @Override
            public Iterator<Object> getProcessor(final ImportItem item, final Session session) {
                try(final BufferedReader reader = Files.newBufferedReader(item.getPath())) {
                    final Bro2ConnImport broImport = new Bro2ConnImport(config, item, new Bro2ConnJsonParser(config, item));
                    return broImport.getIterator();
                } catch(IOException ex) {
                    return null;
                }
            }
            @Override
            public boolean itemIsValidTarget(final ImportItem item) {
                return true;
            }
        };
    }


    @Override
    public String getName() {
        return "Bro Conn Logs";
    }

    public String getVersion() {
        return VERSION;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    @Override
    public Collection<ImportProcessorWrapper> getImportProcessors() {
        return Arrays.asList(this.wrapperBro2Conn, this.wrapperBro2ConnJson);
    }

    @Override
    public HashMap<Integer, FactoryPacketHandler> getPcapHandlerFactories() {
        return null;
    }
}