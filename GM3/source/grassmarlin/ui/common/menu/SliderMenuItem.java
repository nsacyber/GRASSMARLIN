package grassmarlin.ui.common.menu;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;

public class SliderMenuItem extends MenuItem {
    private final Slider slider;

    public SliderMenuItem(final String name, final String units, final double initial, final double minimum, final double maximum) {
        super(name);

        this.slider = new Slider(minimum, maximum, initial);
        this.setGraphic(this.slider);
        this.slider.setSnapToTicks(true);

        final double magnitude = Math.ceil(Math.log10(maximum - minimum)) - 1.0;
        slider.setMajorTickUnit(Math.pow(10, magnitude));
        slider.setMinorTickCount(10);
        slider.setShowTickMarks(true);

        this.textProperty().bind(new ReadOnlyStringWrapper(name).concat(" (").concat(slider.valueProperty()).concat(units).concat(")"));
    }

    public SliderMenuItem(final String name, final String units, final DoubleProperty value, final double minimum, final double maximum) {
        this(name, units, value, minimum, maximum, null);
    }
    public SliderMenuItem(final String name, final String units, final DoubleProperty value, final double minimum, final double maximum, final Parent target) {
        this(name, units, value.get(), minimum, maximum);

        this.slider.valueProperty().bindBidirectional(value);
        if(target != null) {
            this.slider.valueProperty().addListener(observable -> {
                target.requestLayout();
            });
        }
    }
    public SliderMenuItem(final String name, final String units, final IntegerProperty value, final int minimum, final int maximum, final Parent target) {
        this(name, units, (double)value.get(), (double)minimum, (double)maximum);

        this.slider.valueProperty().bindBidirectional(value);
        if(target != null) {
            this.slider.valueProperty().addListener(observable -> {
                target.requestLayout();
            });
        }

        this.slider.setMajorTickUnit(1.0);
        this.slider.setMinorTickCount(0);
    }
}
