/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.platform;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.Box.Filler;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.border.EmptyBorder;


/**
 *
 */
public class TitleBar extends UIPanel {
    static final Color defaultGray = new Color( 240, 240,240 );
    JLabel l;
    boolean active;
    
    public TitleBar() {
        setLayout( new BorderLayout() );
        l = null;
        Dimension dim = new Dimension(24,24);
        Filler filler = new Filler( dim, dim, dim );
        JMenuBar menu = new JMenuBar();
        menu.add(filler);
        add( menu, BorderLayout.CENTER );
        active = false;
    }
    
	public TitleBar(String title) {
        active = false;
		setLayout( new BorderLayout() );
		
        l = new JLabel(title);
        l.setBorder(new EmptyBorder(5,5,5,5));
		
        JMenuBar menu = new JMenuBar();
		menu.add( l );
		add( menu, BorderLayout.CENTER );
	}
    
    public void setActive( Boolean active ) {
        if( l != null ) {
            this.active = active;
            l.setForeground( active ? Color.BLUE : Color.BLACK );
        }
    }

    @Override
    public void paint(Graphics g) {
        if( active ) {
            g.setColor(defaultGray);
            g.fillRect(
                getX(),
                getY(),
                getWidth(),
                getHeight()
            );
            if( l != null )
                l.paint(g);
            paintBorder(g);
        }
        else super.paint(g);
    }  
}
