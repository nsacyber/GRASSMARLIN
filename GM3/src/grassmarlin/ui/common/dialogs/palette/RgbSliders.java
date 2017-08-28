package grassmarlin.ui.common.dialogs.palette;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

public class RgbSliders extends GridPane {
    private final ObjectProperty<Color> color;

    private final Slider r;
    private final Slider g;
    private final Slider b;

    private boolean enableSliderHandlers;

    public RgbSliders(final ObjectProperty<Color> color) {
        this.color = color;

        this.r = new Slider();
        this.g = new Slider();
        this.b = new Slider();

        this.enableSliderHandlers = true;

        initComponents();
    }

    private void initComponents() {
        r.setMin(0.0);
        r.setMax(1.0);
        r.setValue(color.get().getRed());
        r.valueProperty().addListener(this::Handle_sliderChanged);

        g.setMin(0.0);
        g.setMax(1.0);
        g.setValue(color.get().getGreen());
        g.valueProperty().addListener(this::Handle_sliderChanged);

        b.setMin(0.0);
        b.setMax(1.0);
        b.setValue(color.get().getBlue());
        b.valueProperty().addListener(this::Handle_sliderChanged);

        this.add(new Text("R"), 0, 0);
        this.add(r, 1, 0);
        this.add(new Text("G"), 0, 1);
        this.add(g, 1, 1);
        this.add(new Text("B"), 0, 2);
        this.add(b, 1, 2);

        this.color.addListener(this::Handle_colorChanged);
    }

    private void Handle_sliderChanged(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
        if(enableSliderHandlers) {
            color.set(Color.rgb((int)(255.0 * r.getValue()), (int)(255.0 * g.getValue()), (int)(255.0 * b.getValue())));
        }
    }

    private void Handle_colorChanged(final ObservableValue<? extends Color> observable, final Color oldValue, final Color newValue) {
        enableSliderHandlers = false;

        r.setValue(newValue.getRed());
        g.setValue(newValue.getGreen());
        b.setValue(newValue.getBlue());

        enableSliderHandlers = true;
    }

    public ObjectProperty<Color> selectedColorProperty() {
        return this.color;
    }
}
