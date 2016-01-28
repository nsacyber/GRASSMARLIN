package prefuse;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import prefuse.action.Action;
import prefuse.activity.Activity;
import prefuse.activity.ActivityManager;
import prefuse.activity.ActivityMap;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.CompositeTupleSet;
import prefuse.data.tuple.DefaultTupleSet;
import prefuse.data.tuple.TupleManager;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.util.PrefuseConfig;
import prefuse.util.PrefuseLib;
import prefuse.util.collections.CompositeIterator;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;
import prefuse.visual.VisualTree;
import prefuse.visual.VisualTupleSet;
import prefuse.visual.expression.ValidatedPredicate;
import prefuse.visual.expression.VisiblePredicate;
import prefuse.visual.tuple.TableDecoratorItem;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

/**
 * <p>Central data structure representing an interactive Visualization.
 * This class is responsible for
 * managing the mappings between source data and onscreen VisualItems,
 * maintaining a list of {@link Display} instances responsible for rendering
 * of and interaction with the contents of this visualization, and
 * providing a collection of named Action instances for performing
 * data processing such as layout, animation, and size, shape, and color
 * assignment.</p>
 * 
 * <p>The primary responsibility of the Visualization class is the creation
 * of <em>visual abstractions</em> of input data. Regardless of the data
 * structure (i.e., {@link prefuse.data.Table}, {@link prefuse.data.Graph},
 * or {@link prefuse.data.Tree}), this class takes source data such as that
 * loaded from a file (see {@link prefuse.data.io}) or from a relational
 * database (see {@link prefuse.data.io.sql}) and creates a visual
 * representation of the data. These visual representations of the data are
 * data sets in their own right, providing access to the underlying source
 * data to be visualized while also adding addition data fields specific to a
 * visualization. These fields include spatial location (x, y
 * coordinates and item bounds), color (for stroke, fill, and text), size,
 * shape, and font. For a given input data set of type
 * {@link prefuse.data.Table}, {@link prefuse.data.Graph}, or
 * or {@link prefuse.data.Tree}, a corresponding instance of
 * {@link prefuse.visual.VisualTable}, {@link prefuse.visual.VisualGraph}, or
 * {@link prefuse.visual.VisualTree} is created and stored in the
 * visualization. These data types inherit the data values of the source
 * data (and indeed, manipulate it directly) while additionally providing
 * the aforementioned visual variables unique to that generated
 * visual abstraction. Similarly, all {@link prefuse.data.Tuple},
 * {@link prefuse.data.Node}, or {@link prefuse.data.Edge}
 * instances used to represent an entry in the source data have a
 * corresponding {@link prefuse.visual.VisualItem},
 * {@link prefuse.visual.NodeItem}, or {@link prefuse.visual.EdgeItem}
 * representing the interactive, visual realization of the backing data.</p>
 * 
 * <p>The mapping of source data to a visual abstraction is accomplished
 * using {@link #add(String, TupleSet)} and the other "add" methods. These
 * methods will automatically create the visual abstraction, and store it
 * in this visualization, associating it with a provided <em>data group name
 * </em>. This group name allows for queries to this visualization that
 * consider only VisualItem instances from that particular group. This is
 * quite useful when crafting {@link prefuse.action.Action} instances that
 * process only a particular group of visual data. The Visualization class
 * provides mechanisms for querying any or all groups within the visualization,
 * using one or both of the group name or a filtering
 * {@link prefuse.data.expression.Predicate} to determine the items to
 * include (see {@link #items(Predicate)} for an examples). Source data
 * may be added multiple times to a Visualization under different group
 * names, allowing for multiple representations of the same backing data.</p>
 * 
 * <p>Additionally, the Visualization class supports VisualItem instances
 * that are not directly grounded in backing source data. Examples include
 * {@link prefuse.visual.DecoratorItem} which "decorates" another pre-existing
 * VisualItem with a separate interactive visual object, and
 * {@link prefuse.visual.AggregateItem} which provides an interactive visual
 * representation of an aggregated of other VisualItems. Methods for adding
 * data groups of these kinds include {@link #addDecorators(String, String)}
 * and {@link #addAggregates(String)}.</p>
 * 
 * <p>All of the examples discussed above are examples of <em>primary, or
 * visual, data groups</em> of VisualItems. Visualizations also support
 * <em>secondary, or focus data groups</em> that maintain additional
 * collections of the VisualItems stored in the primary groups. Examples
 * include a set of focus items (such as those that have been clicked
 * by the user), selected items (items selected by a user), or search
 * items (all matches to a search query). The exact semantics of these
 * groups and the mechanisms by which they are populated is determined by
 * application creators, but some defaults are provided. The Visualization
 * class includes some default group names, namely {@link #FOCUS_ITEMS},
 * {@link #SELECTED_ITEMS}, and {@link #SEARCH_ITEMS} for the above 
 * mentioned tasks. By default, both the {@link #FOCUS_ITEMS},
 * {@link #SELECTED_ITEMS} focus groups are included in the Visualization,
 * represented using {@link prefuse.data.tuple.DefaultTupleSet} instances.
 * Also, some of the interactive controls provided by the
 * {@link prefuse.controls} package populate these sets by default. See
 * {@link prefuse.controls.FocusControl} for an example.</p>
 * 
 * <p>Visualizations also maintain references to all the {@link Display}
 * instances providing interactive views of the content of this
 * visualization. {@link Display} instances registers themselves with
 * the visualization either in their constructor or through
 * the {@link Display#setVisualization(Visualization)} method, so they
 * do not otherwise need to be added manually. Displays can be configured
 * to show all or only a subset of the data in the Visualization. A
 * filtering {@link prefuse.data.expression.Predicate} can be used to
 * control what items are drawn by the displaying, including limiting
 * the Display to particular data groups (for example, using a
 * {@link prefuse.visual.expression.InGroupPredicate}). The Visualization's
 * {@link #repaint()} method will trigger a repaint on all Displays
 * associated with the visualization.</p>
 * 
 * <p>Finally, the Visualization class provides a map of named
 * {@link prefuse.action.Action} instances that can be invoked to perform
 * processing on the VisualItems contained in the visualization.
 * Using the {@link #putAction(String, Action)} will add a named Action
 * to the visualization, registering the Action such that a reference
 * to this Visualization will be available within the scope of the
 * Action's {@link prefuse.action.Action#run(double)} method. Processing
 * Actions can later be invoked by name using the {@link #run(String)}
 * method and other similar methods. This functionality not only
 * provides a convenient means of organizing a Visualization-specific
 * collection of processing Actions, it also allows for a layer of indirection
 * between an Action and its name. This allows Actions to be dynamically
 * swapped at runtime. For example, an application may make a call to
 * invoke an Action named "layout", but the actual layout processing maybe
 * be dynamically swapped by changing the Action that corresponds to that
 * name. For more information on processing Actions, see the
 * {@link prefuse.action} packages and the top-level
 * {@link prefuse.action.Action} class.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Visualization {

    /** Data group name for indicating all groups */
    public static final String ALL_ITEMS = PrefuseConfig.get("visualization.allItems");
    /** Default data group name for focus items */
    public static final String FOCUS_ITEMS = PrefuseConfig.get("visualization.focusItems");
    /** Default data group name for selected items */
    public static final String SELECTED_ITEMS = PrefuseConfig.get("visualization.selectedItems");
    /** Default data group name for search result items */
    public static final String SEARCH_ITEMS = PrefuseConfig.get("visualization.searchItems");
    // visual abstraction
    // filtered tables and groups
    private Map m_visual;
    private Map m_source;
    private Map m_focus;
    // actions
    private ActivityMap m_actions;
    // renderers
    private RendererFactory m_renderers;
    // displays
    private ArrayList m_displays;

    // ------------------------------------------------------------------------
    // Constructor
    /**
     * Create a new, empty Visualization. Uses a DefaultRendererFactory.
     */
    public Visualization() {
        m_actions = new ActivityMap();
        m_renderers = new DefaultRendererFactory();
        m_visual = new LinkedHashMap();
        m_source = new HashMap();
        m_focus = new HashMap();
        m_displays = new ArrayList();

        addFocusGroup(Visualization.FOCUS_ITEMS, new DefaultTupleSet());
        addFocusGroup(Visualization.SELECTED_ITEMS, new DefaultTupleSet());
    }

    public ActivityMap getActions() {
        return m_actions;
    }
    
    // ------------------------------------------------------------------------
    // Data Methods
    /**
     * Add a data set to this visualization, using the given data group name.
     * A visual abstraction of the data will be created and registered with
     * the visualization. An exception will be thrown if the group name is
     * already in use.
     * @param group the data group name for the visualized data
     * @param data the data to visualize
     * @return a visual abstraction of the input data, a VisualTupleSet
     * instance
     */
    public synchronized VisualTupleSet add(String group, TupleSet data) {
        return add(group, data, null);
    }

    /**
     * Add a data set to this visualization, using the given data group name.
     * A visual abstraction of the data will be created and registered with
     * the visualization. An exception will be thrown if the group name is
     * already in use.
     * @param group the data group name for the visualized data
     * @param data the data to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input data set are visualized
     * @return a visual abstraction of the input data, a VisualTupleSet
     * instance
     */
    public synchronized VisualTupleSet add(
            String group, TupleSet data, Predicate filter) {
        if (data instanceof Table) {
            return addTable(group, (Table) data, filter);
        } else if (data instanceof Tree) {
            return addTree(group, (Tree) data, filter);
        } else if (data instanceof Graph) {
            return addGraph(group, (Graph) data, filter);
        } else {
            throw new IllegalArgumentException("Unsupported TupleSet type.");
        }
    }

    protected void checkGroupExists(String group) {
        if (m_visual.containsKey(group) || m_focus.containsKey(group)) {
            throw new IllegalArgumentException(
                    "Group name \'" + group + "\' already in use");
        }
    }

    protected void addDataGroup(String group, VisualTupleSet ts, TupleSet src) {
        checkGroupExists(group);
        m_visual.put(group, ts);
        if (src != null) {
            m_source.put(group, src);
        }
    }

    // -- Tables --------------------------------------------------------------
    /**
     * Add an empty VisualTable to this visualization, using the given data
     * group name. This adds a group of VisualItems that do not have a
     * backing data set, useful for creating interactive visual objects
     * that do not represent data. An exception will be thrown if the group
     * name is already in use.
     * @param group the data group name for the visualized data
     * @return the added VisualTable
     */
    public synchronized VisualTable addTable(String group) {
        VisualTable vt = new VisualTable(this, group);
        addDataGroup(group, vt, null);
        return vt;
    }

    /**
     * Add an empty VisualTable to this visualization, using the given data
     * group name and table schema. This adds a group of VisualItems that do
     * not have a backing data set, useful for creating interactive visual
     * objects that do not represent data. An exception will be thrown if the
     * group name is already in use.
     * @param group the data group name for the visualized data
     * @param schema the data schema to use for the VisualTable
     * @return the added VisualTable
     */
    public synchronized VisualTable addTable(String group, Schema schema) {
        VisualTable vt = new VisualTable(this, group, schema);
        addDataGroup(group, vt, null);
        return vt;
    }

    /**
     * Adds a data table to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized data
     * @param table the data table to visualize
     */
    public synchronized VisualTable addTable(String group, Table table) {
        return addTable(group, table, (Predicate) null);
    }

    /**
     * Adds a data table to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized data
     * @param table the data table to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input table are visualized
     */
    public synchronized VisualTable addTable(
            String group, Table table, Predicate filter) {
        VisualTable vt = new VisualTable(table, this, group, filter);
        addDataGroup(group, vt, table);
        return vt;
    }

    /**
     * Adds a data table to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized data
     * @param table the data table to visualize
     * @param schema the data schema to use for the created VisualTable
     */
    public synchronized VisualTable addTable(
            String group, Table table, Schema schema) {
        return addTable(group, table, null, schema);
    }

    /**
     * Adds a data table to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized data
     * @param table the data table to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input table are visualized
     * @param schema the data schema to use for the created VisualTable
     */
    public synchronized VisualTable addTable(
            String group, Table table, Predicate filter, Schema schema) {
        VisualTable vt = new VisualTable(table, this, group, filter, schema);
        addDataGroup(group, vt, table);
        return vt;
    }

    /**
     * Add a VisualTable to this visualization, using the table's
     * pre-set group name. An exception will be thrown if the group
     * name is already in use. This method allows you to insert custom
     * implementations of VisualTable into a Visualization. It is intended
     * for advanced users and should <b>NOT</b> be used if you do not know
     * what you are doing. In almost all cases, one of the other add methods
     * is preferred.
     * @param table the pre-built VisualTable to add
     * @return the added VisualTable
     */
    public synchronized VisualTable addTable(VisualTable table) {
        addDataGroup(table.getGroup(), table, table.getParentTable());
        table.setVisualization(this);
        return table;
    }

    // -- Graphs and Trees ----------------------------------------------------
    /**
     * Adds a graph to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized graph. The nodes
     * and edges will be available in the "group.nodes" and "group.edges"
     * subgroups.
     * @param graph the graph to visualize
     */
    public synchronized VisualGraph addGraph(String group, Graph graph) {
        return addGraph(group, graph, null);
    }

    /**
     * Adds a graph to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized graph. The nodes
     * and edges will be available in the "group.nodes" and "group.edges"
     * subgroups.
     * @param graph the graph to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input graph are visualized
     */
    public synchronized VisualGraph addGraph(
            String group, Graph graph, Predicate filter) {
        return addGraph(group, graph, filter, VisualItem.SCHEMA, VisualItem.SCHEMA);
    }

    /**
     * Adds a graph to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized graph. The nodes
     * and edges will be available in the "group.nodes" and "group.edges"
     * subgroups.
     * @param graph the graph to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input graph are visualized
     * @param nodeSchema the data schema to use for the visual node table
     * @param edgeSchema the data schema to use for the visual edge table
     */
    public synchronized VisualGraph addGraph(String group, Graph graph,
            Predicate filter, Schema nodeSchema, Schema edgeSchema) {
        checkGroupExists(group); // check before adding sub-tables
        String ngroup = PrefuseLib.getGroupName(group, Graph.NODES);
        String egroup = PrefuseLib.getGroupName(group, Graph.EDGES);

        VisualTable nt, et;
        nt = addTable(ngroup, graph.getNodeTable(), filter, nodeSchema);
        et = addTable(egroup, graph.getEdgeTable(), filter, edgeSchema);

        VisualGraph vg = new VisualGraph(nt, et,
                graph.isDirected(), graph.getNodeKeyField(),
                graph.getEdgeSourceField(), graph.getEdgeTargetField());
        vg.setVisualization(this);
        vg.setGroup(group);

        addDataGroup(group, vg, graph);

        TupleManager ntm = new TupleManager(nt, vg, TableNodeItem.class);
        TupleManager etm = new TupleManager(et, vg, TableEdgeItem.class);
        nt.setTupleManager(ntm);
        et.setTupleManager(etm);
        vg.setTupleManagers(ntm, etm);

        return vg;
    }

    /**
     * Adds a tree to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized tree. The nodes
     * and edges will be available in the "group.nodes" and "group.edges"
     * subgroups.
     * @param tree the tree to visualize
     */
    public synchronized VisualTree addTree(String group, Tree tree) {
        return addTree(group, tree, null);
    }

    /**
     * Adds a tree to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized tree. The nodes
     * and edges will be available in the "group.nodes" and "group.edges"
     * subgroups.
     * @param tree the tree to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input graph are visualized
     */
    public synchronized VisualTree addTree(
            String group, Tree tree, Predicate filter) {
        return addTree(group, tree, filter, VisualItem.SCHEMA, VisualItem.SCHEMA);
    }

    /**
     * Adds a tree to this visualization, using the given data group
     * name. A visual abstraction of the data will be created and registered
     * with the visualization. An exception will be thrown if the group name
     * is already in use.
     * @param group the data group name for the visualized tree. The nodes
     * and edges will be available in the "group.nodes" and "group.edges"
     * subgroups.
     * @param tree the tree to visualize
     * @param filter a filter Predicate determining which data Tuples in the
     * input graph are visualized
     * @param nodeSchema the data schema to use for the visual node table
     * @param edgeSchema the data schema to use for the visual edge table
     */
    public synchronized VisualTree addTree(String group, Tree tree,
            Predicate filter, Schema nodeSchema, Schema edgeSchema) {
        checkGroupExists(group); // check before adding sub-tables
        String ngroup = PrefuseLib.getGroupName(group, Graph.NODES);
        String egroup = PrefuseLib.getGroupName(group, Graph.EDGES);

        VisualTable nt, et;
        nt = addTable(ngroup, tree.getNodeTable(), filter, nodeSchema);
        et = addTable(egroup, tree.getEdgeTable(), filter, edgeSchema);

        VisualTree vt = new VisualTree(nt, et, tree.getNodeKeyField(),
                tree.getEdgeSourceField(), tree.getEdgeTargetField());
        vt.setVisualization(this);
        vt.setGroup(group);

        addDataGroup(group, vt, tree);

        TupleManager ntm = new TupleManager(nt, vt, TableNodeItem.class);
        TupleManager etm = new TupleManager(et, vt, TableEdgeItem.class);
        nt.setTupleManager(ntm);
        et.setTupleManager(etm);
        vt.setTupleManagers(ntm, etm);

        return vt;
    }

    // -- Aggregates ----------------------------------------------------------
    /**
     * Add a group of aggregates to this visualization. Aggregates are
     * used to visually represent groups of VisualItems.
     * @param group the data group name for the aggregates.
     * @return the generated AggregateTable
     * @see prefuse.visual.AggregateTable
     */
    public synchronized AggregateTable addAggregates(String group) {
        return addAggregates(group, VisualItem.SCHEMA);
    }

    /**
     * Add a group of aggregates to this visualization. Aggregates are
     * used to visually represent groups of VisualItems.
     * @param group the data group name for the aggregates.
     * @param schema the data schema to use for the AggregateTable
     * @return the generated AggregateTable
     * @see prefuse.visual.AggregateTable
     */
    public synchronized AggregateTable addAggregates(String group,
            Schema schema) {
        AggregateTable vat = new AggregateTable(this, group, schema);
        addDataGroup(group, vat, null);
        return vat;
    }

    // -- Derived Tables and Decorators ---------------------------------------
    /**
     * Add a derived table, a VisualTable that is cascaded from an
     * existing VisualTable. This is useful for creating VisualItems
     * that inherit a set of visual properties from another group of
     * VisualItems. This might be used, for example, in the creation
     * of small multiples where only a few visual attributes vary
     * across the multiples.
     * @param group the data group to use for the derived table
     * @param source the source data group to derive from
     * @param filter a Predicate filter indicating which tuples of the
     * source group should be inheritable by the new group
     * @param override a data schema indicating which data fields
     * should not be inherited, but managed locally by the derived group
     * @return the derived VisualTable
     */
    public synchronized VisualTable addDerivedTable(
            String group, String source, Predicate filter, Schema override) {
        VisualTable src = (VisualTable) getGroup(source);
        VisualTable vt = new VisualTable(src, this, group, filter, override);

        addDataGroup(group, vt, getSourceData(source));
        return vt;
    }

    /**
     * Add a group of decorators to an existing visual data group. Decorators
     * are VisualItem instances intended to "decorate" another VisualItem,
     * such as providing a label or dedicated interactive control, and are
     * realizeed as {@link prefuse.visual.DecoratorItem} instances that provide
     * access to the decorated item in addition to the standard VisualItem
     * properties. The generated table is created using the
     * {@link #addDerivedTable(String, String, Predicate, Schema)} method,
     * but with no VisualItem properties inherited from the source group.
     * @param group the data group to use for the decorators
     * @param source the source data group to decorate
     * @return the generated VisualTable of DecoratorItem instances
     */
    public synchronized VisualTable addDecorators(String group, String source) {
        return addDecorators(group, source, (Predicate) null);
    }

    /**
     * Add a group of decorators to an existing visual data group. Decorators
     * are VisualItem instances intended to "decorate" another VisualItem,
     * such as providing a label or dedicated interactive control, and are
     * realizeed as {@link prefuse.visual.DecoratorItem} instances that provide
     * access to the decorated item in addition to the standard VisualItem
     * properties.
     * @param group the data group to use for the decorators
     * @param source the source data group to decorate
     * @param schema schema indicating which variables should <b>not</b> be
     * inherited from the source data group and instead be managed locally
     * by the generated VisualTable
     * @return the generated VisualTable of DecoratorItem instances
     */
    public synchronized VisualTable addDecorators(
            String group, String source, Schema schema) {
        return addDecorators(group, source, null, schema);
    }

    /**
     * Add a group of decorators to an existing visual data group. Decorators
     * are VisualItem instances intended to "decorate" another VisualItem,
     * such as providing a label or dedicated interactive control, and are
     * realizeed as {@link prefuse.visual.DecoratorItem} instances that provide
     * access to the decorated item in addition to the standard VisualItem
     * properties.
     * @param group the data group to use for the decorators
     * @param source the source data group to decorate
     * @param filter a Predicate filter indicating which tuples of the
     * source group should be inheritable by the new group
     * @return the generated VisualTable of DecoratorItem instances
     */
    public synchronized VisualTable addDecorators(
            String group, String source, Predicate filter) {
        VisualTable t = addDerivedTable(group, source, filter, VisualItem.SCHEMA);
        t.setTupleManager(new TupleManager(t, null, TableDecoratorItem.class));
        return t;
    }

    /**
     * Add a group of decorators to an existing visual data group. Decorators
     * are VisualItem instances intended to "decorate" another VisualItem,
     * such as providing a label or dedicated interactive control, and are
     * realizeed as {@link prefuse.visual.DecoratorItem} instances that provide
     * access to the decorated item in addition to the standard VisualItem
     * properties.
     * @param group the data group to use for the decorators
     * @param source the source data group to decorate
     * @param filter a Predicate filter indicating which tuples of the
     * source group should be inheritable by the new group
     * @param schema schema indicating which variables should <b>not</b> be
     * inherited from the source data group and instead be managed locally
     * by the generated VisualTable
     * @return the generated VisualTable of DecoratorItem instances
     */
    public synchronized VisualTable addDecorators(
            String group, String source, Predicate filter, Schema schema) {
        VisualTable t = addDerivedTable(group, source, filter, schema);
        t.setTupleManager(new TupleManager(t, null, TableDecoratorItem.class));
        return t;
    }

    // -- Data Removal --------------------------------------------------------
    /**
     * Removes a data group from this Visualization. If the group is a focus
     * group, the group will simply be removed, and any subsequent attempts to
     * retrieve the group will return null. If the group is a primary group, it
     * will be removed, and any members of the group will also be removed
     * from any registered focus groups.
     * @param group the data group to remove
     * @return true if the group was found and removed, false if the group
     * was not found in this visualization.
     */
    public synchronized boolean removeGroup(String group) {
        // check for focus group first
        TupleSet ts = getFocusGroup(group);
        if (ts != null) {
            // invalidate the item to reflect group membership change
            for (Iterator items = ts.tuples(ValidatedPredicate.TRUE);
                    items.hasNext();) {
                ((VisualItem) items.next()).setValidated(false);
            }
            ts.clear(); // trigger group removal callback
            m_focus.remove(group);
            return true;
        }

        // focus group not found, check for primary group
        ts = getVisualGroup(group);
        if (ts == null) {
            // exit with false if group not found
            return false;
        }
        // remove group members from focus sets and invalidate them
        TupleSet[] focus = new TupleSet[m_focus.size()];
        m_focus.values().toArray(focus);
        for (Iterator items = ts.tuples(); items.hasNext();) {
            VisualItem item = (VisualItem) items.next();
            for (int j = 0; j < focus.length; ++j) {
                try {
                    focus[j].removeTuple(item);
                } catch (java.lang.UnsupportedOperationException e) {
                    focus[j].clear();
                  //  System.out.println("error in removing focus item tuple");
                }
            }
            item.setValidated(false);
        }
        // remove data
        if (ts instanceof CompositeTupleSet) {
            CompositeTupleSet cts = (CompositeTupleSet) ts;
            for (Iterator names = cts.setNames(); names.hasNext();) {
                String name = (String) names.next();
                String subgroup = PrefuseLib.getGroupName(group, name);
                m_visual.remove(subgroup);
                m_source.remove(subgroup);
            }
        }
        m_visual.remove(group);
        m_source.remove(group);
        return true;
    }

    /**
     * Reset this visualization, clearing out all visualization tuples. All
     * data sets added using the "addXXX" methods will be removed from the
     * visualization. All registered focus groups added using the 
     * addFocusGroup() methods will be retained, but will be cleared of all
     * tuples.
     */
    public synchronized void reset() {
        
        setRendererFactory(null);
        // first clear out all the focus groups
        Iterator iter = m_focus.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            TupleSet ts = (TupleSet) entry.getValue();
            ts.clear();
        }
        // finally clear out all map entries

        LinkedHashMap processed_m_visual = new LinkedHashMap(m_visual);
        iter = processed_m_visual.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();

            removeGroup(key);

        }

        LinkedHashMap processed_m_source = new LinkedHashMap(m_source);
        iter = processed_m_source.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            removeGroup(key);
        }

        for (Object name : m_actions.allKeys()) {
            cancel(name.toString());
        }
        ActivityManager.stopThread();
        //   m_actions.clear();
//        m_actions.
//          LinkedHashMap processed_m_actions = new LinkedHashMap(m_actions);
//        iter = processed_m_actions.keySet().iterator();
//        while (iter.hasNext()) {
//            String key = (String) iter.next();
//            removeGroup(key);
//        }


//        m_visual.clear();
//        m_source.clear();
//      //  m_actions = new ActivityMap();
//       m_focus = new HashMap();
       
       
         m_actions = new ActivityMap();
        m_renderers = new DefaultRendererFactory();
        m_visual = new LinkedHashMap();
        m_source = new HashMap();
        m_focus = new HashMap();
        m_displays = new ArrayList();

        addFocusGroup(Visualization.FOCUS_ITEMS, new DefaultTupleSet());
        addFocusGroup(Visualization.SELECTED_ITEMS, new DefaultTupleSet());
    }

    // ------------------------------------------------------------------------
    // Groups
    /**
     * Get the source data TupleSet backing the given visual data group.
     * @return the backing source data set, or null if there is no such
     * data set
     */
    public TupleSet getSourceData(String group) {
        return (TupleSet) m_source.get(group);
    }

    /**
     * Get the source data TupleSet backing the given visual tuple set.
     * @return the backing source data set, or null if there is no such
     * data set
     */
    public TupleSet getSourceData(VisualTupleSet ts) {
        return (TupleSet) m_source.get(ts.getGroup());
    }

    /**
     * Get the Tuple from a backing source data set that corresponds most
     * closely to the given VisualItem.
     * @param item the VisualItem for which to retreive the source tuple
     * @return the data source tuple, or null if no such tuple could
     * be found
     */
    public Tuple getSourceTuple(VisualItem item) {
        // get the source group and tuple set, exit if none
        String group = item.getGroup();
        TupleSet source = getSourceData(group);
        if (source == null) {
            return null;
        }

        // first get the source table and row value
        int row = item.getRow();
        Table t = item.getTable();
        while (t instanceof VisualTable) {
            VisualTable vt = (VisualTable) t;
            row = vt.getParentRow(row);
            t = vt.getParentTable();
        }

        // now get the appropriate source tuple
        // graphs maintain their own tuple managers so treat them specially
        String cgroup = PrefuseLib.getChildGroup(group);
        if (cgroup != null) {
            String pgroup = PrefuseLib.getParentGroup(group);
            Graph g = (Graph) getSourceData(pgroup);
            if (t == g.getNodeTable()) {
                return g.getNode(row);
            } else {
                return g.getEdge(row);
            }
        } else {
            return t.getTuple(row);
        }
    }

    /**
     * Get the VisualItem associated with a source data tuple, if it exists.
     * @param group the data group from which to lookup the source tuple,
     * only primary visual groups are valid, focus groups will not work
     * @param t the source data tuple
     * @return the associated VisualItem from the given data group, or
     * null if no such VisualItem exists
     */
    public VisualItem getVisualItem(String group, Tuple t) {
        TupleSet ts = getVisualGroup(group);
        VisualTable vt;
        if (ts instanceof VisualTable) {
            vt = (VisualTable) ts;
        } else if (ts instanceof Graph) {
            Graph g = (Graph) ts;
            vt = (VisualTable) (t instanceof Node ? g.getNodeTable()
                    : g.getEdgeTable());
        } else {
            return null;
        }
        int pr = t.getRow();
        int cr = vt.getChildRow(pr);
        return cr < 0 ? null : vt.getItem(cr);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the TupleSet associated with the given data group name. 
     * @param group a visual data group name
     * @return the data group TupleSet
     */
    public TupleSet getGroup(String group) {
        TupleSet ts = getVisualGroup(group);
        if (ts == null) {
            ts = getFocusGroup(group);
        }
        return ts;
    }

    /**
     * Indicates if a given VisualItem is contained in the given visual
     * data group.
     * @param item the VisualItem instance
     * @param group the data group to check for containment
     * @return true if the VisualItem is in the group, false otherwise
     */
    public boolean isInGroup(VisualItem item, String group) {
        if (ALL_ITEMS.equals(group)) {
            return true;
        }
        if (item.getGroup() == group) {
            return true;
        }

        TupleSet tset = getGroup(group);
        return (tset == null ? false : tset.containsTuple(item));
    }

    /**
     * Add a new secondary, or focus, group to this visualization. By
     * default the added group is an instance of
     * {@link prefuse.data.tuple.DefaultTupleSet}.
     * @param group the name of the focus group to add
     */
    public void addFocusGroup(String group) {
        checkGroupExists(group);
        m_focus.put(group, new DefaultTupleSet());
    }

    /**
     * Add a new secondary, or focus, group to this visualization.
     * @param group the name of the focus group to add
     * @param tset the TupleSet for the focus group
     */
    public void addFocusGroup(String group, TupleSet tset) {
        checkGroupExists(group);
        m_focus.put(group, tset);
    }

    // ------------------------------------------------------------------------
    // VisualItems
    /**
     * Get the size of the given visual data group.
     * @param group the visual data group
     * @return the size (number of tuples) of the group
     */
    public int size(String group) {
        TupleSet tset = getGroup(group);
        return (tset == null ? 0 : tset.getTupleCount());
    }

    /**
     * Retrieve the visual data group of the given group name. Only primary
     * visual groups will be considered.
     * @param group the visual data group
     * @return the requested data group, or null if not found
     */
    public TupleSet getVisualGroup(String group) {
        return (TupleSet) m_visual.get(group);
    }

    /**
     * Retrieve the focus data group of the given group name. Only secondary,
     * or focus, groups will be considered.
     * @param group the focus data group
     * @return the requested data group, or null if not found
     */
    public TupleSet getFocusGroup(String group) {
        return (TupleSet) m_focus.get(group);
    }

    /**
     * Invalidate the bounds of all VisualItems in the given group. This
     * will cause the bounds to be recomputed for all items upon the next
     * redraw.
     * @param group the visual data group to invalidate
     */
    public void invalidate(String group) {
        Iterator items = items(group, ValidatedPredicate.TRUE);
        while (items.hasNext()) {
            VisualItem item = (VisualItem) items.next();
            item.setValidated(false);
        }
    }

    /**
     * Invalidate the bounds of all VisualItems in this visualization. This
     * will cause the bounds to be recomputed for all items upon the next
     * redraw.
     */
    public void invalidateAll() {
        invalidate(ALL_ITEMS);
    }

    /**
     * Get an iterator over all visible items.
     * @return an iterator over all visible items.
     */
    public Iterator visibleItems() {
        return items(VisiblePredicate.TRUE);
    }

    /**
     * Get an iterator over all visible items in the specified group.
     * @param group the visual data group name
     * @return an iterator over all visible items in the specified group
     */
    public Iterator visibleItems(String group) {
        return items(group, VisiblePredicate.TRUE);
    }

    /**
     * Get an iterator over all items, visible or not.
     * @return an iterator over all items, visible or not.
     */
    public Iterator items() {
        return items((Predicate) null);
    }

    /**
     * Get an iterator over all items which match the given
     * Predicate filter.
     * @param filter a Predicate indicating which items should be included
     * in the iteration
     * @return a filtered iterator over VisualItems
     */
    public Iterator items(Predicate filter) {
        int size = m_visual.size();
        if (size == 0) {
            return Collections.EMPTY_LIST.iterator();
        } else if (size == 1) {
            Iterator it = m_visual.keySet().iterator();
            return items((String) it.next(), filter);
        } else {
            CompositeIterator iter = new CompositeIterator(m_visual.size());
            Iterator it = m_visual.keySet().iterator();
            for (int i = 0; it.hasNext();) {
                String group = (String) it.next();
                if (!PrefuseLib.isChildGroup(group)) {
                    iter.setIterator(i++, items(group, filter));
                }
            }
            return iter;
        }
    }

    /**
     * Get an iterator over all items in the specified group.
     * @param group the visual data group name
     * @return an iterator over all items in the specified group.
     */
    public Iterator items(String group) {
        return items(group, (Predicate) null);
    }

    /**
     * Get an iterator over all items in the given group which match the given
     * filter expression.
     * @param group the visual data group to iterate over
     * @param expr an expression string that should parse to a Predicate
     * indicating which items should be included in the iteration. The input
     * string will be parsed using the
     * {@link prefuse.data.expression.parser.ExpressionParser} class. If a
     * parse error occurs, an empty iterator is returned.
     * @return a filtered iterator over VisualItems
     */
    public Iterator items(String group, String expr) {
        Expression e = ExpressionParser.parse(expr);
        if (!(e instanceof Predicate) || ExpressionParser.getError() != null) {
            return Collections.EMPTY_LIST.iterator();
        }
        return items(group, (Predicate) e);
    }

    /**
     * Get an iterator over all items in the given group which match the given
     * Predicate filter.
     * @param group the visual data group to iterate over
     * @param filter a Predicate indicating which items should be included in
     * the iteration.
     * @return a filtered iterator over VisualItems
     */
    public Iterator items(String group, Predicate filter) {
        if (ALL_ITEMS.equals(group)) {
            return items(filter);
        }

        TupleSet t = getGroup(group);
        return (t == null ? Collections.EMPTY_LIST.iterator()
                : t.tuples(filter));
    }

    // ------------------------------------------------------------------------
    // Batch Methods
    /**
     * Set a data field value for all items in a given data group matching a
     * given filter predicate.
     * @param group the visual data group name
     * @param p the filter predicate determining which items to modify
     * @param field the data field / column name to set
     * @param val the value to set
     */
    public void setValue(String group, Predicate p, String field, Object val) {
        Iterator items = items(group, p);
        while (items.hasNext()) {
            VisualItem item = (VisualItem) items.next();
            item.set(field, val);
        }
    }

    /**
     * Sets the visbility status for all items in a given data group matching
     * a given filter predicate.
     * @param group the visual data group name
     * @param p the filter predicate determining which items to modify
     * @param value the visibility value to set
     */
    public void setVisible(String group, Predicate p, boolean value) {
        Iterator items = items(group, p);
        while (items.hasNext()) {
            VisualItem item = (VisualItem) items.next();
            item.setVisible(value);
        }
    }

    /**
     * Sets the interactivity status for all items in a given data group
     * matching a given filter predicate.
     * @param group the visual data group name
     * @param p the filter predicate determining which items to modify
     * @param value the interactivity value to set
     */
    public void setInteractive(String group, Predicate p, boolean value) {
        Iterator items = items(group, p);
        while (items.hasNext()) {
            VisualItem item = (VisualItem) items.next();
            item.setInteractive(value);
        }
    }

    // ------------------------------------------------------------------------
    // Action Methods
    /**
     * Add a data processing Action to this Visualization. The Action will be
     * updated to use this Visualization in its data processing.
     * @param name the name of the Action
     * @param action the Action to add
     */
    public Action putAction(String name, Action action) {
        action.setVisualization(this);
        m_actions.put(name, action);
        return action;
    }

    /**
     * Get the data processing Action with the given name.
     * @param name the name of the Action
     * @return the requested Action, or null if the name was not found
     */
    public Action getAction(String name) {
        return (Action) m_actions.get(name);
    }

    /**
     * Remove a data processing Action registered with this visualization.
     * If the removed action is currently running, it will be canceled.
     * The visualization reference held by the removed Action will be set to
     * null.<br/>
     * <strong>NOTE:</strong> Errors may occur if the removed Action is 
     * included in an "always run after" relation with another registered
     * Action that has not been removed from this visualization. It is the
     * currently the responsibility of clients to avoid this situation. 
     * @param name the name of the Action
     * @return the removed Action, or null if no action was found
     */
    public Action removeAction(String name) {
        // TODO: create registry of always run after relations to automatically
        // resolve action references?
        Action a = getAction(name);
        if (a != null) {
            a.cancel();
            m_actions.remove(name);
            a.setVisualization(null);
        }
        return a;
    }

    /**
     * Schedule the Action with the given name to run immediately. The running
     * of all Actions is managed by the
     * {@link prefuse.activity.ActivityManager}, which runs in a dedicated
     * thread.
     * @param action the name of the Action to run
     * @return the Action scheduled to run
     */
    public Activity run(String action) {
        return m_actions.run(action);
    }

    /**
     * Schedule the Action with the given name to run after the specified
     * delay. The running of all Actions is managed by the
     * {@link prefuse.activity.ActivityManager}, which runs in a dedicated
     * thread.
     * @param action the name of the Action to run
     * @param delay the amount of time to wait, in milliseconds, before
     * running the Action
     * @return the Action scheduled to run
     */
    public Activity runAfter(String action, long delay) {
        return m_actions.runAt(action, System.currentTimeMillis() + delay);
    }

    /**
     * Schedule the Action with the given name to run at the specified
     * time. The running of all Actions is managed by the
     * {@link prefuse.activity.ActivityManager}, which runs in a dedicated
     * thread.
     * @param action the name of the Action to run
     * @param startTime the absolute system time, in milliseconds since the
     * epoch, at which to run the Action.
     * @return the Action scheduled to run
     */
    public Activity runAt(String action, long startTime) {
        return m_actions.runAt(action, startTime);
    }

    /**
     * Schedule the Action with the given name to run after another Action
     * finishes running. This relationship will only hold for one round of
     * scheduling. If the "before" Action is run a second time, the "after"
     * action will not be run a second time. The running of all Actions is
     * managed by the {@link prefuse.activity.ActivityManager}, which runs
     * in a dedicated thread.
     * @param before the name of the Action to wait for
     * @param after the name of the Action to run after the first one finishes
     * @return the Action scheduled to run after the first one finishes
     */
    public Activity runAfter(String before, String after) {
        return m_actions.runAfter(before, after);
    }

    /**
     * Schedule the Action with the given name to always run after another Action
     * finishes running. The running of all Actions is managed by the
     * {@link prefuse.activity.ActivityManager}, which runs in a dedicated
     * thread.
     * @param before the name of the Action to wait for
     * @param after the name of the Action to run after the first one finishes
     * @return the Action scheduled to always run after the first one finishes
     */
    public Activity alwaysRunAfter(String before, String after) {
        return m_actions.alwaysRunAfter(before, after);
    }

    /**
     * Cancel the Action with the given name, if it has been scheduled.
     * @param action the name of the Action to cancel
     * @return the canceled Action
     */
    public Activity cancel(String action) {
        return m_actions.cancel(action);
    }

    // ------------------------------------------------------------------------
    // Renderers
    /**
     * Set the RendererFactory used by this Visualization. The RendererFactory
     * is responsible for providing the Renderer instances used to draw
     * the VisualItems.
     * @param rf the RendererFactory to use.
     */
    public void setRendererFactory(RendererFactory rf) {
        invalidateAll();
        m_renderers = rf;
    }

    /**
     * Get the RendererFactory used by this Visualization.
     * @return this Visualization's RendererFactory
     */
    public RendererFactory getRendererFactory() {
        return m_renderers;
    }

    /**
     * Get the renderer for the given item. Consults this visualization's
     * {@link prefuse.render.RendererFactory} and returns the result.
     * @param item the item to retreive the renderer for
     * @return the {@link prefuse.render.Renderer} for drawing the
     * given item
     */
    public Renderer getRenderer(VisualItem item) {
        if (item.getVisualization() != this) {
            throw new IllegalArgumentException(
                    "Input item not a member of this visualization.");
        }
        return m_renderers.getRenderer(item);
    }

    /**
     * Issue a repaint request, causing all displays associated with this
     * visualization to be repainted.
     */
    public synchronized void repaint() {
        Iterator items = items(ValidatedPredicate.FALSE);
        while (items.hasNext()) {
            ((VisualItem) items.next()).validateBounds();
        }
        for (int i = 0; i < m_displays.size(); ++i) {
            getDisplay(i).repaint();
        }
    }

    /**
     * Get the bounding rectangle for all items in the given group.
     * @param group the visual data group
     * @return the bounding box of the items
     */
    public Rectangle2D getBounds(String group) {
        return getBounds(group, new Rectangle2D.Double());
    }

    /**
     * Get the bounding rectangle for all items in the given group.
     * @param group the visual data group name
     * @param r a rectangle in which to store the computed bounding box
     * @return the input rectangle r, updated to hold the computed
     * bounding box
     */
    public Rectangle2D getBounds(String group, Rectangle2D r) {
        Iterator iter = visibleItems(group);
        if (iter.hasNext()) {
            VisualItem item = (VisualItem) iter.next();
            r.setRect(item.getBounds());
        }
        while (iter.hasNext()) {
            VisualItem item = (VisualItem) iter.next();
            Rectangle2D.union(item.getBounds(), r, r);
        }
        return r;
    }

    // ------------------------------------------------------------------------
    // Displays
    /**
     * Get the number of displays associated with this visualization.
     * @return the number of displays
     */
    public int getDisplayCount() {
        return m_displays.size();
    }

    /**
     * Add a display to this visualization. Called automatically by the
     * {@link prefuse.Display#setVisualization(Visualization)} method.
     * @param display the Display to add
     */
    void addDisplay(Display display) {
        m_displays.add(display);
    }

    /**
     * Get the display at the given list index. Displays are numbered by the
     * order in which they are added to this visualization.
     * @param idx the list index
     * @return the Display at the given index
     */
    public Display getDisplay(int idx) {
        return (Display) m_displays.get(idx);
    }

    /**
     * Remove a display from this visualization.
     * @param display the display to remove
     * @return true if the display was removed, false if it was not found
     */
    boolean removeDisplay(Display display) {
        return m_displays.remove(display);
    }

    /**
     * Report damage to associated displays, indicating a region that will need
     * to be redrawn.
     * @param item the item responsible for the damage
     * @param region the damaged region, in item-space coordinates
     */
    public void damageReport(VisualItem item, Rectangle2D region) {
        for (int i = 0; i < m_displays.size(); ++i) {
            Display d = getDisplay(i);
            if (d.getPredicate().getBoolean(item)) {
                d.damageReport(region);
            }
        }
    }
} // end of class Visualization
