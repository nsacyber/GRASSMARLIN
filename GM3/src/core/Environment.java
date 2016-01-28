/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core;

// core
import core.types.LogEmitter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 * <pre>
 * This class assumes that Core.java did it's job and ensured that all things in the DistributionFiles folder
 * were properly copied into the defaut, or the JVM argument "-Ddir.install" directory. (aka installation directory)
 *
 * "-Ddir.install" must be set if a user wishes to run GM out of another directory that isn't the
 * Java default platform indipendant user directory.
 * </pre>
 */
public enum Environment {

    DIR_INSTALL("dir"),
    DIR_CONVERTED_PCAP("dir.misc.convertedpcap"),
    DIR_LIVE_CAPTURE("dir.misc.livecaptures"),
    DIR_PROPERTIES("dir.properties"),
    PROP_MISC_PROPERTIES("prop.properties.Misc"),
    DIR_DATA("dir.data"),
    DIR_MISC("dir.misc"),
    DIR_LOGS("dir.logs"),
    DIR_IMAGES("dir.images"),
    DIR_IMAGES_ICON("dir.images.icon"),
    DIR_QUICKLIST("dir.quicklist"),
    DIR_FPRINT("dir.data.fingerprint"),
    DIR_FPRINT_GM("dir.data.fingerprint.def"),
    DIR_FPRINT_USER("dir.data.fingerprint.user"),
    DIR_KNOWLEDGEBASE("dir.data.kb"),
    WIRESHARK_EXEC("path.wireshark"),
    TEXT_EDITOR_EXEC("path.texteditor"),
    PDF_VIEWER_EXEC("path.pdfviewer")
    ;
    
    static final String PROP_PREFIX = "prop";
    static final String DIR_PREFIX = "dir";

    public static final String INSTALL_PROPERTY_KEY = "dir.install";
    public static final String APPLICATION_NAME = "GRASSMARLIN3";
    private static final Logger logger = Logger.getLogger(Environment.class.getName());

    static String installPath;

    final String propertyKey;
    String canonicalPath;
    File fileResource;

    Environment(String propertyKey) {
        this.propertyKey = propertyKey;
        if( propertyKey.startsWith(DIR_PREFIX) ) {
            fileResource = new File(formatPath(this.propertyKey, DIR_PREFIX));
            try {
                canonicalPath = fileResource.getCanonicalPath();
            } catch (IOException ex) {
                Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if(propertyKey.startsWith(PROP_PREFIX)) {
            fileResource = new File(formatPath(this.propertyKey, PROP_PREFIX).concat(".properties"));
        } else {
            String path = System.getProperty(this.propertyKey);
            if( path == null ) {
                LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, String.format("Missing argument -D%s", propertyKey));
            } else {
                tryOverwrite(path);
            }
        }
    }
    
    /**
     * Will attempt to create a new File from the provided path, if the file exists it will attempt
     * to get its canonical path. 
     * If it fails Environment::isAvailable will return false
     * @param path Path to try and set this Environment to.
     * @return True if path resolves to an existing file and the canonical path can be retrieved.
     */
    public boolean tryOverwrite(String path) {
        this.fileResource = new File(path);
        if( !this.fileResource.exists() ) {
            this.fileResource = null;
            return false;
        } else {
            try {
                this.canonicalPath = this.fileResource.getCanonicalPath();
            } catch (IOException ex) {
                Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;
    }
    
    public void forceOverwrite(String path) {
        this.fileResource = new File(path);
        this.canonicalPath = path;
    }

    public boolean isAvailable() {
        return this.fileResource != null && this.canonicalPath != null;
    }
    
    public String getKey() {
        return propertyKey;
    }
    
    /**
     * Gets the properties object of the on the enum's file path.
     * @return Loaded property file Properties, else null.
     */
    public Properties getProperties() {
        Properties p = new Properties( );
        try ( FileReader r = new FileReader( getDir() ) ) {
            p.load(r);
        } catch (IOException ex) {
            Logger.getLogger(Environment.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return p;
    }
    
    /**
     * Get a file within this directory, if the file does not exist, it will be created.
     * @param filename Name of the file to get.
     * @return The File of object of the requested file.
     * @throws IOException see File::createNewFile.
     */
    public File getFile( String filename ) throws IOException {
        File f = FileUtils.getFile(getDir(), filename);
        if( !f.exists() ) {
            f.createNewFile();
        }
        return f;
    }

    /**
     * Allows access to each environment file which are assured to exist at
     * startup.
     *
     * @return File object containing this Environment-keys' file.
     */
    public File getDir() {
        return fileResource;
    }

    /**
     * Allows access to canonical paths without throwing an IOException.
     *
     * @return Complete canonical file path to this Environment-keys' folder.
     */
    public String getPath() {
        return canonicalPath;
    }

    public static File getPropertiesFile(Object o) {
        String className;
        if (o instanceof Class) {
            className = ((Class) o).getSimpleName();
        } else {
            className = o.getClass().getSimpleName();
        }
        File file = new File(Environment.DIR_PROPERTIES.getPath() + File.separator + className + ".properties");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Environment.logger.log(Level.SEVERE, "property file [" + file.getAbsolutePath() + "] does not exist and cant be created", e);
            }
        }
        return file;
    }

    public static String getInstallationDir() {
        if (installPath == null) {
            String s = System.getProperty(Environment.INSTALL_PROPERTY_KEY);

            if (s == null || s.isEmpty()) {
                installPath = FileUtils.getUserDirectoryPath() + File.separator + APPLICATION_NAME;
            } else {
                installPath = s;
            }

        }
        return installPath;
    }
    
    private static String formatPath(String path, String prefix) {
        return getInstallationDir() + path.substring(prefix.length()).replace(".", File.separator);
    }

    /**
     * Delete all files in the temp directory that contain "gmdb.g3".
     */
    public static void preClean() {
        File[] files = FileUtils.getTempDirectory().listFiles();
        int i = files.length;
        while (--i > 0) {
            File f = files[i];
            if (f.getName().endsWith("gmdb.g3")) {
                if( FileUtils.deleteQuietly(f) ) {
                    Logger.getGlobal().log(Level.FINEST, "Deleted old resource file{0}", f.getName());
                }
            }
        }
    }

}
