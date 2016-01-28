/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.tree;

import ui.views.tree.visualnode.VisualNode;
import core.types.VisualDetails;
import java.util.function.BiConsumer;
import javax.swing.Icon;
import javax.swing.tree.DefaultTreeCellRenderer;
import ui.icon.Icons;


public class LabelDecorator<T extends DefaultTreeCellRenderer> implements TreeNodeDecorator<T> {
    
    private String text;
    private Icon icon;
    private final BiConsumer<T,VisualNode> setLabelFunction;
    
    public LabelDecorator() {
        this.setLabelFunction = this::updateBoth;
    }
    
    public LabelDecorator(String text, Icons icon) {
        this.setLabelFunction = this::neverChangeLabel;
        this.text = text;
        this.icon = icon.getIcon();
    }
    
    public LabelDecorator(Icons icon) {
        this.setLabelFunction = this::updateTextOnly;
        this.icon = icon.getIcon();
    }
    
    private void updateTextOnly(T label, VisualNode node) {
        label.setText(node.getText());
        label.setIcon(icon);
    }
    
    private void updateBoth(T label, VisualNode node) {
        label.setText(node.getText());
        label.setIcon(node.getIcon());
    }
    
    /**
     * Sets a never changing text and icon, used for root node.
     * @param label Label to setText and setIcon.
     * @param node Ignored.
     */
    private void neverChangeLabel(T label, VisualNode node) {
        label.setText(text);
        label.setIcon(icon);
    }
    
    @Override
    public void accept(T label, VisualNode node, boolean selected, Long time) {
        setLabelFunction.accept(label, node);
    }
    
    @Override
    public int hashCode() {
        return LabelDecorator.class.hashCode();
    }
    
}
