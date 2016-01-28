/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import ICSDefines.Category;
import core.topology.Entities;
import core.topology.Mac;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 */
public class EntityEditor extends JFrame {
    
    private Entities entity;
    
    DefaultTableModel model;
    
    public EntityEditor(Entities entity) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setTitle(entity.getName());
        this.entity = entity;
        initComponent();
        setup();
        pack();
    }
    
    private void initComponent() {
        JTable table = new JTable();
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            String last = "";
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if( column == 0 && row > 1 ) {
                    if( model.getValueAt(row-1, column).equals(value) ) {
                        label.setText("");
                    }
                }
                
                return label;
            }
        });
        model = (DefaultTableModel) table.getModel();
        model.addColumn("Name");
        model.addColumn("Value");
        JScrollPane scroll = new JScrollPane(table);
        setLayout(new BorderLayout());
        add(scroll, BorderLayout.CENTER);
    }
    
    private void setup() {
        Entities ent = entity;
        this.setIconImage(ent.getImage16());
        
        Set<String> collections = new HashSet<>();
        
        ent.getAttributes().forEach((k,v) -> {
            
            if( v instanceof Collection ) {
                collections.add((String)k);
                
            } else if( v instanceof File ) {
                addRow(k, ((File)v).getName());
                
            } else if( v instanceof Category ) {
                Category type = (Category) v;
                addRow(k, type.getText());
                
            } else if( v instanceof ICSDefines.Role ) {
                ICSDefines.Role role = (ICSDefines.Role) v;
                addRow(k, role.toString());
                
            } else if( v instanceof Mac ) {
                addRow( "Physical Address", v );
                
            } else {
                addRow( k, v );
            }
            
        });
        
        collections.forEach( key -> {
            ent.getCollection(key).forEach( obj -> {
                addRow( key, obj );
            });
        });
        
    }    
            
    private int addRow(Object left, Object right) {
        if( left == null || right == null ) {
            return -1;
        }
        int row = model.getRowCount();
        model.addRow(row(left,right));
        return row;
    }
    
    private Object[] row( Object left, Object right ) {
        return new Object[] { left.toString(), right.toString() };
    }
    
}
