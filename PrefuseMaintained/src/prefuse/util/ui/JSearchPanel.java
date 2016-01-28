package prefuse.util.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import prefuse.Visualization;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.search.PrefixSearchTupleSet;
import prefuse.data.search.SearchTupleSet;
import prefuse.data.tuple.TupleSet;
import prefuse.util.ColorLib;

/**
 * Swing component that enables keyword search over prefuse data tuples.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.data.query.SearchQueryBinding
 */
public class JSearchPanel extends JPanel
    implements DocumentListener, ActionListener
{
    private Object m_lock;
    private SearchTupleSet m_searcher;

    private JTextField m_queryF  = new JTextField(15);
    private JLabel     m_resultL = new JLabel("          ");
    private JLabel     m_searchL = new JLabel("search >> ");
    private Box        m_sbox    = new Box(BoxLayout.X_AXIS);

    private String[] m_fields;
    
    private Color m_cancelColor = ColorLib.getColor(255,75,75);
    
    private boolean m_includeHitCount = false;
    private boolean m_monitorKeys = false;
    private boolean m_autoIndex = true;
    
    private boolean m_showBorder = true;
    private boolean m_showCancel = true;

    // ------------------------------------------------------------------------
    // Free form constructors
    
    /**
     * Create a new JSearchPanel.
     * @param search the search tuple set conducting the searches
     * @param field the data field being searched
     */
    public JSearchPanel(SearchTupleSet search, String field) {
        this(search, field, false);
    }
    
    /**
     * Create a new JSearchPanel.
     * @param search the search tuple set conducting the searches
     * @param field the data field being searched
     * @param monitorKeystrokes indicates if each keystroke event should result
     * in a new search being issued (true) or if searches should only be
     * initiated by hitting the enter key (false)
     */
    public JSearchPanel(SearchTupleSet search, String field, 
            boolean monitorKeystrokes)
    {
        this(null, search, new String[] {field}, false, monitorKeystrokes);
    }
    
    /**
     * Create a new JSearchPanel.
     * @param source the source set of tuples that should be searched over
     * @param search the search tuple set conducting the searches
     * @param fields the data fields being searched
     * @param monitorKeystrokes indicates if each keystroke event should result
     * in a new search being issued (true) or if searches should only be
     * initiated by hitting the enter key (false)
     */
    public JSearchPanel(TupleSet source, SearchTupleSet search, 
            String[] fields, boolean autoIndex, boolean monitorKeystrokes)
    {
        m_lock = new Object();
        m_fields = fields;
        m_autoIndex = autoIndex;
        m_monitorKeys = monitorKeystrokes;

        m_searcher = ( search != null ? search : new PrefixSearchTupleSet() );
        
        init(source);
    }
    
    // ------------------------------------------------------------------------
    // Visualization-based constructors
    
    /**
     * Create a new JSearchPanel. The default search tuple set for the
     * visualization will be used.
     * @param vis the Visualization to search over
     * @param field the data field being searched
     */
    public JSearchPanel(Visualization vis, String field) {
        this(vis, Visualization.ALL_ITEMS, field, true);
    }
    
    /**
     * Create a new JSearchPanel. The default search tuple set for the
     * visualization will be used.
     * @param vis the Visualization to search over
     * @param group the particular data group to search over
     * @param field the data field being searched
     */
    public JSearchPanel(Visualization vis, String group, String field) {
        this(vis, group, field, true);
    }
    
    /**
     * Create a new JSearchPanel. The default search tuple set for the
     * visualization will be used.
     * @param vis the Visualization to search over
     * @param group the particular data group to search over
     * @param field the data field being searched
     * @param autoIndex indicates if items should be automatically
     * indexed and unindexed as their membership in the source group
     * changes.
     */
    public JSearchPanel(Visualization vis, String group, String field, 
            boolean autoIndex)
    {
        this(vis, group, Visualization.SEARCH_ITEMS, 
                new String[] {field}, autoIndex, false);
    }
    
    /**
     * Create a new JSearchPanel. The default search tuple set for the
     * visualization will be used.
     * @param vis the Visualization to search over
     * @param group the particular data group to search over
     * @param field the data field being searched
     * @param autoIndex indicates if items should be automatically
     * indexed and unindexed as their membership in the source group
     * changes.
     * @param monitorKeystrokes indicates if each keystroke event should result
     * in a new search being issued (true) or if searches should only be
     * initiated by hitting the enter key (false)
     */
    public JSearchPanel(Visualization vis, String group, String field, 
            boolean autoIndex, boolean monitorKeystrokes)
    {
        this(vis, group, Visualization.SEARCH_ITEMS, 
                new String[] {field}, autoIndex, true);
    }
    
    /**
     * Create a new JSearchPanel.
     * @param vis the Visualization to search over
     * @param group the particular data group to search over
     * @param searchGroup the group name that resolves to the SearchTupleSet
     * to use
     * @param field the data field being searched
     * @param autoIndex indicates if items should be automatically
     * indexed and unindexed as their membership in the source group
     * changes.
     * @param monitorKeystrokes indicates if each keystroke event should result
     * in a new search being issued (true) or if searches should only be
     * initiated by hitting the enter key (false)
     */
    public JSearchPanel(Visualization vis, String group, String searchGroup, 
            String field, boolean autoIndex, boolean monitorKeystrokes)
    {
        this(vis, group, searchGroup, new String[] {field}, autoIndex,
                monitorKeystrokes);
    }
    
    /**
     * Create a new JSearchPanel.
     * @param vis the Visualization to search over
     * @param group the particular data group to search over
     * @param searchGroup the group name that resolves to the SearchTupleSet
     * to use
     * @param fields the data fields being searched
     * @param autoIndex indicates if items should be automatically
     * indexed and unindexed as their membership in the source group
     * changes.
     * @param monitorKeystrokes indicates if each keystroke event should result
     * in a new search being issued (true) or if searches should only be
     * initiated by hitting the enter key (false)
     */
    public JSearchPanel(Visualization vis, String group, String searchGroup, 
            String[] fields, boolean autoIndex, boolean monitorKeystrokes)
    {
        m_lock = vis;
        m_fields = fields;
        m_autoIndex = autoIndex;
        m_monitorKeys = monitorKeystrokes;

        TupleSet search = vis.getGroup(searchGroup);

        if ( search != null ) {
            if ( search instanceof SearchTupleSet ) {
                m_searcher = (SearchTupleSet)search;
            } else {
                throw new IllegalStateException(
                    "Search focus set not instance of SearchTupleSet!");
            }
        } else {
            m_searcher = new PrefixSearchTupleSet();
            vis.addFocusGroup(searchGroup, m_searcher);
        }
        
        init(vis.getGroup(group));
    }

    // ------------------------------------------------------------------------
    // Initialization
    
    private void init(TupleSet source) {
        if ( m_autoIndex && source != null ) {
            // index everything already there
            for ( int i=0; i < m_fields.length; i++ )
                m_searcher.index(source.tuples(), m_fields[i]);
            
            // add a listener to dynamically build search index
            source.addTupleSetListener(new TupleSetListener() {
                public void tupleSetChanged(TupleSet tset, 
                        Tuple[] add, Tuple[] rem)
                {
                    if ( add != null ) {
                        for ( int i=0; i<add.length; ++i ) {
                            for ( int j=0; j<m_fields.length; j++ )
                                m_searcher.index(add[i], m_fields[j]);
                        }
                    }
                    if ( rem != null && m_searcher.isUnindexSupported() ) {
                        for ( int i=0; i<rem.length; ++i )  {
                            for ( int j=0; j<m_fields.length; j++ )
                                m_searcher.unindex(rem[i], m_fields[j]);
                        }
                    }
                }
            });
        }
        
        m_queryF.addActionListener(this);
        if ( m_monitorKeys )
            m_queryF.getDocument().addDocumentListener(this);
        m_queryF.setMaximumSize(new Dimension(400, 100));
        m_queryF.setPreferredSize(new Dimension(200, 20));
        m_queryF.setBorder(null);
        setBackground(Color.WHITE);
        initUI();
    }
    
    private void initUI() {
        this.removeAll();
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        
        m_sbox.removeAll();
        m_sbox.add(Box.createHorizontalStrut(3));
        m_sbox.add(m_queryF);
        m_sbox.add(Box.createHorizontalStrut(3));
        if ( m_showCancel ) {
            m_sbox.add(new CancelButton());
            m_sbox.add(Box.createHorizontalStrut(3));
        }
        if ( m_showBorder )
            m_sbox.setBorder(BorderFactory.createLineBorder(getForeground()));
        else
            m_sbox.setBorder(null);
        m_sbox.setMaximumSize(new Dimension(400, 100));
        m_sbox.setPreferredSize(new Dimension(171, 20));
        
        Box b = new Box(BoxLayout.X_AXIS);
        if ( m_includeHitCount ) {
            b.add(m_resultL);
            b.add(Box.createHorizontalStrut(10));
            //b.add(Box.createHorizontalGlue());
        }
        b.add(m_searchL);
        b.add(Box.createHorizontalStrut(3));
        b.add(m_sbox);
        
        this.add(b);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Request the keyboard focus for this component.
     */
    public void requestFocus() {
        this.m_queryF.requestFocus();
    }
    
    /**
     * Set the lock, an object to synchronize on while issuing queries.
     * @param lock the synchronization lock
     */
    public void setLock(Object lock) {
        m_lock = lock;
    }
    
    /**
     * Indicates if the component should show the number of search results.
     * @param b true to show the result count, false to hide it
     */
    public void setShowResultCount(boolean b) {
        this.m_includeHitCount = b;
        initUI();
        validate();
    }
    
    /**
     * Indicates if the component should show a border around the text field.
     * @param b true to show the text field border, false to hide it
     */
    public void setShowBorder(boolean b) {
        m_showBorder = b;
        initUI();
        validate();
    }
    
    /**
     * Indicates if the component should show the cancel query button.
     * @param b true to show the cancel query button, false to hide it
     */
    public void setShowCancel(boolean b) {
        m_showCancel = b;
        initUI();
        validate();
    }
    
    /**
     * Update the search results based on the current query.
     */
    protected void searchUpdate() {
        String query = m_queryF.getText();
        synchronized ( m_lock ) {
            m_searcher.search(query);
            if ( m_searcher.getQuery().length() == 0 )
                m_resultL.setText(null);
            else {
                int r = m_searcher.getTupleCount();
                m_resultL.setText(r + " match" + (r==1?"":"es"));
            }
        }
    }
    
    /**
     * Set the query string in the text field.
     * @param query the query string to use
     */
    public void setQuery(String query) {
        Document d = m_queryF.getDocument();
        d.removeDocumentListener(this);
        m_queryF.setText(query);
        if ( m_monitorKeys )
            d.addDocumentListener(this);
        searchUpdate();
    }
    
    /**
     * Get the query string in the text field.
     * @return the current query string
     */
    public String getQuery() {
        return m_queryF.getText();
    }
    
    /**
     * Set the fill color of the cancel 'x' button that appears
     * when the button has the mouse pointer over it. 
     * @param c the cancel color
     */
    public void setCancelColor(Color c) {
        m_cancelColor = c;
    }
    
    /**
     * @see java.awt.Component#setBackground(java.awt.Color)
     */
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if ( m_queryF  != null ) m_queryF.setBackground(bg);
        if ( m_resultL != null ) m_resultL.setBackground(bg);
        if ( m_searchL != null ) m_searchL.setBackground(bg);
    }
    
    /**
     * @see java.awt.Component#setForeground(java.awt.Color)
     */
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if ( m_queryF  != null ) {
            m_queryF.setForeground(fg);
            m_queryF.setCaretColor(fg);
        }
        if ( m_resultL != null ) m_resultL.setForeground(fg);
        if ( m_searchL != null ) m_searchL.setForeground(fg);
        if ( m_sbox != null && m_showBorder )
            m_sbox.setBorder(BorderFactory.createLineBorder(fg));
    }
    
    /**
     * @see javax.swing.JComponent#setOpaque(boolean)
     */
    public void setOpaque(boolean opaque) {
        super.setOpaque(opaque);
        if ( m_queryF  != null ) {
            m_queryF.setOpaque(opaque);
        }
        if ( m_resultL != null ) m_resultL.setOpaque(opaque);
        if ( m_searchL != null ) m_searchL.setOpaque(opaque);
    }

    /**
     * @see java.awt.Component#setFont(java.awt.Font)
     */
    public void setFont(Font f) {
        super.setFont(f);;
        if ( m_queryF  != null ) m_queryF.setFont(f);
        if ( m_resultL != null ) m_resultL.setFont(f);
        if ( m_searchL != null ) m_searchL.setFont(f);
    }
    
    /**
     * Set the label text used on this component.
     * @param text the label text, use null to show no label
     */
    public void setLabelText(String text) {
        m_searchL.setText(text);
    }
    
    
    /**
     * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
     */
    public void changedUpdate(DocumentEvent e) {
        searchUpdate();
    }
    /**
     * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
     */
    public void insertUpdate(DocumentEvent e) {
        searchUpdate();
    }
    /**
     * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
     */
    public void removeUpdate(DocumentEvent e) {
        searchUpdate();
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ( src == m_queryF ) {
            searchUpdate();
        }
    }

    /**
     * A button depicted as an "X" that allows users to cancel the current query
     * and clear the query field.
     */
    public class CancelButton extends JComponent implements MouseListener {

        private boolean hover = false;
        private int[] outline = new int[] {
            0,0, 2,0, 4,2, 5,2, 7,0, 9,0, 9,2, 7,4, 7,5, 9,7, 9,9,
            7,9, 5,7, 4,7, 2,9, 0,9, 0,7, 2,5, 2,4, 0,2, 0,0
        };
        private int[] fill = new int[] {
            1,1,8,8, 1,2,7,8, 2,1,8,7, 7,1,1,7, 8,2,2,8, 1,8,8,1
        };
        
        public CancelButton() {
            // set button size
            Dimension d = new Dimension(10,10);
            this.setPreferredSize(d);
            this.setMinimumSize(d);
            this.setMaximumSize(d);
            
            // prevent the widget from getting the keyboard focus
            this.setFocusable(false);
            
            // add callbacks
            this.addMouseListener(this);
        }
        
        public void paintComponent(Graphics g) {
            if ( hover ) { // draw fill
                g.setColor(m_cancelColor);
                for ( int i=0; i+3 < fill.length; i+=4 ) {
                    g.drawLine(fill[i],fill[i+1],fill[i+2],fill[i+3]);
                }
            }
            g.setColor(JSearchPanel.this.getForeground());
            for ( int i=0; i+3 < outline.length; i+=2 ) {
                g.drawLine(outline[i],   outline[i+1],
                           outline[i+2], outline[i+3]);
            }
        }

        public void mouseClicked(MouseEvent arg0) {
            setQuery(null);
        }

        public void mousePressed(MouseEvent arg0) {
        }

        public void mouseReleased(MouseEvent arg0) {
        }

        public void mouseEntered(MouseEvent arg0) {
            hover = true;
            repaint();
        }

        public void mouseExited(MouseEvent arg0) {
            hover = false;
            repaint();
        }
        
    } // end of class CancelButton

} // end of class JSearchPanel
