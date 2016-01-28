/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter.viewfilters;

import core.Pipeline;
import core.types.ByteTreeItem;
import core.types.VisualDetails;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import ui.dialog.DialogManager;
import ui.icon.Icons;
import ui.views.viewfilter.AbstractViewFilter;

import javax.swing.*;

/**
 *
 * @author BESTDOG 10/15/2015
 */
public class FingerprintViewFilter extends AbstractViewFilter {
    
    public static final String NAME = "Fingerprint";
    public static final String NO_FINGERPRINT = "N/A";
    
    boolean acceptMissing;
    List<String> fingerprintNames;
    
    public FingerprintViewFilter() {
        super();
        this.acceptMissing = true;
        this.fingerprintNames = new CopyOnWriteArrayList() {
            @Override
            public boolean addAll(Collection c) {
                acceptMissing = c.contains(NO_FINGERPRINT);
                return super.addAll(c);
            }
        };
    }

    @Override
    public boolean test(ByteTreeItem t) {
        boolean test;
        if( t.hasDetail() ) {
            Set<String> fingerprints = t.getDetails().getFingerprintNames();
            if( fingerprints.isEmpty() ) {
                test = this.acceptMissing;
            } else {
                test = Collections.disjoint(fingerprints, fingerprintNames);
            }
        } else {
            test = this.acceptMissing;
        }
        return test;
    }

    @Override
    public String getName() {
        return FingerprintViewFilter.NAME;
    }

    @Override
    public void update() {
        HashSet<String> set = new HashSet<>();
        getPipeline().streamTerminals()
                .map(ByteTreeItem::getDetails)
                .map(VisualDetails::getFingerprintNames)
                .forEach(set::addAll);
        set.add(NO_FINGERPRINT);
        DialogManager.showFilterChooser(getName(), new ArrayList(set), fingerprintNames);
    }

    @Override
    public boolean skip() {
        return fingerprintNames.isEmpty();
    }

    @Override
    public Icon getIcon() {
        return Icons.Fingerprint.getIcon();
    }
    
}
