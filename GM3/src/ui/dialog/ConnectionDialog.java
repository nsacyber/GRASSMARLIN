/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.Core;
import core.Environment;
import core.ViewUtils;
import core.types.LogEmitter;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.util.StringConverter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.stringtemplate.v4.ST;
import ui.custom.ComparableLabel;
import ui.icon.Icons;
import ui.views.tree.visualnode.HostVisualNode;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Dialog showing connections 2015.07.31 - CC - Adding graph and reformatting
 */
public class ConnectionDialog extends JFrame {

    public static final Charset CHARSET = Charset.forName("UTF-8");
    public static final String[] COLUMNS = {"Source", "Port", "Dest", "Port", "File", "Filter", "Packet #", "Protocol", "Size (B)", "Date"};
    public static final Class[] CLASSES = {String.class, Integer.class, String.class, Integer.class, String.class, String.class, Long.class, String.class, Integer.class, String.class};
    private JCheckBox hideBroadCast;
    private JButton exportBtn;
    private JButton searchBtn;
    private JLabel iconLabel;
    private JTable table;
    private JLabel title;
    private JTextField search;
    //private JLabel graphLabel = new JLabel();
    private JFXPanel graphPanel = new JFXPanel();
    private DefaultTableModel model;
    private DefaultTableModel modelBackup;

    //    public static int SOURCEIP = 0;
//    public static int SOURCEPORT = 1;
//    public static int DESTIP = 2;
//    public static int DESTPORT = 3;
    public static int FILE = 4;
//    public static int FILTER = 5;
    public static final int PACKET = 6;
    //    public static int PROTOCOL = 7;
    public static final int SIZE = 8;
//    public static int DATE = 9;

//    public static String TIME_COL = "time";
//    public static String SIZE_COL = "size";
//    public static String VISUAL = "vis";
    private final HostVisualNode treeViewNode;

    private void run() {
        toggle(false);
        new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                ConnectionDialog.this.treeViewNode.getData().edges.forEach((destinationHostIp, protocols) -> {
                    String destinationIp = ViewUtils.ipString(destinationHostIp);
                    protocols.forEach((protocol, sources) -> {
                        String protocolString = ViewUtils.proto(protocol);
                        sources.forEach((source, destinations)
                                -> destinations.forEach((destination, data)
                                        -> data.right.forEach(frameInformation
                                                -> ConnectionDialog.this.model.addRow(new Object[]{
                                    ConnectionDialog.this.treeViewNode.getAddress(),
                                    port(source),
                                    destinationIp,
                                    port(destination),
                                    ConnectionDialog.this.treeViewNode.getData().originator.getPath(),
                                    "",
                                    frameInformation.packet,
                                    protocolString,
                                    frameInformation.size,
                                    DateFormatUtils.format(frameInformation.date, "MM/dd/yyyy HH:mm:s", Locale.US)
                                })
                                        )
                                )
                        );
                    });
                });

                // all inbound connections to this treeViewNode
                ConnectionDialog.this.treeViewNode.getData().backEdges.stream().forEach(peer -> { // a peer has an inbound connection to this treeViewNode
                    String sourceIp = ViewUtils.ipString(peer);
                    peer.edges.get(treeViewNode.getData()).forEach((protocol, sources) -> {
                        String protocolString = ViewUtils.proto(protocol);
                        sources.forEach((source, destinations)
                                -> destinations.forEach((destination, data)
                                        -> data.right.forEach(frameInformation
                                                -> ConnectionDialog.this.model.addRow(new Object[]{
                                    sourceIp,
                                    port(source),
                                    ConnectionDialog.this.treeViewNode.getAddress(),
                                    port(destination),
                                    ConnectionDialog.this.treeViewNode.getData().originator.getPath(),
                                    "",
                                    frameInformation.packet,
                                    protocolString,
                                    frameInformation.size,
                                    DateFormatUtils.format(frameInformation.date, "MM/dd/yyyy HH:mm:s", Locale.US)
                                })
                                        )
                                )
                        );
                    });
                });
                ConnectionDialog.this.table.getRowSorter().toggleSortOrder(PACKET);
                ConnectionDialog.this.model.fireTableDataChanged();
                updateGraph();
                toggle(true);
                return true;
            }
        }.execute();
    }

    public String port(Integer i) {
        if (i == -1) {
            return "?";
        } else {
            return i.toString();
        }
    }

    /**
     * Expects a VisualNode with a proper {@link HostVisualNode#getData() } return value.
     * @param n Node to show all connections for.
     */
    public ConnectionDialog(HostVisualNode n) {
        if (n == null || !n.isHost()) {
            this.treeViewNode = null;
            return;
        }

        initComponents();
        setTitle(String.format("%s Connections", n.getName()));
        this.iconLabel.setIcon(n.getDetails().image.getIcon());
        this.model = getNewModel();
        this.model.setColumnIdentifiers(ConnectionDialog.COLUMNS);
        this.table.setModel(this.model);
        this.table.setAutoCreateRowSorter(true);
        DefaultRowSorter sorter = (DefaultRowSorter) this.table.getRowSorter();
        Comparator comp = (o1,o2) -> {
            int compare;
            try {
                compare = Integer.compare(Integer.valueOf(o1.toString()), Integer.valueOf(o2.toString()));
            } catch(Exception ex) {
                compare = o1.toString().compareTo(o2.toString());
            }
            return compare;
        };
        sorter.setComparator(1, comp);
        sorter.setComparator(3, comp);
        sorter.setComparator(8, comp);
        sorter.setComparator(9, comp);
        this.table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    Object obj = table.getValueAt(row, PACKET);
                    try {
                        Integer frame = Integer.valueOf(obj.toString());
                        String path = table.getValueAt(row, FILE).toString();
                        ConnectionDialog.this.openPopup(e.getPoint(), frame, path);
                    } catch (Exception ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Failed to get pcap frame number.", ex);
                    }
                }
            }
        });
        this.treeViewNode = n;
        pack();
    }

    final DefaultTableModel getNewModel() {
        return new DefaultTableModel() {
            @Override
            public int getColumnCount() {
                return ConnectionDialog.COLUMNS.length;
            }

            @Override
            public String getColumnName(int column) {
                return ConnectionDialog.COLUMNS[column];
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return ConnectionDialog.CLASSES[columnIndex];
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    @Override
    public void setVisible(boolean b) {
        if (this.treeViewNode == null) {
            dispose();
            return;
        }
        super.setVisible(b);
        if (b) {
            run();
        }
    }

    private void toggleBroadcastVisibility(java.awt.event.ActionEvent evt) {
        int i = this.model.getRowCount();
        while (--i > -1) {
            this.model.removeRow(i);
        }
        run();
    }

    void toggle(boolean b) {
        this.exportBtn.setEnabled(b);
        this.hideBroadCast.setEnabled(b);
        this.searchBtn.setEnabled(b);
    }

    @SuppressWarnings("unchecked")
    private void exportBtnActionPerformed(java.awt.event.ActionEvent evt) {
        toggle(false);
        JFileChooser fc = new JFileChooser();
        String path = FileUtils.getUserDirectoryPath() + File.separator + treeViewNode.getText() + ".csv";
        fc.setSelectedFile(new File(path));
        fc.showSaveDialog(this);
        File target = fc.getSelectedFile();
        int row = model.getRowCount();
        try {
            boolean written;
            try (FileOutputStream fos = new FileOutputStream(target)) {
                while (--row > -1) {
                    Vector v = (Vector) model.getDataVector().get(row);
                    for (int i = 0; i < v.size(); i++) {
                        if (v.get(i).toString().contains(",")) {
                            v.set(i, ST.format("\"<%1>\"", v.get(i)));
                        }
                    }
                    String line = ST.format("<%1:{ x |, <x>}>", v).substring(2);
                    fos.write(line.getBytes(CHARSET));
                    fos.write(Character.LINE_SEPARATOR);
                }
                written = true;
            }
            if (written) {
                LogEmitter.factory.get().emit(this, Core.ALERT.INFO, ST.format("<%1> hosts written to <%2>", model.getRowCount(), target.getPath()));
            } else {
                LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, ST.format("Failed to export <%1>", target.getPath()));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ConnectionDialog.class.getName()).log(Level.SEVERE, null, ex);
            LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, ST.format("Failed to export <%1>", target.getPath()));
        } catch (IOException ex) {
            Logger.getLogger(ConnectionDialog.class.getName()).log(Level.SEVERE, null, ex);
            LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, ST.format("Failed to export <%1>", target.getPath()));
        }
        toggle(true);
    }

    @Override
    final public void setTitle(String title) {
        this.title.setText(title);
        super.setTitle(title + " Connections");
    }

    @SuppressWarnings("unchecked")
    private void updateGraph() {
        //HashMap<ComparableLabel, Integer> dataPoints = new java.util.HashMap<>();
        HashMap<String,HashMap<Long, Integer>> dataNumberPoints = new java.util.HashMap<>();

        this.treeViewNode.getData().edges.forEach((destinationHostIp, protocols) -> {
                    String destinationIp = ViewUtils.ipString(destinationHostIp);
                    protocols.forEach((protocol, sources)
                                -> sources.forEach((source, destinations)
                                        -> destinations.forEach((destination, data)
                                                -> data.right.forEach(frameInformation
                                                        ->  {
                                                    //dataPoints.put(new DateAxisGraphLabel(frameInformation.date, DateFormatUtils.format(frameInformation.date, "HH:mm\nss:SSS", Locale.US)), frameInformation.size);
                                                    String key = destinationIp;
                                                    if(dataNumberPoints.containsKey(key)) {
                                                        dataNumberPoints.get(key).put(frameInformation.date, frameInformation.size);
                                                    }
                                                    else {
                                                        HashMap<Long, Integer> newMap = new HashMap<>();
                                                        newMap.put(frameInformation.date, frameInformation.size);
                                                        dataNumberPoints.put(key,newMap);
                                                    }
                                                    //dataNumberPoints.put(frameInformation.date, frameInformation.size);
                                                }
                                        )
                                )
                    ));
        });

        this.treeViewNode.getData().backEdges.stream().forEach(peer -> {// a peer has an inbound connection to this treeViewNode
            String sourceIp = ViewUtils.ipString(peer);
                        peer.edges.get(treeViewNode.getData()).forEach((protocol, sources)
                                        -> sources.forEach((source, destinations)
                                                -> destinations.forEach((destination, data)
                                                        -> data.right.forEach(frameInformation
                                                                -> {
                                                            //dataPoints.put(new DateAxisGraphLabel(frameInformation.date, DateFormatUtils.format(frameInformation.date, "HH:mm\nss:SSS", Locale.US)), frameInformation.size);
                                                            String key = sourceIp;
                                                            if(dataNumberPoints.containsKey(key)) {
                                                                dataNumberPoints.get(key).put(frameInformation.date, frameInformation.size);
                                                            }
                                                            else {
                                                                HashMap<Long, Integer> newMap = new HashMap<>();
                                                                newMap.put(frameInformation.date, frameInformation.size);
                                                                dataNumberPoints.put(key,newMap);
                                                            }
                                                        }
                                                )
                                        )
                        ));
        });
        Platform.runLater(() -> {
            this.graphPanel.setScene(createScene());
            updateGraph(dataNumberPoints);
        });
    }

//    private final NumberAxis xAxis = new NumberAxis();//lMin,lMax,100000l);
//    private final NumberAxis yAxis = new NumberAxis();//iMin-20,iMax+20,20);
    private LineChart<Number,Number> lineChart;// = new LineChart<>(xAxis,yAxis);

    private Scene createScene() {
        final NumberAxis xAxis = new NumberAxis();//lMin,lMax,100000l);
        final NumberAxis yAxis = new NumberAxis();//iMin-20,iMax+20,20);
        this.lineChart = new LineChart<>(xAxis,yAxis);
        ((NumberAxis)this.lineChart.getXAxis()).setAutoRanging(false);
        ((NumberAxis)this.lineChart.getXAxis()).setLowerBound(0);
        ((NumberAxis)this.lineChart.getXAxis()).setUpperBound(1000);
        ((NumberAxis)this.lineChart.getXAxis()).setTickUnit(100000l);
        ((NumberAxis)this.lineChart.getXAxis()).setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number number) {
                return DateFormatUtils.format(number.longValue(), "yyyy-MM-dd\nHH:mm:ss", Locale.US);
            }

            @Override
            public Number fromString(String string) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd\nHH:mm:ss");
                Date date = null;
                try {
                    date = timeFormat.parse(string);
                    return date.getTime();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0l;
            }
        });

        ((NumberAxis)this.lineChart.getYAxis()).setAutoRanging(false);
        ((NumberAxis)this.lineChart.getYAxis()).setLowerBound(0);
        ((NumberAxis)this.lineChart.getYAxis()).setUpperBound(1000);
        ((NumberAxis)this.lineChart.getYAxis()).setTickUnit(100);

        this.lineChart.setAnimated(false);
        this.lineChart.setTitle("Size Over Time");
        Scene scene = new Scene(this.lineChart,976,228);
        return scene;
    }

    private void updateGraph(HashMap<String,HashMap<Long, Integer>> dataPoints) {
        Platform.runLater(() -> {
            this.lineChart.getData().clear();

            dataPoints.entrySet().stream().forEach(entry -> {
                XYChart.Series series = new XYChart.Series();
                series.setName(entry.getKey());
                entry.getValue().entrySet().stream().forEach(entryMap -> {
                    series.getData().add(new XYChart.Data(entryMap.getKey(), entryMap.getValue()));
                });
                this.lineChart.getData().add(series);
            });
            Function<Map<Long, Integer>,Set<Long>> keySet = Map::keySet;
            Function<Set<Long>, Stream<Long>> stream = Set::stream;
            Function<Map<Long, Integer>, Collection<Integer>> valueSet = Map::values;
            Function<Collection<Integer>, Stream<Integer>> toStream = Collection::stream;
            long lMin = dataPoints.values().stream().map(keySet).flatMap(stream).min(Long::compare).get();
            long lMax = dataPoints.values().stream().map(keySet).flatMap(stream).max(Long::compare).get();
            int iMin = dataPoints.values().stream().map(valueSet).flatMap(toStream).min(Integer::compare).get();
            int iMax = dataPoints.values().stream().map(valueSet).flatMap(toStream).max(Integer::compare).get();
            ((NumberAxis)this.lineChart.getXAxis()).setLowerBound(lMin);
            ((NumberAxis)this.lineChart.getXAxis()).setUpperBound(lMax);
            ((NumberAxis)this.lineChart.getYAxis()).setLowerBound(iMin - 20);
            ((NumberAxis)this.lineChart.getYAxis()).setUpperBound(iMax + 20);
        });
    }

    private void openPopup(Point p, Integer frame, String path) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(String.format("Analyze in Wireshark", frame));
        item.addActionListener(e -> {
            String wireshark = Environment.WIRESHARK_EXEC.getPath();
            openInWireshark(wireshark, frame, path);
        });
        menu.add(item);
        menu.show(this.table, p.x, p.y);
    }

    private void openInWireshark(String wireshark, Integer frame, String path) {
        if (wireshark.contains(" ")) {
            wireshark = String.format("\"%s\"", wireshark);
        }
        if (path.contains(" ")) {
            path = String.format("\"%s\"", path);
        }
        try {
            String command = String.format("%s -g %s -r %s", wireshark, frame, path);
            LogEmitter.factory.get().emit(this, Core.ALERT.MESSAGE, String.format("Opening %s in wireshark at frame %s", path, frame));
            LogEmitter.factory.get().emit(this, Core.ALERT.INFO, command);
            Runtime.getRuntime().exec(command);
        } catch (IOException ex) {
            Logger.getLogger(ConnectionDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private class DateAxisGraphLabel implements ComparableLabel {

        private long date;
        private String label;

        public DateAxisGraphLabel(long date, String s) {
            this.date = date;
            this.label = s;
        }

        @Override
        public String getLabel() {
            return this.label;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof DateAxisGraphLabel) {
                return Long.compare(this.date, ((DateAxisGraphLabel) o).getDate());
            }
            return 0;
        }

        public long getDate() {
            return this.date;
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                         
    private void initComponents() {
        this.title = new javax.swing.JLabel();
        this.iconLabel = new javax.swing.JLabel();
        this.exportBtn = new javax.swing.JButton();
        this.searchBtn = new javax.swing.JButton();
        JScrollPane scroll = new JScrollPane();
        this.table = new javax.swing.JTable();
        this.search = new javax.swing.JTextField();

        Platform.runLater(() -> {
            this.graphPanel.setScene(createScene());
        });

        this.title.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        this.title.setText("text");

        this.search = new JTextField("Search");
        this.search.setForeground(Color.LIGHT_GRAY);

        this.searchBtn.setText("Search");
        this.searchBtn.addActionListener((e) -> {
            String searchString = search.getText();

            DefaultTableModel resultModel = getNewModel();
            DefaultTableModel oldModel = this.modelBackup == null ? (DefaultTableModel) this.table.getModel() : this.modelBackup;
            Pattern p = Pattern.compile(String.format(".*?%s.*?", searchString));
            int rows = oldModel.getRowCount();
            int cols = oldModel.getColumnCount();
            Vector dv = oldModel.getDataVector();

            for (int row = 0; row < rows; row++) {
                Vector v = (Vector) dv.get(row);
                for (int col = 0; col < cols; col++) {
                    Object o = v.get(col);
                    if (o != null) {
                        Matcher m = p.matcher(o.toString());
                        if (m.matches()) {
                            resultModel.addRow(v);
                            break;
                        }
                    }
                }
            }

            if (resultModel.getRowCount() != oldModel.getRowCount() && resultModel.getRowCount() > 0) {
                if (this.modelBackup == null) {
                    this.modelBackup = oldModel;
                }
                this.table.setModel(resultModel);
                resultModel.fireTableDataChanged();
            }
        });
        
        table.setDefaultRenderer(Integer.class, new DefaultTableCellRenderer(){
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(JLabel.LEFT);
                return label;
            }
        });

        this.search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if ("search".equalsIgnoreCase(ConnectionDialog.this.search.getText())) {
                    ConnectionDialog.this.search.setForeground(Color.LIGHT_GRAY);
                } else {
                    ConnectionDialog.this.search.setForeground(Color.BLACK);
                }
                if (ConnectionDialog.this.search.getText().length() == 0 && ConnectionDialog.this.modelBackup != null) {
                    ConnectionDialog.this.table.setModel(ConnectionDialog.this.modelBackup);
                    ConnectionDialog.this.modelBackup.fireTableDataChanged();
                    ConnectionDialog.this.modelBackup = null;
                }
            }
        });
        MouseAdapter removableListner = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if ("search".equalsIgnoreCase(ConnectionDialog.this.search.getText())) {
                    ConnectionDialog.this.search.setText("");
                    ConnectionDialog.this.search.removeMouseListener(this);
                }
            }
        };
        this.search.addMouseListener(removableListner);

        this.iconLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        this.iconLabel.setIcon(Icons.Error.getIcon32()); // NOI18N
        this.iconLabel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 1, 1, 1, new java.awt.Color(204, 204, 204)));

        this.exportBtn.setText("Export to CSV");
        this.exportBtn.addActionListener(this::exportBtnActionPerformed);
        this.hideBroadCast = new JCheckBox("Hide Broadcast / Multicast");
        this.hideBroadCast.addActionListener(this::toggleBroadcastVisibility);
        this.hideBroadCast.setSelected(true);
        scroll.setViewportView(this.table);
        JPanel p = new JPanel();
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(p);
        p.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 976, Short.MAX_VALUE)
                                .addComponent(this.graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 976, Short.MAX_VALUE)
                                //                    .addComponent(display)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(this.iconLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(this.title)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(this.searchBtn)
                                        .addContainerGap()
                                        .addComponent(this.search)))
                        .addContainerGap())
        );
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(this.iconLabel)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(this.title)
                                        .addComponent(this.searchBtn)
                                        .addComponent(this.search, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(scroll, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                        .addComponent(this.graphPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE)
                        .addContainerGap())
        );
        //457, 228
        setLayout(new BorderLayout());
        add(p, BorderLayout.CENTER);

        JPanel jPanel1 = new JPanel();
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(this.hideBroadCast)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 395, Short.MAX_VALUE)
                        .addComponent(this.exportBtn)
                        .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        //                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(this.hideBroadCast, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(this.exportBtn, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addContainerGap())
        );

        add(jPanel1, BorderLayout.SOUTH);
    }// </editor-fold>

}
