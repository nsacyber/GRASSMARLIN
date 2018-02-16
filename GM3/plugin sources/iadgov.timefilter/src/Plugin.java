package iadgov.timefilter;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages, IPlugin.AcceptsCommandLineArguments {
    public static final String NAME = "Time Filter";

    private List<PipelineStage> stages;

    public Plugin(RuntimeConfiguration config) {
        this.stages = new ArrayList<>();
        this.stages.add(new PipelineStage(true, StageTimeFilter.NAME, StageTimeFilter.class, StageTimeFilter.DEFAULT_OUTPUT, StageTimeFilter.REJECTED));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    private final Image iconLarge = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        return iconLarge;
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return this.stages;
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if (stage.getStage().isAssignableFrom(StageTimeFilter.class)) {
            Timespan current;
            if (configuration instanceof Timespan) {
                current = ((Timespan) configuration);
            } else {
                current = ((Timespan) getDefaultConfiguration(stage));
            }

            DialogTimeSelection timeSelection = new DialogTimeSelection(current);

            Optional<Timespan> span = timeSelection.showAndWait();

            return span.orElse(current);
        } else {
            return null;
        }
    }

    @Override
    public Serializable getDefaultConfiguration(PipelineStage stage) {
        if (stage.getStage().isAssignableFrom(StageTimeFilter.class)) {
            return getDefaultTimespan();
        } else {
            return null;
        }
    }

    /**
     *
     * @param arg
     * @return true if args can be parsed, false otherwise
     */
    @Override
    public boolean processArg(String arg) {
        return false;
    }

    private Timespan getDefaultTimespan() {
        return new Timespan(Instant.now(), Instant.now(), ZoneOffset.UTC);
    }


    private class DialogTimeSelection extends Dialog<Timespan> {

        private static final String UTC = "UTC";
        private static final String LOCAL = "Local";

        public DialogTimeSelection(Timespan current) {
            super();

            this.initStyle(StageStyle.UTILITY);
            this.setTitle("Time Filter");
            RuntimeConfiguration.setIcons(this);
            this.setGraphic(null);
            this.setHeaderText("Filter Times");

            VBox content = new VBox(5);

            HBox entryRow = new HBox(10);
            HBox entryStart = new HBox(3);
            HBox entryEnd = new HBox(3);
            Label labelStart = new Label("Start:");
            TextField fieldStart = new TextField();
            labelStart.setLabelFor(fieldStart);
            entryStart.getChildren().addAll(labelStart, fieldStart);
            Label labelEnd = new Label("End:");
            TextField fieldEnd = new TextField();
            labelEnd.setLabelFor(fieldEnd);
            entryEnd.getChildren().addAll(labelEnd, fieldEnd);

            entryRow.getChildren().addAll(entryStart, entryEnd);

            HBox zoneSelection = new HBox(3);
            Label labelZone = new Label("Time Zone: ");
            ToggleGroup groupZone = new ToggleGroup();
            HBox radios = new HBox(12);
            RadioButton buttonUtc = new RadioButton(UTC);
            buttonUtc.setUserData(ZoneOffset.UTC);
            RadioButton buttonLocal = new RadioButton(LOCAL);
            buttonLocal.setUserData(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
            buttonUtc.setToggleGroup(groupZone);
            buttonLocal.setToggleGroup(groupZone);
            radios.getChildren().addAll(buttonUtc, buttonLocal);

            zoneSelection.getChildren().addAll(labelZone, radios);

            content.getChildren().addAll(entryRow, zoneSelection);

            this.getDialogPane().setContent(content);
            this.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            this.setOnShowing(event -> {
                Timespan span = current != null ? current : getDefaultTimespan();

                ZoneOffset offset = span.offset;
                if (offset.equals(ZoneOffset.UTC)) {
                    groupZone.selectToggle(buttonUtc);
                } else {
                    groupZone.selectToggle(buttonLocal);
                }
                if (span != null) {
                    fieldStart.setText(span.start.atOffset(offset).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    fieldEnd.setText(span.end.atOffset(offset).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                }
            });

            this.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    try {
                        LocalDateTime dateStart = LocalDateTime.parse(fieldStart.getText());
                        LocalDateTime dateEnd = LocalDateTime.parse(fieldEnd.getText());

                        ZoneOffset offset = (ZoneOffset)groupZone.getSelectedToggle().getUserData();

                        return new Timespan(dateStart.toInstant(offset), dateEnd.toInstant(offset), offset);
                    } catch (DateTimeParseException dte) {
                        Logger.log(Logger.Severity.WARNING, "Invalid date text");
                        return null;
                    }
                } else if (button == ButtonType.CANCEL) {
                    return null;
                }

                return null;
            });
        }
    }
}
