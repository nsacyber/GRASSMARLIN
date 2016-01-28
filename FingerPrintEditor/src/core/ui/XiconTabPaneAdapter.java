package core.ui;

import java.awt.*;

/**
 * Facilitates interaction with the XiconTabPane 
 * 
 * 2007.04.21 - New
 */
public interface XiconTabPaneAdapter {

    /**
     * Called when a user requests a tab to close.  
     * @param pane The tabbed pane on which the action was fired
     * @param selectedTab The tab that would like to be closed
     */
    public void processTabClose(XiconTabPane pane, Component selectedTab);
    
    
    /**
     * Called when a user requests a new tab.  Will not display the
     * tab if the response is null
     * @param pane The tabbed pane on which the new tab will be added
     * @param selectedTab The tab that is currently selected
     */
    public void processRequestNewTab(XiconTabPane pane, Component selectedTab);
    
    
    
}
