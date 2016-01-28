/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.Core;
import core.ViewUtils;
import core.types.LogEmitter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.commons.lang3.time.DateFormatUtils;
import ui.GrassMarlin;
import ui.icon.Icons;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 */
public class ReportDialog extends JDialog {

    JRadioButton networkBtn;
    JRadioButton destBtn;
    JRadioButton allBtn;
    JRadioButton sourceBtn;
    JTable table;

    final static String network = "Networks";
    final static String dest = "Destination IP";
    final static String source = "Source IP";
    final static String all = "All IP";

    final static Comparator<String> SORT_BY_IP;

    static {
        SORT_BY_IP = ReportDialog::addressComparator;
    }

    private static Integer addressComparator(String s1, String s2) {
        int[] ints1 = ViewUtils.ipToInts(s1);
        int[] ints2 = ViewUtils.ipToInts(s2);
        int ret = 0;
        int len = Math.min(ints1.length, ints2.length);
        len = Math.min(len, 4);
        for (int i = 0; i < len && ret == 0; ++i) {
            ret = Integer.compare(ints1[i], ints2[i]);
        }
        return ret == 0 ? s1.compareTo(s2) : ret;
    }

    public ReportDialog() {
        networkBtn = new javax.swing.JRadioButton();
        destBtn = new javax.swing.JRadioButton();
        allBtn = new javax.swing.JRadioButton();
        sourceBtn = new javax.swing.JRadioButton();
        networkBtn.setSelected(true);
        initComponents();
    }

    public void export(Object nill) {
        JFileChooser jfc = new JFileChooser();
        jfc.setSelectedFile(new File(String.format("%s_%s.csv", getSelectedText(), getTimestamp())));
        int res = jfc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            LogEmitter log = LogEmitter.factory.get();
            File f = jfc.getSelectedFile();
            if (f.exists()) {
                log.emit(this, Core.ALERT.WARNING, "File already exists, cannot save \"" + f.getName() + "\".");
                return;
            }

            try {
                if (!f.createNewFile()) {
                    log.emit(this, Core.ALERT.DANGER, "Failed to save file \"" + f.getName() + "\".");
                }
                FileWriter fw = new FileWriter(f);
                int rows = table.getRowCount();
                int cols = table.getColumnCount();
                TableModel m = table.getModel();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        if (j != 0) {
                            sb.append(",");
                        }
                        sb.append(m.getValueAt(i, j).toString());
                    }
                    sb.append("\n");
                    fw.write(sb.toString());
                    sb.delete(0, sb.length());
                }
                fw.close();
                log.emit(sb, Core.ALERT.INFO, "\"" + f.getName() + "\" saved.");

            } catch (IOException ex) {
                Logger.getLogger(ReportDialog.class
                        .getName()).log(Level.SEVERE, null, ex);
                log.emit(this, Core.ALERT.DANGER,
                        "Failed to save file \"" + f.getName() + "\".");
            }
        }
    }

    private String getTimestamp() {
        return DateFormatUtils.format(System.currentTimeMillis(), "dd MM yyyy HH_mm_ss");
    }

    public void columnsChanged(ActionEvent nill) {
        if (GrassMarlin.window == null) {
            return;
        }

        VisualNode root = GrassMarlin.window.tree.getRoot();

        String text = getSelectedText();
        if (text.isEmpty()) {
            return;
        }

        DefaultTableModel model = new DefaultTableModel();
        switch (text) {
            case "Networks":
                model.setColumnIdentifiers(new String[]{"Network Name", "Network ID", "Network Subnet Mask", "Comment"});
                root.getChildren().forEach(networkNode -> {
                    model.addRow(new Object[]{
                        networkNode.getName(),
                        networkNode.getAddress(),
                        networkNode.getSubnet(),
                        networkNode.getDetails().getOrDefault("comment", "")
                    });
                });
                table.setModel(model);
                break;
            case "Destination IP":
                model.addColumn("Destination IP");
                root.getChildren()
                        .stream()
                        .map(VisualNode::getChildren)
                        .flatMap(List::stream)
                        .filter(n -> !n.getData().isSource())
                        .map(n -> new Object[]{n.getAddress()})
                        .forEach(model::addRow);
                table.setModel(model);
                break;
            case "All IP":
                model.addColumn("All IP");
                root.getChildren()
                        .stream()
                        .map(VisualNode::getChildren)
                        .flatMap(List::stream)
                        .map(n -> new Object[]{n.getAddress()})
                        .forEach(model::addRow);
                table.setModel(model);
                break;
            case "Source IP":
                model.addColumn("Source IP");
                root.getChildren()
                        .stream()
                        .map(VisualNode::getChildren)
                        .flatMap(List::stream)
                        .filter(n -> n.getData().isSource())
                        .map(n -> new Object[]{n.getAddress()})
                        .forEach(model::addRow);
                table.setModel(model);
            default:
        }

        TableRowSorter<DefaultTableModel> rowSorter = (TableRowSorter<DefaultTableModel>) table.getRowSorter();
        boolean single = table.getColumnCount() == 1;
        if (single) {
            rowSorter.setComparator(0, ReportDialog.SORT_BY_IP);
        } else {
            rowSorter.setComparator(0, ReportDialog.SORT_BY_IP);
            rowSorter.setComparator(1, ReportDialog.SORT_BY_IP);
            rowSorter.setComparator(2, ReportDialog.SORT_BY_IP);
            
        }
        SwingUtilities.invokeLater(()->{
            rowSorter.toggleSortOrder(single ? 0 : 1);
        });
//        table.setRowSorter(rowSorter);
//        table.sorterChanged(new RowSorterEvent(rowSorter));
    }

    public String getSelectedText() {

        int num = getContentPane().getComponentCount();
        while (--num != -1) {
            Object o = getContentPane().getComponents()[num];
            if (o instanceof JRadioButton) {
                JRadioButton radioBtn = (JRadioButton) o;
                if (radioBtn.isSelected()) {
                    return radioBtn.getText();
                }
            }
        }
        return "";
    }

    @Override
    public void setVisible(boolean b) {
        if (b) {
            columnsChanged(null);
        }
        super.setVisible(b);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {
        setTitle("Connections");
        setIconImage(Icons.Grassmarlin.get());
        javax.swing.JButton exportBtn = new javax.swing.JButton();

        javax.swing.JLabel dsplyLabel = new javax.swing.JLabel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        table = new javax.swing.JTable() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table.setAutoCreateRowSorter(true);

        exportBtn.setText("Export to CSV");
        exportBtn.addActionListener(this::export);
        networkBtn.setText(network);
        destBtn.setText(dest);
        allBtn.setText(all);
        sourceBtn.setText(source);
        networkBtn.addActionListener(this::columnsChanged);
        destBtn.addActionListener(this::columnsChanged);
        allBtn.addActionListener(this::columnsChanged);
        sourceBtn.addActionListener(this::columnsChanged);

        ButtonGroup grp = new ButtonGroup();
        grp.add(networkBtn);
        grp.add(destBtn);
        grp.add(allBtn);
        grp.add(sourceBtn);

        dsplyLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        dsplyLabel.setText("Display");

        jScrollPane1.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(dsplyLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(networkBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(destBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(sourceBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(allBtn)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(exportBtn)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(dsplyLabel)
                                .addComponent(networkBtn)
                                .addComponent(allBtn)
                                .addComponent(sourceBtn)
                                .addComponent(destBtn)
                                .addComponent(exportBtn))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
                        .addContainerGap())
        );
        pack();
    }// </editor-fold>  
}
