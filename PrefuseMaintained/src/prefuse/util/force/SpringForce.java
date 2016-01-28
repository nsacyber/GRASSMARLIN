package prefuse.util.force;

/**
 * Force function that computes the force acting on ForceItems due to a
 * given Spring.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class SpringForce extends AbstractForce {

    private static String[] pnames 
        = new String[] { "SpringCoefficient", "DefaultSpringLength" };
    
    public static final float DEFAULT_SPRING_COEFF = 1E-4f;
    public static final float DEFAULT_MAX_SPRING_COEFF = 1E-3f;
    public static final float DEFAULT_MIN_SPRING_COEFF = 1E-5f;
    public static final float DEFAULT_SPRING_LENGTH = 50;
    public static final float DEFAULT_MIN_SPRING_LENGTH = 0;
    public static final float DEFAULT_MAX_SPRING_LENGTH = 200;
    public static final int SPRING_COEFF = 0;
    public static final int SPRING_LENGTH = 1;
    
    /**
     * Create a new SpringForce.
     * @param springCoeff the default spring co-efficient to use. This will
     * be used if the spring's own co-efficient is less than zero.
     * @param defaultLength the default spring length to use. This will
     * be used if the spring's own length is less than zero.
     */
    public SpringForce(float springCoeff, float defaultLength) {
        params = new float[] { springCoeff, defaultLength };
        minValues = new float[] 
            { DEFAULT_MIN_SPRING_COEFF, DEFAULT_MIN_SPRING_LENGTH };
        maxValues = new float[] 
            { DEFAULT_MAX_SPRING_COEFF, DEFAULT_MAX_SPRING_LENGTH };
    }
    
    /**
     * Constructs a new SpringForce instance with default parameters.
     */
    public SpringForce() {
        this(DEFAULT_SPRING_COEFF, DEFAULT_SPRING_LENGTH);
    }

    /**
     * Returns true.
     * @see prefuse.util.force.Force#isSpringForce()
     */
    public boolean isSpringForce() {
        return true;
    }
    
    /**
     * @see prefuse.util.force.AbstractForce#getParameterNames()
     */
    protected String[] getParameterNames() {
        return pnames;
    } 
    
    /**
     * Calculates the force vector acting on the items due to the given spring.
     * @param s the Spring for which to compute the force
     * @see prefuse.util.force.Force#getForce(prefuse.util.force.Spring)
     */
    public void getForce(Spring s) {
        ForceItem item1 = s.item1;
        ForceItem item2 = s.item2;
        float length = (s.length < 0 ? params[SPRING_LENGTH] : s.length);
        float x1 = item1.location[0], y1 = item1.location[1];
        float x2 = item2.location[0], y2 = item2.location[1];
        float dx = x2-x1, dy = y2-y1;
        float r  = (float)Math.sqrt(dx*dx+dy*dy);
        if ( r == 0.0 ) {
            dx = ((float)Math.random()-0.5f) / 50.0f;
            dy = ((float)Math.random()-0.5f) / 50.0f;
            r  = (float)Math.sqrt(dx*dx+dy*dy);
        }
        float d  = r-length;
        float coeff = (s.coeff < 0 ? params[SPRING_COEFF] : s.coeff)*d/r;
        item1.force[0] += coeff*dx;
        item1.force[1] += coeff*dy;
        item2.force[0] += -coeff*dx;
        item2.force[1] += -coeff*dy;
    }
    
} // end of class SpringForce
