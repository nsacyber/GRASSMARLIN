package core.ui.graph;

import java.awt.*;
import java.awt.geom.Ellipse2D;


/**
 * Animated version of the genius link, adds a ball along arrow
 *  2007.06.06 - (CC) New...
 *  2007.06.13 - (FrontMan) Added framework for animation thread; removed animated boolean - it is always animated
 *  2007.06.14 - (CC) Added back in animated boolean: AnimationIntervalSource rate check if 
 *               interval is 0 dont animate - it is NOT always animated!  
 */
public class AnimatedLink extends DefaultLink {
    //ball's offset
    private int offset;
    /** Thread to animate this link **/
    private AnimationThread animationThread;
    private boolean animated = false;
    
    /**
     * custructs a new animated link using default links constructor
     * @param origin
     * @param target
     * @param container Container in which this Link is contained
     */
    public AnimatedLink(ConnectionPoint origin, ConnectionPoint target, AnimationIntervalSource intervalSource) {
        super(origin, target);
        this.animationThread = new AnimationThread(intervalSource);
        this.animationThread.start();
        
    }
    
    
    /**
     * paints the link.  calls default links paint then animates if boolena is set
     */
    @Override
    public Graphics2D paintLink(Graphics2D g2dd) {
        Graphics2D g2d = super.paintLink(g2dd);
        if(isAnimated()) {
            int ax = this.origin.getPoint().x;
            int ay = this.origin.getPoint().y - 3;
            Shape circle = new Ellipse2D.Float(ax + this.offset , ay, 10, 10);
            g2d.setStroke(new BasicStroke(1));
            g2d.setPaint(Color.yellow);
            g2d.fill(circle);
            g2d.setPaint(Color.black);
            g2d.draw(circle);
        }
        return g2d;
    }
    
    /**
     * Updates the offset of the bubble on the link
     */
    private class AnimationThread extends Thread {
        private AnimationIntervalSource intervalSource;

        public AnimationThread(AnimationIntervalSource source) { 
            this.intervalSource = source;
        }
        
        public void run() { 
            while(true) {
                try {
                    if(this.intervalSource.getAnimationIntervalInMillis() != 0) {
                        AnimatedLink.this.setAnimated(true);
                        if(AnimatedLink.this.offset > AnimatedLink.this.length-5) {
                            AnimatedLink.this.offset = 0;
                        }
                        else {
                            AnimatedLink.this.offset += 1;
                        }
                        AnimatedLink.this.setChanged();
                        AnimatedLink.this.notifyObservers();
                        Thread.sleep(this.intervalSource.getAnimationIntervalInMillis());
                    }
                    else {
                        AnimatedLink.this.setAnimated(false);
                        //sleep a little
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return Returns the animated.
     */
    private boolean isAnimated () {
        return animated;
    }


    /**
     * @param animated The animated to set.
     */
    private void setAnimated (boolean animated) {
        this.animated = animated;
    }
    
    
}
