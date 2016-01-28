package prefuse.data.io;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.parser.DataParseException;
import prefuse.data.parser.DataParser;
import prefuse.data.parser.ParserFactory;
import prefuse.util.collections.IntIterator;


/**
 * GraphReader instance that reads in graph file formatted using the
 * GraphML file format. GraphML is an XML format supporting graph
 * structure and typed data schemas for both nodes and edges. For more
 * information about the format, please see the
 * <a href="http://graphml.graphdrawing.org/">GraphML home page</a>.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class GraphMLReader extends AbstractGraphReader  implements GraphReader {
    
    /**
     * @see prefuse.data.io.GraphReader#readGraph(java.io.InputStream)
     */
    public Graph readGraph(InputStream is) throws DataIOException {
        try {       
            SAXParserFactory factory   = SAXParserFactory.newInstance();
            SAXParser        saxParser = factory.newSAXParser();
            
            GraphMLHandler   handler   = new GraphMLHandler();
            saxParser.parse(is, handler);
            return handler.getGraph();
        } catch ( Exception e ) {
            if ( e instanceof DataIOException ) {
                throw (DataIOException)e;
            } else {
                throw new DataIOException(e);
            }
        }
    }
    
    /**
     * String tokens used in the GraphML format.
     */
    public interface Tokens {
        public static final String ID         = "id";
        public static final String GRAPH      = "graph";
        public static final String EDGEDEF    = "edgedefault";
        public static final String DIRECTED   = "directed";
        public static final String UNDIRECTED = "undirected";
        
        public static final String KEY        = "key";
        public static final String FOR        = "for";
        public static final String ALL        = "all";
        public static final String ATTRNAME   = "attr.name";
        public static final String ATTRTYPE   = "attr.type";
        public static final String DEFAULT    = "default";
        
        public static final String NODE   = "node";
        public static final String EDGE   = "edge";
        public static final String SOURCE = "source";
        public static final String TARGET = "target";
        public static final String DATA   = "data";
        public static final String TYPE   = "type";
        
        public static final String INT = "int";
        public static final String INTEGER = "integer";
        public static final String LONG = "long";
        public static final String FLOAT = "float";
        public static final String DOUBLE = "double";
        public static final String REAL = "real";
        public static final String BOOLEAN = "boolean";
        public static final String STRING = "string";
        public static final String DATE = "date";
    }
    
    /**
     * A SAX Parser for GraphML data files.
     */
    public static class GraphMLHandler extends DefaultHandler implements Tokens
    {
        protected ParserFactory m_pf = ParserFactory.getDefaultFactory();
        
        protected static final String SRC = Graph.DEFAULT_SOURCE_KEY;
        protected static final String TRG = Graph.DEFAULT_TARGET_KEY;
        protected static final String SRCID = SRC+'_'+ID;
        protected static final String TRGID = TRG+'_'+ID;
        
        protected Schema m_nsch = new Schema();
        protected Schema m_esch = new Schema();
        
        protected String m_graphid;
        protected Graph m_graph = null;
        protected Table m_nodes;
        protected Table m_edges;
        
        // schema parsing
        protected String m_id;
        protected String m_for;
        protected String m_name;
        protected String m_type;
        protected String m_dflt;
        
        protected StringBuffer m_sbuf = new StringBuffer();
        
        // node,edge,data parsing
        private String m_key;
        private int m_row = -1;
        private Table m_table = null;
        protected HashMap m_nodeMap = new HashMap();
        protected HashMap m_idMap = new HashMap();
        
        private boolean m_directed = false;
        private boolean inSchema;
        
        public void startDocument() {
            m_nodeMap.clear();
            inSchema = true;
            
            m_esch.addColumn(SRC, int.class);
            m_esch.addColumn(TRG, int.class);
            m_esch.addColumn(SRCID, String.class);
            m_esch.addColumn(TRGID, String.class);
        }
        
        public void endDocument() throws SAXException {
            // time to actually set up the edges
            IntIterator rows = m_edges.rows();
            while (rows.hasNext()) {
                int r = rows.nextInt();

                String src = m_edges.getString(r, SRCID);
                if (!m_nodeMap.containsKey(src)) {
                    throw new SAXException(
                        "Tried to create edge with source node id=" + src
                        + " which does not exist.");
                }
                int s = ((Integer) m_nodeMap.get(src)).intValue();
                m_edges.setInt(r, SRC, s);

                String trg = m_edges.getString(r, TRGID);
                if (!m_nodeMap.containsKey(trg)) {
                    throw new SAXException(
                        "Tried to create edge with target node id=" + trg
                        + " which does not exist.");
                }
                int t = ((Integer) m_nodeMap.get(trg)).intValue();
                m_edges.setInt(r, TRG, t);
            }
            m_edges.removeColumn(SRCID);
            m_edges.removeColumn(TRGID);

            // now create the graph
            m_graph = new Graph(m_nodes, m_edges, m_directed);
            if (m_graphid != null)
                m_graph.putClientProperty(ID, m_graphid);
        }
        
        public void startElement(String namespaceURI, String localName, 
                                 String qName, Attributes atts)
        {
            // first clear the character buffer
            m_sbuf.delete(0, m_sbuf.length());
            
            if ( qName.equals(GRAPH) )
            {
                // parse directedness default
                String edef = atts.getValue(EDGEDEF);
                m_directed = DIRECTED.equalsIgnoreCase(edef);
                m_graphid = atts.getValue(ID);
            }
            else if ( qName.equals(KEY) )
            {
                if ( !inSchema ) {
                    error("\""+KEY+"\" elements can not"
                        + " occur after the first node or edge declaration.");
                }
                m_for = atts.getValue(FOR);
                m_id = atts.getValue(ID);
                m_name = atts.getValue(ATTRNAME);
                m_type = atts.getValue(ATTRTYPE);
            }
            else if ( qName.equals(NODE) )
            {
                schemaCheck();
                
                m_row = m_nodes.addRow();
                
                String id = atts.getValue(ID);
                m_nodeMap.put(id, new Integer(m_row));
                m_table = m_nodes;
            }
            else if ( qName.equals(EDGE) )
            {
                schemaCheck();
                
                m_row = m_edges.addRow();
                
                // do not use the id value
//                String id = atts.getValue(ID);
//                if ( id != null ) {
//                    if ( !m_edges.canGetString(ID) )
//                        m_edges.addColumn(ID, String.class);
//                    m_edges.setString(m_row, ID, id);
//                }
                m_edges.setString(m_row, SRCID, atts.getValue(SRC));
                m_edges.setString(m_row, TRGID, atts.getValue(TRG));
                
                // currently only global directedness is used
                // ignore directed edge value for now
//                String dir = atts.getValue(DIRECTED);
//                boolean d = m_directed;
//                if ( dir != null ) {
//                    d = dir.equalsIgnoreCase("false");
//                }
//                m_edges.setBoolean(m_row, DIRECTED, d);
                m_table = m_edges;
            }
            else if ( qName.equals(DATA) )
            {
                m_key = atts.getValue(KEY);
            }
        }

        public void endElement(String namespaceURI, 
                String localName, String qName)
        {
            if ( qName.equals(DEFAULT) ) {
                // value is in the buffer
                m_dflt = m_sbuf.toString();
            }
            else if ( qName.equals(KEY) ) {
                // time to add to the proper schema(s)
                addToSchema();
            }
            else if ( qName.equals(DATA) ) {
                // value is in the buffer
                String value = m_sbuf.toString();
                String name = (String)m_idMap.get(m_key);
                Class type = m_table.getColumnType(name);
                try {
                    Object val = parse(value, type);
                    m_table.set(m_row, name, val);
                } catch ( DataParseException dpe ) {
                    error(dpe);
                }
            }
            else if ( qName.equals(NODE) || qName.equals(EDGE) ) {
                m_row = -1;
                m_table = null;
            }
        }
        
        public void characters(char[] ch, int start, int length) throws SAXException {
            m_sbuf.append(ch, start, length);
        }

        // --------------------------------------------------------------------
        
        protected void schemaCheck() {
            if ( inSchema ) {
                m_nsch.lockSchema();
                m_esch.lockSchema();
                m_nodes = m_nsch.instantiate();
                m_edges = m_esch.instantiate();
                inSchema = false;
            }
        }
        
        protected void addToSchema() {
            if ( m_name == null || m_name.length() == 0 )
                error("Empty "+KEY+" name.");
            if ( m_type == null || m_type.length() == 0 )
                error("Empty "+KEY+" type.");
            
            try {
                Class type = parseType(m_type);
                Object dflt = m_dflt==null ? null : parse(m_dflt, type);
                
                if ( m_for == null || m_for.equals(ALL) ) {
                    m_nsch.addColumn(m_name, type, dflt);
                    m_esch.addColumn(m_name, type, dflt);
                } else if ( m_for.equals(NODE) ) {
                    m_nsch.addColumn(m_name, type, dflt);
                } else if ( m_for.equals(EDGE) ) {
                    m_esch.addColumn(m_name, type, dflt);
                } else {
                    error("Unrecognized \""+FOR+"\" value: "+ m_for);
                }
                m_idMap.put(m_id, m_name);
                
                m_dflt = null;
            } catch ( DataParseException dpe ) {
                error(dpe);
            }
        }
        
        protected Class parseType(String type) {
            type = type.toLowerCase();
            if ( type.equals(INT) || type.equals(INTEGER) ) {
                return int.class;
            } else if ( type.equals(LONG) ) {
                return long.class;
            } else if ( type.equals(FLOAT) ) {
                return float.class;
            } else if ( type.equals(DOUBLE) || type.equals(REAL)) {
                return double.class;
            } else if ( type.equals(BOOLEAN) ) {
                return boolean.class;
            } else if ( type.equals(STRING) ) {
                return String.class;
            } else if ( type.equals(DATE) ) {
                return Date.class;
            } else {
                error("Unrecognized data type: "+type);
                return null;
            }
        }
        
        protected Object parse(String s, Class type)
            throws DataParseException
        {
            DataParser dp = m_pf.getParser(type);
            return dp.parse(s);
        }
        
        public Graph getGraph() {
            return m_graph;
        }
        
        protected void error(String s) {
            throw new RuntimeException(s);
        }
        
        protected void error(Exception e) {
            throw new RuntimeException(e);
        }
        
    } // end of inner class GraphMLHandler

} // end of class XMLGraphReader
