/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.visual;

import java.util.Iterator;

import prefuse.data.expression.Predicate;

/**
 * VisualItem that represents an aggregation of one or more other VisualItems.
 * AggregateItems include methods adding and removing items from the aggregate
 * collection, and are backed by an {@link AggregateTable} instance.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface AggregateItem extends VisualItem {
    
    /**
     * Get the size of this AggregateItem, the number of visual items
     * contained in the aggregation.
     * @return the aggregate size
     */
    public int getAggregateSize();
    
    /**
     * Indicates is a given VisualItem is contained in the aggregation.
     * @param item the VisualItem to check for containment
     * @return true if the given item is contained in this aggregate,
     * false otherwise.
     */
    public boolean containsItem(VisualItem item);
    
    /**
     * Add a VisualItem to this aggregate.
     * @param item the item to add
     */
    public void addItem(VisualItem item);
    
    /**
     * Remove a VisualItem from this aggregate.
     * @param item the item to remove
     */
    public void removeItem(VisualItem item);
    
    /**
     * Remove all items contained in this aggregate.
     */
    public void removeAllItems();
    
    /**
     * Get an iterator over all the items contained in this aggregate.
     * @return an iterator over the items in this aggregate
     */
    public Iterator items();
    
    /**
     * Get a filtered iterator over all the items contained in this aggregate.
     * @param filter a Predicate instance indicating the filter criteria
     * @return an iterator over the items in this aggregate
     */
    public Iterator items(Predicate filter);
    
} // end of interface AggregateItem
