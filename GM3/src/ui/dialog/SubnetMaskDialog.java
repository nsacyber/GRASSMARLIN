/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.border.Border;
import ui.icon.Icons;

/**
 * Dialog which provides a new subnet mask to a callback when the original
 * subnet mask changes and is saved.
 */
public class SubnetMaskDialog extends JDialog {

    javax.swing.JButton saveChangesBtn;
    javax.swing.JCheckBox updateNetworkNameCheckbox;
    javax.swing.JTextField currentSubnet;
    javax.swing.JTextField newIPField;
    javax.swing.JTextField currentIP;
    javax.swing.JTextField newSubnet;

    final BiConsumer<int[], int[]> callback;
    final Border goodBorder, errorBorder;
    boolean closeOnCallback;
    final int[] originalIP;
    final int[] buffer;

    /**
     * @param callback Expects a consumer of two int[] the first argument is the
     * subnet mask, the second is the IP address
     * @param subnet Original subnet
     * @param ip Ip address
     */
    public SubnetMaskDialog(BiConsumer<int[], int[]> callback, String subnet, String ip) {
        this.errorBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0xA94442));
        this.goodBorder = BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(0x31708F));
        this.buffer = new int[]{255, 255, 255, 255};
        this.originalIP = new int[4];
        copyIfValid(ip, this.originalIP, false);
        this.callback = callback;
        this.closeOnCallback = false;

        initComponent();

        currentSubnet.setText(subnet);
        newSubnet.setText(subnet);
        currentIP.setText(ip);
        newIPField.setText(ip);

        newSubnet.setCaretPosition(newSubnet.getText().length());
        newSubnet.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                validate(newSubnet.getText());
            }
        });

        saveChangesBtn.addActionListener(e -> {
            save();
        });

        validate(newSubnet.getText());
    }

    public final void save() {
        if (this.closeOnCallback) {
            setVisible(false);
            this.dispose();
        }
        int[] ipBuffer = new int[4];
        if (copyIfValid(newIPField.getText(), ipBuffer, false)) {
            callback.accept(buffer, ipBuffer);
        }
    }
    
    /**
     * Allows the input text to be set external programmatically, for testing.
     *
     * @param text Input text to update from.
     */
    public final void externalValidate(String text) {
        newSubnet.setText(text);
        validate(text);
    }

    /**
     * Validates the input and updates UI accordingly.
     *
     * @param text Input text to update from.
     */
    public final void validate(String text) {
        if (copyIfValid(text, buffer, true)) {
            newSubnet.setBorder(goodBorder);
            saveChangesBtn.setEnabled(true);

            int[] newIP = new int[4];
            int[] oldIP = originalIP;
            int[] nMask = buffer;

            for (int i = 0; i < 4; i++) {
                newIP[i] = oldIP[i] & nMask[i];
            }

            newIPField.setText(bytesToString(newIP));

        } else {
            newSubnet.setBorder(errorBorder);
            saveChangesBtn.setEnabled(false);
        }
    }

    /**
     * Gets the users input text from the textfield.
     *
     * @return Text of newSubnet textfield.
     */
    String getText() {
        return newSubnet.getText();
    }

    /**
     * Converts each byte in an integer to an integer, places them in MSB order.
     *
     * @param val Integer to separate.
     * @return Array of shifted bytes as an int[].
     */
    public static int[] toBytes(int val) {
        int[] bytes = new int[4];
        for (int i = 0; i < 4; i++) {
            bytes[3 - i] = 0b11111111 & val;
            val >>= 8;
        }
        return bytes;
    }

    /**
     * Prints a int[] as a string separated by ".", if the string is to short
     * the default is "0.0.0.0"
     *
     * @param b int[] to print the first four indexes of.
     * @return The formatted string or default value.
     */
    public static String bytesToString(int[] b) {
        if (b.length < 4) {
            return "0.0.0.0";
        }
        return String.format("%d.%d.%d.%d", b[0], b[1], b[2], b[3]);
    }

    /**
     * parses the numbers out of the text and stores them in buffer if the text
     * is a valid netmask.
     *
     * @param text IP text to parse.
     * @param buffer Buffer to fill with values.
     * @param isSubnet True if the IP to check is a Subnet (no high bits below a
     * low significant), else false and regular will IPs copy.
     * @return true if the text parsed and buffer was written to, else false.
     */
    public static boolean copyIfValid(String text, int[] buffer, boolean isSubnet) {
        if (text.isEmpty()) {
            return false;
        }

        String[] parts = text.split("\\D+");
        if (parts.length != 4) {
            return false;
        }

        int[] ints = new int[4];
        for (int i = 0; i < 4; i++) {
            if (parts[i].isEmpty()) {
                return false;
            }
            ints[i] = Integer.valueOf(parts[i]);
            if (ints[i] > 255 || ints[i] < 0) {
                return false;
            }

        }

        int hash = intHash(ints);

        if (isSubnet && !isValidSubnet(hash)) {
            return false;
        }

        System.arraycopy(ints, 0, buffer, 0, 4);

        return true;
    }

    /**
     * Makes sure there are no high bits less significant than low bits.
     *
     * @param hash
     * @return
     */
    public static boolean isValidSubnet(int hash) {
        int bits = 0;
        while ((hash & 0b1) == 0 && bits < 31) {
            hash >>= 1;
            bits++;
        }

        while ((hash & 0b1) == 1 && bits < 32) {
            hash >>= 1;
            bits++;
        }
        return bits == 32;
    }

    public static Integer intHash(int[] ints) {
        return (ints[0] << 24) + (ints[1] << 16) + (ints[2] << 8) + ints[3];
    }

    public void setCloseOnCallback(boolean closeOnCallback) {
        this.closeOnCallback = closeOnCallback;
    }

    //<editor-fold defaultstate="collapsed" desc="gui code">
    private void initComponent() {
        javax.swing.JLabel jLabel1;
        javax.swing.JLabel currentNetworkIP;
        javax.swing.JLabel currentSubnetMask;
        javax.swing.JLabel jLabel4;
        javax.swing.JLabel jLabel5;
        javax.swing.JPanel jPanel1;
        javax.swing.JPanel jPanel2;
        javax.swing.JPanel jPanel3;
        javax.swing.JPanel jPanel4;
        javax.swing.JPanel panel;
        javax.swing.JSeparator jSeparator1;
        javax.swing.JSeparator jSeparator2;
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        currentNetworkIP = new javax.swing.JLabel();
        currentSubnetMask = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        currentIP = new javax.swing.JTextField();
        currentSubnet = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        panel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        newIPField = new javax.swing.JTextField();
        newSubnet = new javax.swing.JTextField();
        updateNetworkNameCheckbox = new javax.swing.JCheckBox();
        saveChangesBtn = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();

        setIconImage(Icons.Grassmarlin.get32());
        setTitle("Edit Subnet Mask");

        updateNetworkNameCheckbox.setSelected(true);

        currentSubnet.setEditable(false);
        currentIP.setEditable(false);
        newIPField.setEditable(false);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel1.setText("Edit Subnet Mask");

        currentNetworkIP.setText("Current Network IP");
        currentNetworkIP.setMaximumSize(new java.awt.Dimension(120, 20));
        currentNetworkIP.setMinimumSize(new java.awt.Dimension(120, 20));
        currentNetworkIP.setPreferredSize(new java.awt.Dimension(120, 20));

        currentSubnetMask.setText("Current Subnet Mask");
        currentSubnetMask.setMaximumSize(new java.awt.Dimension(120, 20));
        currentSubnetMask.setMinimumSize(new java.awt.Dimension(120, 20));
        currentSubnetMask.setPreferredSize(new java.awt.Dimension(120, 20));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(currentNetworkIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(currentSubnetMask, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(currentNetworkIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(currentSubnetMask, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        currentIP.setText("0");

        currentSubnet.setText("1");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(currentIP, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(currentSubnet, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE))
                        .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(currentIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(currentSubnet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel4.setText("New Network IP");
        jLabel4.setMaximumSize(new java.awt.Dimension(120, 20));
        jLabel4.setMinimumSize(new java.awt.Dimension(120, 20));
        jLabel4.setPreferredSize(new java.awt.Dimension(120, 20));

        jLabel5.setText("New Subnet Mask");
        jLabel5.setMaximumSize(new java.awt.Dimension(120, 20));
        jLabel5.setMinimumSize(new java.awt.Dimension(120, 20));
        jLabel5.setPreferredSize(new java.awt.Dimension(120, 20));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
                jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        newIPField.setText("0");

        newSubnet.setText("1");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(newIPField, javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(newSubnet, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE))
                        .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(newIPField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(newSubnet, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        updateNetworkNameCheckbox.setText("Update Network Name");

        saveChangesBtn.setText("Save Changes");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(jSeparator2)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(updateNetworkNameCheckbox)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(saveChangesBtn)))
                        .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addGap(7, 7, 7)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(11, 11, 11)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(updateNetworkNameCheckbox)
                                .addComponent(saveChangesBtn))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        add(panel);
        pack();
    }
    //</editor-fold>

}
