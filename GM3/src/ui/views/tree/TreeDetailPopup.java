/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import core.ViewUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import static java.util.Collections.sort;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import ui.views.tree.visualnode.VisualNode;

/**
 * <pre>
 *
 * </pre>
 */
public class TreeDetailPopup extends JPopupMenu {

    public static final Color bg = new Color(204, 204, 204);
    static final Logger log = Logger.getLogger(TreeDetailPopup.class.getName());
    public static final Font BOLD_FONT = new Font("Tahoma", 1, 14);
    public static final Font WARNING_FONT = new Font("Tahoma", 1, 14);

    JLabel title;
    JPanel iconPanel;
    JTable table;
    JScrollPane scroll;
    
    public TreeDetailPopup() {
        title = new JLabel();
        table = new JTable();
        iconPanel = new JPanel();
        scroll = new JScrollPane();
        this.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseExited(MouseEvent e) {
                setVisible(false);
            }
        });
        initComponent();
    }

    public void showDetails(Component invoker, Point p, VisualNode n) {
        //setVisible(false);
        if (p != null) {
            Point point = new Point((int)p.getX()+15, (int)p.getY());
            setLocation(point);
        }
        
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        List<String[]> rows = new ArrayList<>();
        while( model.getRowCount() > 0 ) {
            model.removeRow(0);
        }
        
        n.getDetails().forEach(( key, value ) -> {
            rows.add( new String[] { key, value });
        });
        
        int count = rows.size();
        n.getDetails().getNames().forEach( name -> {
            String key = rows.size() == count ? "Fingerprint" : "";
            rows.add(new String[] { key, name } );
        });
        
        sort(rows);
        
        rows.forEach(model::addRow);
        
        Icon icon = n.getDetails().image.getIcon();
        JLabel label = new JLabel(icon);
        label.setMinimumSize(new Dimension(32,32));
        
        iconPanel.removeAll();
        iconPanel.add(label, BorderLayout.CENTER);
        title.setText( n.getAddress() );
        
        int height = table.getRowHeight() * model.getRowCount() + 2;
        scroll.setPreferredSize(new Dimension(scroll.getWidth(), height));
        pack();
        setVisible(true);
    }

    private void sort(List<String[]> list) {
        list.sort(this::compareRows);
    }
    
    private int compareRows(String[] a, String[] b) {
        return compareRows(a[0], a[1], b[0], b[1]);
    }
    
    private int compareRows(String key1, String value1, String key2, String value2) {
        if( "Fingerprint".equals(key1) || key1.isEmpty() ) {
            return 5;
        } else if( "Name".equalsIgnoreCase(key1) ) {
            return -5;
        } else if( "Primary Address".equalsIgnoreCase(key1) ) {
            return -4;
        } else if( "warning".equalsIgnoreCase(key1) ) {
            return -3;
        }
        return key1.compareTo(key2);
    }
    
    private void initComponent() {
        JPanel jPanel1 = new JPanel();
        title.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(iconPanel, 34, 34, 34)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(title)
                .addContainerGap(86, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(title)
                    .addComponent(iconPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5))
        );

        add(jPanel1);
        iconPanel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.gray));
        iconPanel.setBackground(Color.gray);
        Dimension d32 = new Dimension(34,34);
        iconPanel.setMinimumSize(d32);
        iconPanel.setMaximumSize(d32);
        iconPanel.setPreferredSize(d32);
        iconPanel.setPreferredSize(null);
        iconPanel.setLayout(new BorderLayout());
        table.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        table.setRowSelectionAllowed(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        scroll.setViewportView(table);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setResizable(false);
            table.getColumnModel().getColumn(1).setResizable(false);
        }
        table.setTableHeader(null);
        add(scroll, java.awt.BorderLayout.CENTER);
        pack();
    }
}
