/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import core.types.InvokeObservable;
import javax.swing.JFrame;
import ui.icon.Icons;

/**
 *
 */
public class GraphSettingsDialog extends JFrame {
    
    public static final String DEFAULT_NETWORK_COLLAPSE = "default.network.collapse";
    public static final int DEFAULT_NETWORK_COLLAPSE_VAL = 10;
    
    public static final String DEFAULT_CLOUD_COLLAPSE = "default.cloud.collapse";
    public static final int DEFAULT_CLOUD_COLLAPSE_VAL = 10;
    
    public static final String DEFAULT_VIEW_DELAY = "default.view.delay";
    public static final int DEFAULT_VIEW_DELAY_VAL = 1500;
    
    private javax.swing.JSpinner networkLimit;
    private javax.swing.JSpinner cloudLimit;
    private javax.swing.JButton saveChanges;
    private javax.swing.JSpinner viewDelay;
    private javax.swing.JButton cancel;

    final InvokeObservable update;
    
    public Integer getNetworkLimit() {
        return networkCollapse;
    }
    public Integer getCloudLimit() {
        return cloudCollapse;
    }
    public Integer getViewDelay() {
        return viewDelayMS;
    }
    
    Runnable onSave, beforeSave, onOpen;
   
    
    int networkCollapse, cloudCollapse, viewDelayMS;
    
    public GraphSettingsDialog() {
        networkCollapse = DEFAULT_NETWORK_COLLAPSE_VAL;
        cloudCollapse = DEFAULT_CLOUD_COLLAPSE_VAL;
        viewDelayMS = DEFAULT_VIEW_DELAY_VAL;
        update = new InvokeObservable(this);
        setTitle("Configure View Settings");
        setIconImage(Icons.Cog.get32());
        initComponents();
        reset(null);
        saveChanges.addActionListener(this::fireChange);
        cancel.addActionListener(this::reset);
    }
    
    public void onSave(Runnable onSave) {
        this.onSave = onSave;
    }
    
    public void onOpen(Runnable onOpen) {
        this.onOpen = onOpen;
    }
   
    public void beforeSave(Runnable beforeSave) {
        this.beforeSave = beforeSave;
    }
    
    final void reset(Object o) {
        networkLimit.setValue(networkCollapse);
        cloudLimit.setValue(cloudCollapse);
        viewDelay.setValue(viewDelayMS);
        setVisible(false);
    }
    
    void fireChange(Object e) {
        networkCollapse = (Integer)networkLimit.getValue();
        cloudCollapse = (Integer)cloudLimit.getValue();
        viewDelayMS = (Integer)viewDelay.getValue();
        this.beforeSave.run();
        update.setChanged();
        update.notifyObservers();
        this.setVisible(false);
        this.onSave.run();
    }
    
    public InvokeObservable getUpdate() {
        return update;
    }
    
    private void updateSpinners() {
        networkLimit.setValue(this.networkCollapse);
        cloudLimit.setValue(this.cloudCollapse);
        viewDelay.setValue(this.viewDelayMS);
    }
    
    @Override
    public void setVisible(boolean b) {
        if(b && onOpen != null) {
            onOpen.run();
            updateSpinners();
        }
        super.setVisible(b);
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {
        javax.swing.JLabel jLabel1;
        javax.swing.JLabel jLabel2;
        javax.swing.JLabel jLabel3;
        javax.swing.JTextArea jTextArea1;
        javax.swing.JTextArea jTextArea2;
        javax.swing.JTextArea jTextArea3;
        javax.swing.JSeparator jSeparator1;
        javax.swing.JPanel panel;

        saveChanges = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        networkLimit = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        cloudLimit = new javax.swing.JSpinner();
        jTextArea1 = new javax.swing.JTextArea();
        jTextArea2 = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        viewDelay = new javax.swing.JSpinner();
        jTextArea3 = new javax.swing.JTextArea();
        panel = new javax.swing.JPanel();
        
        saveChanges.setText("Save Changes");

        cancel.setText("Cancel");

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Network Collapse Host Limit");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("Collapse Cloud Host Limit");

        jTextArea1.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        jTextArea1.setColumns(20);
        jTextArea1.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("When the amount of hosts in a physical view cloud exceeds this limit the cloud will collapse automatically.");
        jTextArea1.setWrapStyleWord(true);
        jTextArea1.setBorder(null);

        jTextArea2.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        jTextArea2.setColumns(20);
        jTextArea2.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        jTextArea2.setLineWrap(true);
        jTextArea2.setRows(5);
        jTextArea2.setText("When the amount of hosts in a logical view network exceeds this limit the network will collapse automatically.");
        jTextArea2.setWrapStyleWord(true);
        jTextArea2.setBorder(null);

        jLabel3.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel3.setText("View Update Delay (ms)");

        jTextArea3.setBackground(javax.swing.UIManager.getDefaults().getColor("Panel.background"));
        jTextArea3.setColumns(20);
        jTextArea3.setFont(new java.awt.Font("Tahoma", 0, 13)); // NOI18N
        jTextArea3.setLineWrap(true);
        jTextArea3.setRows(5);
        jTextArea3.setText("The millisecond delay between each view update.");
        jTextArea3.setWrapStyleWord(true);
        jTextArea3.setBorder(null);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(saveChanges)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancel))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cloudLimit, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(viewDelay, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(networkLimit, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextArea1, javax.swing.GroupLayout.PREFERRED_SIZE, 242, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextArea3, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextArea2, javax.swing.GroupLayout.PREFERRED_SIZE, 258, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(networkLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextArea2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(cloudLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextArea1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(viewDelay, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextArea3, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveChanges)
                    .addComponent(cancel))
                .addContainerGap())
        );
//        setLayout(new BorderLayout());
        add(panel);
        pack();
    }// </editor-fold>                        

}
