package iadgov.fingerprint.manager.filters;

import core.fingerprint3.ObjectFactory;
import grassmarlin.session.pipeline.ITcpPacketMetadata;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FlagsFilter implements Filter<String> {

    private ObjectFactory factory;
    private List<ITcpPacketMetadata.TcpFlags> flags;
    private SimpleObjectProperty<JAXBElement<String>> element;

    public FlagsFilter(JAXBElement<String> value) {
        factory = new ObjectFactory();
        element = new SimpleObjectProperty<>();
        flags = new ArrayList<>();
        if (null == value) {
            element.setValue(factory.createFingerprintFilterFlags(String.join(" ", flags.stream().map(flag -> flag.name()).collect(Collectors.toList()))));
        } else {
            flags.addAll(Arrays.stream(value.getValue().split(" ")).map(name -> ITcpPacketMetadata.TcpFlags.valueOf(name)).collect(Collectors.toList()));
            element.setValue(value);
        }
    }

    public FlagsFilter() {
        this(null);
    }

    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Label flagLabel = new Label("Flags:");

        HBox checks = new HBox(10);
        checks.setAlignment(Pos.CENTER);

        for (ITcpPacketMetadata.TcpFlags flag : ITcpPacketMetadata.TcpFlags.values()) {
            CheckBox check = new CheckBox(flag.name());
            check.setId(flag.name());

            check.setTooltip(new Tooltip("The " + flag.name() + " flag is set in the tcp header"));

            if (flags.contains(flag)) {
                check.setSelected(true);
            }
            check.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (check.isSelected()) {
                    flags.add(ITcpPacketMetadata.TcpFlags.valueOf(check.getId()));
                } else {
                    flags.remove(ITcpPacketMetadata.TcpFlags.valueOf(check.getId()));
                }

                element.setValue(factory.createFingerprintFilterFlags(String.join(" ", flags.stream().map(checked -> checked.name()).collect(Collectors.toList()))));
            });

            checks.getChildren().add(check);
        }

        input.setAlignment(Pos.CENTER_RIGHT);
        input.setSpacing(8);
        input.getChildren().addAll(flagLabel, checks);

        return input;
    }

    @Override
    public FilterType getType() {
        return FilterType.FLAGS;
    }

    @Override
    public SimpleObjectProperty<JAXBElement<String>> elementProperty() {
        return element;
    }
}
