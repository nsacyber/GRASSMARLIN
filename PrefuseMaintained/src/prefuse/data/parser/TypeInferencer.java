package prefuse.data.parser;

import java.util.ArrayList;

/**
 * Infers the data types for a table of data by testing each value
 * of the data against a bank of parsers and eliminating candidate
 * parsers that do not successfully parse the data. This class leverages
 * the mechanisms of {@link ParserFactory}, but while that class only
 * supports one data field at a time, TypeInferencer maintains a collection
 * of ParserFactory instances to infer type for multiple data columns
 * simultaneously.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see ParserFactory
 */
public class TypeInferencer {

    public ParserFactory m_template;
    public ArrayList m_factories = new ArrayList();
    
    /**
     * Create a new TypeInferencer using the default ParserFactory
     * settings, thus the default parsers and parser ordering will be used.
     */
    public TypeInferencer() {
        this(new ParserFactory());
    }

    /**
     * Create a new TypeInferenced using the input ParserFactory as a
     * template for the desired parsers to check and parser ordering
     * to use.
     * @param template the template ParserFactory to use for included
     * parsers and their precedence.
     */
    public TypeInferencer(ParserFactory template) {
        m_template = template;
    }
    
    // ------------------------------------------------------------------------
    
    private void rangeCheck(int column, boolean grow) {
        if ( column < 0 || (!grow && column >= m_factories.size()))
            throw new IndexOutOfBoundsException(
                "Index out of bounds: "+column);
        
        if ( column < m_factories.size() )
            return;
        
        for ( int i=m_factories.size(); i<=column; ++i )
            m_factories.add(m_template.clone());
    }
    
    /**
     * Sample the given text string for the given data column index.
     * @param column the data column index of the sample
     * @param value the text string sample
     */
    public void sample(int column, String value) {
        rangeCheck(column, true);
        ((ParserFactory)m_factories.get(column)).sample(value);
    }
    
    /**
     * Get the data type for the highest ranking candidate parser
     * still in the running for the given column index.
     * @param column the data column index
     * @return the currently inferred type of that column
     */
    public Class getType(int column) {
        return getParser(column).getType();
    }
    
    /**
     * Get the top-ranking candidate data parser for the given column index.
     * @param column the data column index
     * @return the data parser to use for that column
     */
    public DataParser getParser(int column) {
        rangeCheck(column, false);
        return ((ParserFactory)m_factories.get(column)).getParser();
    }
    
} // end of class TypeInferencer
