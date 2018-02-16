package grassmarlin.plugins.internal.physical.view;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.Serializable;

public interface IPhysicalViewApi {
    String PROPERTY_PORT = "Physical Interface";
    String PROPERTY_PORT_POSITION_X = "PositionX";
    String PROPERTY_PORT_POSITION_Y = "PositionY";
    String PROPERTY_PORT_CONTROL_ANGLE = "Control Angle";

    final class ImageProperties {
        private final Image image;
        private final Rectangle2D viewport;

        public ImageProperties(final Image image) {
            this(image, null);
        }
        public ImageProperties(final Image image, final Rectangle2D viewport) {
            this.image = image;
            this.viewport = viewport;
        }

        public void apply(final ImageView view) {
            view.setImage(this.image);
            view.setViewport(this.viewport);
        }
    }

    void addPortImage(final ImageProperties proeprties, final Serializable valuePortProperty);
}
