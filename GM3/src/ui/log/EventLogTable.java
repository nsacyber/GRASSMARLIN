/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.log;

import core.Core.ALERT;
import core.types.LogEmitter;
import core.types.LogEmitter.Log;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <pre>
 *
 * </pre>
 */
public class EventLogTable extends JTable {

    static final String[] columns = { /*"Level", */"Message", "Time"};
    static final Class[] classes = { /*String.class, */String.class, String.class};

    final EventLogModel model;

    Log current;

    public EventLogTable() {
        super();
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        setAutoscrolls(true);
        setDragEnabled(false);
        setGridColor(Color.WHITE);
        setBackground(Color.WHITE);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem clear = new JMenuItem(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.clear();
            }
        });
        clear.setText("Clear");
        JMenuItem expand = new JMenuItem(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                expandCurrent();
            }
        });
        expand.setText("Expand");
        JMenuItem copy = new JMenuItem(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = EventLogTable.this.getSelectedRows();
                int len = rows.length;
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < len; ++i) {
                    b.append(EventLogTable.this.getRowText(rows[i])).append("\n");
                }

                StringSelection stringSelection = new StringSelection(b.toString());
                Clipboard clipBoard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipBoard.setContents(stringSelection, null);
            }
        });
        JMenuItem collapse = new JMenuItem(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.collapse();
            }
        });
        collapse.setText("Collapse");
        copy.setText("Copy");
        menu.add(copy);
        menu.add(expand);
        menu.add(collapse);
        menu.add(clear);

        model = new EventLogModel();
        setModel(model);
        DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {
            Border leftBorder = BorderFactory.createEmptyBorder(0, 5, 0, 0);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Log l = model.logs.get(row);
                c.setBackground(isSelected ? l.TYPE.text : l.TYPE.background);
                c.setForeground(isSelected ? l.TYPE.background.brighter() : l.TYPE.text);
                int i = table.getColumnModel().getTotalColumnWidth();
                if (column == 0) {
                    table.getColumnModel().getColumn(column).setPreferredWidth(i - 64);
                    if (c instanceof JLabel) {
                        JLabel text = (JLabel) c;
                        text.setHorizontalTextPosition(JLabel.LEFT);
                        text.setHorizontalAlignment(JLabel.LEFT);
                        text.setBorder(leftBorder);
                    }
                } else if (column == 1) {
                    table.getColumnModel().getColumn(column).setPreferredWidth(64);
                    if (c instanceof JLabel) {
                        ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                    }
                }
                return c;
            }
        };
        JTable ref = this;
        setDefaultRenderer(String.class, cr);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    menu.setLocation(e.getLocationOnScreen());
                    menu.setInvoker(ref);
                    menu.setVisible(true);
                    current = model.getRow(ref.getSelectedRow());
                }
            }
        });

        this.setTableHeader(null);
    }

    public String getRowText(int rowNumber) {
        String s1 = getValueAt(rowNumber, 0).toString();
        String s2 = getValueAt(rowNumber, 1).toString();
        if (s1.contains("\"")) {
            s1 = String.format("\"%s\"", s1);
        }
        /* s2 never contains quotes */
        return String.format("%s, %s", s1, s2);
    }

    public void expandCurrent() {
        if (current == null) return;
        model.expand(current);
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return classes[column];
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void log(Log log) {
        model.add(log);
    }

    public void log(ALERT l, String s) {
        model.add(new LogEmitter.Log(this, l, s));
    }

    /**
     * EventLogModel is a list based table model for the EventLogTable
     */
    public class EventLogModel extends AbstractTableModel {
        final List<Log> logs;

        public EventLogModel() {
            logs = Collections.synchronizedList(new ArrayList<>());
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        public void add(Log entry) {
            if (logs.isEmpty() || !logs.get(logs.size() - 1).shouldCondense(entry)) {
                logs.add(entry);
            } else {
                logs.get(logs.size() - 1).condenseLog(entry);
            }
            this.fireTableRowsInserted(logs.size() - 1, logs.size() - 1);
        }

        public void clear() {
            logs.clear();
            this.fireTableDataChanged();
        }

        /**
         * @param row Row to retrieve
         * @return The row at that index, or null if out of bounds
         */
        public Log getRow(int row) {
            try {
                return logs.get(row);
            } catch (java.lang.ArrayIndexOutOfBoundsException ex) {
                return null;
            }
        }

        @Override
        public int getRowCount() {
            return logs.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Log e = logs.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return e.toString();
                case 1:
                    return DateFormatUtils.ISO_TIME_NO_T_FORMAT.format(e.TIME);
                default:
                    return null;
            }
        }

        private void expand(Log current) {
            Integer i = logs.indexOf(current);
            if (i == -1) return;
            Log log = logs.get(i);
            logs.addAll(i + 1, log.getCondensedMessages());
            fireTableDataChanged();
            log.getCondensedMessages().clear();
        }

        public void collapse() {
            boolean changed = true;
            int i = 1;
            List<Log> remove = new ArrayList<>();
            while(changed) {
                changed = false;
                --i;
                for (; i < logs.size() - 1 && i > -1; i++) {
                    Log log = logs.get(i);
                    Log next = logs.get(i+1);
                    if( log.TYPE.equals(next.TYPE) ) {
                        log.condenseLog(next);
                        remove.add(next);
                        int x = i+2;
                        for(; x < logs.size(); x++) {
                            Log next2 = logs.get(x);
                            if( log.TYPE.equals(next2.TYPE) ) {
                                log.condenseLog(next2);
                                remove.add(next2);
                            }
                        }
                        i = x;
                        changed = true;
                    }
                }
            }
            if( logs.removeAll(remove) ) {
                this.fireTableDataChanged();
            }
        }
    }

}
