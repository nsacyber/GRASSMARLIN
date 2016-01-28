package prefuse.activity;

/**
 * A pacing function that provides slow-in, slow-out animation, where the
 * animation begins at a slower rate, speeds up through the middle of the
 * animation, and then slows down again before stopping.
 * 
 * This is calculated by using an appropriately phased sigmoid function of
 * the form 1/(1+exp(-x)).
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class SlowInSlowOutPacer implements Pacer {
    
    /**
     * Pacing function providing slow-in, slow-out animation
     * @see prefuse.activity.Pacer#pace(double)
     */
    public double pace(double f) {
        return ( f == 0.0 || f == 1.0 ? f : sigmoid(f) );
    }
    
    /**
     * Computes a normalized sigmoid
     * @param x input value in the interval [0,1]
     */
    private double sigmoid(double x) {
        x = 12.0*x - 6.0;
        return (1.0 / (1.0 + Math.exp(-1.0 * x)));
    }

} // end of class SlowInSlowOutPacer
