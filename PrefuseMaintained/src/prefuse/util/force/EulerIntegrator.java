package prefuse.util.force;

import java.util.Iterator;

/**
 * Updates velocity and position data using Euler's Method. This is the
 * simplest and fastest method, but is somewhat inaccurate and less smooth
 * than more costly approaches.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see RungeKuttaIntegrator
 */
public class EulerIntegrator implements Integrator {
    
    /**
     * @see prefuse.util.force.Integrator#integrate(prefuse.util.force.ForceSimulator, long)
     */
    public void integrate(ForceSimulator sim, long timestep) {
        float speedLimit = sim.getSpeedLimit();
        Iterator iter = sim.getItems();
        while ( iter.hasNext() ) {
            ForceItem item = (ForceItem)iter.next();
            item.location[0] += timestep * item.velocity[0];
            item.location[1] += timestep * item.velocity[1];
            float coeff = timestep / item.mass;
            item.velocity[0] += coeff * item.force[0];
            item.velocity[1] += coeff * item.force[1];
            float vx = item.velocity[0];
            float vy = item.velocity[1];
            float v = (float)Math.sqrt(vx*vx+vy*vy);
            if ( v > speedLimit ) {
                item.velocity[0] = speedLimit * vx / v;
                item.velocity[1] = speedLimit * vy / v;
            }
        }
    }

} // end of class EulerIntegrator
