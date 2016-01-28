package prefuse.util;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import prefuse.util.io.IOLib;

/**
 * <p>Runtime configuration settings for the prefuse framework. Maintains a set
 * of hardwired configuration settings that can be overridden by creating a
 * text properties file containing custom values. By default, prefuse will
 * look for the file "prefuse.conf" in the current working directory for
 * configuration name/value pairs. The framework can be instructed to look for
 * a different file by putting the full path to the file into the
 * "prefuse.config" System property (for example by using a -D flag at the
 * Java runtime command line).</p>
 * 
 * <p>
 * Some of the supported configuration properties include:
 * <ul>
 * <li><code>activity.threadPriority</code> - the thread priority of the
 * ActivityManager thread. The value should be between 1 and 10, with 5 being
 * the standard Java default. The default prefuse setting is 6.</li>
 * <li><code>data.io.worker.threadPriority</code> - the thread priority of
 * asynchronous database worker threads. The default prefuse setting is 5
 * (same as the Java thread default).</li>
 * <li><code>data.filter.optimizeThreshold</code> - the minimum number of items
 * that must be contained in a table for optimized query plans to be
 * considered. The default value is 300.</li>
 * <li><code>util.logdir</code> - the directory in which to write prefuse log
 * files. The default is "null" which defaults logging output to standard
 * output.</li> 
 * <li><code>util.logfile</code> - the filename pattern to use for naming
 * prefuse log files. The default is "prefuse_log_%g.txt", where the %g
 * indicates a unique number for the log file.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Application creators are welcome to add their own custom properties
 * to the configuration files and use the PrefuseConfig instance to
 * access those properties. This class is a singleton, accessed through
 * a static accessor method.
 * </p>
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class PrefuseConfig extends Properties {

    private static final Logger s_logger 
        = Logger.getLogger(PrefuseConfig.class.getName());
    
    private static final PrefuseConfig s_config = new PrefuseConfig();
    
    /**
     * Get the global PrefuseConfig instance.
     * @return the configuration instance
     */
    public static PrefuseConfig getConfig() {
        return s_config;
    }
    
    private PrefuseConfig() {
        setDefaults();
        
        String configFile;
        try {
            configFile = System.getProperty("prefuse.config");
        } catch ( Exception e ) {
            // in applet mode, we could run afoul of the security manager
            configFile = null;
        }
        if ( configFile == null )
            configFile = "prefuse.conf";
        try {
            load(IOLib.streamFromString(configFile));
            s_logger.info("Loaded config file: "+configFile);
        } catch ( Exception e ) {
            // do nothing, just go with the defaults
        }
        
        // direct logging file directory, as set by config properties
        // default of java.util.Logger is to output to standard error
        String logdir = getProperty("util.logdir");
        String logfile = getProperty("util.logfile");
        if ( logdir != null ) {
            try {
                Logger logger = Logger.getLogger("prefuse");
                logger.setUseParentHandlers(false);
                Handler fileHandler = new FileHandler(logdir+"/"+logfile);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Get a prefuse configuration property.
     * @param key the name of the property to lookup
     * @return the property value, or null if the key is not found
     */
    public static String get(String key) {
        return s_config.getProperty(key);
    }

    /**
     * Get a prefuse configuration property as an integer.
     * @param key the name of the property to lookup
     * @return the property value, or the minimum possible
     * integer value if the key is not found or parsing
     * of the number fails.
     */
    public static int getInt(String key) {
        String val = s_config.getProperty(key);
        try {
            return Integer.parseInt(val);
        } catch ( NumberFormatException nfe ) {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Get a prefuse configuration property as a long.
     * @param key the name of the property to lookup
     * @return the property value, or the minimum possible
     * long value if the key is not found or parsing
     * of the number fails.
     */
    public static long getLong(String key) {
        String val = s_config.getProperty(key);
        try {
            return Long.parseLong(val);
        } catch ( NumberFormatException nfe ) {
            return Long.MIN_VALUE;
        }
    }
    
    /**
     * Get a prefuse configuration property as a float.
     * @param key the name of the property to lookup
     * @return the property value, or a Float.NaN
     * value if the key is not found or parsing
     * of the number fails.
     */
    public static float getFloat(String key) {
        String val = s_config.getProperty(key);
        try {
            return Float.parseFloat(val);
        } catch ( NumberFormatException nfe ) {
            return Float.NaN;
        }
    }
    
    /**
     * Get a prefuse configuration property as a double.
     * @param key the name of the property to lookup
     * @return the property value, or a Double.NaN
     * value if the key is not found or parsing
     * of the number fails.
     */
    public static double getDouble(String key) {
        String val = s_config.getProperty(key);
        try {
            return Double.parseDouble(val);
        } catch ( NumberFormatException nfe ) {
            return Double.NaN;
        }
    }
    
    /**
     * Get a prefuse configuration property as a boolean.
     * @param key the name of the property to lookup
     * @return the property value. False is returned
     * if the key is not found or does not parse to
     * a true/false value.
     */
    public static boolean getBoolean(String key) {
        String val = s_config.getProperty(key);
        return "true".equalsIgnoreCase(val);
    }
    
    /**
     * Sets default values for Prefuse properties
     */
    private void setDefaults() {        
        setProperty("size.scale2D", "0.5");
        setProperty("activity.threadPriority", "6");
        setProperty("data.delimiter", ".");
        setProperty("data.graph.nodeGroup", "nodes");
        setProperty("data.graph.edgeGroup", "edges");
        setProperty("data.visual.fieldPrefix", "_");
        setProperty("data.io.worker.threadPriority", 
                String.valueOf(Thread.NORM_PRIORITY));
        
        // prefuse will only attempt to optimize filtering operations
        // on tables with more rows than this threshold value
        setProperty("data.filter.optimizeThreshold", "300");
        
        // setProperty("data.graph.nodeKey", null); // intentionally null
        setProperty("data.graph.sourceKey", "source");
        setProperty("data.graph.targetKey", "target");
        setProperty("data.tree.sourceKey", "parent");
        setProperty("data.tree.targetKey", "child");
        setProperty("visualization.allItems", "_all_");
        setProperty("visualization.focusItems", "_focus_");
        setProperty("visualization.selectedItems", "_selected_");
        setProperty("visualization.searchItems", "_search_");
        
        // setProperty("util.logdir", null); // intentionally null
        setProperty("util.logfile", "prefuse_log_%g.txt");
    }
    
} // end of class PrefuseConfig
