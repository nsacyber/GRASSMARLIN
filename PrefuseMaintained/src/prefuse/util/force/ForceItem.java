package prefuse.util.force;

/**
 * Represents a point particle in a force simulation, maintaining values for
 * mass, forces, velocity, and position.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ForceItem implements Cloneable {
    
    /**
     * Create a new ForceItem.
     */
    public ForceItem() {
        mass = 1.0f;
        force = new float[] { 0.f, 0.f };
        velocity = new float[] { 0.f, 0.f };
        location = new float[] { 0.f, 0.f };
        plocation = new float[] { 0.f, 0.f };
        k = new float[4][2];
        l = new float[4][2];
    }
    
    /**
     * Clone a ForceItem.
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        ForceItem item = new ForceItem();
        item.mass = this.mass;
        System.arraycopy(force,0,item.force,0,2);
        System.arraycopy(velocity,0,item.velocity,0,2);
        System.arraycopy(location,0,item.location,0,2);
        System.arraycopy(plocation,0,item.plocation,0,2);
        for ( int i=0; i<k.length; ++i ) {
            System.arraycopy(k[i],0,item.k[i],0,2);
            System.arraycopy(l[i],0,item.l[i],0,2);
        }
        return item;
    }
    
    /** The mass value of this ForceItem. */
    public float   mass;
    /** The values of the forces acting on this ForceItem. */
    public float[] force;
    /** The velocity values of this ForceItem. */
    public float[] velocity;
    /** The location values of this ForceItem. */
    public float[] location;
    /** The previous location values of this ForceItem. */
    public float[] plocation;
    /** Temporary variables for Runge-Kutta integration */
    public float[][] k;
    /** Temporary variables for Runge-Kutta integration */
    public float[][] l;
    
    /**
     * Checks a ForceItem to make sure its values are all valid numbers
     * (i.e., not NaNs).
     * @param item the item to check
     * @return true if all the values are valid, false otherwise
     */
    public static final boolean isValid(ForceItem item) {
        return
          !( Float.isNaN(item.location[0])  || Float.isNaN(item.location[1])  || 
             Float.isNaN(item.plocation[0]) || Float.isNaN(item.plocation[1]) ||
             Float.isNaN(item.velocity[0])  || Float.isNaN(item.velocity[1])  ||
             Float.isNaN(item.force[0])     || Float.isNaN(item.force[1]) );
    }
    
} // end of class ForceItem
