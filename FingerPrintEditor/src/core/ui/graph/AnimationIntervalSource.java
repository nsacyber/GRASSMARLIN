package core.ui.graph;

/**
 * Provides the animation rate information for an animated object.
 * Initially for use in the AnimatedLink object
 *
 * 2007.06.13 - New
 */
public interface AnimationIntervalSource {
    
    /**
     * Returns the interval in millis at which the animation actions
     * should occur
     */
    public long getAnimationIntervalInMillis();

}
