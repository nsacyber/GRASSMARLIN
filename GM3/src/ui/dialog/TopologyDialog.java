/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import ui.icon.Icons;

/**
 *
 */
public class TopologyDialog extends JDialog {
    
    public TopologyDialog(Object[][] vector) {
        initComponent(vector);
    }

    private void initComponent(Object[][] vector) {
        JLabel label;
        JTable table;
        
        try {
            for( int i = 0; i < vector.length; i++ ) {
                vector[i][0] = new JLabel( Icons.valueOf(vector[i][0].toString()).getIcon32());
                Color color = new Color( (int)vector[i][1] );
                vector[i][1] = new JPanel() {
                    @Override
                    public Color getBackground() {
                        return color;
                    }
                };
                vector[i][2] = new JLabel(" " + vector[i][2].toString());
            }
            
        } catch( Exception ex ) {
            Logger.getLogger(TopologyDialog.class.getName()).log(Level.SEVERE, "cannot display Topology Key", ex);
        }
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener((e)->{
            TopologyDialog.this.setVisible(false);
        });
        
        

        label = new javax.swing.JLabel("Topology Key: Understanding colors and icons");
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

        setTitle("Topology Key");
        setIconImage(Icons.Grassmarlin.get());
        label.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N

        table.setModel(new javax.swing.table.DefaultTableModel(
            vector,
            new String [] {
                "Icon", "Color", "Host Type"
            }
        ) {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        });
        jScrollPane1.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setResizable(false);
            table.getColumnModel().getColumn(0).setPreferredWidth(32);
            table.getColumnModel().getColumn(1).setResizable(false);
            table.getColumnModel().getColumn(1).setPreferredWidth(64);
        }

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(label, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(closeBtn)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(label)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 327, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(closeBtn)
                .addContainerGap())
        );
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return (Component) value;
            }
        });
        table.setRowHeight(32);
        TableColumn col0 = table.getColumnModel().getColumn(0);
        col0.setPreferredWidth(32);
        col0.setMinWidth(32);
        col0.setMaxWidth(32);



        TableColumn col1 = table.getColumnModel().getColumn(1);
        col1.setPreferredWidth(64);
        col1.setMinWidth(64);
        col1.setMaxWidth(64);

        pack();
    }
    
}
