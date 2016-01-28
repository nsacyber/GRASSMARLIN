/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.data.io;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;

import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.util.io.XMLWriter;

/**
 * GraphWriter instance that writes a tree file formatted using the
 * TreeML file format. TreeML is an XML format originally created for
 * the 2003 InfoVis conference contest. A DTD (Document Type Definition) for
 * TreeML is
 * <a href="http://www.nomencurator.org/InfoVis2003/download/treeml.dtd">
 *  available online</a>.
 * 
 * <p>The GraphML spec only supports the data types <code>Int</code>,
 * <code>Long</code>, <code>Float</code>, <code>Real</code> (double),
 * <code>Boolean</code>, <code>String</code>, and <code>Date</code>.
 * An exception will be thrown if a data type outside these allowed
 * types is encountered.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TreeMLWriter extends AbstractGraphWriter {

    /**
     * String tokens used in the TreeML format.
     */
    public interface Tokens extends TreeMLReader.Tokens {}
    
    /**
     * Map containing legal data types and their names in the GraphML spec
     */
    private static final HashMap TYPES = new HashMap();
    static {
        TYPES.put(int.class, Tokens.INT);
        TYPES.put(long.class, Tokens.LONG);
        TYPES.put(float.class, Tokens.FLOAT);
        TYPES.put(double.class, Tokens.REAL);
        TYPES.put(boolean.class, Tokens.BOOLEAN);
        TYPES.put(String.class, Tokens.STRING);
        TYPES.put(Date.class, Tokens.DATE);
    }
    
    /**
     * @see prefuse.data.io.GraphWriter#writeGraph(prefuse.data.Graph, java.io.OutputStream)
     */
    public void writeGraph(Graph graph, OutputStream os) throws DataIOException
    {
        // first, check the schemas to ensure GraphML compatibility
        Schema ns = graph.getNodeTable().getSchema();
        checkTreeMLSchema(ns);
        
        XMLWriter xml = new XMLWriter(new PrintWriter(os));
        xml.begin();
        
        xml.comment("prefuse TreeML Writer | "
                + new Date(System.currentTimeMillis()));
                
        // print the tree contents
        xml.start(Tokens.TREE);
        
        // print the tree node schema
        xml.start(Tokens.DECLS);
        String[] attr = new String[] {Tokens.NAME, Tokens.TYPE };
        String[] vals = new String[2];

        for ( int i=0; i<ns.getColumnCount(); ++i ) {
            vals[0] = ns.getColumnName(i);
            vals[1] = (String)TYPES.get(ns.getColumnType(i));
            xml.tag(Tokens.DECL, attr, vals, 2);
        }
        xml.end();
        xml.println();
        
        
        // print the tree nodes
        attr[0] = Tokens.NAME;
        attr[1] = Tokens.VALUE;
        
        Node n = graph.getSpanningTree().getRoot();
        while ( n != null ) {
            boolean leaf = (n.getChildCount() == 0);
            
            if ( leaf ) {
                xml.start(Tokens.LEAF);
            } else {
                xml.start(Tokens.BRANCH);
            }
            
            if ( ns.getColumnCount() > 0 ) {
                for ( int i=0; i<ns.getColumnCount(); ++i ) {
                    vals[0] = ns.getColumnName(i);
                    vals[1] = n.getString(vals[0]);
                    if (vals[1] != null) {
                    	xml.tag(Tokens.ATTR, attr, vals, 2);
                    }
                }
            }
            n = nextNode(n, xml);
        }
        
        // finish writing file
        xml.end();
        xml.finish();
    }
    
    /**
     * Find the next node in the depth first iteration, closing off open
     * branch tags as needed.
     * @param x the current node
     * @param xml the XMLWriter
     * @return the next node
     */
    private Node nextNode(Node x, XMLWriter xml) {
        Node n, c;
        if ( (c=x.getChild(0)) != null ) {
            // do nothing
        } else if ( (c=x.getNextSibling()) != null ) {
            xml.end();
        } else {
            c = x.getParent();
            xml.end();
            while ( c != null ) {
                if ( (n=c.getNextSibling()) != null ) {
                    c = n;
                    xml.end();
                    break;
                }
                c = c.getParent();
                xml.end();
            }
        }
        return c;
    }
    
    /**
     * Checks if all Schema types are compatible with the TreeML specification.
     * The TreeML spec only allows the types <code>int</code>,
     * <code>long</code>, <code>float</code>, <code>double</code>,
     * <code>boolean</code>, <code>string</code>, and <code>date</code>.
     * @param s the Schema to check
     */
    private void checkTreeMLSchema(Schema s) throws DataIOException {
        for ( int i=0; i<s.getColumnCount(); ++i ) {
            Class type = s.getColumnType(i);
            if ( TYPES.get(type) == null ) {
                throw new DataIOException("Data type unsupported by the "
                    + "TreeML format: " + type.getName());
            }
        }
    }
    
} // end of class GraphMLWriter