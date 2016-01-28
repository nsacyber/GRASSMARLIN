package prefuse.util.ui;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * Swing component that acts much like a JLabel, but does not revalidate
 * its bounds when updated, making it much faster but suitable only for
 * use in situations where the initial bounds are sufficient.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class JFastLabel extends JComponent {

    private String m_text;
    private int m_valign = SwingConstants.TOP;
    private int m_halign = SwingConstants.LEFT;
    private int m_fheight = -1;
    private boolean m_quality = false;
    
    /**
     * Create a new JFastLabel with no text.
     */
    public JFastLabel() {
        this(null);
    }
    
    /**
     * Create a new JFastLabel with the given text.
     * @param text the label text.
     */
    public JFastLabel(String text) {
        m_text = text;
        setFont(getFont());
    }

    /**
     * Get the label text.
     * @return the label text
     */
    public String getText() {
        return m_text;
    }

    /**
     * Set the label text
     * @param text the label text to set
     */
    public void setText(String text) {
        this.m_text = text;
        repaint();
    }
    
    /**
     * @see java.awt.Component#setFont(java.awt.Font)
     */
    public void setFont(Font f) {
        super.setFont(f);
        m_fheight = -1;
    }
    
    /**
     * Set the vertical alignment.
     * @param align the vertical alignment
     * @see javax.swing.SwingConstants
     */
    public void setVerticalAlignment(int align) {
        m_valign = align;
        m_fheight = -1;
    }
    
    /**
     * Set the horizontal alignment.
     * @param align the horizontal alignment
     * @see javax.swing.SwingConstants
     */
    public void setHorizontalAlignment(int align) {
        m_halign = align;
    }
    
    /**
     * Get the quality level of this label. High quality results in
     * anti-aliased rendering.
     * @return true for high quality, false otherwise
     */
    public boolean getHighQuality() {
        return m_quality;
    }
    
    /**
     * Set the quality level of this label. High quality results in
     * anti-aliased rendering.
     * @param b true for high quality, false otherwise
     */
    public void setHighQuality(boolean b) {
        m_quality = b;
    }
    
    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics g) {
        Insets ins = getInsets();
        int w = getWidth()-ins.left-ins.right;
        int h = getHeight()-ins.top-ins.bottom;
        if ( m_fheight == -1 ) {
            FontMetrics fm = g.getFontMetrics(getFont());
            if ( m_valign == SwingConstants.BOTTOM )
                m_fheight = fm.getDescent();
            else if ( m_valign == SwingConstants.TOP )
                m_fheight = fm.getAscent();
        }
        g.setColor(getBackground());
        g.fillRect(ins.left,ins.top,w,h);
        
        if ( m_text == null )
            return;
        
        g.setFont(getFont());
        g.setColor(getForeground());
        if ( m_valign == SwingConstants.BOTTOM ) {
            h = h-m_fheight-ins.bottom;
        } else {
            h = ins.top+m_fheight;
        }
        
        switch ( m_halign ) {
        case SwingConstants.RIGHT: {
            FontMetrics fm = g.getFontMetrics(getFont());
            w = w-ins.right-fm.stringWidth(m_text);
            break;
        } case SwingConstants.CENTER: {
            FontMetrics fm = g.getFontMetrics(getFont());
            w = ins.left + w/2 - fm.stringWidth(m_text)/2;
            break;
        } default:
            w = ins.left;
        }
        if ( m_quality ) {
            ((Graphics2D)g).setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        g.drawString(m_text, w, h); 
    }
    
} // end of class JFastLabel
