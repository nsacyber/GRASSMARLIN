/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.types;

// java
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 *
 * @param <T> The value type this Linkable will contain.
 * A Linked list implementation that contains both children - and siblings
 * @param <L> Linkable&lt;T,U,L&gt;
 */
public interface ByteTreeNode < T, L extends ByteTreeNode<T,L> > extends
    Comparable<T>,
    Collection<L> {
    
    /**
     *
     * @return the type T this Linkable contains.
     */
    T getValue();
	
    void setHash(Integer hash);
    
    /**
     * Instantiates this member if it is null.
     * @param sup Supplier of the sibling Set.
     */
//    void ifSiblingsNotPresent( Supplier<Set<L>> sup );
    
    /**
     *
     * @param l - The sibling node to add to this collectable. This is a one-ary relation.
     */
//	void addSibling( L l );
    
    /**
     *
     * @return Set&lt;Linkable&lt;T&gt;&gt; of siblings
     */
    Set<L> getSiblings();
    
    /**
     *
     * @param t Type value
     * @return True if children contain this value, else false
     */
    Optional<L> childContaining( T t );
    
    /**
     *
     * @param terminal Boolean flag set to mark this node as the source of
     * an unique data artifact.
     */
    void setTerminal(Boolean terminal);
    /**
     *
     * @return True if this Linkable is marked as terminal.
     * NOTE terminal does not indicate this is a leaf node - just that some 
     * unique data artifact exists here.
     */
    Boolean isTerminal();
    
    /**
     *
     * @return True if Linkable has children, else false.
     */
    boolean hasChildren();
	
    /**
     *
     * @return True if parent is present, else false.
     */
    boolean hasParent();
	
    /**
     *
     * @return the Collection&lt;Linkable&lt;T&gt;&gt; of this Linkable
     */
    Collection<L> getChildren();

    /**
     *
     * @param it - Iterator&lt;T&gt; to creates Linkable children for
     * @param depth - the depth of the children
     * @param cbDepth The tree depth for when the setup CB will trigger
     * @param setup the callback to run in the terminal nodes constructor.
     * @return - The terminal node created or found at the last T
     */
    L add(Iterator<T> it, Integer depth, Integer cbDepth, Consumer<L> setup);	
    /**
     *
     * @param parent - The Linkable&lt;T&gt; to attach as this Links parent.
     */
    void setParent( L parent );
    
    /**
     *
     * @return Linkable&lt;T&gt; parent
     * MAY be null
     */
    L getParent();
	
    /**
     *
     * @return Stream&lt;Linkable&lt;T&gt;&gt; of all upstream Linkable&lt;T&lt;
     */
    Stream<L> upstream();
    
    /**
     * @return The dree-depth of this Linkable
     */
    int depth();
    
    /**
     * @return True if VisualDetails has been instantiated, else false.
     */
    public boolean hasDetail();
    /**
     * Retrieves the VisualDetails member of this node.
     * MAY be null.
     * @return VisualDetails on this node.
     */
    public VisualDetails getDetails();
    /**
     * Instantiates this member if it is null.
     * @param sup Supplier of VisualDetails.
     */
    void ifDetailNotPresent(  Supplier<VisualDetails> sup );
   
	// we probably do not need to support these, if the goal is to spread 
    // modifiable surface on-demand then we do not need to pre-allocate
    // or mass-add things
    // most likely we'll call things like link.parent.remove(link) or other
    // cascading method-chains like that in implementation
    //
	@Override
	boolean containsAll(Collection<?> c);
	@Override
	boolean addAll(Collection<? extends L> c);
	@Override
	boolean removeAll(Collection<?> c);
	@Override
	boolean retainAll(Collection<?> c);

}
