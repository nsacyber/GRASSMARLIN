/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.custom;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.function.Supplier;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import ui.icon.Icons;

/**
 * @author BESTDOG provides a preferred Image or ImageIcon based on whats
 * selected by user or set programmatic, preferring user selected over
 * programmatic selection.
 */
public class ProxyImage implements Supplier<Image> {

    Icons icon;
    /** 32x32 icon, mutable */
    ImageIcon imageIcon;
    /** 16x16 icon, mutable */
    ImageIcon scaledImageIcon;
    /** 32x32 image */
    Image flag;
    /** 16x16 image */
    Image scaledFlag;
    /** set when user chooses the icon, overriding the flag. */
    boolean userDefinedImage;
    /** name of the country for the flag set by {@link #setFlag(java.awt.Image, java.lang.String) */
    String country;

    public ProxyImage(Icons icon) {
        setIcon(icon);
        setFlag(icon.get(), null);
        updateIcon();
    }
    
    public ProxyImage() {
        this(Icons.Original_unknown);
        userDefinedImage = false;
        updateIcon();
    }

    /**
     * Preferring user set icons over the flag, returns the image for this Icon.
     * @return 32x32 Image.
     */
    public Image getDefault() {
        Image image;
        if (userDefinedImage) {
            image = icon.get();
        } else {
            image = flag;
        }
        return image;
    }

    @Override
    public Image get() {
        Image image;
        if (userDefinedImage) {
            image = icon.get32();
        } else {
            image = scaledFlag;
        }
        return image;
    }

    /**
     * @param flag Flag to use on this object when there are no specified icons.
     */
    public void setFlag(Image flag, String country) {
        this.flag = flag;
        this.scaledFlag = flag.getScaledInstance(16, 16, BufferedImage.SCALE_SMOOTH);
        this.country = country;
        updateIcon();
    }

    /**
     * Sets the preferred icon primarily used by this image.
     * @param icon Primary icon to use.
     */
    public void setIcon(Icons icon) {
        this.icon = icon;
        userDefinedImage = true;
        updateIcon();
    }

    /**
     * Created the ImageIcon for the preferred image.
     */
    private void updateIcon() {
        if( this.userDefinedImage ) {
            this.imageIcon = icon.getIcon32();
            this.scaledImageIcon = icon.getIcon();
        } else {
            this.imageIcon = new ImageIcon(flag);
            this.scaledImageIcon = new ImageIcon(scaledFlag);
        }
    }
    
    /**
     * Sets icons by the name of a {@link Icons} value.
     * @param iconName value of {@link Icons#valueOf(java.lang.String) }.
     */
    public void set(String iconName) {
        try {
            Icons newIcon = Icons.valueOf(iconName);
            setIcon(newIcon);
        } catch( java.lang.IllegalArgumentException ex ) {
            setIcon(Icons.Error);
        }
    }

    public Icon getIcon() {
        return this.imageIcon;
    }
    
    public Icon getScaledIcon() {
        return this.scaledImageIcon;
    }

    /** retrieves the last country name of the the flag image.
     * A resulting null value from this method indicates there is no flag set for this proxy image.
     * @return country name, else null.
     */
    public String getCountry() {
        return country;
    }
    
}
