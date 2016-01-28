/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.menu;


import javax.swing.JCheckBoxMenuItem;


/**
 * Check box menu item
 * 
 * @author rdguill
 */
public class CheckBoxMenuItem extends JCheckBoxMenuItem {

    public CheckBoxMenuItem(Menu item) {
        super(MenuAction.actionMap.get(item));
        this.setText(item.text);
        this.setSelected(true);
    }

}
