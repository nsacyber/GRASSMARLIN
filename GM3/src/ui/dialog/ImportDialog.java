/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.dialog;


import core.Core.ALERT;
import core.Environment;
import core.importmodule.Import;
import core.importmodule.ImportItem;
import core.types.LogEmitter;
import org.apache.commons.lang3.time.DateFormatUtils;
import ui.custom.FlatProgressBar;
import ui.icon.Icons;
import ui.log.EventLogTable;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <pre>
 * ImportDialog obsers ImportItem errors.
 * </pre>
 * <p>
 * 2015.09.25 - CC - added imported items importing
 */
public final class ImportDialog extends JFrame implements Observer {

    static final Logger LOGGER = Logger.getLogger(ImportDialog.class.getName());
    static final Class[] types = {String.class, Integer.class, String.class, Integer.class, Boolean.class};
    static final String[] cols = new String[]{"File Name", "Size(kB)", "Import Type", "Progress", "Include"};
    static final Boolean[] editable = {false, false, true, false, true};
    final ImportTableModel model;
    final LogEmitter log;
    // these buttons will be linked to external actions, to plug their actions
    //JButton configureBtn;
    JButton cancelBtn;
    JButton startBtn;
    // internal use only buttons
    JButton quickSaveBtn, quickLoadBtn, addFile, addFolder, selectAll, removeSelection, applyType;
    // notification list
    EventLogTable notifications = new EventLogTable();
    // drop down
    JComboBox typeSelect;
    // table
    JTable importTable;
    // file chooser popup
    JFileChooser fileChooser;
    Supplier<Boolean> activityIndicator;

    public ImportDialog() {
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setIconImage(Icons.Download.get32());
        model = new ImportTableModel(notifications);
        model.setTimerShutdown(this::updateState);
        model.addTableModelListener((e) -> {
            if (e.getColumn() == model.INCLUDED) {
                updateState();
            }
        });
        log = new LogEmitter();
        log.addObserver((invoker, arg) -> {
            notifications.log((LogEmitter.Log) arg);
        });

        // window properties
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setType(Window.Type.NORMAL);
        setTitle("Import");
        setMinimumSize(new Dimension(320, 240));

        // layout
        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());
        JSplitPane center = new JSplitPane();
        ((BasicSplitPaneUI) center.getUI()).getDivider().setBorder(null);
        center.setResizeWeight(1.0);
        JPanel south = new JPanel();
        center.setBorder(null);
        south.setBorder(null);

        container.add(center, BorderLayout.CENTER);
        container.add(south, BorderLayout.SOUTH);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(container, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(container, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        initSouth(south);
        initCenter(center);
        initFileChooser();
        initTableModel();
        initActions();
//        initStates();
        updateState();
        pack();
    }

    public void setActivityIndicator(Supplier<Boolean> activityIndicator) {
        this.activityIndicator = activityIndicator;
    }

    public LogEmitter getLogEmitter() {
        return log;
    }

    void initActions() {
        ImportDialog self = this;
        addFile.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                get(self::mergeList);
                updateState();
            }
        });
        addFolder.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                get(self::mergeList);
                updateState();
            }
        });
        selectAll.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importTable.selectAll();
                updateState();
            }
        });
        removeSelection.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (model) {
                    model.removeItems(importTable.getSelectedRows());
                }
                updateState();
            }
        });

        quickLoadBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setCurrentDirectory(Environment.DIR_QUICKLIST.getDir());
                fileChooser.showOpenDialog(self);
                File f = fileChooser.getSelectedFile();
                if (f == null) return;
                List<ImportItem> items = new ArrayList<>();
                try {
                    Properties p = new Properties();
                    p.load(new FileInputStream(f));
                    p.forEach((k, v) -> {
                        ImportItem i = getItem(k.toString(), v.toString());
                        if (i != null) {
                            items.add(i);
                        } else {
                            log.emit(v, ALERT.WARNING, String.format("Failed to load import %s %s", k, v));
                        }
                    });
                    if (!items.isEmpty()) {
                        mergeList(items);
                        notifications.log(ALERT.MESSAGE, "Quicklist " + f.getName() + " loaded");
                    } else {
                        log.emit(items, ALERT.DANGER, "Invalid quicklist " + f.getName() + " did not load.");
                    }
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Not a valid quicksave", ex);
                }
                updateState();
            }
        });

        quickSaveBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String TS = "import" + DateFormatUtils.format(System.currentTimeMillis(), " dd MM yyyy HH_mm_ss").concat(".g3");
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setSelectedFile(new File(TS));
                fileChooser.setCurrentDirectory(Environment.DIR_QUICKLIST.getDir());
                fileChooser.showSaveDialog(self);
                File f = fileChooser.getSelectedFile();
                Properties p = new Properties();
                model.forEach(i -> {
                    try {
                        p.put(i.getCanonicalPath(), i.getType().name());
                    } catch (IOException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                });
                try {
                    p.store(new FileOutputStream(f), f.getName() + "\n" + TS);
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
                updateState();
            }
        });

        applyType.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] rows = importTable.getSelectedRows();
                int l = rows.length;
                while (--l >= 0) {
                    importTable.setValueAt(typeSelect.getSelectedItem(), rows[l], 2);
                }
                updateState();
            }
        });
    }

    // the south most component containing three buttons
    void initSouth(JPanel south) {
        //configureBtn = new JButton();
        //configureBtn.setText("Configure Options");
        cancelBtn = new JButton();
        cancelBtn.setText("Cancel");
        startBtn = new JButton();
        startBtn.setText("Start Import");

        GroupLayout layout = new GroupLayout(south);
        south.setLayout(layout);

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addGroup(layout.createSequentialGroup()
                                                        //.addComponent(configureBtn)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 420, Short.MAX_VALUE)
                                                        .addComponent(cancelBtn)
                                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                        .addComponent(startBtn))
                                ))
        );

        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                //.addComponent(configureBtn)
                                                .addComponent(cancelBtn, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(startBtn))
                                        .addContainerGap()
                        )
        );
    }

    // the center most component contains the main body of this dialog
    void initCenter(JSplitPane center) {
        // set container properties
        center.setDividerLocation(450);
        // create left side
        JPanel left = new JPanel();
        left.setLayout(new BorderLayout());
        center.setLeftComponent(left);
        initCenterWestNorth(left);
        initCenterLeftCenter(left);
        initCenterWestSouth(left);

        // create right side
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        center.setRightComponent(right);
        initCenterEastNorth(right);
        initCenterEastCenter(right);
        right.setPreferredSize(new Dimension(180, this.getHeight()));
    }

    // center-west-north is the quicklist buttons
    void initCenterWestNorth(JPanel left) {
        // add to parent
        JPanel top = new JPanel();
        left.add(top, BorderLayout.NORTH);

        // setup button in this area
        quickSaveBtn = new JButton("Save Quicklist");
        quickLoadBtn = new JButton("Load Quicklist");

        // setup top
        GroupLayout layout = new GroupLayout(top);
        top.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(1, Short.MAX_VALUE)
                                .addComponent(quickSaveBtn)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(quickLoadBtn))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap(1, 1)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(quickLoadBtn)
                                        .addComponent(quickSaveBtn))
                                .addContainerGap(1, 1))
        );

    }

    // center-left-center is the import-list
    void initCenterLeftCenter(JPanel left) {
        JScrollPane scroll = new JScrollPane();
        left.add(scroll, BorderLayout.CENTER);
        //setup table
        importTable = new JTable();
        importTable.getTableHeader().setReorderingAllowed(false);
        importTable.addPropertyChangeListener(null);
        scroll.setBackground(Color.WHITE);
        scroll.setViewportView(importTable);
//        scroll.setBorder(BorderFactory.createEmptyBorder());
//        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
    }

    // center-west-south is the addfile-addfolder-selectAll-remove buttons
    void initCenterWestSouth(JPanel left) {
        JPanel bottom = new JPanel();
        left.add(bottom, BorderLayout.SOUTH);
        addFile = new JButton("Add Files");
        addFolder = new JButton("Add Folders");
        selectAll = new JButton("Select All");
        removeSelection = new JButton("Remove Selected");

        GroupLayout layout = new GroupLayout(bottom);
        bottom.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(addFile)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(addFolder)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectAll)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 125, Short.MAX_VALUE)
                                .addComponent(removeSelection))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(addFile)
                                        .addComponent(addFolder)
                                        .addComponent(selectAll)
                                        .addComponent(removeSelection))
                                .addContainerGap())
        );
    }

    // center-right-north is the set-import-type box
    void initCenterEastNorth(JPanel right) {
        applyType = new JButton();
        typeSelect = new JComboBox();
        JPanel north = createSelectorPanel(applyType, typeSelect);
        JPanel n2 = new JPanel();
        GroupLayout layout = new GroupLayout(n2);
        n2.setLayout(layout);
        n2.setBorder(null);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(north, 200, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(north, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        right.add(n2, BorderLayout.NORTH);
    }

    void initTableModel() {
        importTable.setDragEnabled(false);
        importTable.setShowVerticalLines(false);
        importTable.setBorder(null);
        importTable.setBackground(Color.WHITE);
        importTable.setGridColor(new Color(204, 204, 204));
        importTable.setModel(model);
        importTable.getColumnModel().getColumn(3).setCellRenderer((JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) -> {
            synchronized (model) {
                FlatProgressBar col = model.getProgressBar(row);
                col.setBounds(table.getCellRect(row, column, false));
                col.setHighlight(isSelected);
                col.repaint();
                return col;
            }
        });

        try {

            ((DefaultTableCellRenderer) importTable.getDefaultRenderer(String.class))
                    .setHorizontalAlignment(JLabel.CENTER);

            ((DefaultTableCellRenderer) importTable.getTableHeader().getDefaultRenderer())
                    .setHorizontalAlignment(JLabel.CENTER);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }


        JComboBox<Import> editor = new JComboBox<>();
        Import.list.forEach(editor::addItem);
        importTable.setDefaultEditor(String.class, new DefaultCellEditor(editor));
        importTable.setDragEnabled(false);
        importTable.setBorder(null);
        importTable.getSelectionModel().addListSelectionListener(this::updateState);
    }

    private void updateState(Object nill) {
        this.updateState();
    }

    JPanel createSelectorPanel(JButton applyType, JComboBox typeSelect) {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createLineBorder(new java.awt.Color(130, 135, 144)));
        JLabel label = new JLabel("Import Type");
        applyType.setText("Apply");
        typeSelect.setModel(new DefaultComboBoxModel(Import.list.toArray()));
        typeSelect.setBorder(null);
        JSeparator sep = new JSeparator();

        GroupLayout layout = new GroupLayout(p);
        p.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(sep)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(label)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(typeSelect, 68, 68, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(applyType)))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(label)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(typeSelect, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(applyType))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        return p;
    }

    // center-right-center is the notification list
    void initCenterEastCenter(JPanel right) {
        JPanel center = new JPanel();
        right.add(center, BorderLayout.CENTER);
        center.setBorder(null/*BorderFactory.createEtchedBorder(null, new Color(204,204,204))*/);
        JLabel label = new JLabel("Import Notifications");
        label.setFont(new Font("Tahoma", 1, 11));
        JScrollPane scroll = new JScrollPane();
        scroll.setBackground(Color.WHITE);
        scroll.setViewportView((notifications/* = new EventLogTable()*/));

        GroupLayout layout = new GroupLayout(center);
        center.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
//                .addContainerGap()
                                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                .addComponent(scroll)
                                                .addGroup(layout.createSequentialGroup()
                                                        .addComponent(label)
                                                        .addGap(0, 240, Short.MAX_VALUE)))
            /*.addContainerGap()*/)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(label)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scroll, GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                                .addContainerGap())
        );
    }

    void initFileChooser() {
        fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
    }

    void updateState() {
        if (!isVisible()) {
            return;
        }
        boolean hasItems = !model.getList().isEmpty();
        boolean itemsSelected = importTable.getSelectedRowCount() > 0;
        if (model.isActive()) {
            applyType.setEnabled(false);
            quickLoadBtn.setEnabled(false);
            removeSelection.setEnabled(false);
        } else {
            applyType.setEnabled(itemsSelected);
            quickLoadBtn.setEnabled(true);
            removeSelection.setEnabled(itemsSelected);
        }
        boolean active = activityIndicator.get();
        startBtn.setEnabled(!active);
        cancelBtn.setEnabled(active);
        quickSaveBtn.setEnabled(hasItems);
    }

    public List<ImportItem> getImportItems() {
        return model.getFilteredList();
    }

    // lazy initializers for external button actions
    public void setConfigureBtnAction(Action act) {
        //configureBtn.addActionListener( act);
    }

    public void setCancelBtnAction(Action act) {
        cancelBtn.addActionListener(act);
    }

    public void setStartBtnAction(Action act) {
        startBtn.addActionListener(act);
    }

    /********************************************/
    /* HELPER METHODS AND BUISINESS LOGIC BELOW */

    /**
     * This is the "add file" business logic.
     * Will open the "Open" popup and fill the list with new entries.
     *
     * @return List of new IngestItems - may return empty List.
     */
    void get(Consumer<List<ImportItem>> cb) {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            List<File> files = new ArrayList<>();
            switch (fileChooser.getFileSelectionMode()) {
                case JFileChooser.FILES_ONLY:
                    files.addAll(Arrays.asList(fileChooser.getSelectedFiles()));
                    break;
                case JFileChooser.DIRECTORIES_ONLY:
                    Arrays.asList(fileChooser.getSelectedFiles()).stream()
                            .map(File::listFiles)
                            .map(Arrays::asList)
                            .forEach(files::addAll);
                    break;
            }
            processFileList(files, cb);
        }
    }

    void processFileList(List<File> files, Consumer<List<ImportItem>> cb) {
        cb.accept(files.stream().map(this::guessType).collect(Collectors.toList()));
        /*if (autoSelect.isSelected()) {
        } else {
            showSetTypePopup(files, cb);
        }*/
    }

    /**
     * Will popup the temporary set-import-type window.
     * @deprecated To be removed.
     * @param files List of files to convert to {@link ImportItem}s and set the type for.
     * @param cb    Callback to run when the choice is made.
     */
/*    void showSetTypePopup(List<File> files, Consumer<List<ImportItem>> cb) {
        *//* create temp instance *//*
        JDialog dialog = new JDialog();
        JComboBox<Import> type = new JComboBox<>();
        JCheckBox auto = new JCheckBox();
        String s = System.getProperty("ImportDialog.autoSelectPopup " + System.currentTimeMillis());
        auto.setEnabled(s == null ? true : Boolean.valueOf(s));
        JButton apply = new JButton();
        apply.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Import t = Import.valueOf(type.getSelectedItem().toString());
                applyTypeToList(t, files, cb);
                dialog.setVisible(false);
            }
        });
        JPanel p = createSelectorPanel(auto, apply, type);
        *//* put isntance of selector in a dialog *//*
        dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.add(p);
        dialog.pack();
        dialog.setVisible(true);
    }*/

    /**
     * Will map the List of files into a List of IngestItems and pass them to the callback.
     *
     * @param t     Ingest type to set each IngestItem to.
     * @param files List of Files to map to IngestItems.
     * @param cb    Callback to accept the List of IngestItems upon success.
     */
    void applyTypeToList(Import t, List<File> files, Consumer<List<ImportItem>> cb) {
        if (cb == null) return;
        if (files == null || files.isEmpty()) return;
        cb.accept(files.stream().map(f ->
            getItem(f, t)
        ).collect(Collectors.toList()));
    }

    /**
     * Create Ingest items by guessing their type by extension.
     * This should eventually use each Reader's AbstractReader::test method
     *
     * @param f File to create IngestItem of.
     * @return An {@link ImportItem} with the likely type chosen or NONE otherwise.
     */
    public ImportItem guessType(File f) {
        Pattern extp = Pattern.compile(".*?(?<Ext>(\\.[^\\.]+)$)");
        ImportItem item;
        Matcher m = extp.matcher(f.getName());
        if (m.matches()) {
            item = getItem(f, matchExtension(m.group("Ext")));
        } else {
            item = getItem(f, Import.None);
        }
        return item;
    }

    /**
     * @param ext String containing a file extension, case insensitive.
     * @return Ingest that tends to associate with a given type, else NONE;
     */
    Import matchExtension(String ext) {
        switch (ext.toLowerCase()) {
            case ".pcap":
                return Import.Pcap;
            case ".pcapng":
                return Import.PcapNG;
            case ".00":
                return Import.Bro2;
            case ".log":
                return Import.CiscoShow;
            case ".xml":
                return Import.GM3;
            default:
                return Import.None;
        }
    }

    /**
     * Will merge the parameter list with the {@link #model}.
     *
     * @param items Items to merge.
     */
    public void mergeList(List<ImportItem> items) {
        items.removeAll(model.getList());
        model.addAll(items);
    }

    ImportItem getItem(String f, String type) {
        Import t = Import.None;
        try {
            t = Import.valueOf(type);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Import type not recognized", ex);
            return null;
        }
        return getItem(new File(f), t);
    }

    ImportItem getItem(File f, Import type) {
        ImportItem item = null;
        try {
            if (Import.Pcap.equals(type)) {
                if (isPcapNg(f)) {
                    type = Import.PcapNG;
                }
            } else if( Import.PcapNG.equals(type) ) {
                if( !isPcapNg(f) ) {
                    type = Import.Pcap;
                }
            }
            String path = f.getCanonicalPath();
            item = type.newItem(path);

            item.getLogObserver().addObserver(this);
        } catch (IOException ex) {
            Logger.getLogger(ImportDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        return item;
    }

    private boolean isPcapNg(File file) throws IOException {
        final String pcapId = "a1b2c3d4";
        final String pcapIdRev = "d4c3b2a1";
//        final String pcapNgRev = "1a2b3c4d";
        FileInputStream inputStream = new FileInputStream(file);
        ByteBuffer byteBuffer = ByteBuffer.allocate(32); /** size of {@link #MAGIC} */
        inputStream.getChannel().read(byteBuffer);
        byteBuffer.rewind();
        final String magic = Integer.toHexString(byteBuffer.getInt());
        inputStream.close();
        if (pcapId.equals(magic) || pcapIdRev.equals(magic)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!this.isVisible()) return;
        if (!(arg instanceof ImportItem)) return;
        ImportItem item = (ImportItem) arg;
        SwingUtilities.invokeLater(() -> {
            item.getLog().forEach(log -> pushLogs(item, log));
            item.getLog().clear();
            updateState();
        });
    }

    void pushLogs(ImportItem item, LogEmitter.Log log) {
        notifications.log(log);
    }

    public void notifyImportComplete() {
        updateState();
    }

    public void notifyImportCancellation() {
        model.resetIfNotComplete();
        model.running.clear();
        updateState();
    }

    public void reset() {
        model.reset();
        updateState();
    }

    public List<ImportItem> getCompletedItems() {
        return model.getList().stream().filter(ImportItem::isComplete).collect(Collectors.toList());
    }

}