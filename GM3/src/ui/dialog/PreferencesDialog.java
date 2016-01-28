/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.Core;
import core.Environment;
import core.types.LogEmitter;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import org.apache.commons.io.FileUtils;
import ui.GrassMarlin;
import ui.icon.Icons;

/**
 * When save and close is clicked this dialog is no longer usable, do not use a singleton of this.
 */
public class PreferencesDialog extends JDialog {
    private javax.swing.JPanel wiresharkPanel;
    private javax.swing.JButton wiresharkSetBtn;
    private javax.swing.JTextField wiresharkValueField;
    
    private javax.swing.JButton saveBtn;
    private javax.swing.JButton cancelBtn;
    
    final List<Runnable> saveActions;
    final Map<String,Object> map;
    
    final Border badBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(0xA94442));
    
    final LogEmitter emitter = LogEmitter.factory.get();
        
    public PreferencesDialog() {
        saveActions = new ArrayList<>();
        map = new HashMap<>();
        ArrayList<JPanel> panels = new ArrayList<>();
        setupWiresharkPanel(panels);
        setupTextEditorPanel(panels);
        setupPDFViewer(panels);
        setupPacketLimit(panels);
        initComponents(panels);
    }
    
    //<editor-fold defaultstate="collapsed" desc="components init">
    private void initComponents(ArrayList<JPanel> panels) {
        setTitle("User Preferences");
        setIconImage(Icons.Cog.get());
        
        cancelBtn = new javax.swing.JButton("Cancel");
        cancelBtn.addActionListener( e -> {
            PreferencesDialog.this.setVisible(false);
            PreferencesDialog.this.dispose();
            saveActions.clear();
        });
        
        saveBtn = new javax.swing.JButton("Save & Close");
        saveBtn.addActionListener( e -> {
            saveActions.parallelStream().forEach(Runnable::run);
            setVisible(false);
        });
        
        JPanel panel = new JPanel();
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(panel);
        panel.setLayout(layout);
        
        ParallelGroup pg = layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING);
        
        panels.forEach( p -> 
            pg.addComponent(p, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(pg
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGap(0, 249, Short.MAX_VALUE)
                            .addComponent(saveBtn)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(cancelBtn)))
                    .addContainerGap())
        );
        
        SequentialGroup sg = layout.createSequentialGroup();
        
        panels.forEach( p -> {
                sg.addContainerGap()
                .addComponent(p, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE);
        });
        
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(sg
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(saveBtn)
                        .addComponent(cancelBtn))
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        add(panel);
        pack();
    }
    //</editor-fold>

    private void setupWiresharkPanel(ArrayList<JPanel> panels) {
        
        saveActions.add(() -> {
            String path = wiresharkValueField.getText();
            String original = (String) map.get("wiresharkValueField");
            
            if( original != null ) {
                if( path.equals(original) ) { 
                    return;
                }
            }
            
            if( path.isEmpty() ) {
                return;
            }
            
            File file = new File( path );
            if( file.exists() && file.isFile() ) {
                Environment.WIRESHARK_EXEC.tryOverwrite( path );
            }
            
            emitter.emit(this, Core.ALERT.INFO, "Wireshark path saved.");
        });
        
        wiresharkPanel = new javax.swing.JPanel();
        wiresharkValueField = new javax.swing.JTextField();
        wiresharkSetBtn = new javax.swing.JButton();
        
        if( Environment.WIRESHARK_EXEC.isAvailable() ) {
            wiresharkValueField.setText(Environment.WIRESHARK_EXEC.getPath());
            wiresharkValueField.setCaretPosition(wiresharkValueField.getText().length());
            map.put("wiresharkValueField", Environment.WIRESHARK_EXEC.getPath());
        }
        
        Border badBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(0xA94442));
        Border originalBorder = wiresharkValueField.getBorder();
        wiresharkValueField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyReleased(KeyEvent e) {
                String path = wiresharkValueField.getText().toLowerCase();
                File file = new File( path );
                if( !path.contains("wireshark") || !file.exists() ) {
                    wiresharkValueField.setBorder(badBorder);
                } else {
                    wiresharkValueField.setBorder(originalBorder);
                }
            }
        });
        
        wiresharkPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Wireshark Executable Path"));

        wiresharkSetBtn.setText("Find");
        wiresharkSetBtn.addActionListener( e -> {
            JFileChooser jfc = new JFileChooser();
            try {
                jfc.setSelectedFile(FileUtils.getUserDirectory().getParentFile());
            } catch( Exception ex ) {
                Logger.getLogger(PreferencesDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
            jfc.showOpenDialog(this);
            File f = jfc.getSelectedFile();
            if( !f.isFile() ) {
                LogEmitter.factory.get().emit(f, Core.ALERT.DANGER, "File does not appear a valid Wireshark executable.");
            } else {
                wiresharkValueField.setText(f.getAbsolutePath());
            }
        });

        javax.swing.GroupLayout wiresharkPanelLayout = new javax.swing.GroupLayout(wiresharkPanel);
        wiresharkPanel.setLayout(wiresharkPanelLayout);
        wiresharkPanelLayout.setHorizontalGroup(
            wiresharkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wiresharkPanelLayout.createSequentialGroup()
                .addComponent(wiresharkValueField, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wiresharkSetBtn))
        );
        wiresharkPanelLayout.setVerticalGroup(
            wiresharkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wiresharkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(wiresharkValueField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(wiresharkSetBtn))
        );
        panels.add(wiresharkPanel);
    }
    private void setupTextEditorPanel(ArrayList<JPanel> panels) {
        javax.swing.JPanel textEditorPanel = new javax.swing.JPanel();
        javax.swing.JTextField textEditorValueField = new javax.swing.JTextField();
        javax.swing.JButton textEditorSetBtn = new javax.swing.JButton();
        
        textEditorSetBtn.setText("Set");
        textEditorSetBtn.addActionListener( e -> {
            Environment.TEXT_EDITOR_EXEC.forceOverwrite(textEditorValueField.getText());
        });
        
        textEditorValueField.setText(Environment.TEXT_EDITOR_EXEC.getPath());
        
        genericPanel(
                "textEditorValueField",
                Environment.TEXT_EDITOR_EXEC,
                null,
                "Preferred text editor",
                "Text editor saved.",
                "Failed to set preffered text editor.",
                textEditorPanel,
                textEditorSetBtn,
                textEditorValueField
        );
        
        panels.add(textEditorPanel);
    }
    private void setupPDFViewer(ArrayList<JPanel> panels) {
        javax.swing.JPanel panel = new javax.swing.JPanel();
        javax.swing.JTextField field = new javax.swing.JTextField();
        javax.swing.JButton btn = new javax.swing.JButton();
        
        btn.setText("Set");
        btn.addActionListener( e -> {
            Environment.PDF_VIEWER_EXEC.forceOverwrite(field.getText());
        });
        
        field.setText(Environment.PDF_VIEWER_EXEC.getPath());
        
        genericPanel(
                "pdfviewerExec",
                Environment.PDF_VIEWER_EXEC,
                null,
                "Preferred PDF Viewer",
                "PDF Viewer saved.",
                "Failed to set preffered PDF Viewer.",
                panel,
                btn,
                field
        );
        
        panels.add(panel);
    }

    private void setupPacketLimit(ArrayList<JPanel> panels) {
        JPanel panel = new javax.swing.JPanel();
        JTextField field = new javax.swing.JTextField();
        JButton btn = new javax.swing.JButton("Set");
        
        String title = "Packet Capture Limit";
        
        Color infinityColor = Color.lightGray;
        String infinityString = "Inifity (-1)";
        
        field.setForeground(infinityColor);
 
        Border goodBorder = field.getBorder();
        
        int[] buffer = new int[1];
        buffer[0] = GrassMarlin.window.preferences.captureLimit;
        
        if( buffer[0] == -1 ) {
            field.setText(infinityString);
        } else {
            field.setText( Integer.toString( buffer[0] ) );
        }
        
        field.addKeyListener(new KeyAdapter(){
            @Override
            public void keyReleased(KeyEvent e) {
                String text = field.getText();
                
                int val = -2;
                
                try {
                    if( infinityString.equals(text) ) {
                        val = 0;
                    } else {
                        val = Integer.valueOf(text);
                    }
                } catch( java.lang.NumberFormatException ex ) {
                    field.setBorder(badBorder);
                    btn.setEnabled(false);
                }
                
                if( val >= -1 ) {
                    field.setBorder(goodBorder);
                    btn.setEnabled(true);
                }
                
                if( val == -1 ) {
                    field.setText(infinityString);
                    field.setForeground(infinityColor);
                } else {
                    field.setForeground(Color.BLACK);
                }
                
                buffer[0] = val;
                
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
                String text = field.getText();
                if( infinityString.equals(text) ) {
                    field.setText("");
                }
            }
        });
        
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                String text = field.getText();
                if( infinityString.equals(text) ) {
                    field.setText("");
                }
            }
        });
        
        btn.addActionListener( e -> {
            GrassMarlin.window.preferences.setCaptureLimit( buffer[0] );
        });
        
        genericPanel(
                "packetLimit",
                null,
                null,
                title,
                null,
                null,
                panel,
                btn,
                field
        );
        
        panels.add(panel);
    }
    
    private void genericPanel( String propKey, Environment env, String containsText, String title, String savedText, String errorText, JPanel panel, JButton btn, JTextField textField ) {
        if( env != null ) {
            saveActions.add(() -> {
                String path = textField.getText();
                String original = (String) map.get(propKey);

                if( original != null ) {
                    if( path.equals(original) ) { 
                        return;
                    }
                }

                if( path.isEmpty() ) {
                    return;
                }

                File file = new File( path );
                if( file.exists() && file.isFile() ) {
                    env.tryOverwrite( path );
                }

                emitter.emit(this, Core.ALERT.INFO, savedText);
            });

            if( env.isAvailable() ) {
                textField.setText(env.getPath());
                textField.setCaretPosition(textField.getText().length());
                map.put(propKey, env.getPath());
            }
        }
        
        Border originalBorder = textField.getBorder();
        if( containsText != null ) {
            textField.addKeyListener(new KeyAdapter(){
                @Override
                public void keyReleased(KeyEvent e) {
                    String path = textField.getText().toLowerCase();
                    File file = new File( path );
                    if( !path.contains(containsText) || !file.exists() ) {
                        textField.setBorder(badBorder);
                    } else {
                        textField.setBorder(originalBorder);
                    }
                }
            });
        }
        
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder(title));

        javax.swing.GroupLayout wiresharkPanelLayout = new javax.swing.GroupLayout(panel);
        panel.setLayout(wiresharkPanelLayout);
        wiresharkPanelLayout.setHorizontalGroup(
            wiresharkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wiresharkPanelLayout.createSequentialGroup()
                .addComponent(textField, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btn))
        );
        wiresharkPanelLayout.setVerticalGroup(
            wiresharkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(wiresharkPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(textField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(btn))
        );
    }
}
