/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.Core;
import core.importmodule.Import;
import core.importmodule.ImportItem;
import core.types.LogEmitter;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.Timer;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.io.FileUtils;
import ui.custom.FlatProgressBar;
import ui.log.EventLogTable;

/**
 * <pre>
 *
 * </pre>
 */
public class ImportTableModel extends AbstractTableModel implements Observer {

    final String[] cols = {"File Name", "Size", "Import Type", "Progress", "Include"};
    final Class[] types = {String.class, String.class, String.class, Integer.class, Boolean.class};
    final Boolean[] editable = {false, false, true, false, true};

    final int FILENAME = 0;
    final int SIZE = 1;
    final int IMPORT = 2;
    final int PROGRESS = 3;
    final int INCLUDED = 4;

    public final List<ImportItem> list;
    public final Map<ImportItem, FlatProgressBar> bars;
    public final Timer timer;
    public final int delay = 500;
    public final Map<ImportItem, Integer> running;
    public final Map<Long, Long> times;
    public Long currentCycle;
    Runnable timerShutdown;

    private EventLogTable notifications;

    public ImportTableModel(EventLogTable notifications) {
        this.notifications = notifications;
        currentCycle = 0L;
        bars = new WeakHashMap<>();
        running = Collections.synchronizedMap(new WeakHashMap<ImportItem, Integer>());
        list = Collections.synchronizedList(new ArrayList<ImportItem>());
        times = Collections.synchronizedMap(new HashMap<Long, Long>());
        timer = new Timer(500, this::poll) {
            @Override
            public synchronized void start() {
                currentCycle = System.currentTimeMillis();
                times.put(currentCycle, currentCycle);
                super.start();
            }

            @Override
            public void stop() {
                if (!running.isEmpty()) {
                    return;
                }
                times.put(currentCycle, System.currentTimeMillis());
                if (timerShutdown != null) {
                    timerShutdown.run();
                }
                super.stop();
            }
        };
    }

    public void setTimerShutdown(Runnable timerShutdown) {
        this.timerShutdown = timerShutdown;
    }

    public Map<Long, Long> getTimes() {
        return times;
    }

    public synchronized void addItem(ImportItem item) {
        int row = list.size();
        if (!Import.None.equals(item.getType())) {
            item.getProgressObserver().addObserver(this);
            item.setInclude(true);
        }
        list.add(item);
        bars.put(item, new FlatProgressBar());
        fireTableRowsInserted(row, row);
    }

    public synchronized void removeItems(int[] rows) {
        List<ImportItem> items = new ArrayList();
        for (int i = 0; i < rows.length; ++i) {
            ImportItem item = list.get(rows[i]);
            if (!item.isComplete()) {
                items.add(list.get(rows[i]));
            }
        }
        if (rows.length != items.size()) {
            LogEmitter.factory.get().emit(this, Core.ALERT.WARNING, "Cannot remove completed items.");
            notifications.log(Core.ALERT.DANGER,"Cannot remove completed items.  Clear topology to remove.");
        }
        list.removeAll(items);
        fireTableDataChanged();
    }

    public void forEach(Consumer<ImportItem> cb) {
        list.forEach(cb);
    }

    public List<ImportItem> getList() {
        return list;
    }

    public ImportItem get(int row) {
        return list.get(row);
    }

    public FlatProgressBar getProgressBar(int row) {
        ImportItem item = list.get(row);
        FlatProgressBar bar = bars.get(item);
        bar.setValue(item.getProgress());
        return bar;
    }

    public void addAll(List<ImportItem> items) {
        items.forEach(this::addItem);
    }

    /**
     * Checks given item for completion and notifies updates where needed
     *
     * @param arg The instance of ImportItem to be checked.
     */
    public void checkProgress(Object arg) {
        if (!(arg instanceof ImportItem)) {
            return;
        }
        ImportItem item = (ImportItem) arg;

        if (!item.getProgress().equals(running.get(item))) {
            fireTableCellUpdated(list.indexOf(item), PROGRESS);
            running.put(item, item.getProgress());
        }

        if (item.isComplete()) {
            running.remove(item);
            item.getProgressObserver().deleteObserver(this);
        } else {
//            System.out.println("work in progress " + item.getName() +"   " + item.getTaskCount());
        }

        if (running.isEmpty()) {
            timer.stop();
        }
    }

    public Boolean isActive() {
        return !running.isEmpty() && !running.keySet().stream().anyMatch(i -> i.getProgress() == 0);
    }

    private void poll(ActionEvent e) {
        if (running.isEmpty()) {
            timer.stop();
        } else {
            running.keySet().stream()
                    .filter(i -> !i.isComplete())
                    .forEach(this::checkProgress);
        }
    }

    Stream<ImportItem> stream() {
        timer.start();
        return list.stream().filter(i -> i.isIncluded() && !i.isComplete());
    }

    @Override
    public void update(Observable o, Object arg) {
//        System.out.println( "update fires" );
        checkProgress(arg);
    }

    @Override
    public String getColumnName(int column) {
        return cols[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return types[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editable[columnIndex];
    }

    @Override
    public synchronized void setValueAt(Object aValue, int row, int column) {
        synchronized (list) {
            ImportItem item = list.get(row);
            switch (column) {
                case FILENAME: // filenames don't change
                case SIZE: // file sizes don't change
                    break;
                case IMPORT:
                    Import type = (Import) aValue;
                    if (item.getType().equals(type)) {
                        break;
                    }
//                    item.setType(i);
                    try {
                        String path = item.getCanonicalPath();
                        list.remove(row);
                        FlatProgressBar fpb = bars.remove(item);
                        item = type.newItem(path);
                        list.add(row, item);
                        bars.put( item, fpb );
                    } catch (IOException ex) {
                        Logger.getLogger(ImportDialog.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    if (!item.isIncluded()) {
                        fireTableCellUpdated(row, INCLUDED);
                    }
                case PROGRESS: // progres changes externally but we update anyway
                    fireTableCellUpdated(row, column);
                    break;
                case INCLUDED:
//                    if( item.isIncluded().equals(aValue) ) return;
                    if (Import.None.equals(item.getType())) {
                        aValue = Boolean.FALSE;
                    }
                    if (item.getProgress() == 0) {
                        boolean b = (boolean) aValue;
                        item.setInclude(b);
                        if (!b) {
                            item.getProgressObserver().deleteObserver(this);
                            running.remove(item);
                        } else {
                            item.getProgressObserver().addObserver(this);
                            running.put(item, 0);
                        }
                        fireTableCellUpdated(row, column);
                    }
                default:
            }
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    @Override
    public TableModelListener[] getTableModelListeners() {
        return listenerList.getListeners(TableModelListener.class);
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount() {
        return cols.length;
    }

    @Override
    public Object getValueAt(int row, int column) {
        ImportItem item = list.get(row);
        switch (column) {
            case 0:
                return item.getName();
            case 1:;
                return sizeDisplayText(item.length());
            case 2:
                return item.getType();
            case 3:
                return item.getProgress();
            case 4:
                return item.isIncluded();
            default:
                return "";
        }
    }

    String sizeDisplayText(long size) {
        if (size >= FileUtils.ONE_TB) {
            return divString(size, FileUtils.ONE_TB).concat("TB");
        }
        if (size >= FileUtils.ONE_GB) {
            return divString(size, FileUtils.ONE_GB).concat("GB");
        }
        if (size >= FileUtils.ONE_MB) {
            return divString(size, FileUtils.ONE_MB).concat("MB");
        }
        if (size >= FileUtils.ONE_KB) {
            return divString(size, FileUtils.ONE_KB).concat("KB");
        }
        return "Unknown";
    }

    String divString(long size, long div) {
        return Long.toString(size / div);
    }

    List<ImportItem> getFilteredList() {
        return stream().collect(Collectors.toList());
    }

    void resetIfNotComplete() {
        list.forEach(item -> {
            if (!item.isComplete()) {
                item.reset();
            }
        });
    }

    public void reset() {
        Observer ref = this;
        list.forEach(item -> {
            item.reset();
            item.getProgressObserver().deleteObserver(ref);
            item.getProgressObserver().addObserver(ref);
            checkProgress(item);
        });
        running.clear();
    }

}
