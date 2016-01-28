/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exportmodule;

import core.Core;
import core.Pipeline;
import core.types.LogEmitter;
import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import org.apache.commons.io.FileUtils;
import ui.GrassMarlin;

/**
 *
 */
public class TopologyExporter extends ExportItem {

    public static final String FILE_TYPE = "PNG";
    private final List<Consumer> exportFunctions;
    
    public TopologyExporter(Consumer<File> exportFunction) {
        this();
        add( exportFunction );
    }
    
    public TopologyExporter() {
        exportFunctions = new ArrayList<>();
    }

    @Override
    public File getExportFile() {
        JFileChooser jfc = new JFileChooser();
        File userDir = FileUtils.getUserDirectory();
        jfc.setSelectedFile(userDir);
        int res = jfc.showSaveDialog(GrassMarlin.window);
        if( res == JFileChooser.APPROVE_OPTION ) {
            return jfc.getSelectedFile();
        }
        return null;
    }

    @Override
    public void export(Pipeline pipeline, File file) {
        if( file == null ) {
            complete();
            return;
        }
        exportFunctions.forEach(c->c.accept(file));
        if( file.exists() && file.isDirectory() ) {
            openDesktopFolder(file);
        }
    }

    private void openDesktopFolder(File folder) {
        try {
            Desktop.getDesktop().open(folder);
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public TopologyExporter add(Consumer<File> exportFunction) {
        exportFunctions.add(exportFunction);
        return this;
    }
    
}
