package grassmarlin.ui.common;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.LinkedList;

public class VisualImageList extends HBox {
    private final ImageDirectoryWatcher<Image>.MappedImageList listImages;
    private final SimpleDoubleProperty fitHeight;

    public VisualImageList(final ImageDirectoryWatcher<Image> source) {
        this(source.new MappedImageList());
    }
    public VisualImageList(final ImageDirectoryWatcher.MappedImageList listImages) {
        this.listImages = listImages;
        this.fitHeight = new SimpleDoubleProperty(14.0);

        this.listImages.addListener(this.handlerListChanged);
        this.handleListChanged(this.listImages);
    }

    public DoubleProperty fitHeightProperty() {
        return this.fitHeight;
    }

    private InvalidationListener handlerListChanged = this::handleListChanged;
    private final void handleListChanged(Observable observable) {
        final LinkedList<ImageView> views = new LinkedList<>();

        for(final Image image : this.listImages.getFilteredImageList()) {
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

    public ImageDirectoryWatcher<Image>.MappedImageList getImageList() {
        return this.listImages;
    }
}
