package prefuse.render;

import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.util.PredicateChain;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;


/**
 * <p>Default factory implementation from which to retrieve VisualItem
 * renderers.</p>
 * 
 * <p>
 * This class supports the use of a default renderer for EdgeItems (the default
 * edge renderer) and another for all other non-edge VisualItems (the default
 * item renderer). In addition, any number of additional Renderer mapping rules
 * can be added, by specifying a Predicate to apply and a Renderer to return
 * for matching items. Predicate/Renderer mappings are checked in the order in
 * which they were added to the factory.
 * </p>
 * 
 * <p>If left unspecified, a {@link ShapeRenderer} is used as the default
 * item renderer and an {@link EdgeRenderer} instance is used as the default
 * edge renderer.</p>
 * 
 * <p>For example, the following code snippet creates a new
 * DefaultRendererFactory, changes the default edge renderer to be an
 * EdgeRenderer using curved edges, and adds a new rule which maps items in
 * the group "data" to a text renderer that pulls its text from a field named
 * "label".</p>
 * <pre>
 *   DefaultRendererFactory rf = new DefaultRendererFactory();
 *   rf.setDefaultEdgeRenderer(new EdgeRenderer(Constants.EDGE_TYPE_CURVE));
 *   rf.add("INGROUP('data')", new LabelRenderer("label"));
 * </pre>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DefaultRendererFactory implements RendererFactory {

    private PredicateChain m_chain = new PredicateChain();
    private Renderer m_itemRenderer;
    private Renderer m_edgeRenderer;
    
    /**
     * Default Constructor. A ShapeRenderer instance will be used for the
     * default item renderer and an EdgeRenderer instance will be used for the
     * default edge renderer.
     * @see ShapeRenderer
     * @see EdgeRenderer
     */
    public DefaultRendererFactory() {
       this(new ShapeRenderer());
    }
    
    /**
     * Constructor. Creates a new DefaultRendererFactory with the specified
     * default item renderer. An EdgeRenderer instance will be used for the
     * default edge renderer.
     * @param itemRenderer the default item renderer. This is the default for
     * rendering all items except EdgeItem instances.
     * @see EdgeRenderer
     */
    public DefaultRendererFactory(Renderer itemRenderer) {
        this(itemRenderer, new EdgeRenderer());
    }
    
    /**
     * Constructor. Creates a new DefaultRendererFactory with the specified
     * default item and edge renderers.
     * @param itemRenderer the default item renderer. This is the default for
     * rendering all items except EdgeItem instances.
     * @param edgeRenderer the default edge renderer. This is the default for
     * rendering EdgeItem instances.
     */
    public DefaultRendererFactory(Renderer itemRenderer, Renderer edgeRenderer)
    {
        m_itemRenderer = itemRenderer;
        m_edgeRenderer = edgeRenderer;
    }

    // ------------------------------------------------------------------------
    
    /**
     * Sets the default renderer. This renderer will be returned by
     * {@link #getRenderer(VisualItem)} whenever there are no matching
     * predicates and the input item <em>is not</em> an EdgeItem. To set the
     * default renderer for EdgeItems, see
     * {@link #setDefaultEdgeRenderer(Renderer)}.
     * @param r the Renderer to use as the default
     * @see #setDefaultEdgeRenderer(Renderer)
     */
    public void setDefaultRenderer(Renderer r) {
        m_itemRenderer = r;
    }
    
    /**
     * Gets the default renderer. This renderer will be returned by
     * {@link #getRenderer(VisualItem)} whenever there are no matching
     * predicates and the input item <em>is not</em> an EdgeItem.
     * @return the default Renderer for non-edge VisualItems
     */
    public Renderer getDefaultRenderer() {
        return m_itemRenderer;
    }
    
    /**
     * Sets the default edge renderer. This renderer will be returned by
     * {@link #getRenderer(VisualItem)} whenever there are no matching
     * predicates and the input item <em>is</em> an EdgeItem. To set the
     * default renderer for non-EdgeItems, see
     * {@link #setDefaultRenderer(Renderer)}.
     * @param r the Renderer to use as the default for EdgeItems
     * @see #setDefaultRenderer(Renderer)
     */
    public void setDefaultEdgeRenderer(Renderer r) {
        m_edgeRenderer = r;
    }
    
    /**
     * Gets the default edge renderer. This renderer will be returned by
     * {@link #getRenderer(VisualItem)} whenever there are no matching
     * predicates and the input item <em>is</em> an EdgeItem.
     * @return the default Renderer for EdgeItems
     */
    public Renderer getDefaultEdgeRenderer() {
        return m_edgeRenderer;
    }
    
    /**
     * Adds a new mapping to this RendererFactory. If an input item to
     * {@link #getRenderer(VisualItem)} matches the predicate, then the
     * corresponding Renderer will be returned. Predicates are evaluated in the
     * order in which they are added, so if an item matches multiple
     * predicates, the Renderer for the earliest match will be returned.
     * @param p a Predicate for testing a VisualItem
     * @param r the Renderer to return if an item matches the Predicate
     */
    public void add(Predicate p, Renderer r) {
        m_chain.add(p, r);
    }
    
    /**
     * Adds a new mapping to this RendererFactory. If an input item to
     * {@link #getRenderer(VisualItem)} matches the predicate, then the
     * corresponding Renderer will be returned. Predicates are evaluated in the
     * order in which they are added, so if an item matches multiple
     * predicates, the Renderer for the earliest match will be returned.
     * @param predicate a String in the prefuse expression language. This
     *  String will be parsed to create a corresponding Predicate instance.
     * @param r the Renderer to return if an item matches the Predicate
     */
    public void add(String predicate, Renderer r) {
        Predicate p = (Predicate)ExpressionParser.parse(predicate);
        add(p, r);
    }
    
    /**
     * Return a Renderer instance for the input VisualItem. The VisualItem
     * is matched against the registered Predicates, and if a match is found
     * the corresponding Renderer is returned. Predicate matches are evaluated
     * in the order in which Predicate/Renderer mappings were added to this
     * RendererFactory. If no matches are found, either the default renderer
     * (for all VisualItems except EdgeItems) or the default edge renderer (for
     * EdgeItems) is returned.
     */
    public Renderer getRenderer(VisualItem item) {
        Renderer r = (Renderer)m_chain.get(item);
        if ( r != null )
            return r;
        else if ( item instanceof EdgeItem )
            return m_edgeRenderer;
        else
            return m_itemRenderer;
    }
    
} // end of class DefaultRendererFactory
