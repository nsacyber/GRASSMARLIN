/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter.viewfilters;

import core.types.ByteTreeItem;
import core.types.ByteTreeNode;
import core.types.VisualDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import ui.dialog.DialogManager;
import ui.icon.Icons;
import ui.views.viewfilter.AbstractViewFilter;

import javax.swing.*;

/**
 * This filter uses the criteria that a item is accepted when it is GEOIP
 * located within given country or countries selected by the user.
 *
 * @author BESTDOG
 */
public class CountryViewFilter extends AbstractViewFilter {

    public static final String NAME = "Country";
    public static final String NO_COUNTRY = "N/A";

    boolean acceptMissing;
    List<String> countryNames;

    public CountryViewFilter() {
        super();
        this.countryNames = new CopyOnWriteArrayList() {
            @Override
            public boolean addAll(Collection c) {
                acceptMissing = c.contains(NO_COUNTRY);
                return super.addAll(c);
            }
        };
        this.acceptMissing = false;
    }

    @Override
    public boolean test(ByteTreeItem t) {
        boolean test;
        if (t.hasDetail()) {
            String country = t.getDetails().getCountry();
            if (country == null) {
                test = this.acceptMissing;
            } else {
                test = countryNames.stream().anyMatch(country::equals);
            }
        } else {
            test = this.acceptMissing;
        }
        return test;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void update() {
        HashSet<String> possibleNames = new HashSet<>(countryNames);
        getPipeline().streamTerminals()
                .filter(ByteTreeNode::hasDetail)
                .map(ByteTreeNode::getDetails)
                .map(VisualDetails::getCountry)
                .forEach(possibleNames::add);

        possibleNames.remove(null);
        possibleNames.add(NO_COUNTRY);
        DialogManager.showFilterChooser(getName(), new ArrayList(possibleNames), countryNames);
    }

    @Override
    public boolean skip() {
        return countryNames.isEmpty();
    }

    @Override
    public Icon getIcon() {
        return Icons.Flag_usa.getIcon();
    }

}
