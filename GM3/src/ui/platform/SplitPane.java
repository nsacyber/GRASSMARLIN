/*

 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.


 */
package ui.platform;

import java.awt.Component;
import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;


/**
 *
 * Real layout | users see
 * CCCCCCCCCCC | AAACCCC
 * CBBBBBCCCCC | AAACCCC
 * CBAAABCCCCC | AAACCCC
 * CBAAABCCCCC | BBBCCCC
 * CBAAABCCCCC | 
 * CBBBBBCCCCC | 
 * CBBBBBCCCCC | 
 * CCCCCCCCCCC | 
 * 
 * (A in B) in C
 * 
 */
public class SplitPane extends UIPanel {

    static JSplitPane B;
    static JPanel A;
    static JSplitPane C;

    public static enum Position {
        LEFT, CENTER, RIGHT, LEFT_BOTTOM; 
    }
    
    public SplitPane() {
        A = new JPanel();
        B = new JSplitPane();
        C = new JSplitPane();
        
        noBorder(B);
        //noBorder(C);
        
        B.setOrientation(JSplitPane.VERTICAL_SPLIT);
        A.setBorder(null);
        B.setBorder(null);
        C.setBorder(null);
        
        C.setLeftComponent(B);
        B.setBottomComponent(A);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(
                        C,
                        javax.swing.GroupLayout.Alignment.TRAILING,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        400,
                        Short.MAX_VALUE
                )
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(
                        C,
                        javax.swing.GroupLayout.DEFAULT_SIZE,
                        300,
                        Short.MAX_VALUE
                )
        );
    }

    private void noBorder(JSplitPane pane) {
        try {
            ((BasicSplitPaneUI)pane.getUI()).getDivider().setBorder(null);
        } catch(Exception ex) {
            Logger.getAnonymousLogger().log(Level.FINEST, title, ex);
        }
    } 
    
    public void add(Component component, Position position) {
        switch (position) {
            case RIGHT:
                C.setRightComponent(component);
                break;
            case CENTER: // tree
                B.setTopComponent(component);
                break;
//            case LEFT:
//                A.setLeftComponent(component);
//                break;
            case LEFT_BOTTOM:
//                A.add(component);
                B.setBottomComponent(component);
                break;
            default:
                break;
        }
    }

    public void setInitialSize(int x, int y) {

        int offset= 24; // sum of horizontal border gaps
        int xFrac = (x / 6) - offset;
        int yFrac = (y / 4) - offset;

        Dimension leftDim = new Dimension( xFrac-offset, y);
        Dimension centerDim = new Dimension( xFrac-offset, y);
        Dimension rightDim = new Dimension( xFrac * 2, y);
        
        C.getRightComponent().setPreferredSize(rightDim);
        A.setPreferredSize(centerDim);
//        A.getLeftComponent().setPreferredSize(leftDim);
        
        Dimension topDim = new Dimension(  xFrac*2-offset,  yFrac*3-offset);
        Dimension botDim = new Dimension( xFrac*2-offset ,  yFrac-offset);
        
        B.getTopComponent().setPreferredSize(topDim);
        B.getBottomComponent().setPreferredSize(botDim);
    }

    
    public void sizeDefault() {
//        A.resetToPreferredSizes();
        B.resetToPreferredSizes();
        C.resetToPreferredSizes();
    }

    
    public static void toggleSummaryView(boolean showOrHide) {
        B.getLeftComponent().setVisible(showOrHide);
        B.resetToPreferredSizes();
    }
    
    public static void toggleEventsView(boolean showOrHide) {
        B.getBottomComponent().setVisible(showOrHide);
        B.resetToPreferredSizes();
    }

    public static void toggleNetworkView(boolean showOrHide) {
//        A.getLeftComponent().setVisible(showOrHide);
//        A.resetToPreferredSizes();
    }

    public static void toggleTopologyView(boolean showOrHide) {
//        A.getRightComponent().setVisible(showOrHide);
//        A.resetToPreferredSizes();
        //inner.getP
    }
    
}
