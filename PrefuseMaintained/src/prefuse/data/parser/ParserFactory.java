package prefuse.data.parser;

import java.util.Arrays;

/**
 * Factory class that maintains a collection of parser instances and returns
 * the appropriate parser based on a history of samples presented to the
 * factory. The {@link #sample(String)} method takes a text string and tests
 * it against all available parsers, updating whether or not the parsers can
 * successfully parse the value. This method is used in a more automated
 * fashion by the {@link TypeInferencer} class.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see TypeInferencer
 */
public class ParserFactory implements Cloneable {
    
    private static final DataParser[] DEFAULT_PARSERS = 
        new DataParser[] {
            new IntParser(),
            new LongParser(),
            new DoubleParser(),
            new FloatParser(),
            new BooleanParser(),
            new ColorIntParser(),
            new DateParser(),
            new TimeParser(),
            new DateTimeParser(),
            new IntArrayParser(),
            new LongArrayParser(),
            new FloatArrayParser(),
            new DoubleArrayParser(),
            new StringParser()
        };
    
    private static ParserFactory DEFAULT_FACTORY =
        new ParserFactory(DEFAULT_PARSERS);
    
    private DataParser[] m_parsers;
    private boolean[]    m_isCandidate;
    
    /**
     * Returns the default parser factory. The default factory tests for the
     * following data types (in the provided order of precedence):
     *   int, long, double, float, boolean, Date, Time, DateTime, String.
     * @return the default parser factory.
     */
    public static ParserFactory getDefaultFactory() {
        return DEFAULT_FACTORY;
    }
    
    /**
     * Sets the default parser factory. This factory will be used by default
     * by all readers to parse data values.
     * @param factory the new default parser factory.
     */
    public static void setDefaultFactory(ParserFactory factory) {
    	DEFAULT_FACTORY = factory;
    }
    
    /**
     * Constructor. Uses a default collection of parsers, testing for the
     * following data type in the followinf order of precedence:
     *   int, long, double, float, boolean, Date, Time, DateTime, String.
     */
    public ParserFactory() {
        this(DEFAULT_PARSERS);
    }
    
    /**
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        return new ParserFactory(m_parsers);
    }
    
    /**
     * <p>Constructor. Takes an array of parsers to test. After creating this
     * instance, sample data values can be passed in using the 
     * <code>sample()</code> method, and this class will check the sample
     * against the parsers, computing which parsers can successfully parse the
     * sample. This process of elimination disregards inappropriate parsers.
     * After a series of samples, the <code>getParser()</code>
     * method can be used to retrieve the highest ranking candidate parser.
     * </p>
     * 
     * <p>
     * If no parser can parse all samples, a null value will be returned by
     * getParser(). For this reason, it is recommended to always use a
     * StringParser as the last element of the input array, as it is guaranteed
     * to always parse successfully (by simply returning its input String).
     * </p>
     * 
     * <p>
     * The ordering of parsers in the array is taken to be the desired order 
     * of precendence of the parsers. For example, if both parser[0] and 
     * parser[2] can parse all the available samples, parser[0] will be 
     * returned.
     * </p> 
     * @param parsers the input DataParsers to use.
     */
    public ParserFactory(DataParser[] parsers) {
        // check integrity of input
        for ( int i=0; i<parsers.length; ++i ) {
            if ( parsers[i] == null ) {
                throw new IllegalArgumentException(
                    "Input parsers must be non-null");
            }
        }
        // initialize member variables
        m_parsers = parsers;
        m_isCandidate = new boolean[m_parsers.length];
        reset();
    }
    
    /**
     * Reset the candidate parser settings, making each parser
     * equally likely.
     */
    protected void reset() {
        Arrays.fill(m_isCandidate, true);
    }
    
    /**
     * Sample a data value against the parsers, updating the
     * parser candidates.
     * @param val the String value to sample
     */
    protected void sample(String val) {
        for ( int i=0; i<m_parsers.length; ++i ) {
            if ( m_isCandidate[i] ) {
                m_isCandidate[i] = m_parsers[i].canParse(val);
            }
        }
    }
    
    /**
     * Returns the highest ranking parser that successfully can
     * parse all the input samples viewed by this instance. If
     * no such parser exists, a null value is returned.
     * @return the highest-ranking data parser, or null if none
     */
    protected DataParser getParser() {
        for ( int i=0; i<m_parsers.length; ++i ) {
            if ( m_isCandidate[i] ) {
                return m_parsers[i];
            }
        }
        return null;
    }
    
    /**
     * Returns a parser for the specified data type.
     * @param type the Class for the data type to parse
     * @return a parser for the given data type, or null
     * if no such parser can be found.
     */
    public DataParser getParser(Class type) {
       for ( int i=0; i<m_parsers.length; ++i ) {
           if ( m_parsers[i].getType().equals(type) ) {
               return m_parsers[i];
           }
       }
       return null;
    }
    
    /**
     * Analyzes the given array of String values to determine an
     * acceptable parser data type.
     * @param data an array of String values to parse
     * @param startRow the row from which to begin analyzing the
     * data array, allowing header rows to be excluded.
     * @return the appropriate parser for the inferred data type,
     * of null if none.
     */
    public DataParser getParser(String[] data, int startRow) {
        return getParser(new String[][] { data }, 0, startRow);
    }
    
    /**
     * Analyzes a column of the given array of String values to 
     * determine an acceptable parser data type.
     * @param data an 2D array of String values to parse
     * @param col an index for the column to process
     * @param startRow the row from which to begin analyzing the
     * data array, allowing header rows to be excluded.
     * @return the appropriate parser for the inferred data type,
     * of null if none.
     */
    public DataParser getParser(String[][] data, int col, int startRow) {
        // sanity check input 
        if ( data == null || data.length == 0 )
            return null;
        
        int nrows = data.length;
        
        // analyze each column in turn
        this.reset();
        for ( int row=startRow; row<nrows; ++row ) {
            this.sample(data[row][col]);
        }
        
        DataParser parser = getParser();
        return parser;
    }
    
} // end of class ParserFactory
