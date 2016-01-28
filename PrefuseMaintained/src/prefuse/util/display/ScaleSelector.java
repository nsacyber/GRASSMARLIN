package prefuse.util.display;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Swing widget which displays a preview image and helps select the
 * scale for an exported image.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ScaleSelector extends JComponent implements ChangeListener {

    private final static int MAX_SIZE = 135;
    
    private ImagePanel preview;
    private JLabel     value;
    private JLabel     size;
    private JSlider    slider;
    private Image      image;
    private int width, height;
    
    /**
     * Create a new ScaleSelector.
     */
    public ScaleSelector() {
        slider = new JSlider(1,10,1);
        value  = new JLabel("x1");
        size = new JLabel("   ");
        preview = new ImagePanel();
        
        value.setPreferredSize(new Dimension(25,10));
        size.setHorizontalAlignment(JLabel.CENTER);
        slider.setMajorTickSpacing(1);
        slider.setSnapToTicks(true);
        slider.addChangeListener(this);
        
        setLayout(new BorderLayout());
        
        Box b1 = new Box(BoxLayout.X_AXIS);
        b1.add(Box.createHorizontalStrut(5));
        b1.add(Box.createHorizontalGlue());
        b1.add(preview);
        b1.add(Box.createHorizontalGlue());
        b1.add(Box.createHorizontalStrut(5));
        add(b1, BorderLayout.CENTER);
        
        Box b2 = new Box(BoxLayout.X_AXIS);
        b2.add(slider);
        b2.add(Box.createHorizontalStrut(5));
        b2.add(value);
        
        Box b3 = new Box(BoxLayout.X_AXIS);
        b3.add(Box.createHorizontalStrut(5));
        b3.add(Box.createHorizontalGlue());
        b3.add(size);
        b3.add(Box.createHorizontalGlue());
        b3.add(Box.createHorizontalStrut(5));
        
        Box b4 = new Box(BoxLayout.Y_AXIS);
        b4.add(b2);
        b4.add(b3);
        add(b4, BorderLayout.SOUTH);
    }

    /**
     * Set the preview image.
     * @param img the preview image
     */
    public void setImage(Image img) {
        image = getScaledImage(img);
        stateChanged(null);
    }
    
    /**
     * Get a scaled version of the input image.
     * @param img the input image
     * @return a scaled version of the image
     */
    private Image getScaledImage(Image img) {
        int w = width = img.getWidth(null);
        int h = height = img.getHeight(null);
        double ar = ((double)w)/h;
        
        int nw = MAX_SIZE, nh = MAX_SIZE;
        if ( w > h ) {
            nh = (int)Math.round(nw/ar);
        } else {
            nw = (int)Math.round(nh*ar);
        }
        return img.getScaledInstance(nw,nh,Image.SCALE_SMOOTH);
    }
    
    /**
     * Monitor changes to the scale slider.
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent evt) {
        int scale = slider.getValue();
        value.setText("x"+String.valueOf(scale));
        size.setText("Image Size: "+(width*scale)+" x "+(height*scale)+" pixels");
        preview.repaint();
    }
    
    /**
     * Get the current image scale reported by the slider.
     * @return the image scale to use
     */
    public double getScale() {
        return slider.getValue();
    }
    
    /**
     * Swing component that draws an image scaled to the current
     * scale factor.
     */
    public class ImagePanel extends JComponent {
        Dimension d = new Dimension(MAX_SIZE, MAX_SIZE);
        public ImagePanel() {
            this.setPreferredSize(d);
            this.setMinimumSize(d);
            this.setMaximumSize(d);
        }
        public void paintComponent(Graphics g) {
            double scale = 0.4+(0.06*getScale());
            int w = (int)Math.round(scale*image.getWidth(null));
            int h = (int)Math.round(scale*image.getHeight(null));
            Image img = (scale == 1.0 ? image : 
                image.getScaledInstance(w,h,Image.SCALE_DEFAULT));
            int x = (MAX_SIZE-w)/2;
            int y = (MAX_SIZE-h)/2;
            g.drawImage(img,x,y,null);
        }
    }
    
} // end of class ScaleSelector
