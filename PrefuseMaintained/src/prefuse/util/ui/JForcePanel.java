package prefuse.util.ui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import prefuse.util.force.Force;
import prefuse.util.force.ForceSimulator;

/**
 * Swing component for configuring the parameters of the
 * Force functions in a given ForceSimulator. Useful for exploring
 * different parameterizations when crafting a visualization.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class JForcePanel extends JPanel {
    
    private ForcePanelChangeListener lstnr = new ForcePanelChangeListener();
    private ForceSimulator fsim;
    
    /**
     * Create a new JForcePanel
     * @param fsim the ForceSimulator to configure
     */
    public JForcePanel(ForceSimulator fsim) {
        this.fsim = fsim;
        this.setBackground(Color.WHITE);
        initUI();
    }
    
    /**
     * Initialize the UI.
     */
    private void initUI() {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        Force[] forces = fsim.getForces();
        for ( int i=0; i<forces.length; i++ ) {
            Force f = forces[i];
            Box v = new Box(BoxLayout.Y_AXIS);
            for ( int j=0; j<f.getParameterCount(); j++ ) {
                JValueSlider field = createField(f,j);
                field.addChangeListener(lstnr);
                v.add(field);
            }
            String name = f.getClass().getName();
            name = name.substring(name.lastIndexOf(".")+1);
            v.setBorder(BorderFactory.createTitledBorder(name));
            this.add(v);
        }
    }
    
    /**
     * Create an entry for configuring a single parameter.
     */
    private static JValueSlider createField(Force f, int param) {
        double value = f.getParameter(param);
        double min   = f.getMinValue(param);
        double max   = f.getMaxValue(param);
        String name = f.getParameterName(param);
        
        JValueSlider s = new JValueSlider(name,min,max,value);
        s.setBackground(Color.WHITE);
        s.putClientProperty("force", f);
        s.putClientProperty("param", new Integer(param));
        s.setPreferredSize(new Dimension(300,30));
        s.setMaximumSize(new Dimension(300,30));
        return s;
    }
    
    /**
     * Change listener that updates paramters in response to interaction.
     */
    private static class ForcePanelChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            JValueSlider s = (JValueSlider)e.getSource();
            float val = s.getValue().floatValue();
            Force   f = (Force)s.getClientProperty("force");
            Integer p = (Integer)s.getClientProperty("param");
            f.setParameter(p.intValue(), val);
        }
    } // end of inner class ForcePanelChangeListener
    
    /**
     * Create and displays a new window showing a configuration panel
     * for the given ForceSimulator.
     * @param fsim the force simulator
     * @return a JFrame instance containing a configuration interface
     * for the force simulator
     */
    public static JFrame showForcePanel(ForceSimulator fsim) {
        JFrame frame = new JFrame("prefuse Force Simulator");
        frame.setContentPane(new JForcePanel(fsim));
        frame.pack();
        frame.setVisible(true);
        return frame;
    }
    
} // end of class JForcePanel
