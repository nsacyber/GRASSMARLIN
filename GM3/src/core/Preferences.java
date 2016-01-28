/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core;

import core.types.LogEmitter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jnwizard.JNWizard;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;

/**
 *
 */
public final class Preferences {
    public static final String KEY_MODE = "USER.PREF.MODE";
    public static final int DEFAULT_MODE = 1;
    public static final String KEY_NETMASK = "USER.PREF.NETMASK";
    public static final int DEFAULT_NETMASK = 0xFFFFFF00;
    public static final String KEY_SNAPLEN = "USER.PREF.SNAPLEN";
    public static final int DEFAULT_SNAPLEN = 0xFFFF;
    public static final String KEY_TIMEOUT = "USER.PREF.TIMEOUT";
    public static final int DEFAULT_TIMEOUT = 30 * 1000;
    public static final String KEY_BPF_OPTIMIZE = "USER.PREF.BPF_OPTIMIZE";
    public static final int DEFAULT_BPF_OPTIMIZE = 1;
    public static final String KEY_FILTER_EXPRESSION = "USER.PREF.FILTER_EXPRESSION";
    public static final String DEFAULT_FILTER_EXPRESSION = " ";
    public static final String KEY_FILTER_TITLE = "USER.PREF.FILTER_TITLE";
    public static final String DEFAULT_FILTER_TITLE = "ALLOW ALL TRAFFIC";
    public static final String KEY_CAPTURE_LIMIT = "USER.PREF.CAPTURE_LIMIT";
    public static final int DEFAULT_CAPTURE_LIMIT = -1;
    public static final String KEY_VIS_QUALITY = "USER.PREF.VIS_QUALITY";
    public static final boolean DEFAULT_VIS_QUALITY = true;
    
    public static final String KEY_NETWORK_COLLAPSE = "USER.PREF.NETWORK_COLLAPSE";
    public static final int DEFAULT_NETWORK_COLLAPSE = 10;
    public static final String KEY_CLOUD_COLLAPSE = "USER.PREF.CLOUD_COLLAPSE";
    public static final int DEFAULT_CLOUD_COLLAPSE = 10;
    public static final String KEY_VIEW_DELAY = "USER.PREF.VIEW_UPDATE_DELAY";
    public static final int DEFAULT_VIEW_DELAY = 1500;
    
    private final Level LOG_LEVEL = Level.FINEST;
    public final String[] KEYS = {
        Preferences.KEY_BPF_OPTIMIZE,
        Preferences.KEY_CAPTURE_LIMIT,
        Preferences.KEY_CLOUD_COLLAPSE,
        Preferences.KEY_FILTER_EXPRESSION,
        Preferences.KEY_FILTER_TITLE,
        Preferences.KEY_MODE,
        Preferences.KEY_NETMASK,
        Preferences.KEY_NETWORK_COLLAPSE,
        Preferences.KEY_SNAPLEN,
        Preferences.KEY_TIMEOUT,
        Preferences.KEY_VIEW_DELAY,
        Preferences.KEY_VIS_QUALITY
    };
    /**
     * Flag for checking is pcap is present, avoids loading native library.
     */
    public final boolean pcapAvailable;

    /**
     * Set of flags for PcapReader.java
     */
    public int mode, netmask, snaplen, timeout, optimize, captureLimit;
    public String filterString;
    public String filterTitle;
    public boolean isLive, isOffline, filterIsSet, quality;
    /**
     * settings for graphs and view updates
     */
    public int cloudCollapse;
    public int networkCollapse;
    public int viewUpdateDelay;
    
    private Properties props;
    private File propsOnDisk;

    public Preferences() {
        /* exposed settings */
        filterString = DEFAULT_FILTER_EXPRESSION;
        filterTitle = DEFAULT_FILTER_TITLE;
        mode = DEFAULT_MODE;
        netmask = DEFAULT_NETMASK;
        optimize = DEFAULT_BPF_OPTIMIZE;
        snaplen = DEFAULT_SNAPLEN;
        timeout = DEFAULT_TIMEOUT;
        captureLimit = DEFAULT_CAPTURE_LIMIT;
        cloudCollapse = DEFAULT_CLOUD_COLLAPSE;
        networkCollapse = DEFAULT_NETWORK_COLLAPSE;
        viewUpdateDelay = DEFAULT_VIEW_DELAY;
        quality = DEFAULT_VIS_QUALITY;
        
        /* not exposed settings */
        pcapAvailable = checkAvailable();
        filterIsSet = false;
        isLive = true;
        isOffline = false;
        
    }

    public Preferences loadFromProperties(File file, Properties props) {
        this.propsOnDisk = file;
        this.props = props;
        int propCount = props.size();
        this.mode = tryParseInt(props, KEY_MODE, DEFAULT_MODE);
        this.netmask = tryParseInt(props, KEY_NETMASK, DEFAULT_NETMASK);
        this.snaplen = tryParseInt(props, KEY_SNAPLEN, DEFAULT_SNAPLEN);
        this.timeout = tryParseInt(props, KEY_TIMEOUT, DEFAULT_TIMEOUT);
        this.optimize = tryParseInt(props, KEY_BPF_OPTIMIZE, DEFAULT_BPF_OPTIMIZE);
        this.filterString = tryGetString(props, KEY_FILTER_EXPRESSION, DEFAULT_FILTER_EXPRESSION);
        this.filterTitle = tryGetString(props, KEY_FILTER_TITLE, DEFAULT_FILTER_TITLE);
        this.captureLimit = tryParseInt(props, KEY_CAPTURE_LIMIT, DEFAULT_CAPTURE_LIMIT);
        this.cloudCollapse = tryParseInt(props, KEY_CLOUD_COLLAPSE, DEFAULT_CLOUD_COLLAPSE);
        this.networkCollapse = tryParseInt(props, KEY_NETWORK_COLLAPSE, DEFAULT_NETWORK_COLLAPSE);
        this.viewUpdateDelay = tryParseInt(props, KEY_VIEW_DELAY, DEFAULT_VIEW_DELAY);
        this.quality = tryParseBoolean(props, KEY_VIS_QUALITY, DEFAULT_VIS_QUALITY);
        if( propCount != props.size() ) {
            save();
            LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, "Missing settings were restored to defaults.");
        }
        return this;
    }

    private String tryGetString(Properties prop, String key, String defaultValue ) {
        String strVal = prop.getProperty(key);
        if( strVal == null ) {
            String msg = String.format("Failed to load USER.PREF.%s, default \"%s\" used by default", key, defaultValue);
            Logger.getLogger(Preferences.class.getName()).log(LOG_LEVEL, msg);
            LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, msg);
            prop.put(key, defaultValue);
        }
        return strVal != null ? strVal : defaultValue;
    }

    private boolean tryParseBoolean(Properties prop, String key, boolean defaultValue) {
        String strVal = prop.getProperty(key);
        try {
            boolean val = Boolean.valueOf(strVal);
            return val;
        } catch (Exception ex) {
            String msg = String.format("Failed to load USER.PREF.%s, default \"%d\" used by default", key, defaultValue);
            Logger.getLogger(Preferences.class.getName()).log(LOG_LEVEL, msg, ex);
            LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, msg);
            prop.put(key, Boolean.toString( defaultValue ));
        }
        return defaultValue;
    }

    private int tryParseInt(Properties prop, String key, int defaultValue) {
        String strVal = prop.getProperty(key);
        try {
            int val = Integer.valueOf(strVal);
            return val;
        } catch (Exception ex) {
            String msg = String.format("Failed to load USER.PREF.%s, default \"%d\" used by default", key, defaultValue);
            Logger.getLogger(Preferences.class.getName()).log(LOG_LEVEL, msg, ex);
            LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, msg);
            prop.put(key, Integer.toString( defaultValue ));
        }
        return defaultValue;
    }

    /**
     * Set internal flags to allow an online capture
     *
     * @return this - Reference returned so calls can be chained
     */
    public Preferences setLive() {
        this.isLive = true;
        this.isOffline = false;
        return this;
    }

    public void setActiveFilter(String filterTitle, String filterString) {
        setFilterString(filterString);
        this.filterTitle = filterTitle;
        props.put(Preferences.KEY_FILTER_TITLE, filterTitle);
        props.put(Preferences.KEY_FILTER_EXPRESSION, filterString);
        save();
    }

    public void setFilterString(String filterString) {
        this.filterString = filterString;
    }

    public String getFilterString() {
        return filterString;
    }

    public String getFilterTitle() {
        return filterTitle;
    }

    /**
     * Set internal flags to allow an offline capture
     *
     * @return this - Reference returned so calls can be chained
     */
    public Preferences setOffline() {
        this.isLive = false;
        this.isOffline = true;
        return this;
    }

    /**
     *
     * @param i - Optimization level for a BPF filter program (0 or 1)
     * @return this - Reference returned so calls can be chained
     */
    public Preferences setOptimize(int i) {
        this.optimize = i;
        return this;
    }

    /**
     *
     * @param snaplen - the length of a packet(in bytes) to be buffered for
     * dissection
     * @return this - Reference returned so calls can be chained
     */
    public Preferences setSnaplen(int snaplen) {
        this.snaplen = snaplen;
        return this;
    }

    /**
     *
     * @param netmask - Netmask to be used on a pcap handle
     * @return this - Reference returned so calls can be chained Assume 32 bit
     * integer
     */
    public Preferences setNetmask(int netmask) {
        this.netmask = netmask;
        return this;
    }

    /**
     *
     * @param mode - 1 = Promiscuous, 0 = Nun
     * @return this - Reference returned so calls can be chained
     */
    public Preferences setMode(int mode) {
        this.mode = mode;
        return this;
    }

    public void save() {
        try ( FileWriter fw = new FileWriter(propsOnDisk) ) {
            props.store(fw, "Copyright (C) 2011, 2012\nThis file is part of GRASSMARLIN.");
        } catch (IOException ex) {
            Logger.getLogger(Preferences.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setCaptureLimit(int lim) {
        if( lim >= -1 ) {
            if( this.captureLimit != lim ) {
                this.captureLimit = lim;
                put(KEY_CAPTURE_LIMIT, this.captureLimit);
                save();
            }
        }
    }
    
    public void setViewUpdateDelay(int viewUpdateDelay) {
        this.viewUpdateDelay = viewUpdateDelay;
        put(Preferences.KEY_VIEW_DELAY, this.viewUpdateDelay);
    }

    public void setNetworkCollapse(int networkCollapse) {
        this.networkCollapse = networkCollapse;
        put(Preferences.KEY_NETWORK_COLLAPSE, this.networkCollapse);
    }

    public void setCloudCollapse(int cloudCollapse) {
        this.cloudCollapse = cloudCollapse;
        put(Preferences.KEY_CLOUD_COLLAPSE, this.cloudCollapse);
    }
    
    private void put(String key, int val) {
        put(key, Integer.toString(val));
    }
    
    private void put(String key, String val) {
        this.props.put(key, val);
    }
    
    private boolean checkAvailable() {
        try {
            return new JNWizard().isPresent();
        } catch( Exception | Error err ) {
            return false;
        }
    }
    
    /**
     * Checks high level BPF code.
     *
     * @param filterString String containing a BPF expression.
     * @param errCB A callback which returns the messages of the errors
     * generated here, no exception are thrown or logged this method is meant to
     * fail.
     * @return True if the expression is valid, else false
     */
    @SuppressWarnings("unchecked")
    public static boolean testFilter(String filterString, Consumer<String> errCB) {
        
        if( filterString.isEmpty() ) {
            filterString = " ";
        }
        try {
            try {
                PcapBpfProgram test = new PcapBpfProgram();
                /* check the manual, device type of "1" should be safe to use for all expressions */
                Pcap pcap = Pcap.openDead(1, Pcap.DEFAULT_SNAPLEN);
                /* 1 = always 1 + read manual, -128 is 255.255.255.0 hashed */
                int res = pcap.compile(test, filterString, 1, -128);
                freeFilter(test, true);
                if (errCB != null && !pcap.getErr().isEmpty()) {
                    errCB.accept(pcap.getErr());
                }
                /* anything but -1 means success */
                return res != -1;
            } catch (Error e) {
                if (errCB != null) {
                    errCB.accept(e.getLocalizedMessage());
                }
            }
        } catch (Exception ex) {
            if (errCB != null) {
                errCB.accept(ex.getLocalizedMessage());
            }
        }
        return false;
    }
    
    private static void freeFilter(PcapBpfProgram code, boolean suppressWarnings) {
        try {
            Pcap.freecode(code);
        } catch (Exception | Error ex) {
            if (suppressWarnings) {
                return;
            }
            Logger.getLogger(Preferences.class.getName()).log(Level.WARNING, null, ex);
        }
    }

    public void loadFromProperties(Properties properties) {
        for( int i = 0; i < KEYS.length; ++i ) {
            String key = KEYS[i];
            String value = properties.getProperty(key);
            if( value != null && !value.isEmpty() ) {
                this.put(key, value);
            }
        }
    }

    
}
