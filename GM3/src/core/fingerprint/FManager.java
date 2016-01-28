/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.fingerprint;

import TemplateEngine.Data.Filter;
import TemplateEngine.Util.FPC;
import core.Core;
import core.Environment;
import core.types.DataDetails;
import core.types.InvokeObservable;
import core.types.LogEmitter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import static org.apache.commons.lang3.time.DurationFormatUtils.ISO_EXTENDED_FORMAT_PATTERN;

/**
 * <pre>
 * Manages fingerprints.
 *
 * Debug System.property are as follows,
 * 1. {@link #KEY_PROPERTY_DEBUG}=true will turn on debugging for the compiler.
 * 2. if {@link #KEY_PROPERTY_DEBUG}, {@link #KEY_PROPERTY_PATH}=Path/To/Output/Directory will write all generated sources.
 *
 * System.properties are parsed in the {@link #checkProperties()} method.
 * -Dgm.compiler.debug
 * -Dgm.compiler.path
 * </pre>
 */
public class FManager {

    /**
     * Key for a system.property expecting a boolean string true, false, or
     * empty.
     */
    public static final String KEY_PROPERTY_DEBUG = "gm.compiler.debug";
    /**
     * Key for a directory path to write all generated sources to, should be
     * absolute. Requires {@link #KEY_PROPERTY_DEBUG} == "true"
     */
    public static final String KEY_PROPERTY_PATH = "gm.compiler.path";

    final Set<File> loaded;
    final Set<File> pending;
    /**
     * Fires when a new {@link Filter} object was created as a
     * result of a successful fingerprint compilation.
     */
    final InvokeObservable filterChange;
    /**
     * Fires when the status of a File has changed, it notifies listeners with
     * File arguments.
     */
    final InvokeObservable loadedObserver;
    /**
     * Fires when the manager changes state, such as {@link#BUSY} or
     * {@link#IDLE}, listeners are provided the string describing this state as
     * the update argument.
     */
    final InvokeObservable activityObserver;

    /* flags sent as the activity observers notifyObservers( arg ) */
    public static final String BUSY = "FM IS BUSY";
    public static final String IDLE = "FM IS IDLE";
    public static final String COMPLETE = "FM IS COMPLETE";
    public static final String FAILURE = "FM FAILED";
    public static final String CLEARING = "FM IS CLEARING CLASS LOADER";
    public static final String AUTOLOAD = "AutoLoad"; // a key in the properties file for this class used to set 'auto load'

//    int lastProgress = 0;
    Boolean autoLoad;
    boolean verbose;

    String activity;
    Integer lastReportedProgress = 0;

    Filter activeFilter;
    Thread loader;

    /** Nullable output for output of generated source code. Used in debugging. */
    private Consumer<String> outputMethod;
    /** Nullable output directory. Used in debugging. */
    private File outputFile;
    
    private Optional<Consumer<String>> stdOut;
    private Optional<Consumer<String>> stdErr;
    
    private final FPC fpc;
    
    public FManager() {
        
        loaded = Collections.synchronizedSet(new HashSet<File>() {
            @Override
            public boolean add(File e) {
                synchronized (loadedObserver) {
                    loadedObserver.setChanged();
                    loadedObserver.notifyObservers(e);
                }
                return super.add(e);
            }
        });
        pending = Collections.synchronizedSet(new HashSet<>());
        filterChange = new InvokeObservable(this);
        loadedObserver = new InvokeObservable(this);
        activityObserver = new InvokeObservable(this);
        autoLoad = true;
        verbose = true;
        activity = IDLE;
        stdErr = Optional.empty();
        stdOut = Optional.empty();
        FPC fpcObject = null;
        try {
            fpcObject = new FPC(DataDetails.class);
            fpcObject.onFilterChange(this::setActiveFilter);
        } catch (Exception ex) {
            Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        fpc = fpcObject;
        checkProperties();
    }

    private Map<File, Boolean> activeFingerprintFileMap = new HashMap<>();

    public Map<File, Boolean> getActiveFingerprintFileMap() {
        return activeFingerprintFileMap;
    }

    public void maybeAutoLoadFingerprints() {
        new Thread(() -> {
            activeFingerprintFileMap = getSavedFpPaths();
            //savedFileMap.keySet().stream().filter(file -> savedFileMap.get(file))
            if (activeFingerprintFileMap.keySet().isEmpty()) {
                activeFingerprintFileMap = loadDefault().stream().collect(Collectors.toMap(file -> file, (file1) -> new Boolean(true)));
            }
            if (mayAutoLoad()) {
                reload(activeFingerprintFileMap.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey).collect(Collectors.toList()));
            }
        }).start();
    }

    public ArrayList<File> loadDefault() {
        File fprintFile = Environment.DIR_FPRINT_GM.getDir();
        ArrayList<File> files = new ArrayList<>(Arrays.asList(fprintFile.listFiles()));
        files.removeIf(File::isDirectory);
        return files;
    }

    public synchronized boolean reload(List<File> files) {
        if (isLoading()) {
            return false;
        }
        prepForReload();
        return load(files);
    }

    public synchronized boolean load(List<File> files) {
        if (isLoading()) {
            return false;
        }

        files.forEach(pending::add);
        loader = new Thread(this::load);
        loader.start();

        return true;
    }

    /**
     * Will clear the current classes and reload in another thread.
     *
     * @return true if it worked
     */
    public synchronized boolean clear() {
        if (isLoading()) {
            return false;
        }
        loader = new Thread(this::clearEx);
        loader.start();
        return true;
    }

    private void prepForReload() {
        try {
            verbose = false;
            try {
                clearEx();
            } catch (Exception ex) {
                Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, "FManager did not have to reload.");
            }
            verbose = true;
        } catch (Exception ex) {
            Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, null, ex);
            notify(FManager.FAILURE);
        }
    }

    private void reloadEx(List<File> files) {
        try {
            verbose = false;
            try {
                clearEx();
            } catch (Exception ex) {
                Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, "FManager did not have to reload.");
            }
            verbose = true;
            //pending.addAll(files);
            files.forEach(pending::add);
            load();
        } catch (Exception ex) {
            Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, null, ex);
            notify(FManager.FAILURE);
        }
    }

    private void clearEx() {
        try {
            notify(BUSY);
            notify(CLEARING);

            fpc.clear();

            loaded.clear();
            pending.clear();
        } catch (Exception ex) {
            Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            done();
        }
    }

    private void load() {
        if( fpc == null ) {
            LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, "Fingerprint Compiler is disabled. Do you have the correct JDK installed?");
            return;
        }
        try {
            notify(FManager.BUSY);
            pending.removeIf(f -> {
                try {
                    if (loaded.contains(f)) {
                        
                        loadedObserver.setChanged();
                        loadedObserver.notifyObservers(f);
                        notify(FManager.BUSY);
                        
                        lastReportedProgress = getProgressInt();
                        return true;
                    } else {
                        
                        loaded.remove(f);
                        fpc.generateOnComplete(f, fpc::compile);
                        loaded.add(f);
                        
                        lastReportedProgress = getProgressInt();
                        return true;
                    }
                } catch (Exception ex) {
                    Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, null, ex);
                    LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, f.getName() + " did not load because " + ex.getLocalizedMessage());
                    return false;
                }
            });
            loadedObserver.setChanged();
            loadedObserver.notifyObservers("Parsing complete " + loaded.size() + " of " + (loaded.size() + pending.size()));
            if (loaded.isEmpty()) {
                filterChange.notifyObservers(false);
                notify(FManager.FAILURE);
                return;
            }
            try {
                
                fpc.generateFilter();
                fpc.compileFilter();
                fpc.updateFilter(); /** invokes previously set callback */
                
                filterChange.setChanged();
                filterChange.notifyObservers(true);
            } catch (Exception ex) {
                Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                filterChange.notifyObservers(false);
                notify(FManager.FAILURE);
            }
            notify(FManager.COMPLETE);
        } catch (Exception ex) {
            Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        } finally {
            done();
        }
    }

    private synchronized void done() {
        loader = null;
        lastReportedProgress = getProgressInt();
        notify(FManager.IDLE);
    }

    long getmem() {
        return Runtime.getRuntime().freeMemory();
    }

    private void setActiveFilter(Filter<DataDetails> filter) {
        this.activeFilter = filter;
    }
    
    Filter getAndClearFilterObject() {
        filterChange.setChanged();
        synchronized (filterChange) {
            filterChange.notifyAll();
        }
        Filter last = activeFilter;
        activeFilter = null;
        return last;
    }

    private Integer getProgressInt() {
        if (loaded.isEmpty()) {
            return 0;
        }
        double total = pending.size() + loaded.size();
        double current = loaded.size();
        double p = (current / total) * 100.0f;
        Integer num = (int) p;
        if (pending.isEmpty() && !loaded.isEmpty()) {
            return 100;
        }
        return num;
    }

    public Integer getProgress() {
        int progress;
        if (this.isLoading()) {
            progress = lastReportedProgress;
        } else if (this.loaded.isEmpty()) {
            progress = 0;
        } else {
            progress = 100;
        }
        return progress;
    }

    private synchronized void notify(String state) {
        if (!verbose) {
            return;
        }
        if (activity != state) {
            activityObserver.setChanged();
        }

        if (state == BUSY || state == FAILURE || state == CLEARING) {
            filterChange.setChanged();
            filterChange.notifyObservers(false);
        }

        activity = state;
        activityObserver.notifyObservers(state);
    }

    public String getActivity() {
        return activity;
    }

    public Filter getActiveFilter() {
        return activeFilter;
    }

    public synchronized boolean isLoading() {
        return loader != null;
    }

    public Collection<String> getFingerprintNames() {
        return fpc.getFingerprintNames();
    }

    public List<File> getFiles() {
        return Stream.concat(this.loaded.stream(), this.pending.stream()).collect(Collectors.toList());
    }

    public void addFilterChangeObserver(Observer observer) {
        filterChange.addObserver(observer);
    }

    public void addLoadedObserver(Observer observer) {
        loadedObserver.addObserver(observer);
    }

    public void addActivityObserver(Observer observer) {
        activityObserver.addObserver(observer);
    }

    public static URL getSchema() {
        return FManager.class.getResource("fingerprint3.xsd");
    }

    public boolean mayAutoLoad() {
        try {
            Properties prop = getProperties();
            String val = prop.getProperty(AUTOLOAD);
            if (val == null) {
                return autoLoad;
            }
            Boolean b = Boolean.valueOf(val);
            return b != null && b;
        } catch (Exception Ex) {
            return autoLoad;
        }
    }

    public Map<File, Boolean> getSavedFpPaths() {
        Map<File, Boolean> savedFileList = new HashMap<>();
        try {
            Properties prop = getProperties();
            prop.keySet().stream().filter(propName -> !propName.equals(AUTOLOAD)).forEach(path -> {
                String pathString = path.toString();
                savedFileList.put(new File(pathString), Boolean.valueOf(prop.getProperty(pathString)));
            });
        } catch (Exception Ex) {
            Logger.getLogger(FManager.class.getName()).log(Level.SEVERE, "FManager was not able to load fps");
        }
        return savedFileList;
    }

    public static void saveSetting(String key, String val) throws IOException {
        Properties p;
        File f;
        if ((f = Environment.getPropertiesFile(FManager.class)).exists()) {
            try (InputStream is = new FileInputStream(f)) {
                p = new Properties();
                p.load(is);
                p.setProperty(key, val);
                is.close();
                FileOutputStream fos = new FileOutputStream(f);
                p.store(fos, "Updated " + DateFormatUtils.format(System.currentTimeMillis(), ISO_EXTENDED_FORMAT_PATTERN, Locale.ENGLISH));
                try {
                    fos.close();
                } catch (Exception ex) {
                }
            }
        } else {
            throw new IOException("Cannot store properties.");
        }
    }

    public void saveSettings(Map<String, String> keyValMap, boolean autoLoad) throws IOException {
        Properties p;
        File f;
        if ((f = Environment.getPropertiesFile(FManager.class)).exists()) {
            this.activeFingerprintFileMap.clear();
            try (InputStream is = new FileInputStream(f)) {
                p = new Properties();
                //p.load(is);
                keyValMap.entrySet().stream().forEach(pair -> {
                    p.setProperty(pair.getKey(), pair.getValue());
                    this.activeFingerprintFileMap.put(new File(pair.getKey()), Boolean.valueOf(pair.getValue()));
                });

                p.setProperty(AUTOLOAD, autoLoad + "");

                //is.close();
                FileOutputStream fos = new FileOutputStream(f);
                p.store(fos, "Updated " + DateFormatUtils.format(System.currentTimeMillis(), ISO_EXTENDED_FORMAT_PATTERN, Locale.ENGLISH));
                try {
                    fos.close();
                } catch (Exception ex) {
                }
            }
        } else {
            throw new IOException("Cannot store properties.");
        }
    }

    private Properties getProperties() throws IOException {
        Properties p;
        File f;
        if ((f = Environment.getPropertiesFile(FManager.class)).exists()) {
            try (InputStream is = new FileInputStream(f)) {
                p = new Properties();
                p.load(is);
                is.close();
            }
        } else {
            throw new IOException("Cannot load properties.");
        }
        return p;
    }

    public Set<File> getLoaded() {
        return loaded;
    }

    private void checkProperties() {
        String debug = System.getProperty(FManager.KEY_PROPERTY_DEBUG);
        String path = System.getProperty(FManager.KEY_PROPERTY_PATH);

        if (debug != null && !debug.isEmpty()) {
            if ("true".equalsIgnoreCase(debug)) {
                Logger.getGlobal().setLevel(Level.ALL);
                this.stdErr = Optional.of(System.err::println);
                this.stdOut = Optional.of(System.out::println);
            }
        }

        if (path != null) {
            File output = new File(path);

            if (!output.exists()) {
                output.mkdir();
            }

            if (output.exists() && output.isDirectory()) {
                this.outputFile = output;
            }
        }

    }

    private Optional<Consumer<String>> getOutputMethod(final String fileName) {
        Consumer<String> out;
        if( outputFile != null ) {
            final String outputName = fileName.replace(".xml", ".java");
            final File outFile = FileUtils.getFile(this.outputFile, outputName);
            out = (sourceCode) -> {
                try( FileWriter fw = new FileWriter(outFile) ) {
                    System.out.println("gm.compiler :: writing " + fileName + " to " + outFile.getPath());
                    fw.write(sourceCode);
                } catch ( Exception ex ) {
                    Logger.getAnonymousLogger().log(Level.SEVERE, null, ex);
                }
            };
        } else {
            out = this.outputMethod;
        }
        return Optional.ofNullable(out);
    }
}
