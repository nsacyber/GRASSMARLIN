package iadgov.directorywatcher;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.ui.common.TabController;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks {

    private final RuntimeConfiguration config;
    private final WatchService watcher;
    private final Timer timer;
    private final Map<WatchKey, WatcherConfigInfo> keys;
    private final Map<Path, ImportTimerTask> timerMap;
    private final ObjectProperty<Session> sessionProperty;
    private ArrayList<WatcherConfigInfo> configList;

    private class ImportTimerTask extends TimerTask {

        private final ImportItem item;
        private final long delay;

        public ImportTimerTask(ImportItem item, long delay) {
            this.item = item;
            this.delay = delay;
        }

        public long getDelay() {
            return this.delay;
        }

        public ImportItem getItem() {
            return this.item;
        }

        @Override
        public void run() {
            System.err.println("Running at " + Instant.now());
            try {
                sessionProperty.get().processImport(item);
            } catch (Exception e) {
                Logger.log(Logger.Severity.WARNING, "Error importing %s: %s", item.getPath().toString(), e.getMessage());
            }
            updateTimerMap();
        }

        private void updateTimerMap() {
            synchronized (timerMap) {
                timerMap.remove(item);
            }
        }
    }

    private class WatcherConfigDialog extends Dialog<List<WatcherConfigInfo>> {

        private final ObservableList<WatcherConfigInfo> itemList;

        /**
         * The dialog for changing the configuration of the directory watcher
         *
         * @param configList The new config list if ok is selected, null if cancel is selected
         */
        public WatcherConfigDialog(@NotNull List<WatcherConfigInfo> configList) {
            itemList = new ObservableListWrapper<>(new ArrayList<>(configList));
            this.initStyle(StageStyle.UTILITY);
            this.setResizable(true);
            this.getDialogPane().setPrefSize(1050, 600);
            this.setResultConverter(this::buttonPressed);
            config.setIcons(this);
            this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            BorderPane contentPane = new BorderPane();
            TableView<WatcherConfigInfo> content = new TableView<>(itemList);
            content.setEditable(true);

            TableColumn<WatcherConfigInfo, Path> directoryColumn = new TableColumn<>("Import Directory");
            directoryColumn.setEditable(true);
            directoryColumn.setCellValueFactory(features -> features.getValue().watchDirProperty());
            directoryColumn.setCellFactory(column -> {
                TableCell<WatcherConfigInfo, Path> cell = new PathTableCell();
                cell.setEditable(true);
                return cell;
            });

            TableColumn<WatcherConfigInfo, Boolean> recursiveColumn = new TableColumn<>("Recursive");
            recursiveColumn.setCellValueFactory(features -> features.getValue().recursiveProperty());
            recursiveColumn.setCellFactory(CheckBoxTableCell.forTableColumn(recursiveColumn));

            TableColumn<WatcherConfigInfo, Long> delayColumn = new TableColumn<>("Delay (ms)");
            delayColumn.setCellValueFactory(features -> features.getValue().modifyDelayProperty().asObject());
            delayColumn.setCellFactory(column -> {
                TableCell<WatcherConfigInfo, Long> cell = new TextFieldTableCell<>(new StringConverter<Long>() {
                    @Override
                    public String toString(Long value) {
                        return value != null ? value.toString() : "0";
                    }

                    @Override
                    public Long fromString(String string) {
                        Long value;
                        try {
                            value =  Long.parseLong(string);
                        } catch (NumberFormatException e) {
                            value = 0L;
                        }

                        return value;
                    }
                });
                cell.setAlignment(Pos.CENTER_LEFT);

                return cell;
            });

            TableColumn<WatcherConfigInfo, String> filterColumn = new TableColumn<>("File Filter");
            filterColumn.setCellValueFactory(features -> features.getValue().fileFilterProperty());
            filterColumn.setCellFactory(column -> {
                TableCell<WatcherConfigInfo, String> cell = new TextFieldTableCell<WatcherConfigInfo, String>(new DefaultStringConverter()){
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null && !empty) {
                            try {
                                Pattern.compile(item);
                                this.setTextFill(Color.BLACK);
                            } catch (PatternSyntaxException e) {
                                // turn text red if does not compile
                                this.setTextFill(Color.RED);
                            }
                        }
                    }
                };

                cell.setAlignment(Pos.CENTER_LEFT);

                return cell;
            });

            TableColumn<WatcherConfigInfo, String> entryColumn = new TableColumn<>("Entry Point");
            entryColumn.setCellValueFactory(features -> features.getValue().entryPointProperty());
            entryColumn.setCellFactory(column -> {
                TableCell<WatcherConfigInfo, String> cell = new TextFieldTableCell<>(new DefaultStringConverter());
                cell.setAlignment(Pos.CENTER_LEFT);

                return cell;
            });

            directoryColumn.prefWidthProperty().bind(content.widthProperty()
                    .subtract(recursiveColumn.widthProperty()
                            .add(delayColumn.widthProperty())
                            .add(filterColumn.widthProperty())
                            .add(entryColumn.widthProperty())
                            .add(2))); //some padding
            content.getColumns().addAll(directoryColumn, recursiveColumn, delayColumn, filterColumn, entryColumn);

            HBox newRow = new HBox(7);

            HBox boxPath = new HBox(3);
            boxPath.setAlignment(Pos.CENTER_LEFT);
            Label labelPath = new Label("Watch Dir:");
            TextField fieldPath = new TextField();
            labelPath.setLabelFor(fieldPath);
            boxPath.getChildren().addAll(labelPath, fieldPath);

            HBox boxRecursive = new HBox(3);
            boxRecursive.setAlignment(Pos.CENTER_LEFT);
            Label labelRecursive = new Label("Recursive:");
            CheckBox checkRecursive = new CheckBox();
            labelRecursive.setLabelFor(checkRecursive);
            boxRecursive.getChildren().addAll(labelRecursive, checkRecursive);

            HBox boxDelay = new HBox(3);
            boxDelay.setAlignment(Pos.CENTER_LEFT);
            Label labelDelay = new Label("Modify Delay:");
            TextField fieldDelay = new TextField("100");
            labelDelay.setLabelFor(fieldDelay);
            fieldDelay.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!(newValue.matches("\\d*") || newValue.isEmpty() || newValue == null)) {
                    fieldDelay.setText(oldValue);
                }
            });
            boxDelay.getChildren().addAll(labelDelay, fieldDelay);

            HBox boxFilter = new HBox(3);
            boxFilter.setAlignment(Pos.CENTER_LEFT);
            Label labelFilter = new Label("File Filter:");
            TextField fieldFilter = new TextField();
            labelFilter.setLabelFor(fieldFilter);
            fieldFilter.setOnKeyPressed(event -> fieldFilter.setStyle("-fx-text-inner-color: black;"));
            boxFilter.getChildren().addAll(labelFilter, fieldFilter);

            HBox boxEntry = new HBox(3);
            boxEntry.setAlignment(Pos.CENTER_LEFT);
            Label labelEntry = new Label("Entry Point:");
            TextField fieldEntry = new TextField("Default");
            labelEntry.setLabelFor(fieldEntry);
            boxEntry.getChildren().addAll(labelEntry, fieldEntry);

            Button buttonAdd = new Button("Add");
            buttonAdd.disableProperty().bind(fieldPath.textProperty().isEmpty());
            buttonAdd.setOnAction(event -> {
                try {
                    if (fieldFilter.getText() != null) {
                        Pattern.compile(fieldFilter.getText());
                    }

                    WatcherConfigInfo info = new WatcherConfigInfo();
                    info.setWatchDir(Paths.get(fieldPath.getText()));
                    info.setRecursive(checkRecursive.isSelected());
                    try {
                        BigInteger delay = new BigInteger(fieldDelay.getText());
                        if (delay.compareTo(new BigInteger("0")) >= 0) {
                            info.setModifyDelay(new BigInteger(fieldDelay.getText()).longValueExact());
                        } else {
                            info.setModifyDelay(0);
                        }
                    } catch (ArithmeticException e) {
                        info.setModifyDelay(Long.MAX_VALUE);
                    }
                    info.setFileFilter(fieldFilter.getText());
                    info.setEntryPoint(fieldEntry.getText());

                    itemList.add(info);
                } catch (PatternSyntaxException e) {
                    fieldFilter.setStyle("-fx-text-inner-color: red;");
                }
            });

            newRow.getChildren().addAll(boxPath, boxRecursive, boxDelay, boxFilter, boxEntry, buttonAdd);
            newRow.setPadding(new Insets(5, 0, 0, 0));

            contentPane.setCenter(content);
            contentPane.setBottom(newRow);

            this.getDialogPane().setContent(contentPane);
        }

        private List<WatcherConfigInfo> buttonPressed(ButtonType button) {
            if (button == ButtonType.OK) {
                return this.itemList;
            } else {
                return null;
            }
        }
    }

    public Plugin(RuntimeConfiguration config) {
        this.config = config;
        this.sessionProperty = new SimpleObjectProperty<>();

        this.keys = new HashMap<>();
        this.timerMap = new HashMap<>();
        this.configList = new ArrayList<>();


        WatchService temp = null;
        try {
            temp = FileSystems.getDefault().newWatchService();
        } catch (IOException ioe) {
            Logger.log(Logger.Severity.ERROR, "Directory Watch Importer not able to start: %s", ioe.getMessage());
        }
        this.watcher = temp;
        this.timer = new Timer("Directory Watcher Modify Delay Timer", true);

        Thread processThread = new Thread(this::watch);
        processThread.setDaemon(true);
        processThread.start();

    }


    @Override
    public String getName() {
        return "Directory Watch Importer";
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        ArrayList<MenuItem> items = new ArrayList<>();
        MenuItem menuConfig = new ActiveMenuItem("Configure", event -> {
            Optional<List<WatcherConfigInfo>> newConfig = new WatcherConfigDialog(configList).showAndWait();
            if (newConfig.isPresent()) {
                List<Path> currentPaths = configList.stream()
                        .map(info -> info.getWatchDir())
                        .collect(Collectors.toList());
                List<Path> incomingPaths = newConfig.get().stream()
                        .map(info -> info.getWatchDir())
                        .collect(Collectors.toList());
                List<WatcherConfigInfo> newPaths = newConfig.get().stream()
                        .filter(info -> !currentPaths.contains(info.getWatchDir()))
                        .collect(Collectors.toList());
                List<WatcherConfigInfo> removedPaths = configList.stream()
                        .filter(info -> !incomingPaths.contains(info.getWatchDir()))
                        .collect(Collectors.toList());

                for (WatcherConfigInfo info : newPaths) {
                    try {
                        register(info);
                    } catch (IOException e) {
                        Logger.log(Logger.Severity.ERROR, "Unable to register %s in Directory Watcher: %s", info.getWatchDir().toString(), e.getMessage());
                    }
                }

                keys.entrySet().stream()
                        .filter(entry -> removedPaths.contains(entry.getValue()))
                        .forEach(entry -> entry.getKey().cancel());

                configList.clear();
                configList.addAll(newConfig.get());
            }
        });
        items.add(menuConfig);

        return items;
    }

    @Override
    public void sessionCreated(Session session, TabController tabs) {
        this.sessionProperty.set(session);
    }

    @Override
    public void sessionClosed(Session session) {
        this.sessionProperty.set(null);
    }

    private void register(WatcherConfigInfo info) throws IOException {
        // if recursive walk full depth of directory tree otherwise only the given directory
        int depth = info.isRecursive() ? Integer.MAX_VALUE : 0;

        Files.walkFileTree(info.getWatchDir(), EnumSet.noneOf(FileVisitOption.class), depth, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                WatcherConfigInfo childInfo = new WatcherConfigInfo(dir, info.isRecursive(), info.getModifyDelay(), info.getFileFilter(), info.getEntryPoint());
                keys.put(key, childInfo);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                Logger.log(Logger.Severity.WARNING, "Error registering watch dir - %s : %s", file.toString(), exc.getMessage());
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }
        });

        WatchKey key = info.getWatchDir().register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
        keys.put(key, info);

    }

    private void watch() {
        for (;;) {
            timer.purge();
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                return;
            }

            if (!key.isValid()) {
                keys.remove(key);
                continue;
            }

            WatcherConfigInfo info = keys.get(key);
            if (info == null) {
                Logger.log(Logger.Severity.WARNING, "Watch Key not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = (WatchEvent<Path>)event;
                Path dir = info.getWatchDir();
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (kind == ENTRY_CREATE) {
                    System.err.println("Created at " + Instant.now());
                    Pattern pattern = info.getCompiledFilter();
                    if (Files.isDirectory(child) && info.isRecursive()) {
                        WatcherConfigInfo childInfo = new WatcherConfigInfo(child, info.isRecursive(), info.getModifyDelay(), info.getFileFilter(), info.getEntryPoint());
                        try {
                            WatchKey childKey = child.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                            keys.put(childKey, childInfo);
                        } catch (IOException e) {
                            Logger.log(Logger.Severity.ERROR, "Unable to register new child directory %s: %s", child.toString(), e.getMessage());
                        }
                    } else if ((pattern == null && info.getFileFilter() == null) || info.getFileFilter().isEmpty() || pattern.matcher(child.toString()).matches()) {
                        ImportItem.FromPlugin item = new ImportItem.FromPlugin(child, info.getEntryPoint());
                        ImportProcessorWrapper wrapper = config.importerForFile(child);
                        if (wrapper != null) {
                            item.importerPluginNameProperty().set(config.pluginFor(wrapper.getClass()).getName());
                            item.importerFunctionNameProperty().set(wrapper.getName());

                            if (info.getModifyDelay() == 0) {
                                sessionProperty.get().processImport(item);
                            } else {
                                ImportTimerTask task = new ImportTimerTask(item, info.getModifyDelay());
                                this.timerMap.put(child, task);
                                this.timer.schedule(task, info.getModifyDelay());
                            }
                        } else {
                            Logger.log(Logger.Severity.WARNING, "Unable to find importer for %s", child.toString());
                        }
                    }
                }

                if (kind == ENTRY_MODIFY) {
                    System.err.println("Modified at " + Instant.now());
                    Pattern pattern = info.getCompiledFilter();
                    if ((pattern == null && info.getFileFilter() == null) || info.getFileFilter().isEmpty() || pattern.matcher(child.toString()).matches()) {
                        ImportTimerTask previous = this.timerMap.get(child);
                        if (previous != null && previous.cancel()) {
                            ImportItem item = previous.getItem();
                            ImportTimerTask task = new ImportTimerTask(item, previous.getDelay());
                            this.timer.schedule(task, previous.getDelay());
                            this.timerMap.put(child, task);
                        }
                    }
                }

            }

            if (!key.reset()) {
                keys.remove(key);
            }
        }
    }
}