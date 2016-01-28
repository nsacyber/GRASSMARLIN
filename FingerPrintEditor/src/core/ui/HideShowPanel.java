package core.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Simple component to use for hiding/showing a Component
 * 
 * 2007.05.02 - Extracted from innner classes
 * 2007.05.07 - Updated to use DisplayState enum
 * 2007.09.04 - Imported to repository from Xware; added default contructor; added isShow()
 */
public class HideShowPanel extends JPanel {
    private static final long serialVersionUID = 10001;
    private CardLayout cardLayout = null;
    private DisplayState currentState = DisplayState.HIDE;
    private enum DisplayState { 
        HIDE,
        SHOW
    }
    
    /**
     * Creates a new instance of HideShowPanel with no component. 
     * MUST call buildInterface before attempting to use this compnent.
     */
    protected HideShowPanel() { 
    }
    
    /**
     * Creates a new instance of HideShowPanel for component
     * @param componentToAdd
     */
    public HideShowPanel(Component component) {
        this.buildInterface(component);
    }
    
    /**
     * Builds the interface for the hide show panel
     * Protected to allow extending classes to build the interface after 
     * instantiation
     */
    protected void buildInterface(Component component){
        this.cardLayout = new CardLayout();
        super.setLayout(this.cardLayout);
        JPanel enabledPanel = new JPanel(new BorderLayout());        
        enabledPanel.add(component, BorderLayout.CENTER);
        super.add(enabledPanel, HideShowPanel.DisplayState.SHOW.toString());
        super.add(new JPanel(), HideShowPanel.DisplayState.HIDE.toString());
    }

    /**
     * Will hide or show the panel accordingly
     */
    public void setShow(boolean bv) {
        if (bv) {
            this.currentState = HideShowPanel.DisplayState.SHOW;
        }
        else {
            this.currentState = HideShowPanel.DisplayState.HIDE;
        }
        this.cardLayout.show(this, this.currentState.toString());
    }
   
    /**
     * Returns true if the panel is set to show its contents
     */
    public boolean isShow() { 
        return this.currentState == DisplayState.SHOW;
    }
        
}