package prefuse.util.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.InputEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.UIManager;

/**
 * Library routines for user interface tasks.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class UILib {

//    private static Image s_appIcon;
    
    /**
     * Not instantiable.
     */
    private UILib() {
        // prevent instantiation
    }
    
//    public static synchronized Image getApplicationIcon() {
//        if ( s_appIcon == null ) {
//            try {
//                s_appIcon = new ImageIcon(
//                        UILib.class.getResource("icon.gif")).getImage();
//            } catch ( Exception e ) {
//                e.printStackTrace();
//            }
//        }
//        return s_appIcon;
//    }
    
    /**
     * Indicates if a given mouse button is being pressed.
     * @param e the InputEvent to check
     * @param button the mouse button to look for
     * @return true if the button is being pressed, false otherwise
     * @see prefuse.controls.Control
     */
    public static boolean isButtonPressed(InputEvent e, int button) {
        return (e.getModifiers() & button) == button;
    }

    /**
     * Set the look and feel of Java Swing user interface components to match
     * that of the platform (Windows, Mac, Linux, etc) on which it is
     * currently running.
     */
    public static final void setPlatformLookAndFeel() {
        try {
            String laf = UIManager.getSystemLookAndFeelClassName();             
            UIManager.setLookAndFeel(laf);  
        } catch ( Exception e ) {}
    }

    /**
     * Convenience method for creating a Box user interface widget container.
     * @param c an array of components to include in the box
     * @param horiz indicated is the box should be horizontal (true) or
     * vertical (false)
     * @param margin the margins, in pixels, to use on the sides of the box
     * @param spacing the minimum spacing, in pixels, to use between
     * components
     * @return a new Box instance with the given properties.
     * @see javax.swing.Box
     */
    public static Box getBox(Component[] c, boolean horiz, 
            int margin, int spacing)
    {
        return getBox(c, horiz, margin, margin, spacing);
    }
    
    /**
     * Convenience method for creating a Box user interface widget container.
     * @param c an array of components to include in the box
     * @param horiz indicated is the box should be horizontal (true) or
     * vertical (false)
     * @param margin1 the margin, in pixels, for the left or top side
     * @param margin2 the margin, in pixels, for the right or bottom side
     * @param spacing the minimum spacing, in pixels, to use between
     * components
     * @return a new Box instance with the given properties.
     * @see javax.swing.Box
     */
    public static Box getBox(Component[] c, boolean horiz,
            int margin1, int margin2, int spacing)
    {
        Box b = new Box(horiz ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS);
        addStrut(b, horiz, margin1);
        for ( int i=0; i<c.length; ++i ) {
            if ( i > 0 ) {
                addStrut(b, horiz, spacing);
                addGlue(b, horiz);
            }
            b.add(c[i]);
        }
        addStrut(b, horiz, margin2);
        return b;
    }
    
    /**
     * Add a strut, or rigid spacing, to a UI component
     * @param b the component to add the strut to, should be either a Box or a
     * Container using a BoxLayout.
     * @param horiz indicates if the strust should horizontal (true) or vertical
     * (false)
     * @param size the length, in pixels, of the strut
     */
    public static void addStrut(JComponent b, boolean horiz, int size) {
        if ( size < 1 ) return;
        b.add(horiz ? Box.createHorizontalStrut(size)
                    : Box.createVerticalStrut(size) );
    }
    
    /**
     * Add a glue, or variable spacing, to a UI component
     * @param b the component to add the glue to, should be either a Box or a
     * Container using a BoxLayout.
     * @param horiz indicates if the glue should horizontal (true) or vertical
     * (false)
     */
    public static void addGlue(JComponent b, boolean horiz) {
        b.add(horiz ? Box.createHorizontalGlue()
                    : Box.createVerticalGlue());
    }
    
    /**
     * Add a strut, or rigid spacing, to a UI component
     * @param b the component to add the strut to, should be either a Box or a
     * Container using a BoxLayout.
     * @param layout the desired layout orientation of the strut. One of
     * {@link javax.swing.BoxLayout#X_AXIS},
     * {@link javax.swing.BoxLayout#Y_AXIS},
     * {@link javax.swing.BoxLayout#LINE_AXIS}, or
     * {@link javax.swing.BoxLayout#PAGE_AXIS}.
     * @param size the length, in pixels, of the strut
     */
    public static void addStrut(JComponent b, int layout, int size) {
        if ( size < 1 ) return;
        b.add( getAxis(b, layout) == BoxLayout.X_AXIS
                ? Box.createHorizontalStrut(size)
                : Box.createVerticalStrut(size) );
    }
    
    /**
     * Add a glue, or variable spacing, to a UI component
     * @param b the component to add the glue to, should be either a Box or a
     * Container using a BoxLayout.
     * @param layout the desired layout orientation of the glue. One of
     * {@link javax.swing.BoxLayout#X_AXIS},
     * {@link javax.swing.BoxLayout#Y_AXIS},
     * {@link javax.swing.BoxLayout#LINE_AXIS}, or
     * {@link javax.swing.BoxLayout#PAGE_AXIS}.
     */
    public static void addGlue(JComponent b, int layout) {
        b.add( getAxis(b, layout) == BoxLayout.X_AXIS 
                ? Box.createHorizontalGlue()
                : Box.createVerticalGlue());
    }
    
    /**
     * Resolve the axis type of a component, given a layout orientation
     * @param c a Swing Component, should be either a Box or a Container
     * using a BoxLayout.
     * @param layout the layout orientation of the component. One of
     * {@link javax.swing.BoxLayout#X_AXIS},
     * {@link javax.swing.BoxLayout#Y_AXIS},
     * {@link javax.swing.BoxLayout#LINE_AXIS}, or
     * {@link javax.swing.BoxLayout#PAGE_AXIS}.
     * @return one of {@link javax.swing.BoxLayout#X_AXIS}, or
     * {@link javax.swing.BoxLayout#Y_AXIS},
     */
    public static int getAxis(JComponent c, int layout) {
        ComponentOrientation o = c.getComponentOrientation();
        switch ( layout ) {
        case BoxLayout.LINE_AXIS:
            return o.isHorizontal() ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS;
        case BoxLayout.PAGE_AXIS:
            return o.isHorizontal() ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS;
        default:
            return layout;
        }
    }
    
    /**
     * Sets the foreground and background color for a component and all
     * components contained within it.
     * @param c the parent component of the component subtree to set
     * @param back the background color to set
     * @param fore the foreground color to set
     */
    public static void setColor(Component c, Color back, Color fore) {
        c.setBackground(back);
        c.setForeground(fore);
        if ( c instanceof Container ) {
            Container con = (Container)c;
            for ( int i=0; i<con.getComponentCount(); ++i )
                setColor(con.getComponent(i), back, fore);
        }
    }
    
    /**
     * Sets the font for a component and all
     * components contained within it.
     * @param c the parent component of the component subtree to set
     * @param font the font to set
     */
    public static void setFont(Component c, Font font) {
        c.setFont(font);
        if ( c instanceof Container ) {
            Container con = (Container)c;
            for ( int i=0; i<con.getComponentCount(); ++i )
                setFont(con.getComponent(i), font);
        }
    }
    
} // end of class UILib
