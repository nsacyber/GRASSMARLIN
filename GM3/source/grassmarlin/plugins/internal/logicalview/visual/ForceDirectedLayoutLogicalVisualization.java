package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.ViewLogical;
import grassmarlin.plugins.internal.logicalview.visual.layouts.ForceDirected;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.BoundCheckMenuItem;
import grassmarlin.ui.common.menu.SliderMenuItem;
import javafx.scene.control.Menu;

/**
 * Truly, the creation of this class was the will of the midichlorians.
 */
public class ForceDirectedLayoutLogicalVisualization extends LogicalVisualization {
    private final ForceDirected layout;

    public ForceDirectedLayoutLogicalVisualization(final ViewLogical view) {
        super(view);

        this.layout = new ForceDirected();

        this.visualizationMenuItems.add(
                new Menu("Force-Directed Layout") {
                    {
                        this.getItems().addAll(
                                new SliderMenuItem("Global Force Multiplier", "", ForceDirectedLayoutLogicalVisualization.this.layout.forceMultiplierProperty(), 0.0, 0.1, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Momentum Persistence", "", ForceDirectedLayoutLogicalVisualization.this.layout.momentumDecayProperty(), 0.0, 1.0, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Equilibrium Distance", "", ForceDirectedLayoutLogicalVisualization.this.layout.equilibriumDistanceProperty(), 0.01, 5.0, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Group Cohesion", "", ForceDirectedLayoutLogicalVisualization.this.layout.groupingCoefficientProperty(), 0.0, 5.0, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Group Repulsion", "", ForceDirectedLayoutLogicalVisualization.this.layout.groupingRepulsionCoefficientProperty(), 0.0, 1.0, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Origin Mass", "", ForceDirectedLayoutLogicalVisualization.this.layout.originMassProperty(), 0.0, 10.0, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Minimum Effective Distance", "", ForceDirectedLayoutLogicalVisualization.this.layout.minEffectiveDistanceProperty(), 0.1, 2.0, ForceDirectedLayoutLogicalVisualization.this),
                                new SliderMenuItem("Noise Reduction", "", ForceDirectedLayoutLogicalVisualization.this.layout.noiseReductionProperty(), 0.0001, 1.0, ForceDirectedLayoutLogicalVisualization.this),
                                new BoundCheckMenuItem("Perspective Distortion", ForceDirectedLayoutLogicalVisualization.this.layout.applyDistortionProperty()),
                                new ActiveMenuItem("Reset", event -> {
                                    ForceDirectedLayoutLogicalVisualization.this.layout.forceMultiplierProperty().set(ForceDirected.FORCE_MULTIPLIER);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.momentumDecayProperty().set(ForceDirected.MOMENTUM_DECAY);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.equilibriumDistanceProperty().set(ForceDirected.EQUILIBRIUM_DISTANCE);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.groupingCoefficientProperty().set(ForceDirected.GROUPING_COEFFICIENT);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.groupingRepulsionCoefficientProperty().set(ForceDirected.GROUPING_REPULSION_COEFFICIENT);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.originMassProperty().set(ForceDirected.ORIGIN_MASS);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.minEffectiveDistanceProperty().set(ForceDirected.MIN_EFFECTIVE_DISTANCE);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.noiseReductionProperty().set(ForceDirected.NOISE_REDUCTION);
                                    ForceDirectedLayoutLogicalVisualization.this.layout.applyDistortionProperty().set(true);
                                    ForceDirectedLayoutLogicalVisualization.this.requestLayout();
                                })

                        );
                    }
                }
        );

        this.layoutProperty.set(this.layout);
    }

    @Override
    public void cleanup() {
        //This probably isn't necessary but it certainly isn't harmful.
        this.layoutProperty.set(null);
    }
}
