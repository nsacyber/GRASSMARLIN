/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.action;

import prefuse.Visualization;
import prefuse.data.expression.Expression;
import prefuse.data.expression.ExpressionVisitor;
import prefuse.data.expression.ObjectLiteral;
import prefuse.data.expression.Predicate;
import prefuse.util.PredicateChain;
import prefuse.visual.VisualItem;

/**
 * ItemAction instance that can also maintain a collection of rule mappings
 * that can be used by subclasses to create particular rule-mappings for
 * encoding data values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class EncoderAction extends ItemAction {

    private PredicateChain m_chain = null;
    
    /**
     * Create a new EncoderAction that processes all data groups.
     */
    public EncoderAction() {
        super();
    }
    
    /**
     * Create a new EncoderAction that processes all groups.
     * @param vis the {@link prefuse.Visualization} to process
     * @see Visualization#ALL_ITEMS
     */
    public EncoderAction(Visualization vis) {
        super(vis);
    }
    
    /**
     * Create a new EncoderAction that processes the specified group.
     * @param group the name of the group to process
     */
    public EncoderAction(String group) {
        super(group);
    }
    
    /**
     * Create a new EncoderAction that processes the specified group.
     * @param group the name of the group to process
     * @param filter the filtering {@link prefuse.data.expression.Predicate}
     */
    public EncoderAction(String group, Predicate filter) {
        super(group, filter);
    }
    
    /**
     * Create a new EncoderAction that processes the specified group.
     * @param vis the {@link prefuse.Visualization} to process
     * @param group the data group to process
     */
    public EncoderAction(Visualization vis, String group) {
        super(vis, group);
    }
    
    /**
     * Create a new EncoderAction that processes the specified group.
     * @param vis the {@link prefuse.Visualization} to process
     * @param group the name of the group to process
     * @param filter the filtering {@link prefuse.data.expression.Predicate}
     */
    public EncoderAction(Visualization vis, String group, Predicate filter) {
        super(vis, group, filter);
    }
    
    // ------------------------------------------------------------------------

    /**
     * Add a mapping rule to this EncoderAction. This method is protected,
     * subclasses should crate public add methods of their own to enforce
     * their own type constraints.
     * @param p the rule Predicate 
     * @param value the value to map to
     */
    protected void add(Predicate p, Object value) {
        if ( m_chain == null ) m_chain = new PredicateChain();
        if ( value instanceof Action )
            ((Action)value).setVisualization(m_vis);
        m_chain.add(p, value);
    }
    
    /**
     * Lookup the value mapped to by the given item.
     * @param item the item to lookup
     * @return the result of the rule lookup
     */
    protected Object lookup(VisualItem item) {
        return (m_chain == null ? null : m_chain.get(item));
    }
    
    /**
     * Remove all rule mappings from this encoder.
     */
    public void clear() {
        if ( m_chain != null ) {
            m_chain.clear();
        }
    }
    
    /**
     * Remove rules using the given predicate from this encoder.
     * This method will not remove rules in which this predicate is used
     * within a composite of clauses, such as an AND or OR. It only removes
     * rules using this predicate as the top-level trigger.
     * @param p the predicate to remove
     * @return true if a rule was successfully removed, false otherwise
     */
    public boolean remove(Predicate p) {
        return ( m_chain != null ? m_chain.remove(p) : false );
    }
    
    /**
     * @see prefuse.action.Action#setVisualization(prefuse.Visualization)
     */
    public void setVisualization(Visualization vis) {
        super.setVisualization(vis);
        if ( m_chain != null )
            m_chain.getExpression().visit(new SetVisualizationVisitor());
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        setup();
        if ( m_chain != null )
            m_chain.getExpression().visit(SetupVisitor.getInstance());
        
        super.run(frac);
        
        if ( m_chain != null )
            m_chain.getExpression().visit(FinishVisitor.getInstance());
        finish();
    }
    
    /**
     * Perform any necessary setup before this encoder can be used. By default
     * does nothing. Subclasses can override this method to perform custom
     * setup before the Action is used.
     */
    protected void setup() {
        // do nothing be default
    }
    
    /**
     * Perform any necessary clean-up after this encoder can be used. By
     * default does nothing. Subclasses can override this method to perform
     * custom clean-up after the Action is used.
     */
    protected void finish() {
        // do nothing be default
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Abstract class for processing the Actions stored in the predicate chain.
     */
    private static abstract class ActionVisitor implements ExpressionVisitor {
        public void visitExpression(Expression expr) {
            if ( expr instanceof ObjectLiteral ) {
                Object val = expr.get(null);
                if ( val instanceof Action )
                    visitAction(((Action)val));
            }
        }
        public abstract void visitAction(Action a);
        public void down() { /* do nothing */ }
        public void up()   { /* do nothing */ }
    }
    
    /**
     * Sets the visualization status for any Actions contained within the
     * predicate chain.
     */
    private class SetVisualizationVisitor extends ActionVisitor {
        public void visitAction(Action a) {
            a.setVisualization(m_vis);
        }
    }
    
    /**
     * Calls the "setup" method for any delegate actions contained within
     * the rule-mappings for this encoder.
     */
    private static class SetupVisitor extends ActionVisitor {
        private static SetupVisitor s_instance;
        public static SetupVisitor getInstance() {
            if ( s_instance == null )
                s_instance = new SetupVisitor();
            return s_instance;
        }
        public void visitAction(Action a) {
            if ( a instanceof EncoderAction )
                ((EncoderAction)a).setup();
        }
    }
    
    /**
     * Calls the "setup" method for any delegate actions contained within
     * the rule-mappings for this encoder.
     */
    private static class FinishVisitor extends ActionVisitor {
        private static FinishVisitor s_instance;
        public static FinishVisitor getInstance() {
            if ( s_instance == null )
                s_instance = new FinishVisitor();
            return s_instance;
        }
        public void visitAction(Action a) {
            if ( a instanceof EncoderAction )
                ((EncoderAction)a).setup();
        }
    }
    
} // end of class EncoderAction