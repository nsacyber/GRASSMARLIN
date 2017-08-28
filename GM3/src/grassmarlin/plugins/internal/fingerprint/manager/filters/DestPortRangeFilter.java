package grassmarlin.plugins.internal.fingerprint.manager.filters;

import core.fingerprint3.Fingerprint;
import core.fingerprint3.ObjectFactory;
import grassmarlin.plugins.internal.fingerprint.manager.FingerPrintGui;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;

public class DestPortRangeFilter implements Filter<Fingerprint.Filter.DstPortRange> {
    private ObjectFactory factory;
    private Fingerprint.Filter.DstPortRange value;
    private SimpleObjectProperty<JAXBElement<Fingerprint.Filter.DstPortRange>> element;

    public DestPortRangeFilter(JAXBElement<Fingerprint.Filter.DstPortRange> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        if (null == value) {
            this.value = new Fingerprint.Filter.DstPortRange();
            this.value.setMin(new BigInteger("0"));
            this.value.setMax(new BigInteger("0"));
            element.setValue(factory.createFingerprintFilterDstPortRange(this.value));
        } else {
            this.value = value.getValue();
            element.setValue(value);
        }
    }

    public DestPortRangeFilter() {
        this(null);
    }

    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label maxLabel = new Label("Max:");
        Label minLabel = new Label("Min:");

        TextField maxField = new TextField(value.getMax().toString());
        TextField minField = new TextField(value.getMin().toString());

        maxField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    BigInteger newMax = new BigInteger(newValue);
                    value.setMax(newMax);
                    element.setValue(factory.createFingerprintFilterDstPortRange(value));
                } catch (NumberFormatException e) {
                    if (maxField.getText().isEmpty()) {
                        maxField.setText("0");
                        FingerPrintGui.selectAll(maxField);
                    } else {
                        maxField.setText(oldValue);
                    }
                }
            }
        });

        maxField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(maxField);
            }
        });

        minField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                //don't allow wrong entries
                try {
                    BigInteger newMin = new BigInteger(newValue);
                    value.setMin(newMin);
                    element.setValue(factory.createFingerprintFilterDstPortRange(value));
                } catch (NumberFormatException e) {
                    if (minField.getText().isEmpty()) {
                        minField.setText("0");
                        FingerPrintGui.selectAll(minField);
                    } else {
                        minField.setText(oldValue);
                    }
                }
            }
        });

        minField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                FingerPrintGui.selectAll(minField);
            }
        });

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(2);
        input.getChildren().addAll(minLabel, minField, maxLabel, maxField);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.DSTPORTRANGE;
    }

    @Override
    public ObjectProperty<JAXBElement<Fingerprint.Filter.DstPortRange>> elementProperty() {
        return this.element;
    }
}
