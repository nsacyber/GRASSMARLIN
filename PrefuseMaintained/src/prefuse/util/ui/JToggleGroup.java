package prefuse.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Swing component representing a group of toggle buttons -- either checkboxes
 * or radio buttons. This class uses a ListModel and ListSelectionModel to
 * represent the selection state of the buttons.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class JToggleGroup extends JPanel {

    public static final int CHECKBOX = 0;
    public static final int RADIO    = 1;
    
    protected final int m_type;
    protected int m_margin  = 0;
    protected int m_spacing = 0;
    protected int m_axis = BoxLayout.X_AXIS;
    
    protected ListModel          m_data;
    protected ListSelectionModel m_sel;
    protected String[]           m_labels;
    protected ButtonGroup        m_group;
    
    private Listener m_lstnr;
    
    /**
     * Create a new JToggleGroup.
     * @param type the toggle button type to use, one of {@link #CHECKBOX}
     * or {@link #RADIO}
     * @param data the list data that should populate the toggle group
     */
    public JToggleGroup(int type, Object[] data) {
        this(type, new DefaultListModel(), 
                new DefaultListSelectionModel());
        
        DefaultListModel model = (DefaultListModel)m_data;
        for ( int i=0; i<data.length; ++i ) {
            model.addElement(data[i]);
        }
        initUI();
    }
    
    /**
     * Create a new JToggleGroup.
     * @param type the toggle button type to use, one of {@link #CHECKBOX}
     * or {@link #RADIO}
     * @param data the list model data backing the toggle group
     */
    public JToggleGroup(int type, ListModel data) {
        this(type, data, new DefaultListSelectionModel());
    }
    
    /**
     * Create a new JToggleGroup.
     * @param type the toggle button type to use, one of {@link #CHECKBOX}
     * or {@link #RADIO}
     * @param data the list model data backing the toggle group
     * @param selection the list selection model to use to monitor selection
     * changes to the various toggle buttons.
     */
    public JToggleGroup(int type, ListModel data, ListSelectionModel selection)
    {
        setLayout(new BoxLayout(this, m_axis));
        m_type = type;
        m_data = data;
        m_sel = selection;

        if ( m_type == RADIO ) {
            m_group = new ButtonGroup();
        }

        m_lstnr = new Listener();
        m_sel.addListSelectionListener(m_lstnr);
        
        if ( m_data.getSize() > 0 )
            initUI();
        setFocusable(false);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Initialize the UI.
     */
    protected void initUI() {
        // unregister all active components
        for ( int i=0; i<getComponentCount(); ++i ) {
            Component c = getComponent(i);
            if ( !(c instanceof JToggleButton) ) continue;
            JToggleButton tb = (JToggleButton)c;
            tb.removeActionListener(m_lstnr);
            if ( m_group != null )
                m_group.remove(tb);
        }
        
        // clear this container and add new components
        removeAll();
        UILib.addStrut(this, m_axis, m_margin);
        for ( int i=0; i<m_data.getSize(); ++i ) {
            if ( i>0 ) UILib.addStrut(this, m_axis, m_spacing);
            
            Object data  = m_data.getElementAt(i);
            String label = m_labels==null ? data.toString() : m_labels[i];
            
            JToggleButton tb = null;
            if ( m_type == CHECKBOX ) {
                tb = new JCheckBox(label);
            } else {
                tb = new JRadioButton(label);
                m_group.add(tb);
            }
            tb.putClientProperty("idx", new Integer(i));
            tb.addActionListener(m_lstnr);
            add(tb);
        }
        UILib.addStrut(this, m_axis, m_margin);
        
        // make sure the selection status shows up
        m_lstnr.valueChanged(null);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Set the Box axis type used to orient the toggle group component.
     * @param axis the axis type, one of
     * {@link javax.swing.BoxLayout#X_AXIS},
     * {@link javax.swing.BoxLayout#Y_AXIS},
     * {@link javax.swing.BoxLayout#LINE_AXIS}, or
     * {@link javax.swing.BoxLayout#PAGE_AXIS}.
     */
    public void setAxisType(int axis) {
        this.setLayout(new BoxLayout(this, axis));
        m_axis = axis;
        initUI();
    }
    
    /**
     * Get the Box axis type used to orient the toggle group component.
     * @return the axis type, one of
     * {@link javax.swing.BoxLayout#X_AXIS},
     * {@link javax.swing.BoxLayout#Y_AXIS},
     * {@link javax.swing.BoxLayout#LINE_AXIS}, or
     * {@link javax.swing.BoxLayout#PAGE_AXIS}.
     */
    public int getAxisType() {
        return m_axis;
    }
    
    /**
     * Set the margin, in pixels, to use at the ends of the JToggleGroup.
     * @param margin the margin in pixels
     */
    public void setMargin(int margin) {
        if ( margin < 0 )
            throw new IllegalArgumentException("Margin is less than zero.");
        m_margin = margin;
        initUI();
    }

    /**
     * Get the margin, in pixels, used at the ends of the JToggleGroup.
     * @return the margin in pixels
     */
    public int getMargin() {
        return m_margin;
    }
    
    /**
     * Set the spacing between toggle group components.
     * @param spacing the spacing, in pixels, to use between components
     */
    public void setSpacing(int spacing) {
        if ( spacing < 0 )
            throw new IllegalArgumentException("Spacing is less than zero.");
        m_spacing = spacing;
        initUI();
    }
    
    /**
     * Get the spacing between toggle group components.
     * @return the spacing, in pixels, to use between components
     */
    public int getSpacing() {
        return m_spacing;
    }
    
    /**
     * Set the ListModel backing this component.
     * @return the list model to use
     */
    public void setModel(ListModel model) {
        m_data = model;
        initUI();
    }
    
    /**
     * Get the ListModel backing this component.
     * @return the list model
     */
    public ListModel getModel() {
        return m_data;
    }
    
    /**
     * Set the ListSelectionModel used by this component.
     * @param sel the list selection model to use
     */
    public void setSelectionModel(ListSelectionModel sel) {
        m_sel.removeListSelectionListener(m_lstnr);
        m_sel = sel;
        m_sel.addListSelectionListener(m_lstnr);
        m_lstnr.valueChanged(null);
    }
    
    /**
     * Get the ListSelectionModel used by this component.
     * @return the list selection model to use
     */
    public ListSelectionModel getSelectionModel() {
        return m_sel;
    }
    
    /**
     * Set the labels to use for the Objects contained in the list model.
     * @param labels the display labels to use in the interface component
     */
    public void setLabels(String[] labels) {
        if ( labels.length < m_data.getSize() ) {
            throw new IllegalArgumentException("Alias array is too short");
        }
        m_labels = labels;
        initUI();
    }
    
    /**
     * Set the background color of this toggle group.
     * @see java.awt.Component#setBackground(java.awt.Color)
     */
    public void setBackground(Color background) {
        for ( int i=0; i<getComponentCount(); ++i ) {
            getComponent(i).setBackground(background);
        }
    }
    
    /**
     * Set the foreground color of this toggle group.
     * @see java.awt.Component#setBackground(java.awt.Color)
     */
    public void setForeground(Color foreground) {
        for ( int i=0; i<getComponentCount(); ++i ) {
            getComponent(i).setForeground(foreground);
        }
    }
    
    /**
     * Set the font used by this toggle group.
     * @see java.awt.Component#setFont(java.awt.Font)
     */
    public void setFont(Font font) {
        for ( int i=0; i<getComponentCount(); ++i ) {
            getComponent(i).setFont(font);
        }
    }
    
    /**
     * Sets if the various toggle buttons can receive the keyboard focus.
     * @param b true to set toggle buttons keyboard accessible, false to
     * set them unaccessible.
     */
    public void setGroupFocusable(boolean b) {
        for ( int i=0; i<getComponentCount(); ++i ) {
            Component c = getComponent(i);
            if ( c instanceof JToggleButton )
                c.setFocusable(b);
        }
    }
    
    // ------------------------------------------------------------------------
    
    private class Listener implements ListSelectionListener, ActionListener {

        private boolean m_ignore = false;
        
        public void valueChanged(ListSelectionEvent neverUsed) {
            if ( m_ignore ) { return; } else { m_ignore = true; }
            
            if ( m_type == RADIO ) {
                int idx = m_sel.getMinSelectionIndex();
                boolean sel = (idx >= 0);
                JToggleButton tb = null;
                
                for ( int i=0, j=0; i<getComponentCount(); ++i ) {
                    Component c = getComponent(i);
                    if ( c instanceof JToggleButton ) {
                        tb = (JToggleButton)c;
                        if ( (!sel && tb.isSelected()) || (sel && idx==j) )
                            break;
                        ++j;
                    }
                }
                tb.setSelected(sel);
            } else {
                for ( int i=0, j=0; i<getComponentCount(); ++i ) {
                    Component c = getComponent(i);
                    if ( c instanceof JCheckBox ) {
                        ((JCheckBox)c).setSelected(m_sel.isSelectedIndex(j++));
                    }
                }
            }
            
            m_ignore = false;
        }

        public void actionPerformed(ActionEvent e) {
            if ( m_ignore ) { return; } else { m_ignore = true; }
            
            JToggleButton tb = (JToggleButton)e.getSource();
            boolean sel = tb.isSelected();
            int idx = ((Integer)tb.getClientProperty("idx")).intValue();
            if ( m_type == RADIO ) {
                m_sel.setSelectionInterval(idx,idx);
            } else if ( sel ) {
                m_sel.addSelectionInterval(idx,idx);
            } else {
                m_sel.removeSelectionInterval(idx,idx);
            }
            
            m_ignore = false;
        }
        
    }
    
} // end of class JToggleGroup
