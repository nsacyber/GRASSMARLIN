package prefuse.data.search;

import java.util.Iterator;

import prefuse.data.Tuple;
import prefuse.data.tuple.DefaultTupleSet;

/**
 * <p>Abstract base class for TupleSet implementations that support text
 * search. These sets provide search engine functionality -- Tuple data fields
 * can be indexed and then searched over using text queries, the results of
 * which populate the TupleSet. A range of search techniques are provided by
 * subclasses of this class.</p>
 * 
 * <p>
 * <b>NOTE:</b> The {@link #addTuple(Tuple)} and
 * {@link #removeTuple(Tuple)}, methods are not supported by this 
 * implementation or its derived classes. Calling these methods will result
 * in thrown exceptions. Instead, membership is determined by the search
 * matches found using the {@link #search(String) search} method, which
 * searches over the terms indexed using the {@link #index(Iterator, String)}
 * and {@link #index(Tuple, String)} methods.
 * </p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.data.query.SearchQueryBinding
 */
public abstract class SearchTupleSet extends DefaultTupleSet {
    
    /**
     * Returns the current search query, if any.
     * @return the currently active search query
     */
    public abstract String getQuery();
    
    /**
     * Searches the indexed fields of this TupleSet for matching
     * strings, adding the Tuple instances for each search match
     * to the TupleSet. The details of how the query is matched to
     * indexed fields is left to subclasses.
     * @param query the query string to search for. Indexed fields
     *  with matching text will be added to the TupleSet.
     */
    public abstract void search(String query);
    
    /**
     * Indexes the data values for the given field name for
     * each Tuple in the provided Iterator. These values are used
     * to construct an internal data structure allowing fast searches
     * over these attributes. To index multiple fields, simply call
     * this method multiple times with the desired field names.
     * @param tuples an Iterator over Tuple instances to index
     * @param field the name of the attribute to index
     * @throws ClassCastException is a non-Tuple instance is
     * encountered in the iteration.
     */
    public void index(Iterator tuples, String field) {
        while ( tuples.hasNext() ) {
            Tuple t = (Tuple)tuples.next();
            index(t, field);
        }
    }
    
    /**
     * Index an individual Tuple field, so that it can be searched for.
     * @param t the Tuple
     * @param field the data field to index
     */
    public abstract void index(Tuple t, String field);

    /**
     * Un-index an individual Tuple field, so that it can no longer be 
     * searched for.
     * @param t the Tuple
     * @param field the data field to unindex
     * @see #isUnindexSupported()
     */
    public abstract void unindex(Tuple t, String field);
    
    /**
     * Indicates if this TupleSearchSet supports the unindex operation.
     * @return true if unindex is supported, false otherwise.
     * @see #unindex(Tuple, String)
     */
    public abstract boolean isUnindexSupported();
    
    // ------------------------------------------------------------------------
    // Unsupported Operations
    
    /**
     * This method is not supported by this implementation. Don't call it!
     * Instead, use the {@link #search(String) search} or
     * {@link #clear() clear} methods.
     */
    public Tuple addTuple(Tuple t) {
        throw new UnsupportedOperationException();
    }
    /**
     * This method is not supported by this implementation. Don't call it!
     * Instead, use the {@link #search(String) search} or
     * {@link #clear() clear} methods.
     */
    public boolean removeTuple(Tuple t) {
        throw new UnsupportedOperationException();
    }
    
} // end of abstract class SearchTupleSet
