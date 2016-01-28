package prefuse.util.force;

/**
 * Interface for numerical integration routines. These routines are used
 * to update the position and velocity of items in response to forces
 * over a given time step.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Integrator {

    public void integrate(ForceSimulator sim, long timestep);
    
} // end of interface Integrator
