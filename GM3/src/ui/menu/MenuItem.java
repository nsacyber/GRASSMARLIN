/*

 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.


 */
package ui.menu;


import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;


/**
 *
 */
public class MenuItem extends JMenuItem {

    public MenuItem(Menu item) {
        super(MenuAction.actionMap.get(item));
        this.setText(item.text);
    }
    
    public MenuItem(Map<String, Object> item) {
        super((AbstractAction) item.get("action"));
        this.setText(item.get("text").toString());
    }
    
}
