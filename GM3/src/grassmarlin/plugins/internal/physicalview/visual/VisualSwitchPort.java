package grassmarlin.plugins.internal.physicalview.visual;

import com.sun.javafx.geom.BaseBounds;
import grassmarlin.plugins.internal.physicalview.graph.Switch;
import grassmarlin.ui.common.NodeOffsetBinding;
import grassmarlin.ui.common.TextMeasurer;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

public class VisualSwitchPort extends VBox implements IHasControlPoint {
    private static final Image imgPorts = new Image(VisualSwitchPort.class.getResourceAsStream("/resources/images/PhysicalPort.png"));
    private static final Rectangle2D rectUprightConnected = new Rectangle2D(0.5, 0.5, 47.0, 47.0);
    private static final Rectangle2D rectUprightDisconnected = new Rectangle2D(48.5, 0.5, 47.0, 47.0);
    private static final Rectangle2D rectInvertedConnected = new Rectangle2D(0.5, 48.5, 47.0, 47.0);
    private static final Rectangle2D rectInvertedDisconnected = new Rectangle2D(48.5, 48.5, 47.0, 47.0);

    protected static final double SIZE_PORT = 36.0; //Width
    protected static final double ASPECT_RATIO = 1.3; //Width / height

    private final BooleanProperty inverted;

    private final Switch.Port port;
    private final VisualSwitch owner;

    public VisualSwitchPort(final VisualSwitch owner, final Switch.Port port, final Switch.PortVisualSettings settings) {
        this.owner = owner;
        this.port = port;
        this.inverted = new SimpleBooleanProperty(settings.isInverted());

        final Text textMacAddress = new Text();
        final Text textName = new Text();
        final ImageView portImage = new ImageView(imgPorts);
        portImage.setFitWidth(SIZE_PORT);
        portImage.setFitHeight(SIZE_PORT / ASPECT_RATIO);

        if(inverted.get()) {
            if(port.isEnabled()) {
                portImage.setViewport(rectInvertedConnected);
            } else {
                portImage.setViewport(rectInvertedDisconnected);
            }
        } else {
            if(port.isEnabled()) {
                portImage.setViewport(rectUprightConnected);
            } else {
                portImage.setViewport(rectUprightDisconnected);
            }
        }

        //TODO: Try subtracting a constant from scaling and remove the floor call.
        textName.setText(port.getName());
        textName.setFontSmoothingType(FontSmoothingType.LCD);
        final BaseBounds boundsName = TextMeasurer.measureText(textName.getText(), textName.getFont());
        final double scalingName = Math.min(
                SIZE_PORT / boundsName.getWidth(),
                1.0
        );
        if(scalingName < 1.0) {
            //textName.getTransforms().add(new Scale(scalingName, scalingName));
            textName.setFont(Font.font(textName.getFont().getFamily(), Math.floor(textName.getFont().getSize() * scalingName)));
            textName.setBoundsType(TextBoundsType.VISUAL);
        }

        textMacAddress.setText(port.getAddress().toString());
        textMacAddress.setFontSmoothingType(FontSmoothingType.LCD);
        final BaseBounds boundsMac = TextMeasurer.measureText(textMacAddress.getText(), textMacAddress.getFont());
        final double scalingMac = Math.min(
                SIZE_PORT / boundsMac.getWidth(),
                1.0
        );
        if(scalingMac < 1.0) {
            textMacAddress.setFont(Font.font(textMacAddress.getFont().getFamily(), Math.floor(textMacAddress.getFont().getSize() * scalingMac)));
            textMacAddress.setBoundsType(TextBoundsType.VISUAL);
        }

        if(this.inverted.get()) {
            this.getChildren().addAll(portImage, textName, textMacAddress);
        } else {
            this.getChildren().addAll(textMacAddress, textName, portImage);
        }
    }

    private NodeOffsetBinding offsetBinding = null;

    @Override
    public DoubleExpression getTerminalX() {
        if(this.offsetBinding == null) {
            this.offsetBinding = new NodeOffsetBinding(this, this.owner);
        }
        return this.offsetBinding.getX().add(this.widthProperty().divide(2.0)).add(this.owner.translateXProperty());
    }
    @Override
    public DoubleExpression getTerminalY() {
        if(this.offsetBinding == null) {
            this.offsetBinding = new NodeOffsetBinding(this, this.owner);
        }
        return this.offsetBinding.getY().add(this.heightProperty().divide(2.0)).add(this.owner.translateYProperty());
    }
    @Override
    public DoubleExpression getControlX() {
        return getTerminalX();
    }
    @Override
    public DoubleExpression getControlY() {
        if(inverted.get()) {
            return getTerminalY().add(this.heightProperty().multiply(2.0));
        } else {
            return getTerminalY().add(this.heightProperty().multiply(-2.0));
        }
    }

}
