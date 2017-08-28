package grassmarlin.ui.common.dialogs.palette;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.HashMap;

public class ColorSelections extends GridPane {
    protected class Sample extends Rectangle {
        private final SimpleObjectProperty<Color> color;

        public Sample() {
            this(Color.WHITE);
        }
        public Sample(final Color color) {
            super(24.0, 16.0);

            this.color = new SimpleObjectProperty<>(color);
            this.fillProperty().bind(this.color);
            this.setStrokeWidth(1.0);
            this.setStroke(Color.BLACK);
        }

        public ObjectProperty<Color> colorProperty() {
            return this.color;
        }
        public Color getColor() {
            return this.color.get();
        }
        public void setColor(final Color color) {
            this.color.set(color);
        }
    }

    private final ObservableList<Color> samples;
    private final HashMap<Color, Sample> mapSamples;
    private final SimpleObjectProperty<Color> selectedColor;

    private int idxNext = 0;
    private final int cntCols;

    public ColorSelections() {
        this(1, 8);
    }
    public ColorSelections(final int cntRows, final int cntCols, final Color... colors) {
        this.cntCols = cntCols;
        this.setPrefWidth(32.0 * cntCols);
        this.setPrefHeight(24.0 * cntRows);

        this.setPadding(new Insets(4.0, 4.0, 4.0, 4.0));
        this.setHgap(8.0);
        this.setVgap(8.0);

        selectedColor = new SimpleObjectProperty<>();

        samples = new ObservableListWrapper<>(new ArrayList<>());
        samples.addListener(this::Handle_SampleListChange);

        mapSamples = new HashMap<>();

        samples.addAll(colors);
    }

    private void Handle_SampleListChange(final ListChangeListener.Change<? extends Color> change) {
        while(change.next()) {
            change.getRemoved().forEach(colorRemoved -> {
                this.getChildren().remove(mapSamples.get(colorRemoved));
            });
            change.getAddedSubList().forEach(colorAdded -> {
                if(!mapSamples.containsKey(colorAdded)) {
                    final Sample sampleNew = new Sample(colorAdded);
                    addSample(sampleNew);
                    mapSamples.put(colorAdded, sampleNew);
                }
            });
        }
    }

    private void addSample(final Sample sampleNew) {
        super.add(sampleNew, idxNext % cntCols, idxNext / cntCols);
        sampleNew.setOnMouseClicked(event -> {
            selectedColor.set(sampleNew.getColor());
        });
        idxNext++;
    }

    public ObjectProperty<Color> selectedColorProperty() {
        return selectedColor;
    }
    public ObservableList<Color> getColors() {
        return samples;
    }
}
