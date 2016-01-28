/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.dialog;

import ICSDefines.Category;
import ui.views.tree.TreeView;
import core.types.LogEmitter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import ui.GrassMarlin;
import ui.icon.Icons;


/**
 *
 */
public final class SummaryDialog extends JFrame {
	
    LogEmitter emitter;
    JPanel target;
    JTable table;
    SummaryDialog ref;
    
    final Dimension fdim = new Dimension(2,2);
    
    public SummaryDialog( ) {
		setTitle("Summary");
        setIconImage(Icons.Editform.get());
        setMinimumSize(new Dimension(340,540));
        emitter = LogEmitter.factory.get();
        target = new JPanel();
        table = new JTable();
        table.remove(table.getTableHeader());
        table.setBackground(Color.WHITE);
        table.setShowGrid(false);
        table.getTableHeader().setVisible(false);
        table.setTableHeader(null);
        JScrollPane scroll;
        scroll = new JScrollPane(table);
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);
        clear();
        initComponent(scroll);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            Font BOLD_FONT = new Font("Tahoma", 1, 13);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); //To change body of generated methods, choose Tools | Templates.
                if( column == 1 ) {
                    c.setHorizontalTextPosition(JLabel.RIGHT);
                }
                if( row < 2 ) {
                    c.setFont(BOLD_FONT);
                    return c;
                }
                if( column == 0 && table.getModel().getValueAt(row, 1).equals("") ) {
                    c.setFont(BOLD_FONT);
                }
                c.invalidate();
                return c;
            }
        });
    }

    public void setLogEmitter(LogEmitter emitter) {
        this.emitter = emitter;
    }
    
    public LogEmitter getLogEmitter() {
        return emitter;
    }
    
    private void initComponent(JScrollPane scroll) {
        JPanel outer = new JPanel();
        Border b = BorderFactory.createEmptyBorder();
        BoxLayout layout;
        scroll.setBorder(b);
        scroll.setViewportBorder(b);
        target.setBorder(b);
        outer.setBorder(b);
        outer.setLayout(new BorderLayout());
        
        Dimension dim = this.getPreferredSize();
        dim.height = 0;
        scroll.setPreferredSize(dim);
        layout = new BoxLayout(target, BoxLayout.PAGE_AXIS);
        target.setLayout(layout);
        
        outer.add( scroll, BorderLayout.CENTER );
        
        add( outer, BorderLayout.CENTER );
    }
    
    public void clear() {
        table.setModel(new DefaultTableModel());
        if( target != null )
            target.removeAll();
    }

    Object[] row( Object arg1, Object arg2 ) {
        return new Object[]{ arg1, arg2 };
    }
    
    public void refresh() {
        refresh(null);
    }
    
    private void refresh(Object nil) {
        TreeView tree = GrassMarlin.window.tree;
        if( tree != null ) {
            update(tree);
        }
    }
    
    public void update(TreeView tree) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        model.setColumnCount(2);
        model.addRow( row("Total Networks", Integer.toString(tree.networkCount())) );
        model.addRow( row("Total Hosts", Integer.toString(tree.hostCount())) );

        Map<Category,Integer> m = new HashMap<>();

        tree.networks().forEach(net->{
            model.addRow( row(net.getName(),"") );
            net.getChildren().stream().map(n->n.getDetails().getCategory()).forEach(c->{
                if( m.containsKey(c) ) {
                    int i = m.get(c);
                    i++;
                    m.put(c, i);
                } else m.put(c, 1);
            });
            m.entrySet().forEach(e->{
                model.addRow( row( e.getKey().name(), Integer.toString(e.getValue())) );
            });
            m.clear();
        });
        SwingUtilities.invokeLater(()->{
            table.setModel(model);
            table.getColumnModel().getColumn(1).setMaxWidth(50);
        });
    }
    public void allowSelection(boolean b) {
        table.setRowSelectionAllowed(false);
    }

}
