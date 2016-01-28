package prefuse.util.force;

import java.util.ArrayList;

/**
 * Represents a spring in a force simulation.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Spring {
    private static SpringFactory s_factory = new SpringFactory();
    
    /**
     * Retrieve the SpringFactory instance, which serves as an object pool
     * for Spring instances.
     * @return the Spring Factory
     */
    public static SpringFactory getFactory() {
        return s_factory;
    }
    
    /**
     * Create a new Spring instance
     * @param fi1 the first ForceItem endpoint
     * @param fi2 the second ForceItem endpoint
     * @param k the spring tension co-efficient
     * @param len the spring's resting length
     */
    public Spring(ForceItem fi1, ForceItem fi2, float k, float len) {
        item1 = fi1;
        item2 = fi2;
        coeff = k;
        length = len;
    }
    
    /** The first ForceItem endpoint */
    public ForceItem item1;
    /** The second ForceItem endpoint */
    public ForceItem item2;
    /** The spring's resting length */
    public float length;
    /** The spring tension co-efficient */
    public float coeff;
    
    /**
     * The SpringFactory is responsible for generating Spring instances
     * and maintaining an object pool of Springs to reduce garbage collection
     * overheads while force simulations are running.
     */
    public static final class SpringFactory {
        private int maxSprings = 10000;
        private ArrayList springs = new ArrayList();
        
        /**
         * Get a Spring instance and set it to the given parameters.
         */
        public Spring getSpring(ForceItem f1, ForceItem f2, float k, float length) {
            if ( springs.size() > 0 ) {
                Spring s = (Spring)springs.remove(springs.size()-1);
                s.item1 = f1;
                s.item2 = f2;
                s.coeff = k;
                s.length = length;
                return s;
            } else {
                return new Spring(f1,f2,k,length);
            }
        }
        /**
         * Reclaim a Spring into the object pool.
         */
        public void reclaim(Spring s) {
            s.item1 = null;
            s.item2 = null;
            if ( springs.size() < maxSprings )
                springs.add(s);
        }
    } // end of inner class SpringFactory

} // end of class Spring
