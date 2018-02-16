package grassmarlin.plugins.internal.logicalview.visual.layouts;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.plugins.internal.logicalview.ILogicalVisualization;
import grassmarlin.plugins.internal.logicalview.IVisualLogicalVertex;
import grassmarlin.plugins.internal.logicalview.visual.ILogicalGraphLayout;
import grassmarlin.session.Property;
import grassmarlin.ui.common.MutablePoint;
import javafx.beans.property.*;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.*;

public class ForceDirected implements ILogicalGraphLayout {
    public ForceDirected() {
        this.velocities = new HashMap<>();
    }

    //Accessors for physics constants
    public DoubleProperty forceMultiplierProperty() {
        return this.forceMultiplier;
    }
    public DoubleProperty momentumDecayProperty() {
        return this.momentumDecay;
    }
    public DoubleProperty equilibriumDistanceProperty() {
        return this.equilibriumDistance;
    }
    public DoubleProperty groupingCoefficientProperty() {
        return this.groupingCoefficient;
    }
    public DoubleProperty groupingRepulsionCoefficientProperty() {
        return this.groupingRepulsionCoefficient;
    }
    public DoubleProperty originMassProperty() {
        return this.originMass;
    }
    public DoubleProperty minEffectiveDistanceProperty() {
        return this.minEffectiveDistance;
    }
    public DoubleProperty noiseReductionProperty() {
        return this.noiseReduction;
    }

    public BooleanProperty applyDistortionProperty() {
        return this.applyDistortion;
    }

    //Properties to allow user/programatic manipulation of the physics parameters
    private final DoubleProperty forceMultiplier = new SimpleDoubleProperty(FORCE_MULTIPLIER);
    private final DoubleProperty momentumDecay = new SimpleDoubleProperty(MOMENTUM_DECAY);
    private final DoubleProperty equilibriumDistance = new SimpleDoubleProperty(EQUILIBRIUM_DISTANCE);
    private final DoubleProperty groupingCoefficient = new SimpleDoubleProperty(GROUPING_COEFFICIENT);
    private final DoubleProperty groupingRepulsionCoefficient = new SimpleDoubleProperty(GROUPING_REPULSION_COEFFICIENT);
    private final DoubleProperty originMass = new SimpleDoubleProperty(ORIGIN_MASS);
    private final DoubleProperty minEffectiveDistance = new SimpleDoubleProperty(MIN_EFFECTIVE_DISTANCE);
    private final DoubleProperty noiseReduction = new SimpleDoubleProperty(NOISE_REDUCTION);

    private final BooleanProperty applyDistortion = new SimpleBooleanProperty(true);

    // == Physics Stuff
    public static final double FORCE_MULTIPLIER = 0.025;                //A global multiplier on all forces.
    public static final double MOMENTUM_DECAY = 0.5;                    //A multiplier applied to prior forces.
    public static final double EQUILIBRIUM_DISTANCE = 2.8;              //The (normalized by unit_scaling) distance at which the attractive and repulsive forces cancel each other.
    public static final double GROUPING_COEFFICIENT = 1.5;              //coefficient applied to the baseline attractive force for elements which are grouped together.
    public static final double GROUPING_REPULSION_COEFFICIENT = 0.0;    //This has its uses, but generally it just serves to force everything away from the origin.
    public static final double ORIGIN_MASS = 1.0 / EQUILIBRIUM_DISTANCE;
    public static final double MIN_EFFECTIVE_DISTANCE = 0.5;
    public static final double NOISE_REDUCTION = 0.01;

    // These cannot be changed
    private static final double INITIAL_SPEED_MAX = 0.75;
    private static final double INITIAL_SPEED_MIN = 0.5;

    //Simulation and timing constraints
    private static final long LIMIT_MS = 3L;
    private static final int LIMIT_ITERATIONS = 10;

    private final Map<IVisualLogicalVertex, MutablePoint> velocities;

    @Override
    public boolean requiresForcedUpdate() {
        return false;
    }

    @Override
    public <T extends Node & ILogicalVisualization> Map<IVisualLogicalVertex, MutablePoint> executeLayout(T visualization) {
        //Cache things that might change while we're running
        final Collection<? extends IVisualLogicalVertex> vertices = visualization.getVertices();
        if(vertices.isEmpty()) {
            return Collections.EMPTY_MAP;
        }
        final String grouping = visualization.getCurrentGrouping();

        //The simulation is sensitive to the aspect ratio of the viewport;  this way the vertices will float to fill the display.
        final Point2D sizeViewport;
        //As a Pane, the width and height are the space occupied by the control.  The Bounds (as a Node) are determined by the content.  We want the control, but will settle for the content if we have to.
        if(visualization instanceof Pane) {
            sizeViewport = new Point2D(((Pane)visualization).getWidth(), ((Pane)visualization).getHeight()).normalize();
        } else {
            final ObjectProperty<Bounds> boundsParent = new SimpleObjectProperty<>();
            Event.PROVIDER_JAVAFX.runNow(() -> {
                boundsParent.set(visualization.getBoundsInParent());
            });
            sizeViewport = new Point2D(boundsParent.get().getWidth(), boundsParent.get().getHeight()).normalize();
        }
        final double UNIT_SCALING_X = sizeViewport.getX() * 160.0;
        final double UNIT_SCALING_Y = sizeViewport.getY() * 160.0;
        //Other constants are pulled from properties, which allows them to be changed between frames
        final double forceMultiplier = this.forceMultiplier.get();
        final double momentumDecay = this.momentumDecay.get();
        final double equilibriumDistance = this.equilibriumDistance.get();
        final double groupingCoefficient = this.groupingCoefficient.get();
        final double groupingRepulsionCoefficient = this.groupingRepulsionCoefficient.get();
        final double originMass = this.originMass.get();
        final double minEffectiveDistance = this.minEffectiveDistance.get();
        final double noiseReduction = this.noiseReduction.get();

        final boolean applyDistortion = this.applyDistortion.get();

        final long msStartFrame = System.currentTimeMillis();
        int cntIterations = 0;
        //This loop is going to run for as many iterations fit in 3ms.  At 60Hz this is 180ms, 18% CPU

        //Calculate initial locations
        //We use the MutablePoint class to minimize memory allocations--Java doesn't seem too keen on reusing memory from frame to frame.
        //While talking about mutable points we should probably also bring up an immutable point.  Die Hard is a Christmas movie.
        final Map<IVisualLogicalVertex, MutablePoint> locations = new HashMap<>();
        for(IVisualLogicalVertex vertexToMove : vertices) {
            final MutablePoint point = new MutablePoint(vertexToMove.getTranslateX(), vertexToMove.getTranslateY());
            if(applyDistortion) {
                undistort(point);
            }
            locations.put(vertexToMove, point);
        }

        do {
            cntIterations++;


            //We don't do entity grouping, as was done in previous incarnations--we're not going to layout group members around a point because:
            // 1) Multi-group membership makes this odd
            // 2) Adding a new member to a group presents a whole new set of layout concerns.
            //Because every vertex is treated as an isolated entity, every vertex will have the same mass; this simplifies a lot of the calculations.

            for(IVisualLogicalVertex vertexToMove : vertices) {
                //We aren't going to move anything that isn't flagged to use layout
                //Normally it wouldn't matter since the layout will check the isSubjectToLayout value, but since this might run for multiple iterations, it can make a difference.
                if(!vertexToMove.isSubjectToLayout() || !Double.isFinite(vertexToMove.getTranslateX()) || !Double.isFinite(vertexToMove.getTranslateY())) {
                    //If it isn't being used in layout it has a velocity of zero--this will be important if it is subject to layout later.
                    final MutablePoint velocity = velocities.get(vertexToMove);
                    if(velocity != null) {
                        velocity.multiply(0.0);
                    }
                    continue;
                }

                //Cache the edges and group memberships.
                final Collection<GraphLogicalEdge> edgesForVertex = visualization.getGraph().getEdgesForEndpoint(vertexToMove.getVertex());
                Set<Property<?>> groups = vertexToMove.getVertex().getProperties().get(grouping);
                if(groups != null) {
                    groups = new HashSet<>(groups);
                }

                //New entries have a random default velocity that is close to but not quite (usually) zero.
                MutablePoint velocity = this.velocities.get(vertexToMove);
                if(velocity == null) {
                    velocity = new MutablePoint(Math.random() - 0.5, Math.random() - 0.5);
                    velocity.normalize();
                    velocity.multiply(Math.random() * (INITIAL_SPEED_MAX - INITIAL_SPEED_MIN) + INITIAL_SPEED_MIN);
                    this.velocities.put(vertexToMove, velocity);
                }
                velocity.multiply(momentumDecay);

                final MutablePoint locationVertex = locations.get(vertexToMove);

                for(final IVisualLogicalVertex vertexOther : vertices) {
                    if(!Double.isFinite(vertexOther.getTranslateX()) || !Double.isFinite(vertexOther.getTranslateY())) {
                        continue;
                    }

                    double forceAttraction = 0.0;
                    final MutablePoint vecForce;

                    if(vertexOther == vertexToMove) {
                        vecForce = new MutablePoint(locationVertex.getX() / UNIT_SCALING_X, locationVertex.getY() / UNIT_SCALING_Y);
                        //Calculate the magnitude before we normalize
                        forceAttraction = -originMass * Math.min(equilibriumDistance, vecForce.magnitude());
                        vecForce.normalize();
                    } else {
                        final MutablePoint locationOther = locations.get(vertexOther);
                        vecForce = new MutablePoint((locationOther.getX() - locationVertex.getX()) / UNIT_SCALING_X, (locationOther.getY() - locationVertex.getY()) / UNIT_SCALING_Y);
                        double distanceSquared = (vecForce.getX() * vecForce.getX() + vecForce.getY() * vecForce.getY());
                        vecForce.normalize();


                        if(distanceSquared < minEffectiveDistance * minEffectiveDistance) {
                            //If they are at the same point then there are no forces to apply.
                            distanceSquared = minEffectiveDistance * minEffectiveDistance;
                        }

                        // All nodes repel all other nodes
                        forceAttraction -= 1.0 / distanceSquared;
                        if(edgesForVertex.stream().anyMatch(edge -> edge.getSource() == vertexOther.getVertex() || edge.getDestination() == vertexOther.getVertex())) {
                            // The vertices are connected, so we apply an attractive force.
                            // The attractive force is the additive inverse of the general repulsive force at a fixed distance
                            forceAttraction += 1.0 / (equilibriumDistance * equilibriumDistance);
                        }

                        //Grouped nodes attract each other
                        if(groups != null) {
                            //If there are properties, check for overlaps.  Increase the strength of attraction for every shared group

                            final Set<Property<?>> groupsOther = vertexOther.getVertex().getProperties().get(grouping);
                            if(groupsOther != null) {
                                double forceGrouping = 0.0;
                                for(Property<?> property : groups) {
                                    //Attract for every matching value, repulse for every value that doesn't match.  this helps enforce boundaries between sets of properties.
                                    if(groupsOther.stream().anyMatch(prop -> prop.getValue().equals(property.getValue()))) {
                                        // The vertices share a group--this can happen multiple times.
                                        forceGrouping += groupingCoefficient / (equilibriumDistance * equilibriumDistance);
                                    } else {
                                        forceGrouping -= groupingRepulsionCoefficient / (equilibriumDistance * equilibriumDistance);
                                    }
                                }
                                forceAttraction += forceGrouping;
                            }
                        }
                    }
                    vecForce.multiply(forceAttraction * forceMultiplier);
                    velocity.add(vecForce);
                }

                //floor on the result of the scaling requires the total change to be at least 1 pixel before it causes an update.
                //This adds a level of imprecision, but reduces annoying visual jittering.
                locationVertex.add(
                        Math.floor(velocity.getX() * UNIT_SCALING_X / noiseReduction) * noiseReduction,
                        Math.floor(velocity.getY() * UNIT_SCALING_Y / noiseReduction) * noiseReduction
                );
            }
        } while(cntIterations < LIMIT_ITERATIONS && (System.currentTimeMillis() - msStartFrame) < LIMIT_MS);

        if(applyDistortion) {
            for(MutablePoint point : locations.values()) {
                distort(point);
            }
        }
        return locations;
    }

    private static void distort(final MutablePoint source) {
        final double ratio;
        if(source.getX() == 0.0 && source.getY() == 0.0) {
            ratio = 1.0;
        } else {
            if (Math.abs(source.getX()) < Math.abs(source.getY())) {
                //X has lower magnitude, which means Y will be set to 1
                ratio = 1.0 / Math.abs(source.getY() / source.magnitude());
            } else {
                ratio = 1.0 / Math.abs(source.getX() / source.magnitude());
            }
        }
        source.multiply(ratio);
    }

    private static void undistort(final MutablePoint source) {
        final double ratio;
        if(source.getX() == 0.0 && source.getY() == 0.0) {
            ratio = 1.0;
        } else {
            if (Math.abs(source.getX()) < Math.abs(source.getY())) {
                //X has lower magnitude, which means Y will be set to 1
                ratio = Math.abs(source.getY()) / source.magnitude();
            } else {
                ratio = Math.abs(source.getX()) / source.magnitude();
            }
        }
        source.multiply(ratio);
    }
}
