/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.dialog;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import ui.icon.Icons;


/**
 * Displays version information for Grassmarlin
 */
public final class AboutDialog extends JDialog {
    
    GroupLayout layout;

    public AboutDialog() {
        this.setTitle("About: GRASSMARLIN");
//        this.setPreferredSize(new Dimension(475, 250));
        this.setResizable(false);
        this.layout = new GroupLayout(getContentPane());
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        this.setLayout(layout);
        initComponent();
    }

    public void initComponent() {

        JLabel lblGM = new JLabel("GRASSMARLIN");
        lblGM.setFont(lblGM.getFont().deriveFont(Font.BOLD, 14));
        
        JLabel lblSub = new JLabel("SCADA and ICS analysis tool");

        JLabel lblVer = new JLabel("Product Version: ");
        lblVer.setFont(lblVer.getFont().deriveFont(Font.BOLD));
        
        JLabel lblVersion = new JLabel("3.0.0");
        
        JLabel lblVen = new JLabel("Vendor: ");
        lblVen.setFont(lblVen.getFont().deriveFont(Font.BOLD));

        JLabel lblVendor = new JLabel("Department of Defense");
        
        JLabel lblIcon = new JLabel();
        lblIcon.setIcon(Icons.Grassmarlin_circle_lg.getIcon());
        lblIcon.revalidate();
        lblIcon.repaint();
        
        JDialog self = this;
        JButton btnClose = new JButton("Close");
        btnClose.addActionListener(new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                self.setVisible(false);
            }
        });
            
        layout.setHorizontalGroup(layout.createSequentialGroup()
            .addComponent(lblIcon)
            .addGap(50)
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblGM)
                .addComponent(lblSub)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(lblVer)
                        .addComponent(lblVersion)
                )
                .addGroup(layout.createSequentialGroup()
                        .addComponent(lblVen)
                        .addComponent(lblVendor)
                )
                .addGroup(layout.createSequentialGroup()
                        .addGap(130)
                        .addComponent(btnClose)
                )
            )
        );
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblIcon)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblGM)
                    .addComponent(lblSub)
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(lblVer)
                            .addComponent(lblVersion)
                    )
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(lblVen)
                            .addComponent(lblVendor)
                    )
                    .addGap(100)
                    .addComponent(btnClose)
                )
            )
        );
        this.pack();
        this.setVisible(true);
    }
   
}
