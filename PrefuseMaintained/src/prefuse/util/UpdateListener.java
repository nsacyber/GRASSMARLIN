package prefuse.util;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import prefuse.data.Tuple;
import prefuse.data.event.ExpressionListener;
import prefuse.data.event.TupleSetListener;
import prefuse.data.expression.Expression;
import prefuse.data.tuple.TupleSet;

/**
 * Convenience listener class that implements ExpressionListener,
 * TupleSetListener, and ComponentListener and routes all the
 * callbacks into a generic {@link #update(Object)} method. For the
 * case of ComponentListener, only the resize event is funneled into
 * the update method.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class UpdateListener 
    implements ExpressionListener, TupleSetListener, ComponentListener
{
    /**
     * Generic update routine triggerred by any of the other callbacks.
     * @param source a source object, either the Expression, TupleSet,
     * or Component that triggered this update event.
     */
    public abstract void update(Object source);
    
    /**
     * @see prefuse.data.event.ExpressionListener#expressionChanged(prefuse.data.expression.Expression)
     */
    public void expressionChanged(Expression expr) {
        update(expr);
    }
    
    /**
     * @see prefuse.data.event.TupleSetListener#tupleSetChanged(prefuse.data.tuple.TupleSet, prefuse.data.Tuple[], prefuse.data.Tuple[])
     */
    public void tupleSetChanged(TupleSet tset, Tuple[] added, Tuple[] removed) {
        update(tset);
    }
    
    /**
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    public void componentResized(ComponentEvent e) {
        update(e.getSource());
    }

    /**
     * Does nothing.
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    public void componentHidden(ComponentEvent e) {
        // do nothing
    }
    /**
     * Does nothing.
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    public void componentMoved(ComponentEvent e) {
        // do nothing
    }
    /**
     * Does nothing.
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    public void componentShown(ComponentEvent e) {
        // do nothing
    }

} // end of abstract class UpdateListener
