/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exportmodule;

import core.Core;
import core.exec.Task;
import core.types.LogEmitter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import ui.GrassMarlin;

/**
 *
 */
public class ExportShareTask extends Task {

    File dataFile;
    
    public void setDataFile(File dataFile) {
        this.dataFile = dataFile;
    }
    
    @Override
    public void run() {
        if( dataFile == null ) {
            LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, "Failed to export data.");
        }
        
        List<File> files = new ArrayList();
        /* included data export */
        files.add(dataFile);
        /* and all imported items */
        files.addAll(GrassMarlin.window.getImports());
        
        File saveZipfile = getSaveFile();
        
        try {
            saveFilesToZip( saveZipfile, files );
        } catch (IOException ex) {
            Logger.getLogger(ExportShareTask.class.getName()).log(Level.SEVERE, null, ex);
            LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, "Failed to export. "+ ex.getMessage());
        }
        
    }

    private File getSaveFile() {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(FileUtils.getUserDirectory());
        jfc.setSelectedFile(getDefaultFile());
        jfc.showSaveDialog(GrassMarlin.window);
        return jfc.getSelectedFile();
    }

    private File getDefaultFile() {
        return new File(getDefaultFileName());
    }

    private String getDefaultFileName() {
        String timeStamp = DateFormatUtils.format(System.currentTimeMillis(), "k_m_s", Locale.ENGLISH);
        String dateStamp = DateFormatUtils.format(System.currentTimeMillis(), "M_d_y", Locale.ENGLISH);
        return String.format("GM_Share_D%sT%s.zip", dateStamp, timeStamp);
    }

    private static void saveFilesToZip(File saveZipfile, List<File> files) throws IOException {
        ZipOutputStream out = new ZipOutputStream( new FileOutputStream(saveZipfile) );
        
        for( File file : files ) {
            String entryName = file.getName();
            ZipEntry zipEntry = new ZipEntry(entryName);
            
            FileInputStream in = new FileInputStream(file);
            out.putNextEntry(zipEntry);
            IOUtils.copy(in, out);
            out.closeEntry();
        }
        
        out.flush();
        out.finish();
    }
    
}
