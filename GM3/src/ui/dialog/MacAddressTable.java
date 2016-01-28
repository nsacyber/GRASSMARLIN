/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.topology.PhysicalNode;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import ui.icon.Icons;

/**
 *
 */
public class MacAddressTable extends JDialog {

    public MacAddressTable( PhysicalNode node ) {
        DefaultTableModel model = initComponents(node);
        populate(node, model);
    }

    private void populate(PhysicalNode node, DefaultTableModel model) {
        node.vlans.forEach( vlan -> {
            
            vlan.ports.forEach( port -> {
                
                port.macs().forEach( mac -> {
                    
                    model.addRow(new Object[] { port.getName(), port.mac.toString(), Integer.toString(vlan.id), mac.toString() });
                });
            });
        });
    }
    
    private DefaultTableModel initComponents( PhysicalNode node ) {
        JLabel textLabel = new javax.swing.JLabel();
        JLabel deviceName = new javax.swing.JLabel();
        JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        JTable jTable1 = new javax.swing.JTable();

        this.setTitle("Mac Address Table Information for " + node.getName());
        this.setIconImage(Icons.Grassmarlin.get32());
        textLabel.setText("Network Device");

        deviceName.setText(node.getName());

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Interface Name", "MAC", "VLAN", "Host"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(textLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deviceName)
                        .addGap(0, 229, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(textLabel)
                    .addComponent(deviceName))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 275, Short.MAX_VALUE)
                .addContainerGap())
        );
        pack();
        return (DefaultTableModel) jTable1.getModel();
    }
    
}
