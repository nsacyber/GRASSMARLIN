/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.menu;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;


/**
 *
 */
public class MenuBar extends JMenuBar {

    static HashMap< Menu.DropDown, DropDown> handles;
    
    Map<Menu.DropDown, JComponent> shim;
    
    public MenuBar() {
        shim = new HashMap<>();
        handles = new HashMap<>();
        Menu.MenuGroups.forEach((Menu.DropDown _dropdown, ArrayList<Menu> _menus) -> {
            DropDown dropdown = new DropDown(_dropdown);
            
            shim.put( _dropdown, dropdown );
            
            _dropdown.item = dropdown;
            
            _menus.forEach((Menu menu) -> {
                DropDown addToMenu = dropdown;
                JMenuItem item = new MenuItem(menu);
                menu.item = item;
                if (menu.parent != null) {
                    addToMenu = (DropDown) dropdown.get(menu.parent);
                }
                if (menu.toggle) {
                    item = new CheckBoxMenuItem(menu);
                } else if (menu.isSub) {
                    item = new DropDown(menu.text);
                }

                addToMenu.addItem(menu, item);

                if (menu.sep) {
                    dropdown.Separate();
                }
            });
            dropdown.setEnabled(_dropdown.enabled);
            add(dropdown);
        });

        
    }

    public void addDropdownAction( String menuName, ActionListener actionListener ) {
        try {
            Menu.DropDown dropDown = Menu.DropDown.valueOf(menuName);
            ((DropDown)shim.get(dropDown)).addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    actionListener.actionPerformed(null);
                }
            });
        } catch( IllegalArgumentException ex ) {
            Logger.getLogger(MenuBar.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public final MenuBar add(DropDown dropdown) {
        handles.put(dropdown.handle, dropdown);
        this.add((Component) dropdown);
        return this;
    }

    public static DropDown get(Menu.DropDown handle) {
        return handles.get(handle);
    }
    
}
