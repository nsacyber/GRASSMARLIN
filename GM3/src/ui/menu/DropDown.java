/*

 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.


 */
package ui.menu;

import java.util.HashMap;
import java.util.function.Consumer;

import javax.swing.JMenu;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


/**
 *
 */
public class DropDown extends JMenu {

    HashMap< Menu, Object> handles;
    Menu.DropDown handle;
    boolean disabled;

    public DropDown(String displayText) {
        super(displayText);
        handles = new HashMap<>();
        disabled = false;
    }

    public DropDown(Menu.DropDown handle) {
        this(handle.displayText);
        this.handle = handle;
    }

    public boolean disabled() {
        return this.disabled;
    }

    public DropDown addItem(Menu menu, Object item) {
        handles.put(menu, item);
        if (item.getClass().getTypeName().equals("ui.menu.MenuItem")) {
            this.add((MenuItem) item);
        }
        if (item.getClass().getTypeName().equals("ui.menu.CheckBoxMenuItem")) {
            this.add((CheckBoxMenuItem) item);
        }
        if (item.getClass().getTypeName().equals("ui.menu.DropDown")) {
            this.add((DropDown) item);
        }

        return this;
    }
    
    
    Consumer<DropDown> onHoverCB;
            
    public void onHover( Consumer<DropDown> onHoverCB ) {
        this.addMenuListener( new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                onHoverCB.accept(DropDown.this);
            }
            @Override
            public void menuDeselected(MenuEvent e) {
            }
            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

    public DropDown Separate() {
        this.addSeparator();
        return this;
    }

    public Object get(Menu menu) {
        Menu key = menu;
        Object obj = handles.get(key);
        if (obj == null) {
            for (Object o : handles.values()) {
                if (o.getClass().getTypeName().equals("ui.menu.DropDown")) {
                    DropDown dropdown = (DropDown) o;
                    obj = dropdown.get(menu);
                }
            }
        }
        return obj;
    }

    public void addAll(JMenu[] viewMenuItems) {
        int size = viewMenuItems.length;
        for( int i = 0; i < size; i++ ) {
            add( viewMenuItems[i] );
        }
    }
    
}
