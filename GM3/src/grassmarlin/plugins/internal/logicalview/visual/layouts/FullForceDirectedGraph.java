package grassmarlin.plugins.internal.logicalview.visual.layouts;

import com.sun.istack.internal.NotNull;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.common.LengthCappedList;
import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.plugins.internal.logicalview.visual.ILogicalGraphFullLayout;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalVertex;
import grassmarlin.session.Property;
import javafx.beans.binding.BooleanExpression;
import javafx.geometry.Point2D;

import java.util.*;
import java.util.stream.Collectors;

public class FullForceDirectedGraph implements ILogicalGraphFullLayout {
    /**
     * Calculates the "mass" of an Entity which contains the given set of vertices
     */
    @FunctionalInterface
    public interface MassFromVertices {
        double calculateMass(final List<VisualLogicalVertex> vertices);
    }

    /**
     * Calculates the strength of the attractive force that other exerts on entity, given the list of vertices and edges.
     */
    @FunctionalInterface
    public interface StrengthFromGraph {
        double calculateStrength(
                final List<VisualLogicalEdge> edges,
                final Entity entity,
                final Entity other);
    }

    private static class Arc {
        private final double radStart;
        private final double radEnd;
        private final double radius;
        private final Entity root;

        public Arc(final double radStart, final double radEnd, final double radius, final Entity root) {
            this.radStart = radStart;
            this.radEnd = radEnd;
            this.radius = radius;
            this.root = root;
        }

        public List<Arc> partition(final Collection<Entity> children) {
            //children should be pre-sorted.
            final double normalizationFactor = 1.0 / children.stream().mapToDouble(Entity::getEstimatedRadius).sum();
            final double span = radEnd - radStart;
            final List<Arc> result = new LinkedList<>();

            final double radius = (this.radius + 2.5 * (this.root == null ? 0.0 : this.root.getEstimatedRadius()) + children.stream().mapToDouble(Entity::getEstimatedRadius).max().orElse(0.0));

            double radOffset = radStart;
            for(final Entity child : children) {
                final double radArc = (span * normalizationFactor * child.getEstimatedRadius());
                result.add(new Arc(radOffset, radOffset + radArc, radius, child));
                radOffset += radArc;
            }

            return result;
        }
    }
    private class Entity {
        private final List<VisualLogicalVertex> vertices;
        private final double mass;
        private final double radius;
        private final Map<Entity, Double> coefficientsAttraction;
        private boolean normalized = false;

        private Point2D ptLocation = Point2D.ZERO;
        private Point2D velocity = Point2D.ZERO;

        private final String title;

        public Entity(final Object title, final List<VisualLogicalVertex> vertices, final double mass) {
            this.title = title.toString();
            this.vertices = vertices;
            this.mass = mass;

            this.coefficientsAttraction = new HashMap<>();

            // sum(perimeters) / 2pi -> radius
            // simplified to:
            // sum(width + height) / pi -> radius
            // And then adjusted by dividing by limitingCoefficient, which is proportional to the size and bounded by [1, 5]
            final double limitingCoefficient = 1.0 + 4.0 * (1.0 - 1.0 / (double)vertices.size());
            this.radius = vertices.stream().mapToDouble(vertex -> (vertex.getLayoutBounds().getWidth() + vertex.getLayoutBounds().getHeight())).sum() / (limitingCoefficient * Math.PI);
        }

        public Map<Entity, Double> getAttractionCoefficients() {
            return this.coefficientsAttraction;
        }

        public void normalizeCoefficients(final double maximum) {
            if(this.normalized) {
                throw new IllegalStateException("Normalization has already been performed.");
            }
            for(final Entity key : this.coefficientsAttraction.keySet()) {
                this.coefficientsAttraction.put(key, this.coefficientsAttraction.get(key) / maximum);
            }
            this.normalized = true;
        }

        public void applyForce(final Point2D force) {
            this.velocity = this.velocity.add(force);
        }

        public double updateLocation() {
            this.ptLocation = ptLocation.add(this.velocity);
            return this.velocity.magnitude();
        }

        public double getEstimatedRadius() {
            return this.radius;
        }

        @Override
        public String toString() {
            return String.format("Entity(%s)", this.title);
        }
    }

    private final MassFromVertices fnMass;
    private final StrengthFromGraph fnStrength;
    private final BooleanExpression animate;

    public FullForceDirectedGraph(Plugin plugin) {
        this.animate = plugin.animateLayoutsBinding();
        this.fnMass = vertices -> vertices.stream().mapToDouble(vertex -> vertex.getLayoutBounds().getWidth() * vertex.getLayoutBounds().getHeight()).sum();
        //this.fnMass = vertices -> vertices.stream().mapToDouble(vertex -> vertex.getLayoutBounds().getWidth()).sum() * vertices.stream().mapToDouble(vertex -> vertex.getLayoutBounds().getHeight()).sum();

        this.fnStrength = (edges, entity, other) -> {
            final double cntBytes = edges.stream()
                    .filter(edge ->
                            (entity.vertices.contains(edge.getSourceVertex()) && other.vertices.contains(edge.getDestinationVertex())) ||
                            (entity.vertices.contains(edge.getDestinationVertex()) && other.vertices.contains(edge.getSourceVertex())))
                    .mapToDouble(edge -> (double) edge.getEdgeData().totalPacketSizeProperty().get())
                    .sum();
            if(cntBytes <= 0.0) {
                return 0.0;
            } else {
                return Math.max(1.0, Math.log10(cntBytes));
            }
        };
    }

    private List<Entity> stepPartitionVertices(final List<VisualLogicalVertex> vertices, final String groupBy) {
        final List<Object> groups = vertices.stream()
                .map(vertex -> vertex.getVertex().getProperties().get(groupBy))
                .filter(properties -> properties != null && !properties.isEmpty())
                .flatMap(properties -> properties.stream())
                .distinct()
                .map(property -> property.getValue())
                .distinct()
                .collect(Collectors.toList());
        final List<Entity> result = new ArrayList<>();

        //Add the groups
        for(final Object group : groups) {
            final List<VisualLogicalVertex> members = vertices.stream()
                    .filter(visual -> {
                        final Set<Property<?>> propertyValues = visual.getVertex().getProperties().get(groupBy);
                        if(propertyValues != null && !propertyValues.isEmpty()) {
                            return propertyValues.stream().anyMatch(element -> element.getValue().equals(group));
                        } else {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
            result.add(new Entity(group, members, fnMass.calculateMass(members)));
        }

        //Add all remaining vertices (those that do not have a value for the groupBy)
        result.addAll(vertices.stream()
                .filter(vertex -> !vertex.getVertex().getProperties().containsKey(groupBy) || vertex.getVertex().getProperties().get(groupBy).isEmpty())
                .map(Arrays::asList)
                .map(vertexAsList -> new Entity(vertexAsList.get(0).getVertex().getRootLogicalAddressMapping(), vertexAsList, fnMass.calculateMass(vertexAsList)))
                .collect(Collectors.toList())
        );

        return result;
    }

    private void stepComputeAttractionCoefficients(final List<Entity> entities, final List<VisualLogicalEdge> edges) {
        double coefficientMax = 0.0;
        for(final Entity target : entities) {
            for(final Entity entity : entities) {
                if (target == entity) {
                    continue;
                }

                final double coefficient = fnStrength.calculateStrength(edges, target, entity);
                target.getAttractionCoefficients().put(entity, coefficient);
                coefficientMax = Math.max(coefficientMax, coefficient);
            }
        }
        Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "The normalization factor for Force Directed Layout was %f", coefficientMax);
        for(final Entity entity : entities) {
            entity.normalizeCoefficients(coefficientMax);
        }
    }

    private static int idxLayout = 0;
    @Override
    @NotNull
    public Map<VisualLogicalVertex, Point2D> layout(final String groupBy, @NotNull final List<VisualLogicalVertex> vertices, @NotNull final List<VisualLogicalEdge> edges) {
        final boolean animate = this.animate.get();
        FullForceDirectedGraph.idxLayout++;
        final List<Entity> entities;
        final Map<VisualLogicalVertex, Entity> entityLookup = new HashMap<>();

        entities = this.stepPartitionVertices(vertices, groupBy);
        for(final Entity entity : entities) {
            for(final VisualLogicalVertex vertex : entity.vertices) {
                entityLookup.put(vertex, entity);
            }
        }
        this.stepComputeAttractionCoefficients(entities, edges);

        // Initial Layout:
        final List<Arc> arcsAll = new ArrayList<>();
        final Map<Entity, Point2D> locationsInitial = new HashMap<>();
        // Partition the graph into disjoint node sets
        final List<Entity> remaining = new LinkedList<>(entities);
        final List<List<Entity>> sets = new LinkedList<>();
        while(!remaining.isEmpty()) {
            final List<Entity> set = new LinkedList<>();
            sets.add(set);

            final List<Entity> pending = new LinkedList<>();
            pending.add(remaining.get(0));

            while(!pending.isEmpty()) {
                final Entity current = pending.remove(0);
                set.add(current);
                remaining.remove(current);

                pending.addAll(current.getAttractionCoefficients().entrySet().stream().filter(entry -> entry.getValue() > 0.0 && remaining.contains(entry.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList()));
            }
        }

        // Sort sets so that the initial positions are deterministic.
        //TODO: Need tiebreaker conditions
        sets.sort((o1, o2) -> o2.size() - o1.size());
        for(final List<Entity> set : sets) {
            set.sort((o1, o2) -> o2.vertices.size() - o1.vertices.size());
        }

        // If there is a single set, place the largest node in the center and assign it an arc of [0, 2pi], otherwise distribute the nodes around the center of a circle, with calculated radius between, and assign an appropriate arc width
        if(sets.isEmpty()) {
            //Not sure how this happened, but we have no data, so we're already done.
            return null;
        } else if(sets.size() == 1) {
            //Special case: one set, layout everything around the center
            final List<Entity> entitiesAll = sets.get(0);
            // - place first element at origin
            arcsAll.add(new Arc(0.0, Math.PI * 2, 0.0, entitiesAll.remove(0)));
            // - identify all connected elements, determine arc width and center angle for each element, then recurse through them

            //We are going to be adding to arcsAll during this loop, so we can't foreach or use a similar construct; we start at index 0 and process every node in order, adding more nodes for each.
            for(int idx = 0; idx < arcsAll.size(); idx++) {
                final Arc arcCurrent = arcsAll.get(idx);
                final List<Entity> connected = arcCurrent.root.getAttractionCoefficients().entrySet().stream().filter(entry -> entry.getValue() > 0.0).map(entry -> entry.getKey()).collect(Collectors.toList());
                connected.retainAll(entitiesAll);
                entitiesAll.removeAll(connected);
                arcsAll.addAll(arcCurrent.partition(connected));
            }

            if(!entitiesAll.isEmpty()) {
                //TODO: Handle error
                throw new IllegalStateException("Unable to determine initial locations: graph is split.");
            }
        } else {
            // layout sets around center
            final Arc arcRoot = new Arc(0.0, Math.PI * 2, 0.0, null);
            // - identify all connected elements, determine arc width and center angle for each element, then recurse through them

            final Map<Entity, List<Entity>> roots = new HashMap<>();
            for(final List<Entity> set : sets) {
                roots.put(set.get(0), set);
            }
            final List<Arc> arcsRoot = arcRoot.partition(roots.keySet());

            for(final Arc rootBranch : arcsRoot) {
                final LinkedList<Entity> remainingEntitiesInArc = new LinkedList<>(roots.get(rootBranch.root));
                remainingEntitiesInArc.remove(rootBranch.root);
                final List<Arc> arcsInBranch = new LinkedList<>();
                arcsInBranch.add(rootBranch);

                for(int idxArc = 0; idxArc < arcsInBranch.size(); idxArc++) {
                    final Arc arcCurrent = arcsInBranch.get(idxArc);
                    final List<Entity> connectedEntities = arcCurrent.root.getAttractionCoefficients().entrySet().stream().filter(entry -> entry.getValue() > 0.0).map(entry -> entry.getKey()).collect(Collectors.toList());
                    connectedEntities.retainAll(remainingEntitiesInArc);
                    remainingEntitiesInArc.removeAll(connectedEntities);
                    arcsInBranch.addAll(arcCurrent.partition(connectedEntities));
                }

                arcsAll.addAll(arcsInBranch);
            }
        }

        //Assign initial positions based on contents of arcsAll
        for(final Arc arc : arcsAll) {
            arc.root.ptLocation = new Point2D(Math.cos((arc.radStart + arc.radEnd) / 2.0) * arc.radius, Math.sin((arc.radStart + arc.radEnd) / 2.0) * arc.radius);
            locationsInitial.put(arc.root, arc.root.ptLocation);
        }

        if(animate) {
            Event.PROVIDER_JAVAFX.runNow(() -> {
                for(final Entity entity : entities) {
                    for (final VisualLogicalVertex vertex : entity.vertices) {
                        vertex.dragTo(entity.ptLocation);
                    }
                }
            });
        }

        //Simulation
        //HACK: Run fixed iterations of simulation; we should be detecting minimal motion and terminating then, with a cap on iterations as a fallback.
        final double scaleForces = 0.125;
        final double scaleInterNodeRepulsion = 3.0;
        final double scaleInterNodeAttraction = 1.0;    //Attraction is also proportional to repulsion
        final double decayFactor = 0.990;
        final double equilibriumMultiplier = 2.0;   // ideal radius := multiplier/(2 - coefficient) * sum(radii)
        final double scaleCenterSeeking = 0.25;  //Multiplied by mass, which starts around 3k and gets much larger fast.

        final LengthCappedList<Double> deltaHistory = new LengthCappedList(5, Double.MAX_VALUE);
        long tsStart = System.currentTimeMillis();
        for(int idxIteration = 0; idxIteration < 3500; idxIteration++) {

            if (idxIteration % 100 == 99) {
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Starting iteration#%d", (1 + idxIteration));
            }


            // Calculate the force vector acting on each entity, for each other entity adding it to the velocity vector
            for (final Entity entity : entities) {
                for (final Entity other : entities) {
                    if (entity == other) {
                        continue;
                    }
                    //final Entity other = entryAttraction.getKey();
                    final double coefficient = entity.getAttractionCoefficients().getOrDefault(other, 0.0);
                    //by using a minimum radius we prevent some of the most extreme multipliers on forces.
                    final double distance = Math.max(entity.ptLocation.distance(other.ptLocation), (entity.getEstimatedRadius() + other.getEstimatedRadius()) / 2.0);
                    //The normalized vector that points away from entryAttraction.getKey()
                    final Point2D vRepulse = entity.ptLocation.subtract(other.ptLocation).normalize();

                    //Calculate repulsive forces (actually, we calculate the effect of that force since we apply it directly as acceleration
                    entity.velocity = entity.velocity.add(vRepulse.multiply(scaleInterNodeRepulsion * scaleForces * other.mass / (distance * distance)));

                    //Calculate attractive forces
                    //This is the additive inverse of the repulsive force at a radius of (1/coefficient) * sum(radii)
                    if (coefficient > 0.0) {
                        //final double distanceEquilibrium = (equilibriumMultiplier / (equilibriumMultiplier - coefficient)) * (entity.radius + other.radius);
                        final double distanceEquilibrium = equilibriumMultiplier * (2.0 - coefficient) * (entity.radius + other.radius);
                        entity.velocity = entity.velocity.add(vRepulse.multiply(scaleInterNodeRepulsion * scaleInterNodeAttraction * -scaleForces * other.mass / (distanceEquilibrium * distanceEquilibrium)));
                    }
                }
            }

            // Everything gets a force drawing it towards its initial location and another force drawing it towards the center
            for (final Entity entity : entities) {
                entity.velocity = entity.velocity.add(entity.ptLocation.normalize().multiply(-scaleForces * scaleCenterSeeking));
                entity.velocity = entity.velocity.multiply(decayFactor);
            }


            //Add the velocity vector to each position
            double totalDelta = 0.0;
            for (final Entity entity : entities) {
                totalDelta += entity.updateLocation();
            }
            deltaHistory.add(totalDelta);
            if(deltaHistory.stream().mapToDouble(item -> item).sum() < 0.5 * (double)entities.size()) {
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Iteration total delta was %f at termination, history total was %f (%d entities)", totalDelta, deltaHistory.stream().mapToDouble(item -> item).sum(), entities.size());
                break;
            }

            //If animated, update positions of elements
            if (animate && idxIteration % 10 == 0) {
                //The 200MB test file, ungrouped, tends to run at half this rate.
                final long msFrame = System.currentTimeMillis() - tsStart;
                if (msFrame < 8) {
                    try {
                        Thread.sleep(8 - msFrame);
                    } catch (InterruptedException ex) {
                        //Ignore
                    }
                }
                tsStart = System.currentTimeMillis();
                Event.PROVIDER_JAVAFX.runNow(() -> {
                    for (final Entity entity : entities) {
                        for (final VisualLogicalVertex vertex : entity.vertices) {
                            vertex.dragTo(entity.ptLocation);
                        }
                    }
                });
            }
        }

        //Position the elements in each entity relative to the entity's location.
        final Map<VisualLogicalVertex, Point2D> result = new HashMap<>();
        for(final Entity entity : entities) {
            final List<Point2D> vectors = new LinkedList<>();
            final double radPerLocation = Math.PI * 2.0 / (double)entity.vertices.size();
            for(int idx = 0; idx < entity.vertices.size(); idx++) {
                //Offset angle by pi/2 so that 2-element groups are arranged vertically rather than horizontally, since the width tends to be disproportionately large relative to the height.
                vectors.add(new Point2D(Math.cos(Math.PI / 2.0 + radPerLocation * (double)idx), Math.sin(Math.PI / 2.0 + radPerLocation * (double)idx)));
            }

            final List<VisualLogicalVertex> verticesOrdered = new ArrayList<>(entity.vertices);

            if(verticesOrdered.size() == 0) {
                //This entity shouldn't exist.
            } if(verticesOrdered.size() == 1) {
                result.put(verticesOrdered.get(0), entity.ptLocation);
            } else {
                verticesOrdered.sort((o1, o2) -> (int)(edges.stream().filter(edge -> edge.getSourceVertex() == o1 || edge.getDestinationVertex() == o1).count() - edges.stream().filter(edge -> edge.getSourceVertex() == o2 || edge.getDestinationVertex() == o2).count()));

                for (VisualLogicalVertex vertex : verticesOrdered) {
                    //Find everything to which this vertex connects
                    final List<Point2D> vectorsLinked = edges.stream()
                            .filter(edge -> edge.getSourceVertex() == vertex || edge.getDestinationVertex() == vertex)
                            .map(edge -> edge.getSourceVertex() == vertex ? edge.getDestinationVertex() : edge.getSourceVertex())
                            //Exclude elements within this entity...
                            .filter(endpoint -> !entity.vertices.contains(endpoint))
                            //Find the entities containing these endpoints (duplicates expected, desired)
                            .map(endpoint -> entityLookup.get(endpoint))
                            .filter(entity1 -> entity1 != null)
                            //Take the location of the entity, relative to the current entity
                            .map(entity1 -> entity1.ptLocation.subtract(entity.ptLocation))
                            .collect(Collectors.toList());
                    Point2D vecAverage = new Point2D(0, 0);
                    for(final Point2D vector : vectorsLinked) {
                        vecAverage = vecAverage.add(vector);
                    }
                    vecAverage = vecAverage.normalize();
                    Point2D vector = null;
                    double error = Double.MAX_VALUE;
                    for (Point2D vec : vectors) {
                        final double errorCurrent = Math.pow((2.0 / vec.add(vecAverage).magnitude()), 2.0);
                        if (errorCurrent < error) {
                            error = errorCurrent;
                            vector = vec;
                        }
                    }

                    if (vector != null) {
                        Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "[%s] belongs to [%s] and has selected [%s] to match an ideal of [%s]", vertex, entity, vector, vecAverage);
                        vectors.remove(vector);
                        result.put(vertex, entity.ptLocation.add(vector.multiply(entity.getEstimatedRadius())));
                    } else {
                        Logger.log(Logger.Severity.WARNING, "The Layout algorithm encountered a size mismatch when positioning group elements; some elements may be stacked.");
                        result.put(vertex, entity.ptLocation);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Force-Directed Graph";
    }
}
