package iadgov.visualpipeline;

import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.NodeOffsetBinding;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

public class VisualPipelineEntry extends HBox implements IDraggable {
    private final NodeOffsetBinding linkPoint;

    public VisualPipelineEntry(final String title) {
        super(2.0);

        final Circle link = new Circle(4.0, Color.GREEN);
        final Text textTitle = new Text(title);
        textTitle.setFill(VisualTab.VISUAL_ELEMENT_TITLE_TEXT_FILL);
        this.getChildren().addAll(textTitle, link);
        link.translateYProperty().bind(this.heightProperty().subtract((link).radiusProperty().multiply(2.0)).divide(2.0));

        this.setBackground(new Background(new BackgroundFill(VisualTab.VISUAL_ELEMENT_TITLE_FILL, VisualTab.VISUAL_ELEMENT_CORNER_RADII, null)));
        this.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, VisualTab.VISUAL_ELEMENT_CORNER_RADII, new BorderWidths(1.0))));

        this.linkPoint = new NodeOffsetBinding(link, this);

        this.makeDraggable(true);
    }

    public NodeOffsetBinding getLinkPoint() {
        return this.linkPoint;
    }
}
