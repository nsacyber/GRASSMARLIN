/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.awt.Image;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.render.LabelRenderer;
import prefuse.visual.VisualItem;

/**
 * BESTDOG - 9/23/2015 added network counts to the bottom of network nodes.
 * BESTDOG - 10/13/2015 Added fix for swapping images with {@link #setGetImageFunction(java.util.function.Function) }.
 */
class ImageSupportLabelRenderer extends LabelRenderer {

    Predicate<VisualItem> isNetworkFunction;
    Function<VisualItem,Image> getImageFunction;
    String ignoredEdge;
    String edgeField;
    
    ImageSupportLabelRenderer() {
        super();
        this.getImageFunction = this::defaultGetImage;
    }
    
    ImageSupportLabelRenderer(String textField, String iconField) {
        super(textField, iconField);
    }

    
    public void setIgnoredEdge(String ignoredEdge) {
        this.ignoredEdge = ignoredEdge;
    }

    public void setEdgeField(String edgeField) {
        this.edgeField = edgeField;
    }
    
    public ImageSupportLabelRenderer setGetImageFunction(Function<VisualItem,Image> getImageFunction) {
        this.getImageFunction = getImageFunction;
        return this;
    }
    
    public ImageSupportLabelRenderer setIsNetworkFunction(Predicate<VisualItem> isNetworkFunction) {
        this.isNetworkFunction = isNetworkFunction;
        return this;
    }

    @Override
    protected String getText(VisualItem item) {
        String text;
        if ( this.isNetworkFunction != null && this.isNetworkFunction.test(item)) {
            Node n = (Node) item.getSourceTuple();
            int degree = n.getDegree();
            Iterator it = n.edges();
            while( it.hasNext() ) {
                if( ((Tuple)it.next()).getString(edgeField).equals(ignoredEdge) ) {
                    --degree;
                }
            }
            degree -= 1; // don't count networks
            String valueText = String.format("\n %d %s", degree, degree == 1? "host" : "hosts");
            text = super.getText(item).concat(valueText);
        } else {
            /*
            // useful test to see if edges are doubling.
            text = super.getText(item).concat("\n").concat( Integer.toString(((Node)item).getDegree()) );
            */
            text = super.getText(item);
        }
        return text;
    }
    
    private Image defaultGetImage(VisualItem item) {
        Image image;
        if (item.canGet(m_imageName, Image.class)) {
            image = (Image) item.get(m_imageName);
        } else {
            image = super.getImage(item);
        }
        return image;
    }

    @Override
    protected Image getImage(VisualItem item) {
        return this.getImageFunction.apply(item);
    }
}
