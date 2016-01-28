package core.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A simple tabbed pane with some custom features. 
 * Most tabs include an x that allows users to close that
 * tab directly.   
 * 
 * 2007.04.21 - Ported from CustomTabPane
 * 2007.09.04 - Imported to repository
 * 2007.09.14 - Added Mapped keys; Added KeyListener //TODO Make sure KeyListener is the best way
 * 2007.09.17 - Tried to add ctrl+TAB and ctrl+shift+TAB actions - failed because they are used in focus events
 */
public class XiconTabPane extends JTabbedPane {
    public static final long serialVersionUID = 1001;

    /** Action to close the current tab */
    private XiconTabPaneAdapter adapter;
    /** Icon to close the tab */
    private ClickableIcon xIcon;
    /** Added to new tabs to catch key events otherwise missed if
     * the component decides to honor them **/
    private KeyListener keyListener;
    
    /**
     * Names for actions supported by this TabPane.
     * By default - an Empty action is set for each of these
     */
    enum KeyActionName { 
        NEW_TAB,
        CLOSE_TAB,
        NEXT_TAB,
        PREVIOUS_TAB
    }

    /** 
     * Default constructor which simply creates a JTabbedPane
     */
    public XiconTabPane () {
        super();
        this.init();
    }

    /** Constructor to create JTabbedPane with placement */
    public XiconTabPane (int tabPlacement) {
        super(tabPlacement);
        this.init();
    }

    /** Constructor to set placement and layout of the JTabbedPane */
    public XiconTabPane (int tabPlacement, int tabLayoutPolicy) {
        super (tabPlacement, tabLayoutPolicy);
        this.init();
    }
    
    
    /**
     * Initializes the custom portion
     */
    private final void init() { 
        this.xIcon = new ClickableIcon (new ImageIcon(this.getClass().getResource("/images/navigate_cross.png")),
                     this, this.mouseListener);
        this.setXiconTabPaneAdapter(this.defaultAdapter);
        this.mapKeyActions();
    }

    /**
     * Maps several KeyStrokes to TabPane behavior
     */
    private final void mapKeyActions() { 
        // crtl+t == open new tab
        KeyStroke newTabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
        AbstractAction newTabAction = new AbstractAction("New Tab") { 
            public static final long serialVersionUID = 10001;
            public void actionPerformed(ActionEvent e) { 
                XiconTabPane.this.adapter.processRequestNewTab(XiconTabPane.this, XiconTabPane.this.getSelectedComponent());
            }
        };
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(newTabKeyStroke, KeyActionName.NEW_TAB);
        this.getActionMap().put(KeyActionName.NEW_TAB, newTabAction);
        // ctrl+w == close tab
        KeyStroke closeTabKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK);
        AbstractAction closeTabAction = new AbstractAction("Close Tab") { 
            public static final long serialVersionUID = 10001;
            public void actionPerformed(ActionEvent e) { 
                XiconTabPane.this.adapter.processTabClose(XiconTabPane.this, XiconTabPane.this.getSelectedComponent());
            }
        };
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(closeTabKeyStroke, KeyActionName.CLOSE_TAB);
        this.getActionMap().put(KeyActionName.CLOSE_TAB, closeTabAction);
        // ctrl+pu && ctrl+shif+tab == previous tab
        final KeyStroke previousTabKeyStroke = KeyStroke.getKeyStroke("ctrl PAGE_UP");
        final KeyStroke previousTabKeyStroke2 = KeyStroke.getKeyStroke("ctrl shift TAB");
        final AbstractAction previousTabAction = new AbstractAction("Previous Tab") { 
            public static final long serialVersionUID = 10001;
            public void actionPerformed(ActionEvent ae) {
                int previousIndex = XiconTabPane.this.getSelectedIndex() - 1;
                if (previousIndex >= 0) {    
                    XiconTabPane.this.setSelectedIndex(previousIndex);
                }
            }
        };
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(previousTabKeyStroke, KeyActionName.PREVIOUS_TAB);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(previousTabKeyStroke2, KeyActionName.PREVIOUS_TAB);
        this.getActionMap().put(KeyActionName.PREVIOUS_TAB, previousTabAction);
        // ctrl+pd && ctrl+tab == next tab
        final KeyStroke nextTabKeyStroke = KeyStroke.getKeyStroke("ctrl PAGE_DOWN");
        final KeyStroke nextTabKeyStroke2 = KeyStroke.getKeyStroke("ctrl TAB");
        final AbstractAction nextTabAction = new AbstractAction("Next Tab") { 
            public static final long serialVersionUID = 10001;
            public void actionPerformed(ActionEvent ae) { 
                int nextIndex = XiconTabPane.this.getSelectedIndex() + 1;
                if (nextIndex < XiconTabPane.this.getTabCount()) { 
                    XiconTabPane.this.setSelectedIndex(nextIndex);
                }
            }
        };
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(nextTabKeyStroke, KeyActionName.NEXT_TAB);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(nextTabKeyStroke2, KeyActionName.NEXT_TAB);
        this.getActionMap().put(KeyActionName.NEXT_TAB, nextTabAction);
        // set up key listener
        this.keyListener = new KeyAdapter() { 
            public void keyPressed(KeyEvent e) {
                KeyStroke stroke = KeyStroke.getKeyStrokeForEvent(e);
                if (stroke == nextTabKeyStroke || stroke == nextTabKeyStroke2) { 
                    nextTabAction.actionPerformed(new ActionEvent(this, 0, ""));
                } else if (stroke == previousTabKeyStroke || stroke == previousTabKeyStroke2) { 
                    previousTabAction.actionPerformed(new ActionEvent(this, 0, ""));
                }
                
            }
        };
    }
    
    
    /**
     * Adds Tab
     * @param component
     */
    public void addTab (Component component){
        this.addTab(component.getName(), this.xIcon, component);
        component.addKeyListener(this.keyListener);
    }

    /**
     * Adds the Tab without adding the X
     * @param title Title for the Tab 
     * @param component Component to display
     */
    public void addTabWithoutIcon(String title, Component component) { 
        super.addTab(title, component);
        component.addKeyListener(this.keyListener);
    }
    
    /**
     * Creates a tab with the default icon, selected title and adds the component listed
     */
    public void addTab (String title, Component component) {
        int componentIndex = this.indexOfComponent(component);
        if (componentIndex == -1) { 
            super.addTab(title, this.xIcon, component);
        }
        super.setSelectedComponent(component);
        component.addKeyListener(this.keyListener);
    }


    /** Set action to be called when a tab is closed */
    public void setXiconTabPaneAdapter(XiconTabPaneAdapter adapter) {
        this.adapter = adapter;
    }
    
    /**
     * Default adapter for tab close event.
     * Removes the component from the tab pane
     */
    private XiconTabPaneAdapter defaultAdapter = new XiconTabPaneAdapter() { 
        public void processTabClose(XiconTabPane pane, Component selectedTab) { 
            pane.remove(selectedTab);
        }
        public void processRequestNewTab(XiconTabPane pane, Component selectedTab) {
        }
    };

    /**
     * Listens for users to clix on the X
     */
    private MouseListener mouseListener = new MouseAdapter() { 
        /** Handes when mouse clicked and the mouse is centered on the "x" icon */
        public void mouseClicked (MouseEvent e) {
            if (XiconTabPane.this.xIcon.getIconPosition ().contains (e.getPoint ()) && XiconTabPane.this.getTabCount () > 0) {
                int index = XiconTabPane.this.getSelectedIndex();
                Component component = XiconTabPane.this.getComponentAt(index);
                XiconTabPane.this.adapter.processTabClose(XiconTabPane.this, component);
            }
            e.consume();
        }
    };
}
