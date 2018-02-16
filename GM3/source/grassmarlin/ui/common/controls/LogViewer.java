package grassmarlin.ui.common.controls;

import com.sun.istack.internal.Nullable;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.ui.common.TextMeasurer;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.*;
import javafx.scene.text.Font;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LogViewer extends TableView<Logger.Message> {
    private class LogMessageTableCell extends TableCell<Logger.Message, String> {
        public LogMessageTableCell() {
            super();
        }

        @Override
        public void updateItem(String item, boolean isEmpty) {
            super.updateItem(item, isEmpty);

            if(isEmpty) {
                setText(null);
                setBackground(null);
                setTextFill(null);
            } else {
                setText(item == null ? "" : item);

                final TableRow<Logger.Message> row = this.getTableRow();
                if(row == null || row.getItem() == null) {
                    setBackground(null);
                    setTextFill(null);
                } else {
                    //Make Pedantic dev spam slightly more tolerable to read.
                    if(row.getItem().getSeverity() == Logger.Severity.PEDANTIC_DEVELOPER_SPAM) {
                        final Paint bkg;
                        final Paint fore;

                        switch(row.getItem().hashCode() % 6) {
                            case 0:
                                bkg = Color.web("0xF0F080");
                                fore = Color.web("0xF060C0");
                                break;
                            case 1:
                                bkg = Color.web("0xFFC060");
                                fore = Color.RED;
                                break;
                            case 2:
                                bkg = Color.web("0xFFA0C0");
                                fore = Color.web("0xF040C0");
                                break;
                            case 3:
                                bkg = Color.web("0xC040E0");
                                fore = Color.WHITE;
                                break;
                            case 4:
                                bkg = Color.web("0xF060C0");
                                fore = Color.BLACK;
                                break;
                            default:
                                bkg = Color.web("0x00D0F0");
                                fore = new LinearGradient(0.0, 0.0, 1.0, 1.0, true, CycleMethod.NO_CYCLE,
                                        new Stop(0.2, Color.RED),
                                        new Stop(0.45, Color.YELLOW),
                                        new Stop(0.65, Color.GREEN),
                                        new Stop(0.8, Color.BLUE)
                                );
                                break;
                        }

                        this.setBackground(new Background(new BackgroundFill(bkg, null, null)));
                        this.setTextFill(fore);
                    } else {
                        this.setBackground(new Background(new BackgroundFill(LogViewer.this.colorBackground.get(row.getItem().getSeverity()), null, null)));
                        this.setTextFill(LogViewer.this.colorForeground.get(row.getItem().getSeverity()));
                    }
                }
                this.setPadding(Insets.EMPTY);
                this.setFont(LogViewer.this.font);
            }
        }
    }

    private final Font font;
    private final ObservableList<Logger.Message> messages;
    private final List<Logger.Message> messagesPending;
    protected final Map<Logger.Severity, Paint> colorBackground;
    protected final Map<Logger.Severity, Paint> colorForeground;
    protected final RuntimeConfiguration config;

    public LogViewer(final RuntimeConfiguration config, @Nullable final List<Logger.Message> initialMessages) {
        this.config = config;
        //HACK: Use a new cell as the basis for the font; there is probably a better way but finding it is not on the list of pressing issues.
        this.font = Font.font(new TableCell<>().getFont().getFamily(), 10.0);
        this.messagesPending = new ArrayList<>(10);
        this.messages = new ObservableListWrapper<>(new LinkedList<>()); //Alternatively, a LengthCappedList can be used if there is reason to think that the length of the log is becoming problematic.
        this.colorBackground = new HashMap<>();
        this.colorForeground = new HashMap<>();

        //TODO: Get colors for foreground/background from config
        colorBackground.put(Logger.Severity.INFORMATION, Color.web("0xD9EDF7"));
        colorBackground.put(Logger.Severity.COMPLETION, Color.web("0xDFF0D8"));
        colorBackground.put(Logger.Severity.WARNING, Color.web("0xFCF8E3"));
        colorBackground.put(Logger.Severity.ERROR, Color.web("0xF2DEDE"));
        colorForeground.put(Logger.Severity.INFORMATION, Color.BLACK);
        colorForeground.put(Logger.Severity.COMPLETION, Color.BLACK);
        colorForeground.put(Logger.Severity.WARNING, Color.BLACK);
        colorForeground.put(Logger.Severity.ERROR, Color.BLACK);

        Logger.getInstance().onMessage.addHandler(handlerNewLogMessage);

        this.initComponents();

        if (initialMessages != null) {
            this.messages.addAll(initialMessages);
        }
    }

    private Event.EventListener<Logger.MessageEventArgs> handlerNewLogMessage = this::Handle_LogMessage;

    private void Handle_LogMessage(Event<Logger.MessageEventArgs> source, Logger.MessageEventArgs arguments) {

        if(arguments.getMessage().getSeverity() != Logger.Severity.PEDANTIC_DEVELOPER_SPAM || this.config.isDeveloperModeProperty().get()) {
            synchronized(this.messagesPending) {
                this.messagesPending.add(arguments.getMessage());
            }
            this.requestLayout();
        }
    }

    @Override
    protected void layoutChildren() {
        synchronized(this.messagesPending) {
            this.messages.addAll(this.messagesPending);
            this.messagesPending.clear();
        }
        if(this.messages.size() > 0) {
            this.scrollTo(this.messages.get(this.messages.size() - 1));
        }

        super.layoutChildren();
    }

    private void initComponents() {
        final TableColumn<Logger.Message, String> colMessage = new TableColumn<>("Message");
        colMessage.setCellValueFactory(this::factoryMessageText);
        colMessage.setCellFactory(this::factoryCell);
        final TableColumn<Logger.Message, String> colTimestamp = new TableColumn<>("Timestamp");
        colTimestamp.setCellValueFactory(this::factoryMessageTimestamp);
        colTimestamp.setCellFactory(this::factoryCell);
        colTimestamp.setResizable(false);

        colTimestamp.setPrefWidth(TextMeasurer.measureText(" 00-00-0000T00:00:00.000Z ", font).getWidth());

        this.setItems(this.messages);
        this.getColumns().addAll(colTimestamp, colMessage);

        colMessage.prefWidthProperty().bind(widthProperty().subtract(colTimestamp.getPrefWidth()));

        this.getSortOrder().add(colTimestamp);
        colTimestamp.setSortType(TableColumn.SortType.DESCENDING);
    }

    private ObservableValue<String> factoryMessageTimestamp(TableColumn.CellDataFeatures<Logger.Message, String> param) {
        return new ReadOnlyStringWrapper(param.getValue().getTimestamp().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_DATE_TIME));
    }
    private ObservableValue<String> factoryMessageText(TableColumn.CellDataFeatures<Logger.Message, String> param) {
        return new ReadOnlyStringWrapper(param.getValue().getText());
    }

    private LogMessageTableCell factoryCell(TableColumn<Logger.Message, String> column) {
        return new LogMessageTableCell();
    }
}
