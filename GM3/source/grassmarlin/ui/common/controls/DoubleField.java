package grassmarlin.ui.common.controls;

import javafx.beans.property.DoubleProperty;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.converter.DoubleStringConverter;

public class DoubleField extends TextField {
    private final double min;
    private final double max;
    private final DoubleProperty value;

    public DoubleField(final double min, final double max, final double initial) {
        super();

        this.min = min;
        this.max = max;

        //TODO: Enforce min < max, initial >= min, initial <= max

        final TextFormatter<Double> formatter = new TextFormatter<>(new DoubleStringConverter() {
            @Override
            public Double fromString(final String value) {
                if(value.equals(".")) {
                    return 0.0;
                } else {
                    return super.fromString(value);
                }
            }
        }, initial, change -> {
            try {
                //If setting to ".", replace with "0." instead
                final double valueNew;
                if(change.getControlNewText().equals(".")) {
                    valueNew = 0.0;
                } else {
                    valueNew = Double.parseDouble(change.getControlNewText());
                }

                //Don't bother with checks if we are only updating the selection
                if(change.getRangeStart() != change.getRangeEnd() || !change.getText().equals("")) {
                    if(valueNew < DoubleField.this.min) {
                        change.setRange(0, change.getControlText().length());
                        change.setText(Double.toString(DoubleField.this.min));
                        change.setAnchor(0);
                        change.setCaretPosition(Double.toString(DoubleField.this.min).length());
                    } else if(valueNew > DoubleField.this.max) {
                        change.setRange(0, change.getControlText().length());
                        change.setText(Double.toString(DoubleField.this.max));
                        change.setAnchor(0);
                        change.setCaretPosition(Double.toString(DoubleField.this.max).length());
                    }
                }
                return change;
            } catch(NumberFormatException ex) {
                return null;
            }
        });
        this.value = DoubleProperty.doubleProperty(formatter.valueProperty());
        this.setTextFormatter(formatter);

        super.setText(Double.toString(initial));
    }

    public DoubleProperty valueProperty() {
        return this.value;
    }
}
