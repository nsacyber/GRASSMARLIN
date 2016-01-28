/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.icon;

import core.Environment;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <pre>
 *
 * </pre>
 */
public enum Icons {
    Search,// 16 32
    Accept,// 16 32
    Delete,// 16 32
    Editform,// 16 32
    Manager,// 16 32
    Grassmarlin,// 16 32
    Broadcast,// 16 32
    EthernetPort,// 16 32
    Host,// 16 32
    House,// 16 32
    Network,// 16 32
    NetworkAdapter,// 16 32
    NetworkDevice,// 16 32
    NetworkDevice2,// 16 ?
    Server, // 16 32
    //Unknown, // 16 32
    Connection, // 16 32
    Fingerprint, // 16 32
    Fingerprint_add, // 16
    Form, // 16 32
    Error, // 16 32
    Report,
    Summary,
    Pdf,
    Computer, // 16 32 
    Computer_minus, // 16 32 
    Computer_plus, // 16 32 
    Firewall, // 16 32
    ICSHostOriginal, // 16 32
    Hub, // 16 32
    Filter, // 16 32
    ServerRack, // 16 32
    ControlPanel, // 16 32
    Original_address, // 16 32 64
    Original_alternate_network_interface, // 16 32 64
    Original_broadcast, // 16 32 64
    Original_connection, // 16 32 64
    Original_grassmarlin, // 16 32 64
    Original_host, // 16 32 64
    Original_ics_host, // 16 32 64
    Original_ics_host_broadcast, // 16 32 64
    Original_network, // 16 32 64
    Original_network_device, // 16 32 64
    Original_network_interface, // 16 32 64
    Original_other_host, // 16 32 64
    Original_port, // 16 32 64
    Original_server, // 16 32 64
    Original_service, // 16 32 64
    Original_unknown, // 16 32 64
    Original_upsidedown_port, // 16 32 64
    Original_cloud, // 128
    Grassmarlin_circle_lg,
    Info,
    Zoomin,
    Zoomout,
    Zoomfit,
    Refresh,
    Cog,
    Eye,
    Upload,
    Download,
    Save_image,
    Save_data,
    Save_document,
    Stop, // 16 32
    Start, // 16 32
    Cross,
    Cross_tiny,
    Cross_tiny_selected,
    Recycle,
    Document, // 16 32
    Flag_usa, // 16 32
    Categories, // 16 32
    Graph_edge, // 16 32
    Draw_line, // 16 32
    Draw_curve, // 16 32
    Legend, // 16 32
    Quality, // 16 32
    Autoscale, // 16 32
    Autoscale_off, // 16 32
    Folder // 16 32
    ;

    public static final Dimension Dim16x16 = new Dimension(16, 16);
    public static final String ORIGINAL_IDENT = "original";
    public static final String ORIGINAL_SEPERATOR = "_";
    public static final Map<String, Image> cache = Collections.synchronizedMap(new HashMap<>());
    public static Boolean runAsJar;

    public static Image get128(Icons icon) {
        String s = icon.name().toLowerCase();
        if (s.contains(ORIGINAL_IDENT + ORIGINAL_SEPERATOR)) {
            s = ORIGINAL_IDENT + File.separator + s;
        }
        return get("128" + File.separator + s + ".png");
    }

    public static Image get64(Icons icon) {
        String s = icon.name().toLowerCase();
        if (s.contains(ORIGINAL_IDENT + ORIGINAL_SEPERATOR)) {
            s = ORIGINAL_IDENT + File.separator + s;
        }
        return get("64" + File.separator + s + ".png");
    }

    public static Image get32(Icons icon) {
        String s = icon.name().toLowerCase();
        if (s.contains(ORIGINAL_IDENT + ORIGINAL_SEPERATOR)) {
            s = ORIGINAL_IDENT + File.separator + s;
        }
        return get("32" + File.separator + s + ".png");
    }

    public static Image get(Icons icon) {
        String s = icon.name().toLowerCase();
        if (s.contains(ORIGINAL_IDENT + ORIGINAL_SEPERATOR)) {
            s = ORIGINAL_IDENT + File.separator + s;
        }
        return get(s + ".png");
    }

    public static Image get(String path) {
        synchronized (cache) {
            try {
                if (!cache.containsKey(path)) {
                    File imageFile = Environment.DIR_IMAGES_ICON.getFile(path);
                    cache.put(path, ImageIO.read(imageFile.toURI().toURL()));
                }
            } catch (Exception ex) {
                Logger.getLogger(Icons.class.getName()).log(Level.SEVERE, "Failed to load internal resource \"{0}\".", path);
                Logger.getLogger(Icons.class.getName()).log(Level.SEVERE, path, ex);
                cache.put(path, new BufferedImage(16, 16, BufferedImage.TYPE_4BYTE_ABGR));
            }
        }
        return cache.get(path);
    }

    public ImageIcon getIcon() {
        return new ImageIcon(get());
    }

    public ImageIcon getIcon32() {
        return new ImageIcon(get32());
    }

    public Image get() {
        return Icons.get(this);
    }

    public Image get32() {
        return Icons.get32(this);
    }

    public Image get64() {
        return Icons.get64(this);
    }

    public Image get128() {
        return Icons.get128(this);
    }

    public JButton createButton() {
        JButton b = new JButton(new ImageIcon(get())) {
            @Override
            public void setAction(Action a) {
                Icon icon = this.getIcon();
                super.setAction(a);
                setIcon(icon);
            }
        };
        b.setPreferredSize(Dim16x16);
        b.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        return b;
    }

}