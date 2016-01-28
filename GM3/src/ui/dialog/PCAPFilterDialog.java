/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.border.Border;
import ui.icon.Icons;

/**
 * PFM must be a list of BPFs 
 * each BPF has a title
 * the PFM list is displays the BPF titles.
 * When a title in the list is selected the BPFE is shown in the EF below the list
 * The EF validates expressions
 * There is an AEF above the list where new expressions may be added
 * When the "ADD" button is clicked a popup asks for a new BPF title.
 * If the new title already exists in the list another popup will ask if you wish to overwrite the existing one. 
 * the PFM list is located on disk in {{Instalation directory}}/data/kb/PCAP FILTER DATA
 * the on disk list is delimited text file, the BPFEs are base64 encoded
 * the default BPE of "ALLOW ALL TRAFFIC" will always be in the list, even if a user deletes it. 
 */
public class PCAPFilterDialog extends JFrame {
    private javax.swing.JButton addFilterBtn;
    private javax.swing.JButton closebtn;
    private javax.swing.JButton deleteBtn;
    private javax.swing.JButton editBtn;
    private javax.swing.JList filterNameList;
    private javax.swing.JTextField newFilterField;
    private javax.swing.JButton saveChangesBtn;
    private javax.swing.JTextField editField;
    private javax.swing.JButton useSelectedBtn;
    private javax.swing.JLabel infoTextLabel;
    
    String newFilterDefaultText, titleForEdittingFilter;
    
    Map<String,String> filterStrings, stringsOnDisk;
    
    final BiFunction<String,Consumer<String>,Boolean> BPFValidator;
    
    public static final String DESIRED_FILE_NAME = "PCAP FILTER DATA";
    
    final File listOnDisk;
    
    public static final String DELIMITER = "//";
    
    Border goodBorder;
    Border badBorder = BorderFactory.createLineBorder(new Color(0xA94442), 2);
    
    private BiConsumer<String,String> applyFilter;
    private Supplier<String> activeFilterSupplier;
    
    public PCAPFilterDialog(BiFunction<String,Consumer<String>,Boolean> BPFValidator, File listOnDisk) {
        this.BPFValidator = BPFValidator;
        this.listOnDisk = listOnDisk;
        
        setTitle("PCAP Filter Manager");
        setIconImage(Icons.Grassmarlin.get());
        initComponent();
        goodBorder = newFilterField.getBorder();
        titleForEdittingFilter = "";
        newFilterDefaultText = newFilterField.getText();
        newFilterField.setForeground(Color.LIGHT_GRAY);
        filterStrings = new HashMap<>();
        stringsOnDisk = new HashMap<>();
        
        if( this.BPFValidator == null ) {
            infoTextLabel.setText("PCAP module is unavailable. PCAP Filter Manager is disabled.");
            infoTextLabel.setIcon(Icons.Error.getIcon32());
            newFilterField.setEditable(false);
            filterNameList.setEnabled(false);
        }
        
        loadListFromDisk();
    }
    
    public void setApplyFilter(BiConsumer<String,String> applyFilter) {
        this.applyFilter = applyFilter;
    }
    
    public void setActiveFilterSupplier(Supplier<String> activeFilterSupplier) {
        this.activeFilterSupplier = activeFilterSupplier;
    }
    
    public void put(String title, String filterString) {
        this.filterStrings.put(title, filterString);
        updateList();
    }
    
    private void saveListToDisk() {
        try( FileWriter fw = new FileWriter(listOnDisk) ) {
            filterStrings.forEach((title, filter)->{
                String base64Filter = Base64.getEncoder().encodeToString( filter.getBytes() );

                String line = String.format("%s%s%s\r\n", title, DELIMITER, base64Filter);

                try {
                    fw.write(line);
                } catch (IOException ex) {
                    Logger.getLogger(PCAPFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            });
        } catch (IOException ex) {
            Logger.getLogger(PCAPFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void loadListFromDisk() {
        if( !listOnDisk.exists() || !listOnDisk.isFile() ) {
            JOptionPane.showMessageDialog(rootPane, this, "Cannot load filter list", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            FileReader fileReader = new FileReader(listOnDisk);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            
            String line;
            while( (line = bufferedReader.readLine()) != null ) {
                int split = line.indexOf(DELIMITER);
                String title = line.substring(0, split);
                String base64Filter = line.substring(split+DELIMITER.length());
                try {
                    if( base64Filter.isEmpty() || base64Filter.matches("\\s+") ) {
                        stringsOnDisk.put(title,"");
                    } else {
                        stringsOnDisk.put(title, new String(Base64.getDecoder().decode(base64Filter)));
                    }
                } catch( java.lang.IllegalArgumentException ex ) {
                    Logger.getLogger(PCAPFilterDialog.class.getName()).log(Level.WARNING, "Cannot parse expression for filter \"{0}\".", title);
                }
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PCAPFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PCAPFilterDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        updateList();
    }
    
    void updateList() {
        filterStrings.putAll(stringsOnDisk);
        filterNameList.setListData(filterStrings.keySet().toArray());
    }
    
    void delete() {
        String title = (String) filterNameList.getSelectedValue();
        filterStrings.remove(title);
        stringsOnDisk.remove(title);
        saveListToDisk();
        updateList();
    }
    
    String getSelectionValue() {
        return filterStrings.get( getSelection() );
    }
    
    String getSelection() {
        if( filterNameList.getSelectedValue() == null ) {
            return "";
        }
        return filterNameList.getSelectedValue().toString();
    }
    
    boolean filterValid(String filterString) {
        if( BPFValidator == null ) {
            return false;
        }
        
        boolean valid = BPFValidator.apply(filterString, null);
        
        newFilterField.setBorder( valid ? goodBorder : badBorder );
        
        return valid;
    }
    
    private void initComponent() {
        javax.swing.JScrollPane scroll;
        javax.swing.JPanel addNewFilterPanel;
        javax.swing.JPanel captureFilterPanel;
        javax.swing.JPanel panel;
        addNewFilterPanel = new javax.swing.JPanel();
        panel = new javax.swing.JPanel();
        newFilterField = new javax.swing.JTextField();
        addFilterBtn = new javax.swing.JButton();
        infoTextLabel = new javax.swing.JLabel();
        closebtn = new javax.swing.JButton();
        captureFilterPanel = new javax.swing.JPanel();
        scroll = new javax.swing.JScrollPane();
        filterNameList = new javax.swing.JList();
        editBtn = new javax.swing.JButton();
        saveChangesBtn = new javax.swing.JButton();
        deleteBtn = new javax.swing.JButton();
        useSelectedBtn = new javax.swing.JButton();
        editField = new javax.swing.JTextField();

        addNewFilterPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Add New Filter"));
        newFilterField.setText("New Filter");
        
        newFilterField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyReleased(KeyEvent e) {
                addFilterBtn.setEnabled( filterValid( newFilterField.getText() ) );
            }
        });
        
        MouseAdapter colorAdapter = new MouseAdapter(){
            @Override
            public void mousePressed(MouseEvent e) {
                newFilterField.removeMouseListener(this);
                newFilterField.setText("");
                newFilterField.setForeground(Color.black);
                if( filterValid("") ) {
                    addFilterBtn.setEnabled(true);
                }
            }
        };
        newFilterField.addMouseListener(colorAdapter);
        newFilterField.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e) {
                newFilterField.removeKeyListener(this);
                newFilterField.setText("");
                newFilterField.setForeground(Color.black);
            }
        });
        
        addFilterBtn.setText("Add");
        addFilterBtn.setEnabled(false);
        addFilterBtn.addActionListener((e)->{
            String filterText = newFilterField.getText();
            boolean trySave = true;
            while( trySave ) {
                String title = JOptionPane.showInputDialog(PCAPFilterDialog.this, "Enter title for this filter.");
                
                if( filterStrings.containsKey(title) ) {
                    int choice = JOptionPane.showConfirmDialog(PCAPFilterDialog.this, "A filter with this title already exists, would you like to overwrite "+title+"?");

                    if( choice == JOptionPane.CANCEL_OPTION ) {
                        trySave = false;
                    } else if( choice == JOptionPane.YES_OPTION ) {
                        filterStrings.put(title, filterText);
                        saveListToDisk();
                        trySave = false;
                    } 
                } else {
                    filterStrings.put(title, filterText);
                    saveListToDisk();
                    trySave = false;
                }
            }
            
            newFilterField.setText("");
            updateList();
        });

        infoTextLabel.setBackground(new java.awt.Color(255, 255, 255));
        infoTextLabel.setForeground(new java.awt.Color(51, 51, 51));
        infoTextLabel.setText("Only valid BPF may be applied during packet capture.");
        infoTextLabel.setIcon(Icons.Info.getIcon32());
        
        javax.swing.GroupLayout addNewFilterPanelLayout = new javax.swing.GroupLayout(addNewFilterPanel);
        addNewFilterPanel.setLayout(addNewFilterPanelLayout);
        addNewFilterPanelLayout.setHorizontalGroup(addNewFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewFilterPanelLayout.createSequentialGroup()
                .addComponent(newFilterField)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(addFilterBtn))
            .addComponent(infoTextLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        addNewFilterPanelLayout.setVerticalGroup(addNewFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, addNewFilterPanelLayout.createSequentialGroup()
                .addComponent(infoTextLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 19, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(addNewFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newFilterField, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(addFilterBtn)))
        );

        closebtn.setText("Close");
        closebtn.addActionListener( e -> {
            this.setVisible(false);
        });

        captureFilterPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Capture Filter"));

        scroll.setBorder(null);

        filterNameList.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        scroll.setViewportView(filterNameList);

        filterNameList.addListSelectionListener((item)->{
            String title = getSelection();
            editField.setText( filterStrings.get(title) );
            editBtn.setEnabled(true);
            useSelectedBtn.setEnabled(true);
            deleteBtn.setEnabled(true);
        });
        
        filterNameList.setCellRenderer( new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String title = (String)value;

                if( PCAPFilterDialog.this.activeFilterSupplier.get().equals( title ) ) {
                    label.setIcon(Icons.Accept.getIcon());
                    label.setText( title + " (ACTIVE)" );
                } else {
                    label.setIcon(null);
                    label.setText(title);
                }
                return label;
            }
        });
        
        editBtn.setText("Edit");
        editBtn.setEnabled(false);
        editBtn.addActionListener((e)->{
            saveChangesBtn.setEnabled(true);
            editField.setEditable(true);
            editBtn.setEnabled(false);
            titleForEdittingFilter = getSelection();
        });

        saveChangesBtn.setText("Save Changes");
        saveChangesBtn.setEnabled(false);
        saveChangesBtn.addActionListener((e)->{
            
            String filter = editField.getText();
            
            if( !filterValid(filter) ) {
                JOptionPane.showMessageDialog(PCAPFilterDialog.this, "Invalid filter, changes not saved.");
            } else {
                saveChangesBtn.setEnabled(false);
                editField.setEditable(false);
                editBtn.setEnabled(true);
                filterStrings.put(PCAPFilterDialog.this.titleForEdittingFilter, filter);
                saveListToDisk();
            }
        });

        deleteBtn.setText("Delete");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener((e)->{
            editBtn.setEnabled(false);
            useSelectedBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            delete();
        });
        
        useSelectedBtn.setText("Use Selected");
        useSelectedBtn.setEnabled(false);
        useSelectedBtn.addActionListener(e -> {
            if( applyFilter != null ) {
                applyFilter.accept( getSelection(), getSelectionValue() );
                updateList();
            }
        });

        editField.setText("");
        editField.setEditable(false);

        javax.swing.GroupLayout captureFilterPanelLayout = new javax.swing.GroupLayout(captureFilterPanel);
        captureFilterPanel.setLayout(captureFilterPanelLayout);
        captureFilterPanelLayout.setHorizontalGroup(captureFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scroll)
            .addGroup(captureFilterPanelLayout.createSequentialGroup()
                .addGroup(captureFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(editField, javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(captureFilterPanelLayout.createSequentialGroup()
                        .addComponent(editBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(saveChangesBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deleteBtn)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(useSelectedBtn)))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        captureFilterPanelLayout.setVerticalGroup(captureFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(captureFilterPanelLayout.createSequentialGroup()
                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 202, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(editField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(captureFilterPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(editBtn)
                    .addComponent(saveChangesBtn)
                    .addComponent(deleteBtn)
                    .addComponent(useSelectedBtn)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(captureFilterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(addNewFilterPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(closebtn)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addNewFilterPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(captureFilterPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(closebtn)
                .addGap(6, 6, 6))
        );
        add(panel);
        pack();
    }
    
}
