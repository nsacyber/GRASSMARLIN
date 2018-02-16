package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.plugins.internal.physical.view.Plugin;
import grassmarlin.plugins.internal.physical.view.data.PhysicalDevice;
import grassmarlin.plugins.internal.physical.view.data.PhysicalGraph;
import grassmarlin.ui.common.MutablePoint;
import javafx.scene.Node;

import java.util.*;

public class ForceDirectedPhysicalVisualization extends PhysicalVisualization implements IPhysicalGraphLayout {
    public static final int LIMIT_ITERATIONS = 10;
    public static final long LIMIT_MS = 3;

    protected static class ElementProperties {
        private final MutablePoint velocity;
        private final MutablePoint location;
        private double weight;
        private boolean useLayoutThisFrame;

        public ElementProperties(final double minSpeed, final double maxSpeed) {
            //The initial velocity is a random direction with a magnitude between minSpeed and maxSpeed.
            //We start with a random vector, then normalized it.
            this.velocity = new MutablePoint(Math.random(), Math.random());
            this.velocity.normalize();
            //Save the X and Y cmponents of the normalized vector for adding the base speed.
            final double normalizedX = this.velocity.getX();
            final double normalizedY = this.velocity.getY();
            //Multiply by the range, then add the minimum component.
            this.velocity.multiply(maxSpeed - minSpeed);
            this.velocity.add(normalizedX * minSpeed, normalizedY * minSpeed);

            this.location = new MutablePoint(0.0, 0.0);

            //The default weight is 1; when an endpoint belongs to a device, the weight will be reduced as the number of ports increases.
            this.weight = 1.0;
            this.useLayoutThisFrame = false;
        }

        public void setLocation(final double worldX, final double worldY) {
            //TODO: Translate world coordinates to simulation coordinates using a more meaningful transformation. (the inverse fisheye that the logical graph does is probably meaningless for this, but the aspect ratio mght be relevant, as would adjusting the scaling based on UI scaling)
            final double simX = worldX / 50.0;
            final double simY = worldY / 50.0;
            /* Maxis was far ahead of their time, releasing SimLife as a pair of full-price games,
               SimX and SimY--a move that left the series in obscurity until the combined release.
               This wouldn't be repeated for almost 20 years, not until StarCraft 2, a game which
               is eerily similar to how most sessions of SimLife played out.
               Well, perhaps that isn't quite how it happened, but it makes a good story. */
            this.location.set(simX, simY);
        }

        /**
         * This method operates with side effets; it returns the internal location after performing a transformation from sim to world coordinates.  This transformation modifies the internal state.
         */
        public MutablePoint getWorldCoordinates() {
            this.location.multiply(50.0);
            return this.location;
        }
    }

    private final Map<IPhysicalElement, ElementProperties> lookupElementProperties;

    public ForceDirectedPhysicalVisualization(final Plugin plugin, final PhysicalGraph graph) {
        super(plugin, graph);

        this.lookupElementProperties = new HashMap<>();
    }

    @Override
    public boolean requiresForcedUpdate() {
        return false;
    }

    @Override
    public <T extends Node & IPhysicalVisualization> Map<IPhysicalElement, MutablePoint> executeLayout(T visualization) {
        //Initialize constants that should, eventually, be bound to sliders.
        final double globalForceMultiplier = 0.025;
        final double minNewHostSpeed = 0.05;
        final double maxNewHostSpeed = 0.15;
        final double minNewDeviceSpeed = 0.5;
        final double maxNewDeviceSpeed = 1.5;
        final double velocityDecayRate = 0.75;
        final double forceCenterSeeking = 0.25;
        final double coefficientControlForce = 0.1;    // This would be a great name for a band.
        final double minEffectiveDistanceSquared = 0.25;    //Treat min effective distance as 0.5
        final double coefficientRepulsion = 1.0;
        final double defaultEquilibriumDistance = 1.0;      //If not specified on the edge, this is the default that will be used.


        final long msStartTime = System.currentTimeMillis();
        int cntIterations = 0;
        //TODO: Get list of endpoints (that is one simulation)
        final Collection<IPhysicalElement> hosts = visualization.getHosts();
        final Collection<IPhysicalElement> ports = visualization.getPorts();
        final Collection<IPhysicalElement> devices = visualization.getDevices();
        final Collection<VisualWire> wires = visualization.getWires();
        //Remove all elements not in the hosts and ports sets.
        for(final Iterator<Map.Entry<IPhysicalElement, ElementProperties>> i = this.lookupElementProperties.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry<IPhysicalElement, ElementProperties> entry = i.next();
            if(!hosts.contains(entry.getKey()) && !ports.contains(entry.getKey()) && !devices.contains(entry.getKey())) {
                i.remove();
            }
        }

        //Hosts and Devices will be created with random velocities but ports won't be moved, so they start with a zero velocity (we track them for the weighting, and because we need intermediate location information (based on port movement) to affect other endpoints).
        final HashSet<IPhysicalElement> hostsToAdd = new HashSet<>(hosts);
        hostsToAdd.removeAll(this.lookupElementProperties.keySet());
        for(final IPhysicalElement host : hostsToAdd) {
            this.lookupElementProperties.put(host, new ElementProperties(minNewHostSpeed, maxNewHostSpeed));
        }
        final HashSet<IPhysicalElement> portsToAdd = new HashSet<>(ports);
        portsToAdd.removeAll(this.lookupElementProperties.keySet());
        for(final IPhysicalElement port : portsToAdd) {
            this.lookupElementProperties.put(port, new ElementProperties(0.0, 0.0));
        }
        final HashSet<IPhysicalElement> devicesToAdd = new HashSet<>(devices);
        devicesToAdd.removeAll(this.lookupElementProperties.keySet());
        for(final IPhysicalElement device : devicesToAdd) {
            this.lookupElementProperties.put(device, new ElementProperties(minNewDeviceSpeed, maxNewDeviceSpeed));
        }

        // Recompute the location and weight values for each object, as appropriate.
        for(final Map.Entry<IPhysicalElement, ElementProperties> entry : this.lookupElementProperties.entrySet()) {
            //Devices are not VisualEndpoints and only Ports have a non-null getOwner()
            if(entry.getKey() instanceof VisualEndpoint) {
                final PhysicalDevice deviceOwner = ((VisualEndpoint) entry.getKey()).getEndpoint().getOwner();
                if (deviceOwner != null) {
                    entry.getValue().weight = 1.0 / deviceOwner.getPorts().size();
                    //We also force the velocity to 0 in case there was a non-zero velocity before this element became a port.
                    entry.getValue().velocity.set(0.0, 0.0);
                }
            }

            //System.out.println(String.format("Element %s at (%f, %f).", entry.getKey(), entry.getKey().translateXProperty().get(), entry.getKey().translateYProperty().get()));
            entry.getValue().setLocation(entry.getKey().translateXProperty().get(), entry.getKey().translateYProperty().get());
            entry.getValue().useLayoutThisFrame = entry.getKey().isSubjectToLayoutProperty().get();
        }

        do {
            /*  This method, like nearly all the GrassMarlin code, was written in an open-floorplan office.
                If this seems like an irrelevant detail to you, imagine for a moment that you're trying to
               count the number of marbles on a table, but you can't touch or otherwise manipulate the
               marbles.  Fortunately, after a little while of rolling around, they come to rest, at which
               point you'd probably mentally associate them as groups and count each group individually,
               then double-check your work for good measure.
                That is what having a private office is like (how I miss those days).
                Working in an open-floorplan office is like that, except every other person in the office is
               tasked with moving the marbles around, each person in their own way.  When responsibilities and
               tasking are similar, the resulting arrangement of marbles is similar, so a small open-floorplan
               office where everyone is working on the same task is what buzzwordy people call "collaborative"
               where they can actually make your job easier.  Working in *this* office, however, has no such
               harmony--everybody is working on something different and most of them (at least today) are *loud*,
               which is the mental equivalent of counting by sight while blindfolded.  Suffice to say that
               establishing a mental model of the physics behind this simulation, translating that to math, ensuring
               that the results of the simulation match the desired outcome, and translating that to code is slightly
               more mentally involved than counting marbles.
                I honestly expect there to be a ton of errors in this code that go unnoticed because the end result
               is close enough to right that nobody (myself included) bothers to delve into it.
            */

            //We enumerate only hosts here because devices are a separate simulation and ports won't be moved.
            //This is calculating the force on each host that every other endpoint exerts, as well as the center-seeking force (which is handled when the endpoint enumeration hits this host).
            for(IPhysicalElement host : hosts) {
                //If this host isn't going to update its position, don't update the velocity, either.
                if(!this.lookupElementProperties.get(host).useLayoutThisFrame) {
                    continue;
                }
                final MutablePoint locationSelf = this.lookupElementProperties.get(host).location;
                for(final Map.Entry<IPhysicalElement, ElementProperties> entry : this.lookupElementProperties.entrySet()) {
                    if(entry.getKey() == host) {
                        // Calculate center-seeking force.
                        final double magnitude = entry.getValue().location.magnitude();
                        if(magnitude != 0.0) {
                            //If the magnitude is 0, then we're already at the center.
                            entry.getValue().velocity.add(
                                    globalForceMultiplier * entry.getValue().location.getX() / magnitude * -forceCenterSeeking,
                                    globalForceMultiplier * entry.getValue().location.getY() / magnitude * -forceCenterSeeking);
                        }
                    } else {
                        // Calculate repulsive force
                        final MutablePoint locationOther = this.lookupElementProperties.get(entry.getKey()).location;
                        final double dX = locationSelf.getX() - locationOther.getX();
                        final double dY = locationSelf.getY() - locationOther.getY();
                        final double distance = Math.sqrt(dX*dX + dY*dY);

                        if(distance == 0.0) {
                            //If the elements are on top of each other, apply no force.
                        } else {
                            this.lookupElementProperties.get(host).velocity.add(
                                    globalForceMultiplier * this.lookupElementProperties.get(entry.getKey()).weight * (dX / distance) * coefficientRepulsion / Math.max(distance * distance, minEffectiveDistanceSquared),
                                    globalForceMultiplier * this.lookupElementProperties.get(entry.getKey()).weight * (dY / distance) * coefficientRepulsion / Math.max(distance * distance, minEffectiveDistanceSquared)
                            );
                        }
                    }
                }
            }

            /* The endpoint simulation was pretty straightforward; it is effectively what the Logical Graph
             * (which was previously written) does, but simpler.  This is all new logic, so the earlier
             * disclaimer regarding accuracy applies primarily to this code.
             */
            for(VisualWire wire : wires) {
                //TODO: Investigate modifying the attractive force into including a component to adjust the strength of the control points to seek a given connection length as well as distance.
                final ElementProperties propSource = this.lookupElementProperties.get(wire.getSource());
                final ElementProperties propDest = this.lookupElementProperties.get(wire.getDestination());
                //Early termination condition: neither endpoint is subject to layout
                if(!propSource.useLayoutThisFrame && !propDest.useLayoutThisFrame) {
                    continue;
                }

                final double dX = propSource.location.getX() - propDest.location.getX();
                final double dY = propSource.location.getY() - propDest.location.getY();
                //Early termination condition: If the endpoints are at the same point then apply no force.
                if(dX == 0.0 && dY == 0.0) {
                    continue;
                }

                if(ports.contains(wire.getSource()) && ports.contains(wire.getDestination())) {
                    //The wire connects two ports, which means the devices (if different) are attracted to each other.
                    //TODO: Have devices attract each other (doing this before devices repel each other is sort of meaningless)
                } else {
                    //  We're now going to prepare all the intermediate values that we probably will need to
                    // calculate the force--there is some special casing regarding what is actually modified,
                    // and it is possible that nothing will be, but it is generally safe to assume that these
                    // values will be used.  Thse cost to always calculate them outweighs the inconvenience of
                    // checking whether or not we will need them.
                    final double distance = Math.sqrt(dX*dX + dY*dY);
                    //vX and vY are the X and Y components of the vector from dest to source, which is the direction of the force applied by the connection on the destination.
                    //We know that distance is non-zero.
                    final double vX = dX / distance;
                    final double vY = dY / distance;

                    //TODO: Attempt to extract the ideal length from wire
                    final double idealDistance = defaultEquilibriumDistance;
                    final double magnitudeAttraction = 1 / (idealDistance * idealDistance);

                    final double scX = wire.getSource().controlXProperty().get();
                    final double scY = wire.getSource().controlYProperty().get();
                    final double ecX = wire.getDestination().controlXProperty().get();
                    final double ecY = wire.getDestination().controlYProperty().get();

                    //To calculate the rigidity force, we need the start-end and c1-c2 vectors
                    double vecStartEndX = propDest.location.getX() - propSource.location.getX();
                    double vecStartEndY = propDest.location.getY() - propSource.location.getY();
                    double vecC1C2X = wire.getDestination().controlXProperty().get() - wire.getSource().controlXProperty().get();
                    double vecC1C2Y = wire.getDestination().controlYProperty().get() - wire.getSource().controlYProperty().get();

                    final double magnitudeStartEnd = Math.sqrt(vecStartEndX * vecStartEndX + vecStartEndY * vecStartEndY);
                    final double magnitudeC1C2 = Math.sqrt(vecC1C2X * vecC1C2X + vecC1C2Y * vecC1C2Y);
                    vecStartEndX /= magnitudeStartEnd;
                    vecStartEndY /= magnitudeStartEnd;
                    vecC1C2X /= magnitudeC1C2;
                    vecC1C2Y /= magnitudeC1C2;

                    //If this evaluates to NaN (one of the vectors has magnitude 0), then we will skip the rigidity force component.
                    final double multiplierBaseRigidity = vecStartEndX * vecC1C2Y - vecStartEndY * vecC1C2X;

                    //TODO: Attempt to extract the rigidity coefficient from the wire.
                    final double coefficientRigidity = 0.05;

                    //We will apply forces to any endpoint that is not a port and that is subject to layout.
                    //There are 4 forces that are applied to the endpoints of a wire:
                    // 1) An attractive force which pulls the endpoint towards the other endpoint.
                    // 2) The local inverse control force which pushes the endpoint away from its control point
                    // 3) The remote control force which pushes the endpoint along the vector from the remote endpoint to the remote endpoint's control point.
                    // 4) The rigidity force which attempts to make the Start-End and Control1-Control2 vectors parallel by moving the endpoints perpendicular to the Start-End vector.
                    /* I can't help but think that "Local Inverse Control Force" sounds like a poorly-translated late-80's shooter in the vein of Zero Wing.
                     * Remote Control Force would, logically, be the sequel with a much better localization team, known for the campy FMV-cutscene mission briefings.
                     * Attractive Force and Rigidity Force, however, would then be cheap NSFW retro-knockoffs littered with microtransactions aimed at the mobile and Steam audiences.
                     *
                     * Have I mentioned how difficult it can be to focus on detail-oriented tasks in an open-floorplan office?
                     */
                    if(!ports.contains(wire.getSource()) && propSource.useLayoutThisFrame) {
                        // Attractive Force:
                        propSource.velocity.add(
                                globalForceMultiplier * magnitudeAttraction * coefficientRepulsion * propDest.weight * -vX,
                                globalForceMultiplier * magnitudeAttraction * coefficientRepulsion * propDest.weight * -vY
                        );
                        // Local Inverse Control Force:
                        propSource.velocity.add(
                                globalForceMultiplier * coefficientControlForce * -scX,
                                globalForceMultiplier * coefficientControlForce * -scY
                        );
                        // Remote Control Force:
                        propSource.velocity.add(
                                globalForceMultiplier * coefficientControlForce * ecX,
                                globalForceMultiplier * coefficientControlForce * ecY
                        );
                        // Rigidity Force:
                        if(!Double.isNaN(multiplierBaseRigidity)) {
                            propSource.velocity.add(
                                    globalForceMultiplier * coefficientRigidity * multiplierBaseRigidity * -vecStartEndY,
                                    globalForceMultiplier * coefficientRigidity * multiplierBaseRigidity * vecStartEndX
                            );
                        }
                    }
                    if(!ports.contains(wire.getDestination()) && propDest.useLayoutThisFrame) {
                        // Attractive Force:
                        propDest.velocity.add(
                                globalForceMultiplier * magnitudeAttraction * coefficientRepulsion * propSource.weight * vX,
                                globalForceMultiplier * magnitudeAttraction * coefficientRepulsion * propSource.weight * vY
                        );
                        // Local Inverse Control Force:
                        propDest.velocity.add(
                                globalForceMultiplier * coefficientControlForce * -ecX,
                                globalForceMultiplier * coefficientControlForce * -ecY
                        );
                        // Remote Control Force:
                        propDest.velocity.add(
                                globalForceMultiplier * coefficientControlForce * scX,
                                globalForceMultiplier * coefficientControlForce * scY
                        );
                        // Rigidity Force:
                        if(!Double.isNaN(multiplierBaseRigidity)) {
                            propDest.velocity.add(
                                    globalForceMultiplier * coefficientRigidity * multiplierBaseRigidity * vecStartEndY,
                                    globalForceMultiplier * coefficientRigidity * multiplierBaseRigidity * -vecStartEndX
                            );
                        }
                    }
                }
            }

            for(final Map.Entry<IPhysicalElement, ElementProperties> entry : this.lookupElementProperties.entrySet()) {
                if(entry.getValue().useLayoutThisFrame) {
                    //When updating the position of a device, also update the position of its ports.
                    //The ports will always have a velocity of zero (there are some race conditions where that isn't necessarily the case, but we assume for now)
                    if(entry.getKey() instanceof VisualDevice) {
                        for(final IPhysicalElement element : visualization.portsOf(entry.getKey())) {
                            this.lookupElementProperties.get(element).location.add(entry.getValue().velocity);
                        }
                    }
                    entry.getValue().location.add(entry.getValue().velocity);
                    entry.getValue().velocity.multiply(velocityDecayRate);
                }
            }
        } while(++cntIterations < LIMIT_ITERATIONS && System.currentTimeMillis() - msStartTime < LIMIT_MS);

        // Translate positions from physics coordinates to world coordiates and return the mapping.
        final Map<IPhysicalElement, MutablePoint> positions = new HashMap<>();
        for(IPhysicalElement host : hosts) {
            //We're not going to move ports, so there is no need to report them.  Ports locations are defined relative to the owning device, so moving the device will move the ports.
            final ElementProperties properties = this.lookupElementProperties.get(host);
            if(properties.useLayoutThisFrame) {
                positions.put(host, properties.getWorldCoordinates());
            }
        }
        for(IPhysicalElement device : devices) {
            final ElementProperties properties = this.lookupElementProperties.get(device);
            if(properties.useLayoutThisFrame) {
                positions.put(device, properties.getWorldCoordinates());
            }
        }
        return positions;
    }
}
