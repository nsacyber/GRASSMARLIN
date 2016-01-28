package prefuse.util.force;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import prefuse.util.ui.JForcePanel;

/**
 * Swing Action components that brings up a dialog allowing users to configure
 * a force simulation.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.util.ui.JForcePanel
 */
public class ForceConfigAction extends AbstractAction {

    private JDialog dialog;
    
    /**
     * Create a new ForceConfigAction.
     * @param frame the parent frame for which to create the dialog
     * @param fsim the force simulator to configure
     */
    public ForceConfigAction(JFrame frame, ForceSimulator fsim) {
        dialog = new JDialog(frame, false);
        dialog.setTitle("Configure Force Simulator");
        JPanel forcePanel = new JForcePanel(fsim);
        dialog.getContentPane().add(forcePanel);
        dialog.pack();
    }
    
    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        dialog.setVisible(true);
    }

} // end of class ForceConfigAction
