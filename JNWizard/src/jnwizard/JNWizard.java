package jnwizard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.jnetpcap.Pcap;

/**
 *
 * @author BESTDOG JNetPcap wizard deals with deployment issues with JNetPcap.
 */
public class JNWizard {

    public static List<Throwable> errors;
    public static String path;

    public JNWizard() {
        errors = new ArrayList<>();
    }

    public boolean isPresent() {
        boolean val;
        try {
            String version = Pcap.libVersion();
            val = true;
        } catch (Exception | Error e) {
            errors.add(e);
            val = false;
        }
        return val;
    }

    public static String getLibraryName() {
        String name = Pcap.LIBRARY;
        String WINDOWS = "windows";
        String os = System.getProperty("os.name").contains("Win") ? WINDOWS : "debian";
        String ext = os.equals(WINDOWS) ? "dll" : "so";
        String prefix = os.equals(WINDOWS) ? "" : "lib";
        String libraryName = prefix + name + "." + ext;
        if( JNWizard.path != null ) {
            libraryName = path + File.separator + libraryName;
        }
        File userDir = FileUtils.getUserDirectory();
        File gm = FileUtils.getFile(userDir, "GRASSMARLIN3");
        return gm.getPath() + File.separator + libraryName;
    }

    public static File getLibraryFile() {
        return new File(getLibraryName());
    }

    public void load() {
        System.load(this.getLibraryFile().getAbsolutePath());
    }

    public String getResourceName() {
        String WINDOWS = "windows";
        String arch = System.getProperty("os.arch").contains("64") ? "64" : "32";
        String os = System.getProperty("os.name").contains("Win") ? WINDOWS : "debian";
        String ext = os.equals(WINDOWS) ? "dll" : "so";
        String prefix = os.equals(WINDOWS) ? "" : "lib";
        return String.format("%s-%s-%sjnetpcap.%s", os, arch, prefix, ext);
    }

    public InputStream getStream() {
        InputStream stream = getClass().getResourceAsStream(this.getResourceName());
        return stream;
    }

    public void run() {
        if (!this.isPresent()) {
            InputStream is = getStream();
            String lib = getLibraryName();
            File file = new File(lib);
            System.out.println(String.format("Creating library '%s'.", file.getPath()));
            try (FileOutputStream os = new FileOutputStream(file)) {
                int read = 0;
                byte[] buffer = new byte[4096];
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.close();
            } catch (Exception ex) {
                errors.add(ex);
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
            try {
                Thread.sleep(120);
            } catch (InterruptedException ex) {
                errors.add(ex);
                Logger.getLogger(JNWizard.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                load();
            } catch(Exception | Error ex ) {
                errors.add(ex);
            }
        }
    }

    public long libSize() {
        return new File(getLibraryName()).length();
    }

    public File getExecutionSource() {
        URL url = this.getClass().getProtectionDomain().getCodeSource().getLocation();
        return new File(url.getFile());
    }

    public String getLibraryVersion() {
        String version;
        if (this.isPresent()) {
            version = Pcap.libVersion();
        } else {
            version = "error";
        }
        return version;
    }

    @Override
    public String toString() {
        boolean present = this.isPresent();
        String resourceName = this.getResourceName();
        boolean streamOpen = this.getStream() != null;
        File source = this.getExecutionSource();
        boolean sourceGood = source != null ? source.exists() && source.canRead() : false;
        String version = this.getLibraryVersion();
        return String.format(
                "okay:%b, resource:%s, stream:%b, source:%b, lib-size:%d, ver:%s",
                present,
                resourceName,
                streamOpen,
                sourceGood,
                libSize(),
                version
        );
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        wizard();
    }

    static void debug() {
        try {
            String lib = "jnetpcap";
            System.loadLibrary(lib);
        } catch (Exception | Error ex) {
            errors.add(ex);
        }
    }

    public static void wizard() {
        wizard(false);
    }

    public static void wizard(boolean debug) {
        JNWizard jnw = new JNWizard();
        debug();
        if (!jnw.isPresent()) {
            jnw.run();
        }
        if( debug ) {
            System.out.println(jnw);
            jnw.errors.forEach(t -> {
                System.out.println(t.getMessage());
                System.out.println(t);
            });
        }
    }

}
