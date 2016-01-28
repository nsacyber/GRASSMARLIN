package prefuse.activity;

/**
 * A Pacer, or pacing function, maps one double value to another; they
 * are used to parameterize animation rates, where the input value f moves
 * from 0 to 1 linearly, but the returned output can vary quite differently
 * in response to the input.
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.action.Action
 * @see prefuse.activity.SlowInSlowOutPacer
 * @see prefuse.activity.ThereAndBackPacer
 */
public interface Pacer {

    /**
     * Maps one double value to another to determine animation pacing. Under
     * most circumstances, both the input and output values should be in the
     * range 0-1, inclusive.
     * @param f the input value, should be between 0-1
     * @return the output value, should be between 0-1
     */
    public double pace(double f);
    
} // end of interface Pacer
