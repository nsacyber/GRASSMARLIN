/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.platform;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class UISplitPanel extends UIPanel implements ActionListener {

    protected UIPanel left;
    protected UIPanel right;
    protected String LEFT = BorderLayout.WEST;
    protected String RIGHT = BorderLayout.EAST;

    public UISplitPanel() {
        FlowLayout r_layout = getSideLayout();
        FlowLayout l_layout = getSideLayout();
        BorderLayout m_layout = new BorderLayout();

        setLayout(m_layout);
        r_layout.setAlignment(FlowLayout.RIGHT);
        l_layout.setAlignment(FlowLayout.LEFT);

        left = new UIPanel();
        right = new UIPanel();

        left.setLayout(l_layout);
        right.setLayout(r_layout);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }
    
    protected FlowLayout getSideLayout() {
        return new FlowLayout();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        revalidate();
    }
    
}
