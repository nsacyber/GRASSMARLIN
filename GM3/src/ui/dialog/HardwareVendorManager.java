/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.Core;
import core.knowledgebase.KnowledgeBaseLoader;
import core.types.LogEmitter;
import org.apache.commons.lang3.SerializationUtils;
import ui.icon.Icons;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 */
public class HardwareVendorManager extends JFrame {
    private JSpinner editConfidenceSpinner;
    private JTextField editOUIField;
    private JTextField editTypeField;
    private JTextField editVendorNameField;
    private JTable table;
    private JTable ouiTable;
    private TableRowSorter<DefaultTableModel> rowSorter;
    
    private static final Pattern csvLine = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    private static final int COL_OUI = 0;
    private static final int COL_VENDOR = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_CONF = 3;
    private Map<String, String> ouiVendorMap;

    public HardwareVendorManager() {
        initComponents();
        TableColumn col0 = this.table.getColumnModel().getColumn(0);
        col0.setPreferredWidth(50);
        col0.setMaxWidth(50);
        col0.setMinWidth(50);
        TableColumn col3 = this.table.getColumnModel().getColumn(3);
        col3.setPreferredWidth(82);
        col3.setMaxWidth(82);
        col3.setMinWidth(82);
        loadFromDisk();
    }
    
    private void loadFromDisk() {
        if(!KnowledgeBaseLoader.Target.HardwareVendor.validate()) {
            fail();
        }
        new Thread(()->{
            File file = KnowledgeBaseLoader.Target.OUIVendor.loadTarget(null);
            byte[] obj_bytes;
            try (FileInputStream fins = new FileInputStream(file)) {
                obj_bytes = new byte[(int)file.length()];
                fins.read(obj_bytes);
                fins.close();
                this.ouiVendorMap = (Map<String,String>)SerializationUtils.deserialize(obj_bytes);
                //map.entrySet().stream().forEach(entry -> System.out.println(entry.getKey() + " : " + entry.getValue()));
                DefaultTableModel model = (DefaultTableModel) ouiTable.getModel();
                this.ouiVendorMap.entrySet().stream().forEach(entry -> {
                    model.addRow(new Object[]{entry.getKey(), entry.getValue()});
                });
                this.ouiTable.getRowSorter().toggleSortOrder(1);

            } catch (IOException e) {
            }
        }).start();
        
        try {
            load( new File(KnowledgeBaseLoader.Target.HardwareVendor.getDefaultPath()) );
        } catch (IOException ex) {
            Logger.getLogger(HardwareVendorManager.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        }
        
    }
    
    private void load( File f ) throws IOException {
        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        
        String line = null;
        Object[] items = new Object[4];
        while( ( line = br.readLine() ) != null ) {
            try {
                String[] strings = line.split(csvLine.pattern());
                
                items[0] = strings[0].replace('"', ' ').trim();
                items[1] = strings[1].replace('"', ' ').trim();
                items[2] = strings[2].replace('"', ' ').trim();
                items[3] = strings[3].replaceAll("\\D", "").trim();
                
                /* a row may be invalid */
                if( items.length == model.getColumnCount() ) {
                    model.addRow(items);
                }
            } catch( Exception ex ) {
                Logger.getLogger(HardwareVendorManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        br.close();
    }
    
    private void save() throws IOException {
        List<String> lines = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        StringBuilder lineBuilder = new StringBuilder();

        model.getDataVector().stream().forEach(rows -> {
            ((Vector) rows).stream().forEach(col -> {
                if (lineBuilder.length() != 0) lineBuilder.append(", ");
                lineBuilder.append("\"").append(col).append("\"");
            });
            lineBuilder.append("\r\n");
            lines.add(lineBuilder.toString());
            lineBuilder.setLength(0);
        });
        if(!lines.isEmpty()) {
            if(KnowledgeBaseLoader.Target.HardwareVendor.save(lines)) {
                LogEmitter.factory.get().emit(this, Core.ALERT.INFO, "Hardware vendors updated.");
            }
        }
        
    }
    
    private void fail() {
        JOptionPane.showMessageDialog(this, "An error occurred while loading hardware vendors.");
        setVisible(false);
    }

    private void initComponents() {
        this.table = new JTable();
        ouiTable = new JTable();
        this.editOUIField = new JTextField();
        this.editVendorNameField = new JTextField();
        this.editTypeField = new JTextField();
        this.editConfidenceSpinner = new JSpinner();
        setTitle("Hardware Vendor Manager");
        setIconImage(Icons.Grassmarlin.get());
        buildDialog(initHardwareTablePanel(), initEditPanel());
    }

    public void buildDialog(JComponent jPanel1, JComponent jPanel2) {
        JPanel panel = new JPanel();
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap()
                                .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );
        add(panel);
        pack();
    }

    private JComponent initHardwareTablePanel() {
        JScrollPane jScrollPane1 = new JScrollPane();
        JPanel jPanel1 = new JPanel();
        JComboBox sortByChooser = new JComboBox();
        JButton deleteBtn = new JButton();

        JTextField searchTable = new JTextField();
        searchTable.setText("Filter Table");
        searchTable.setToolTipText("Type here to filter the table");
        searchTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                rowSorter.setRowFilter(RowFilter.regexFilter(searchTable.getText()));
            }
        });

        jPanel1.setBorder(BorderFactory.createTitledBorder("Hardware Vendors"));
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"OUI", "Vendor", "Type", "Confidence"}
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean[]{
                    false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };
        table.setModel(model);
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setResizable(false);
            table.getColumnModel().getColumn(1).setResizable(false);
            table.getColumnModel().getColumn(2).setResizable(false);
            table.getColumnModel().getColumn(3).setResizable(false);
        }

        table.getSelectionModel().addListSelectionListener((e) -> {
            int row = table.getSelectedRow();
            if (table.getRowCount() < row || row < 0) {
                return;
            }
            //you must do this incase the table was sorted
            row = table.convertRowIndexToModel(row);
            editOUIField.setText((String) table.getModel().getValueAt(row, 0));
            editVendorNameField.setText((String) table.getModel().getValueAt(row, 1));
            editTypeField.setText((String) table.getModel().getValueAt(row, 2));
            editConfidenceSpinner.setValue(Integer.valueOf((String) table.getModel().getValueAt(row, 3)));
        });

        //table.setAutoCreateRowSorter(true);

        //TableRowSorter<DefaultTableModel>
        rowSorter = new TableRowSorter<>(model);

        table.setRowSorter(rowSorter);



        sortByChooser.setModel(new DefaultComboBoxModel(new String[]{"OUI", "Vendor", "Type", "Confidence"}));
        sortByChooser.setBorder(null);
        sortByChooser.addItemListener(e -> {
            TableColumn tc = table.getColumn(e.getItem().toString());
            table.getRowSorter().toggleSortOrder(tc.getModelIndex());
        });

        deleteBtn.setText("Delete Vendor");
        deleteBtn.addActionListener((e) -> {

            int row = table.getSelectedRow();
            if (table.getRowCount() < row || row < 0) {
                return;
            }
            //you must do this incase the table was sorted
            row = table.convertRowIndexToModel(row);
            model.removeRow(row);

            try {
                save();
            } catch (IOException ex) {
                Logger.getLogger(HardwareVendorManager.class.getName()).log(Level.SEVERE, null, ex);
            }

        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addComponent(sortByChooser, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(searchTable)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(deleteBtn))
                                        .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(sortByChooser, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(searchTable)
                                        .addComponent(deleteBtn)).addContainerGap())
        );
        return jPanel1;
    }

    private JComponent initEditPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Edit", createEditPane());
        tabbedPane.addTab("New", createNewVendorPane());
        tabbedPane.addTab("Import", createImportPane(tabbedPane));
        return tabbedPane;
    }

    private JComponent createImportPane(JTabbedPane tabbedPane) {
        JPanel jPanel1 = new JPanel();
        JScrollPane jScrollPane1 = new JScrollPane();
        JButton importBtn = new JButton();
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new String[]{"OUI", "Vendor"}
        ) {
            Class[] types = new Class[]{
                    java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                    false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };
        ouiTable.setModel(model);
        jScrollPane1.setViewportView(ouiTable);
        TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>(model);
        ouiTable.setRowSorter(rowSorter);

        importBtn.setText("Import");
        importBtn.addActionListener((e) -> {

            int row = ouiTable.getSelectedRow();
            if (ouiTable.getRowCount() < row || row < 0) {
                return;
            }
            //you must do this incase the table was sorted
            row = ouiTable.convertRowIndexToModel(row);
            editOUIField.setText((String) ouiTable.getModel().getValueAt(row, 0));
            editVendorNameField.setText((String) ouiTable.getModel().getValueAt(row, 1));
            editTypeField.setText("");
            editConfidenceSpinner.setValue(1);
            tabbedPane.setSelectedComponent(tabbedPane.getComponentAt(0));
            table.getSelectionModel().clearSelection();
        });

        JTextField searchTable = new JTextField();
        searchTable.setText("Filter Table");
        searchTable.setToolTipText("Type here to filter the table");
        searchTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                rowSorter.setRowFilter(RowFilter.regexFilter(searchTable.getText()));
            }
        });

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(searchTable)
                                                .addComponent(importBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
//                                                .addComponent(searchTable)
//                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
//                                                .addComponent(deleteBtn))
                                        .addComponent(jScrollPane1, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane1, GroupLayout.DEFAULT_SIZE, 184, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(importBtn, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(searchTable)))
//                                        .addComponent(deleteBtn)).addContainerGap())
        );

        return jPanel1;
    }

    private JComponent createNewVendorPane() {
        JLabel ouiLabel = new JLabel();
        JLabel typeLabel = new JLabel();
        JLabel vendorLabel = new JLabel();
        JLabel confidenceLabel = new JLabel();
        JPanel jPanel2 = new JPanel();
        JButton saveBtn = new JButton();
        JTextField newOUIField = new JTextField();
        JTextField newTypeField = new JTextField();
        JTextField newVendorNameField = new JTextField();
        JSpinner newConfidenceSpinner = new JSpinner();
        jPanel2.setBorder(BorderFactory.createTitledBorder("New Hardware Vendor"));

        ouiLabel.setText("OUI");

        newOUIField.setText("");
        Border backupBorder = newOUIField.getBorder();
        Border badBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(0xA94442));
        newOUIField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (newOUIField.getText().matches("\\p{XDigit}{6}")) {
                    if(ouiVendorMap.containsKey(newOUIField.getText())) {
                        newOUIField.setBorder(badBorder);
                        saveBtn.setEnabled(false);
                        saveBtn.setText("OUID Already Exists in OUI.txt");
                    }
                    else {
                        newOUIField.setBorder(backupBorder);
                        saveBtn.setEnabled(true);
                        saveBtn.setText("Save New Hardware Vendor");
                    }
                } else {
                    newOUIField.setBorder(badBorder);
                    saveBtn.setEnabled(false);
                    saveBtn.setText("Wrong Length");
                }
            }
        });

        vendorLabel.setText("Vendor Name");

        newVendorNameField.setText("");

        typeLabel.setText("Type");
        newTypeField.setText("");

        newConfidenceSpinner.addChangeListener((e) -> {
            Integer val = (Integer) newConfidenceSpinner.getValue();
            if (val < 0) {
                newConfidenceSpinner.setValue(0);
            } else if (val > 5) {
                newConfidenceSpinner.setValue(5);
            }
        });

        confidenceLabel.setText("Confidence");

        saveBtn.setText("Save New Hardware Vendor");
        saveBtn.addActionListener(e -> {
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.addRow(new Object[]{newOUIField.getText(),newVendorNameField.getText(),newTypeField.getText(),newConfidenceSpinner.getValue()});
            try {
                save();
            } catch (IOException ex) {
                Logger.getLogger(HardwareVendorManager.class.getName()).log(Level.SEVERE, null, ex);
                fail();
            }
        });


        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(vendorLabel)
                                        .addComponent(typeLabel)
                                        .addComponent(ouiLabel)
                                        .addComponent(confidenceLabel))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(newConfidenceSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(saveBtn))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(newVendorNameField, GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                                                        .addComponent(newOUIField)
                                                        .addComponent(newTypeField))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(ouiLabel)
                                        .addComponent(newOUIField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(vendorLabel)
                                        .addComponent(newVendorNameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typeLabel)
                                        .addComponent(newTypeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(confidenceLabel)
                                        .addComponent(newConfidenceSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(saveBtn)))
        );
        
        return jPanel2;
    }

    private JComponent createEditPane() {
        JLabel ouiLabel = new JLabel();
        JLabel typeLabel = new JLabel();
        JLabel vendorLabel = new JLabel();
        JLabel confidenceLabel = new JLabel();
        JPanel jPanel2 = new JPanel();
        JButton saveBtn = new JButton();
        jPanel2.setBorder(BorderFactory.createTitledBorder("Properties"));

        ouiLabel.setText("OUI");
        editOUIField.setEditable(false);
        editOUIField.setText("...");
        Border backupBorder = editOUIField.getBorder();
        Border badBorder = BorderFactory.createMatteBorder(2, 2, 2, 2, new Color(0xA94442));
        editOUIField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (editOUIField.getText().matches("\\p{XDigit}{6}")) {
                    editOUIField.setBorder(backupBorder);
                    saveBtn.setEnabled(true);
                } else {
                    editOUIField.setBorder(badBorder);
                    saveBtn.setEnabled(false);
                }
            }
        });

        vendorLabel.setText("Vendor Name");
        editVendorNameField.setText("...");
        editVendorNameField.setEditable(false);

        typeLabel.setText("Type");

        editTypeField.setText("...");

        editConfidenceSpinner.addChangeListener((e) -> {
            Integer val = (Integer) editConfidenceSpinner.getValue();
            if (val < 0) {
                editConfidenceSpinner.setValue(0);
            } else if (val > 5) {
                editConfidenceSpinner.setValue(5);
            }
        });

        confidenceLabel.setText("Confidence");

        saveBtn.setText("Save");
        saveBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (table.getRowCount() < row || row < 0) {
                //no row selected
                if (editOUIField.getText().isEmpty() || editOUIField.getText().equals("...")) {
                    return;
                } else {
                    //this means its a new row imported from the "DB"
                    DefaultTableModel model = (DefaultTableModel) table.getModel();
                    model.addRow(new Object[]{editOUIField.getText(), editVendorNameField.getText(), editTypeField.getText(), editConfidenceSpinner.getValue()+""});
                }
            } else {
                //you must do this incase the table was sorted
                row = table.convertRowIndexToModel(row);

                if (editOUIField.getText().equals(table.getValueAt(row, COL_OUI))) {
                    //this means its an edit of a row in the table
                    //table.setValueAt(editOUIField.getText(), row, COL_OUI);
                    table.setValueAt(editTypeField.getText(), row, COL_TYPE);
                    table.setValueAt(editConfidenceSpinner.getValue().toString(), row, COL_CONF);
                }

                try {
                    save();
                } catch (IOException ex) {
                    Logger.getLogger(HardwareVendorManager.class.getName()).log(Level.SEVERE, null, ex);
                    fail();
                }
            }
        });


        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(vendorLabel)
                                        .addComponent(typeLabel)
                                        .addComponent(ouiLabel)
                                        .addComponent(confidenceLabel))
                                .addGap(18, 18, 18)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addComponent(editConfidenceSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(saveBtn))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                        .addComponent(editVendorNameField, GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE)
                                                        .addComponent(editOUIField)
                                                        .addComponent(editTypeField))
                                                .addGap(0, 0, Short.MAX_VALUE)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(ouiLabel)
                                        .addComponent(editOUIField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(vendorLabel)
                                        .addComponent(editVendorNameField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(typeLabel)
                                        .addComponent(editTypeField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(confidenceLabel)
                                        .addComponent(editConfidenceSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(saveBtn)))
        );
        return jPanel2;
    }
    
}
