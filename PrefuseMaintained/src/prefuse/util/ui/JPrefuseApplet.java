package prefuse.util.ui;

import javax.swing.JApplet;

import prefuse.activity.ActivityManager;

/**
 * A convenience class for creating applets that incorporate
 * prefuse visualizations. Clients can subclass this class to
 * implement prefuse applets. However if the subclass overrides
 * the {@link #destroy()} or {@link #stop()} methods, it should
 * be sure to also call these methods on the super class.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class JPrefuseApplet extends JApplet {
    
    /**
     * Automatically shuts down the ActivityManager when the applet is
     * destroyed.
     * @see java.applet.Applet#destroy()
     */
    public void destroy() {
        ActivityManager.stopThread();
    }

    /**
     * Automatically shuts down the ActivityManager when the applet is
     * stopped.
     * @see java.applet.Applet#stop()
     */
    public void stop() {
        ActivityManager.stopThread();
    }
    
} // end of class JPrefuseApplet
