/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.log;


import core.Core;
import core.Environment;
import core.types.LogEmitter;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import ui.platform.UIPanel;


/**
 *
 */
public class UILog extends UIPanel {
    
    final EventLogTable table;
    final JScrollPane pane;
    public final Observer observer;
    
    final File logfile;
    final boolean logfileAvailable;
    
    public UILog() {
        super("Event Log");
        String timestamp = DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(System.currentTimeMillis());
        timestamp = timestamp.replaceAll("\\W", "_");
        String path = Environment.DIR_LOGS.getPath() + File.separator + timestamp + ".txt";
        logfile = new File( path );
        
        try {
            logfile.createNewFile();
        } catch (IOException ex) {
            Logger.getLogger(UILog.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        logfileAvailable = logfile.exists();
        
        table = new EventLogTable();
        pane = new JScrollPane(table);
        observer = (Observable o, Object arg) -> {
            if( arg instanceof LogEmitter.Log ) {
                try {
                    LogEmitter.Log mp = (LogEmitter.Log) arg;
                    log( mp );
                    int height = table.getHeight();
                    SwingUtilities.invokeLater(()->{
                        pane.getVerticalScrollBar().setValue( height );
                    });
                } catch( Exception ex ) { System.out.println(ex); }
            }
        };
        initComponent();
    }
    
    private void initComponent() {
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pane.setAutoscrolls(true);
        Border b = BorderFactory.createEmptyBorder();
        setBorder(b);
        pane.setBorder(b);
        table.setBorder(b);
        table.setDragEnabled(false);
        add( pane, BorderLayout.CENTER );
    }
    
    ArrayList<String> buffer = new ArrayList<>();
    public void log( LogEmitter.Log log ) {
        table.log(log);
        buffer.add(0, log.toCSV());
        try {
            FileUtils.writeLines(logfile, buffer, true);
        } catch (IOException ex) {
            Logger.getLogger(UILog.class.getName()).log(Level.SEVERE, null, ex);
        }
        buffer.clear();
    }


    public void log( Core.ALERT a, String msg ) {
        log(new LogEmitter.Log(this, a, msg));
    }

    public void log( Core.ALERT a, String msg, Long timeVal ) {
        log( new LogEmitter.Log( this, a, msg ) );
    }

    public File getLogfile() {
        return logfile;
    }
    
    public String getLogfilePath() {
        return logfile.getAbsolutePath();
    }
    
    public boolean isLogfileAvailable() {
        return logfileAvailable;
    }
    
}
