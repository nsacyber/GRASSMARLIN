package prefuse.data.search;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hits;

import prefuse.data.Tuple;
import prefuse.util.StringLib;
import prefuse.util.collections.IntObjectHashMap;

/**
 * <p>
 * SearchTupleSet implementation that performs text searches on indexed Tuple
 * data using the Lucene search engine.
 * <a href="http://lucene.apache.org/">Lucene</a> is an open source search
 * engine supporting full text indexing and keyword search. Please refer to
 * the Lucene web page for more information. Note that for this class to be
 * used by prefuse applications, the Lucene classes must be included on the
 * application classpath.
 * </p> 
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.data.query.SearchQueryBinding
 */
public class KeywordSearchTupleSet extends SearchTupleSet {
    
    private static final Logger s_logger 
        = Logger.getLogger(KeywordSearchTupleSet.class.getName());
    
    protected IntObjectHashMap m_map = new IntObjectHashMap();
    protected String m_query = "";
    
    protected LuceneSearcher m_lucene = null;
    protected boolean m_storeTermVectors = false;
    
    protected int m_id = 1;
    
    /**
     * Creates a new KeywordSearchFocusSet using an in-memory search index.
     */
    public KeywordSearchTupleSet() {
        m_lucene = new LuceneSearcher();
    }
    
    /**
     * Creates a new TextSearchFocusSet with the given LuceneSearcher.
     * @param searcher the {@link LuceneSearcher} to use.
     */
    public KeywordSearchTupleSet(LuceneSearcher searcher) {
        m_lucene = searcher;
    }
    
    /**
     * Returns the current search query, if any.
     * @return the currently active search query
     */
    public String getQuery() {
        return m_query;
    }
    
    /**
     * Searches the indexed Tuple fields for matching keywords, using
     * the Lucene search engine. Matching Tuples are available as the
     * members of this TupleSet.
     * @param query the query string to search for
     */
    public void search(String query) {
        if ( query == null )
            query = "";
        
        if ( query.equals(m_query) )
            return; // no change
        
        Tuple[] rem = clearInternal();
        m_query = query;
        
        query.trim();
        if ( query.length() == 0 ) {
            this.fireTupleEvent(null, DELETE);
            return;
        }
        
        m_lucene.setReadMode(true);
        try {
            Hits hits = m_lucene.search(query);
            for ( int i=0; i < hits.length(); i++ ) {
                Tuple t = getMatchingTuple(hits.doc(i));
                addInternal(t);
            }
            Tuple[] add = getTupleCount() > 0 ? toArray() : null;
            fireTupleEvent(add, rem);
        } catch (ParseException e) {
            s_logger.warning("Lucene query parse exception.\n"+
                    StringLib.getStackTrace(e));
        } catch (IOException e) {
            s_logger.warning("Lucene IO exception.\n"+
                    StringLib.getStackTrace(e));
        }
        
    }
    
    /**
     * Return the Tuple matching the given Lucene Document, if any.
     * @param d the Document to lookup.
     * @return the matching Tuple, or null if none.
     */
    protected Tuple getMatchingTuple(Document d) {
        int id = Integer.parseInt(d.get(LuceneSearcher.ID));
        return (Tuple)m_map.get(id);
    }
    
    /**
     * @see prefuse.data.search.SearchTupleSet#index(prefuse.data.Tuple, java.lang.String)
     */
    public void index(Tuple t, String field) {
        m_lucene.setReadMode(false);
        String s;
        if ( (s=t.getString(field)) == null ) return;
        
        int id = m_id++;
        m_lucene.addDocument(getDocument(id, s));        
        m_map.put(id, t);
    }

    /**
     * Returns false, as unindexing values is not currently supported.
     * @see prefuse.data.search.SearchTupleSet#isUnindexSupported()
     */
    public boolean isUnindexSupported() {
        return false;
    }
    
    /**
     * This method throws an exception, as unidexing is not supported.
     * @see prefuse.data.search.SearchTupleSet#unindex(prefuse.data.Tuple, java.lang.String)
     * @throws UnsupportedOperationException
     */
    public void unindex(Tuple t, String attrName) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Create a Lucene Document instance with the given document ID and text.
     * @param id the document ID
     * @param text the text the Document should contain
     * @return a new Lucene Document instance
     */
    protected Document getDocument(int id, String text) {
        Document d = new Document();
        d.add(Field.Text(LuceneSearcher.FIELD, text, m_storeTermVectors));
        d.add(Field.Keyword(LuceneSearcher.ID, String.valueOf(id)));
        return d;
    }
    
    /**
     * Get the {@link LuceneSearcher} instance used by this class.
     * @return returns the backing lucene searcher.
     */
    public LuceneSearcher getLuceneSearcher() {
        return m_lucene;
    }
    
    /**
     * Returns a copy of the mapping from Lucene document IDs to prefuse Tuple instances.
     * @return a copy of the map from lucene doc IDs to prefuse Tuples.
     */
    public IntObjectHashMap getTupleMap() {
        return (IntObjectHashMap)m_map.clone();
    }
    
    /**
     * Removes all search hits and clears out the index.
     * @see prefuse.data.tuple.TupleSet#clear()
     */
    public void clear() {
        m_lucene = new LuceneSearcher();
        super.clear();
    }
    
}  // end of class KeywordSearchTupleSet
