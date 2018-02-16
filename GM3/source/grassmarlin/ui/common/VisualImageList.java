package grassmarlin.ui.common;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.LinkedList;

public class VisualImageList extends HBox {
    private final ObservableList<Image> listImages;
    private final SimpleDoubleProperty fitHeight;

    //TODO: Physical Graph needs to be updated to use the new ImageDirectoryWatcher.
    @Deprecated
    public VisualImageList(final ImageDirectoryWatcher source) {
        this((ObservableList<Image>)null);
    }
    public VisualImageList(final ObservableList<Image> listImages) {
        this.listImages = listImages;
        this.fitHeight = new SimpleDoubleProperty(14.0);

        if(this.listImages != null) {
            this.listImages.addListener(this.handlerListChanged);
            this.handleListChanged(this.listImages);
        }
    }

    public DoubleProperty fitHeightProperty() {
        return this.fitHeight;
    }

    private InvalidationListener handlerListChanged = this::handleListChanged;
    private final void handleListChanged(Observable observable) {
        final LinkedList<ImageView> views = new LinkedList<>();

        for(final Image image : this.listImages) {
            final ImageView viewImage = new ImageView(image);
            viewImage.setPreserveRatio(true);
            viewImage.fitHeightProperty().bind(this.fitHeightProperty());

            views.add(viewImage);
        }

        if(Platform.isFxApplicationThread()) {
            this.getChildren().clear();
            this.getChildren().addAll(views);
        } else {
            Platform.runLater(() -> {
                this.getChildren().clear();
                this.getChildren().addAll(views);
            });
        }
    }
}
