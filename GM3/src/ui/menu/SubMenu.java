/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.menu;

import core.importmodule.LivePCAPImport;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.LayoutStyle;
import ui.dialog.DialogManager;
import ui.icon.Icons;


/**
 * Observes the graphs 'auto-update' status.
 */
public class SubMenu extends JPanel implements Observer {
	
	JButton importBtn;
	JButton refreshBtn;
	JButton start;
	JButton stop;
	JCheckBox hideInfo;
	JCheckBox autoUpdate;
	JComboBox<LivePCAPImport> devices;
	
	public SubMenu() {
        importBtn   = new JButton();
        refreshBtn  = new JButton();
        start       = new JButton();
        stop        = new JButton();
        hideInfo    = new JCheckBox();
        autoUpdate  = new JCheckBox();
        devices     = new JComboBox<>();
        devices.setBorder(null);
        
        importBtn.setAction(MenuAction.OpenImportAct.action);
        importBtn.setText("Import");
        refreshBtn.setText("Refresh");
        refreshBtn.setEnabled(false);
        refreshBtn.addActionListener(MenuAction.RefreshViews.action);
        start.setAction(MenuAction.StartLiveCapture.action);
        start.setText("Start");
        stop.setAction(MenuAction.EndLiveCapture.action);
        stop.setText("Stop");
        hideInfo.setText("Show Networks");
        hideInfo.setSelected(true);
        hideInfo.addActionListener(MenuAction.ToggleNetworksInView.action);
        autoUpdate.setText("Auto Update");
        autoUpdate.setSelected(true);
        devices     = new JComboBox();
        
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        layout.setLayoutStyle(new LayoutStyle(){
            @Override
            public int getPreferredGap(JComponent component1, JComponent component2, LayoutStyle.ComponentPlacement type, int position, Container parent) {
                return 4;
            }
            @Override
            public int getContainerGap(JComponent component, int position, Container parent) {
                return 4;
            }
        });
        
        JButton fman = Icons.Manager.createButton();
        
        fman.setAction(new AbstractAction(){
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogManager.FMDialog(true);
            }
        });
        
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
//                .addContainerGap()
//                .addComponent(fman)
                .addContainerGap()
                .addComponent(importBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(refreshBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hideInfo)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(autoUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(devices, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(start)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(stop)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
//                    .addComponent(fman)
                    .addComponent(importBtn)
                    .addComponent(refreshBtn)
                    .addComponent(hideInfo)
                    .addComponent(autoUpdate)
                    .addComponent(start)
                    .addComponent(stop)
                    .addComponent(devices, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        
        devices.setBorder(null);
        devices.setPreferredSize(new Dimension(200, (int) start.getPreferredSize().getHeight()-2)); 
        devices.setToolTipText("Live Capture Device");
        autoUpdate.addActionListener((act)->{
            refreshBtn.setEnabled(!autoUpdate.isSelected());
        });
    }
	
	private void initComponent() {
        enabled(false);
	}

    public SubMenu useList(List<LivePCAPImport> devices) {
        devices.forEach(this.devices::addItem);
        enabled( !devices.isEmpty() );
        return this;
    }
    
    public Object getLiveDevice() {
        return devices.getSelectedItem();
    }
    
    public void enabled(Boolean b) {
        devices.setEnabled(b);
        start.setEnabled(b);
        stop.setEnabled(b);
        
        devices.setToolTipText(b?"Select NetworkInterface":"Feature Unavailable");
        start.setToolTipText(b?"Start capture":"Feature Unavailable");
        stop.setToolTipText(b?"Stop capture":"Feature Unavailable");
        revalidate();
    }

    @Override
    public void update(Observable o, Object arg) {
        System.out.println("omfg");
        if( arg instanceof Map ) {
            Map<String,Boolean> map = (Map<String,Boolean>) arg;
            
            Boolean b = map.get("auto.update");
            if( b != null && autoUpdate.isSelected() != b ) {
                autoUpdate.setSelected(b);
                refreshBtn.setEnabled(!autoUpdate.isSelected());
            }
            
            b = map.get("hide.networks");
            if( b != null && hideInfo.isSelected() != b )
                hideInfo.setSelected(b);
            
        }
    }

    public boolean autoUpdateEnabled() {
        return autoUpdate.isSelected();
    }
	
}