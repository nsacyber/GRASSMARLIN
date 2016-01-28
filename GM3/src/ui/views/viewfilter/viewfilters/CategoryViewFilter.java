/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter.viewfilters;

import ICSDefines.Category;
import core.types.ByteTreeItem;
import core.types.VisualDetails;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ui.dialog.DialogManager;
import ui.icon.Icons;
import ui.views.viewfilter.AbstractViewFilter;

import javax.swing.*;

/**
 *
 * @author BESTDOG - 10/15/15 - init
 */
public class CategoryViewFilter extends AbstractViewFilter {

    public static final String NAME = "Category";
    List<Category> categories;
    
    public CategoryViewFilter() {
        categories = new CopyOnWriteArrayList<>();
    }
    
    @Override
    public boolean test(ByteTreeItem t) {
        boolean test;
        if( t.hasDetail() ) {
            test = this.categories.contains(t.getDetails().getCategory());
        } else {
            test = false;
        }
        return test;
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void update() {
        HashSet<Category> set = new HashSet<>();
        getPipeline().streamTerminals()
                .map(ByteTreeItem::getDetails)
                .map(VisualDetails::getCategory)
                .forEach(set::add);
        DialogManager.showFilterChooser(getName(), new ArrayList(set), categories);
    }

    @Override
    public boolean skip() {
        return this.categories.isEmpty();
    }

    @Override
    public Icon getIcon() {
        return Icons.Categories.getIcon();
    }
    
}
