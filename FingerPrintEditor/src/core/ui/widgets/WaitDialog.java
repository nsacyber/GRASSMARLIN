package core.ui.widgets;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;

/**
 * Utility for showing progress to user while working
 * 
 * 2005.12.23 - Transitioned to Repository
 * 2007.02.16 - Reformatted, added size option
 */
public class WaitDialog extends JDialog {
    public static final long serialVersionUID = 100001;
    private WaitDialog.Runnable runnable  = null;
    private String message = null;
    private JLabel messageJL = null;
    private Container container = null;
    private JProgressBar progressBar = null;
    private static int DEFAULT_WIDTH = 200;
    private static int DEFAULT_HEIGHT = 80;

    /**
     * This class can only be instantiated by itself
     */
    private WaitDialog (JDialog dialog) {
        super(dialog);
    }

    /**
     * This class can only be instantiated by itself
     */
    private WaitDialog (JFrame frame) {
        super(frame);
    }


    /**
     * Will display message in a modal JDialog while in the run method of runnable.
     * Will set the location of the JDialog relative to container
     *
     * @return A Reference to the WaitDialog
     */
    public static WaitDialog showDialog (WaitDialog.Runnable runnable, String message, Container container) {
        return WaitDialog.showDialog(runnable, message, container, WaitDialog.DEFAULT_WIDTH, WaitDialog.DEFAULT_HEIGHT);
    }

    /**
     * Will display message in a modal JDialog while in the run method of runnable.
     * Will set the location of the JDialog relative to container
     *
     * @return A Reference to the WaitDialog
     */
    public static WaitDialog showDialog (WaitDialog.Runnable runnable, String message, Container container, int width, int height) {
        // Create the new WaitDialog
        WaitDialog newDialog = null;
        if (container instanceof JDialog) {
            newDialog = new WaitDialog((JDialog)container);
        }
        else if (container instanceof JFrame) {
            newDialog = new WaitDialog((JFrame)container);
        }
        else {
            newDialog = new WaitDialog( (JDialog)null );
            newDialog.setLocationRelativeTo(container);
        }
        newDialog.buildDialog(width, height);
        // Set parameters
        runnable.setDialog(newDialog);
        newDialog.container = container;
        newDialog.runnable = runnable;
        newDialog.setMessage(message);
        // Create a new thread to run the runnable for the new WaitDialog
        Thread processThread = new ProcessThread(newDialog);
        processThread.start();
        // wait for the dialog to display then return
        while (!newDialog.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }
        // return the reference to the new WaitDialog
        return newDialog;
    }




    /**
     * Builds the interface for this Dialog
     */
    private void buildDialog (int width, int height) {
        JPanel mainJP = new JPanel(new GridBagLayout());
        this.progressBar = new JProgressBar();
        this.progressBar.setIndeterminate(true);
        this.messageJL = new JLabel("No Message To Display");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1f;
        gbc.weighty = 1f;
        gbc.insets = new Insets(5,0,0,0);
        gbc.anchor = GridBagConstraints.CENTER;

        mainJP.add(this.messageJL, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx++;
        gbc.insets = new Insets(5,30,5,30);
        mainJP.add(this.progressBar, gbc);

        this.setTitle("");
        this.setModal(true);
        this.setResizable(false);
        this.addKeyListener(kl);
        this.getContentPane().add(mainJP);
        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setSize(width, height);
    }

    /**
     * Shows the dialog with the message and calls the run method on the runnable
     **/
    private void processShowAction () {
        this.setLocationRelativeTo(this.container);
        DisplayThread displayThread = new DisplayThread(this);
        displayThread.start();
        try {
            this.runnable.run();
        } catch (Throwable t) {
            String errorMessage = "Exception caught in run method of WaitDialog.Runnable!";
            //this.logger.fatal(errorMessage, t);
            this.displayError(errorMessage);
        }
        this.dispose();
    }

    /**
     * Displays the errorMessage in a popup Dialog
     */
    private void displayError (final String errorMessage) {
        java.lang.Runnable runnable = new java.lang.Runnable(){
            public void run(){
                JOptionPane.showMessageDialog(WaitDialog.this,
                                              errorMessage,
                                              "Error",
                                              JOptionPane.ERROR_MESSAGE);
            }
        };
        try {
            SwingUtilities.invokeAndWait(runnable);
        } catch (InterruptedException ie) {
        } catch (InvocationTargetException ite) {
        }
    }


    /**
     * Sets the message currently displayed on the modal dialog
     */
    public void setMessage(String message) {
        this.message = "   " + message + "   ";
        this.messageJL.setText(this.message);
        this.messageJL.setToolTipText(this.message);
    }


    /**
     * Worker Thread for displaying the modal dialog and running the runnable
     */
    private static class ProcessThread extends Thread {
        private WaitDialog dialog;
        public ProcessThread(WaitDialog dialog) {
            super("WaitDialog ProcessThread");
            this.dialog = dialog;
        }
        public void run() {
            dialog.processShowAction();
        }
    }


    /**
     * Worker thread for displaying the modal dialog
     */
    private static class DisplayThread extends Thread {
        private WaitDialog dialog;
        public DisplayThread(WaitDialog dialog) {
            super("WaitDialog DisplayThread");
            this.dialog = dialog;
        }
        public void run(){
            this.dialog.setVisible(true);
        }
    }


    /**
     * Listens for the secret KeyStroke to dispose of the Dialog early
     */
    private KeyListener kl = new KeyListener() {
        private int secretKeyCount = 0;
        public void keyPressed(KeyEvent ke) {
            if (ke.getKeyChar() == '~') {
               secretKeyCount++;
            }
            else {
               if (secretKeyCount == 2) {
                   switch (ke.getKeyChar()) {
                       case 'd':
                                Object source = ke.getSource();
                                if (source instanceof WaitDialog) {
                                    ((WaitDialog)source).dispose();
                                }
                                break;
                   }
               }
               else {
                   secretKeyCount = 0;
               }
            }
        }
        public void keyReleased(KeyEvent ke) {
        }
        public void keyTyped(KeyEvent ke) {
        }
     };

     /**
      * Runnable for use with WaitDialog.  Contains a reference to the
      * WaitDialog so the Runnable can update the status messages while
      * running.
      */
     public static abstract class Runnable {
         protected WaitDialog dialog = null;
         public void setDialog(WaitDialog dialog) {
             this.dialog = dialog;
         }
         public WaitDialog getWaitDialog() {
             return this.dialog;
         }
         public abstract void run();
     }


    /**
     * Debug only
     */
    public static void main(String[] args) {
        WaitDialog.Runnable runnable = new WaitDialog.Runnable() {
            public void run() {
                int counter = 0;
                while (counter < 10) {
                    try { 
                        Thread.sleep(1000);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }         
                    //this.dialog.setMessage("Processing Very Long very very long SQL Query For you Count: " + counter);
                    this.dialog.setMessage("Count: " + counter);
                    counter++;
                }
                throw new IllegalArgumentException();
            }
        };

        JFrame frame = new JFrame();
        frame.getContentPane().add( new JLabel("Noone") );
        frame.setSize(200,200);
        frame.setVisible(true);   
        WaitDialog.showDialog(runnable, "Processing SQL Query For you...", frame);
    }
}
