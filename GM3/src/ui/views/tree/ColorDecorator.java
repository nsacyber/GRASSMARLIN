/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import ui.views.tree.visualnode.VisualNode;
import java.awt.Color;
import java.util.function.BiConsumer;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * @author BESTDOG
 * @param <T> The type of TreeCellRenderer to decorate.
 */
public class ColorDecorator<T extends DefaultTreeCellRenderer> implements TreeNodeDecorator<T> {

    private final Color SELECTED_BACKGROUND = new Color(0x000000);
    private final Color SELECTED_FOREGROUND = new Color(0xFFFFFF);
    private final Color DEFAULT_FOREGROUND = new Color(0x000000);
    private final Color DEFAULT_BACKGROUND = new Color(0xFFFFFF);
    private final double FADE_DURATION = 10000; // fade for 10 seconds
    
    private Color target;
    private Color current;
    long animationStartTime;
    
    BiConsumer<T,Long> animatorFunction;
    
    public ColorDecorator() {
        current = target = DEFAULT_BACKGROUND;
        animationStartTime = 0;
    }
    
    /**
     * starts this animator by fading from the startColor to the default color.
     * @param startColor Initial background color of this node, will fade to white.
     */
    public ColorDecorator(Color startColor) {
        this();
        this.fadeFrom(startColor);
    }
    
    @Override
    public void accept(T label, VisualNode node, boolean selected, Long time) {
        if( animationStartTime == 0 ) {
            animationStartTime = time;
        } else {
            if( selected ) {
                set(label, SELECTED_FOREGROUND, SELECTED_BACKGROUND);
            } else if(animating()) {
                animate(label, time);
            } else {
                setCurrentColor(DEFAULT_BACKGROUND);
                set(label, DEFAULT_FOREGROUND, getCurrentColor());
            }
        }
    }

    /**
     * Set the labels foreground and background.
     * @param label JLabel target apply color change target.
     * @param foreground new foreground color.
     * @param background new background color.
     */
    private void set(T label, Color foreground, Color background) {
        label.setBackgroundSelectionColor(background);
        label.setBackgroundNonSelectionColor(background);
        label.setForeground(foreground);
    }

    /**
     * Using the current color this node will fade target that color slowly.
     * @param to Color target fade target.
     * @return This reference is returned so that methods may be chained.
     */
    public ColorDecorator fadeTo(Color to) {
        this.target = to;
        this.setAnimatorFunction(this::fade);
        return this;
    }
    
    /**
     * Using the provided color, this node will fade target the current color slowly.
     * @param from Color target fade from.
     * @return This reference is returned so that methods may be chained.
     */
    public ColorDecorator fadeFrom(Color from) {
        this.target = getCurrentColor();
        this.setCurrentColor(from);
        this.setAnimatorFunction(this::fade);
        return this;
    }
    
    public boolean animating() {
        return animatorFunction != null;
    }

    private Color getCurrentColor() {
        return current;
    }

    private void setCurrentColor(Color color) {
        this.current = color;
    }

    public void setAnimatorFunction(BiConsumer<T, Long> animatorFunction) {
        this.animatorFunction = animatorFunction;
        this.animationStartTime = 0;
    }
    
    private void animate(T label, Long time) {
        if( current.equals(target) ) {
            this.animatorFunction = null;
        } else {
            BiConsumer<T,Long> function = this.animatorFunction;
            if( function != null ) {
                function.accept(label, time);
            }
        }
    }
    
    private void fade(T label, Long time) {
        Color f = current;
        Color t = target;
        double timeSpan = time - animationStartTime;
        double ratio = timeSpan / FADE_DURATION;
        
        int r = (int) Math.abs((ratio * t.getRed()) + (1 - ratio) * f.getRed());
        int g = (int) Math.abs((ratio * t.getGreen()) + (1 - ratio) * f.getGreen());
        int b = (int) Math.abs((ratio * t.getBlue()) + (1 - ratio) * f.getBlue());
        
        r = Math.min(255, r);
        g = Math.min(255, g);
        b = Math.min(255, b);
        r = Math.max(0, r);
        g = Math.max(0, g);
        b = Math.max(0, b);
        
        this.setCurrentColor(new Color( r, g, b ));
        set(label, DEFAULT_FOREGROUND, getCurrentColor());
        label.invalidate();
    }
    
    @Override
    public int hashCode() {
        return ColorDecorator.class.hashCode();
    }
    
}
