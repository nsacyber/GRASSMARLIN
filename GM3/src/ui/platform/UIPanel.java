/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.platform;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;


/**
 *
 */
public class UIPanel extends JPanel {

    String title;

    public UIPanel(String title) {
        this.title = title;
        this.setBorder(BorderFactory.createEmptyBorder());
        initDefaultComponent();
    }

    public UIPanel() {
        title = null;
        initDefaultComponent();
    }

    private void initDefaultComponent() {
        this.setLayout( new BorderLayout() );
        if (title != null) {
            TitleBar bar = new TitleBar(title);
            this.add(bar, BorderLayout.NORTH);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.setVisible(enabled);
    }
    
}
