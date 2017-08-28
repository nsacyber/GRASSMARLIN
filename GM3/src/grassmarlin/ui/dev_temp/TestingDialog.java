package grassmarlin.ui.dev_temp;

import grassmarlin.ui.pipeline.Linkable;
import grassmarlin.ui.pipeline.LinkingPane;
import grassmarlin.ui.pipeline.LinkingTriangle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Path;
import javafx.util.StringConverter;

public class TestingDialog extends Dialog<ButtonType> {
    final LinkingTriangle triangleBase;
    final Linkable placebo = new Linkable() {
        private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);

        @Override
        public String getName() {
            return "Placebo--Clinically shown to be 10% more effective than Homeopathy.";
        }

        @Override
        public void setSelected(boolean selected) {
            this.selected.set(selected);
        }

        @Override
        public boolean isSelected() {
            return this.selected.get();
        }

        @Override
        public BooleanProperty getSelectedProperty() {
            return this.selected;
        }

        @Override
        public ObservableNumberValue getLinkXProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getLinkYProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getLinkControlXProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getLinkControlYProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getSourceXProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getSourceYProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getSourceControlXProperty() {
            return null;
        }

        @Override
        public ObservableNumberValue getSourceControlYProperty() {
            return null;
        }

        @Override
        public LinkingPane getLinkingPane() {
            return null;
        }
    };

    public TestingDialog() {
        this.triangleBase = null;

        this.initComponents();
    }

    private void initComponents() {
        final Pane content = new StackPane();
        this.setResizable(true);

        final Path path = new Path(new CubicCurveTo(100, 0, 100, 100, 0, 100));
        content.getChildren().add(path);



        //FilteredComboBox<String> comboBox = new FilteredComboBox<>(Arrays.asList("About", "Hunting", "abous", "Acedt", "Indigo", "Aboot", "George", "aboat", "Fred"), true, new Converter());
        //content.getChildren().addAll(comboBox);

        this.getDialogPane().setContent(content);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private class Converter extends StringConverter<String> {

        @Override
        public String toString(String string) {
            return string;
        }

        @Override
        public String fromString(String string) {
            return string;
        }
    }
}
