/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core;

import core.knowledgebase.KnowledgeBase;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import ui.icon.Icons;

/**
 * If the initializers here fail GM is expected to fail.
 *
 * Core is for doing any pre-run initialization.
 */
public final class Core {

    public enum ALERT {

        MESSAGE(0x3C763D, 0XDFF0D8, 0XD6E9C6), // PLEASENT GREEN
        INFO(0x31708F, 0XD9EDF7, 0XBCE8F1), // INTERESTING BLUE
        WARNING(0x8A6D3B, 0XFCF8E3, 0XFAEBCC), // ATTENTION YELLOW
        DANGER(0xA94442, 0XF2DEDE, 0XEBCCD1) // DANGER RED
        ;
        public Color text, background, border;

        ALERT(Integer t, Integer bg, Integer b) {
            background = new Color(bg);
            border = new Color(b);
            text = new Color(t);
        }
    }

    /**
     * determines whether or not Grassmarlin is running from an IDE or JAR file
     *
     * @return true is Grassmarlin is running from a jar, else false.
     */
    public static boolean runAsJar() {
        return Core.class.getResource("Core.class").toString().startsWith("jar");
    }

    public boolean isJar() {
        return Core.runAsJar();
    }

    public static Optional<KnowledgeBase> kb = Optional.empty();

    public Core() {
        /* check to see if we need to load resources from a JAR */
        if (this.isJar()) {
            System.out.println("Starting JAR Application.");
        }
        Icons.runAsJar = Core.runAsJar();

        try {
            validateResources(Environment.DIR_INSTALL.getDir());
            Environment.preClean();
        } catch (Exception ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void withKnowledgebase(Consumer<KnowledgeBase> consumer) {
        Core.kb.ifPresent(consumer);
    }

    private static void validateResources(File installationDirectory) {
        /* to see where GM thinks its running */
        /*LogEmitter.factory.get().emit(installationDirectory, ALERT.INFO, installationDirectory.toString());*/
        try {
            if (Core.runAsJar()) {
                JarFile jar = new JarFile(new File(Core.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()));
                copyMissing(jar, installationDirectory);
            } else {
                String path = System.getProperty("user.dir").concat(File.separator + "DistributionFiles");
                File project = new File(path);
                copyMissing(project, installationDirectory);
            }
        } catch (Exception ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Determines which resources are exlcluded from ebing copied. These are generally .svn directories.
     * @param file File to check.
     * @return True if the file should not be copied.
     */
    private static boolean excludesFile(File file) {
        return file.getName().matches(".*svn.*");
    }

    /**
     * Copies missing resources, assuming DEV mode.
     *
     * @param source The resource directory of a compiled project.
     * @param dest The install directory.
     * @throws IOException Thrown for permissions errors, usually a file is open
     * in another program.
     */
    private static void copyMissing(File source, File dest) throws IOException {
        List<File> files;

        try {
            files = new ArrayList<>(Arrays.asList(source.listFiles()));
        } catch (Exception ex) {
            /** if running directory is not the build directory */
            URL url = Core.class.getProtectionDomain().getCodeSource().getLocation();
            source = FileUtils.toFile(url);
            source = FileUtils.getFile(source, Environment.APPLICATION_NAME);
            files = new ArrayList<>(Arrays.asList(source.listFiles()));
        }

        if (files.size() == 1) {
            File f = files.get(0);
            files.clear();
            files.addAll(Arrays.asList(f.listFiles()));
        }

        files.removeIf(Core::excludesFile);
        while (!files.isEmpty()) {
            File target = files.get(0);
            if (target.isDirectory()) {
                File relativeFile = intersect(target, dest);
                if (!relativeFile.exists()) {
                    relativeFile.mkdirs();
                }
                files.addAll(Arrays.asList(target.listFiles()));
            } else if (target.isFile()) {
                File relativeFile = intersect(target, dest);
                File parent = relativeFile.getParentFile();
                if (!relativeFile.exists()) {
                    FileUtils.copyFileToDirectory(target, parent, true);
                    System.out.println("Unpacking " + target.getPath());
                }
            }
            files.remove(0);
            files.removeIf(Core::excludesFile);
        }
    }

    /**
     * Joins the bottom of the source onto the end of the destination where each
     * path contains an equal qName.
     *
     * @param source Source file which contains at least one qualifier found in
     * the destination.
     * @param dest Destination file which contains at least one qualifier found
     * in the source.
     * @return File with a path of the joined source and destination files
     * intersecting at the common qualifier.
     */
    public static File intersect(File source, File dest) {
        Path sPath = source.toPath().toAbsolutePath();
        Path dPath = dest.toPath().toAbsolutePath();

        int sLen = sPath.getNameCount();
        int dLen = dPath.getNameCount();

        File res = null;

        for (int j = dLen - 1; j != -1 && res == null; j--) {
            for (int i = sLen - 1; i != -1; i--) {

                if (sPath.getName(i).equals(dPath.getName(j))) {

                    StringBuilder b = new StringBuilder();

                    b.append(dPath.getRoot().toString());

                    for (int r = 0; r < j; r++) {
                        b.append(dPath.getName(r));
                        b.append(File.separator);
                    }

                    for (int r = i; r < sLen; r++) {
                        b.append(sPath.getName(r));
                        if (r < sLen - 1) {
                            b.append(File.separator);
                        }
                    }

                    res = new File(b.toString());
                    break;
                }
            }
        }

        return res;
    }

    private static void copyMissing(JarFile jar, File dest) throws IOException {
        Enumeration<JarEntry> entries = jar.entries();
        String qName = dest.getName();
        int missingEntryCount = 0;

        while (entries.hasMoreElements()) {
            JarEntry e = entries.nextElement();
            if (e.getName().matches("(.*.java)|(.*.class)")) {
                continue;
            }
            if (!e.getName().contains(qName)) {
                continue;
            }
            String path = e.getName().replace(qName, dest.getPath());
            File file = new File(normalize(path));
            if (!file.exists()) {
                if (e.isDirectory()) {
                    file.mkdir();
                } else {
                    FileUtils.touch(file);
                    InputStream is = jar.getInputStream(e);
                    FileUtils.copyInputStreamToFile(is, file);
                    System.out.println("Unpacking " + file.getPath());
                    missingEntryCount++;
                }
            }
        }
        if (missingEntryCount != 0) {
            System.out.println(String.format("Missing entries were restored (%d).", missingEntryCount));
        }
    }

    private static String normalize(String path) {
        try {
            path = new File(path).getCanonicalPath();
        } catch (IOException ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }
        return path;
    }

}
