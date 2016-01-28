/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter;

import core.Pipeline;
import core.types.ByteTreeItem;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author BESTDOG - 10/15/15 - init
 *
 * A ViewFilter consisting of other view filters each get tested for all items.
 * Only items that pass the first ViewFilter test will be tested against the
 * next filter. A ViewFilter will be skipped if its {@link ViewFilter#skip()}
 * method returns true.
 *
 */
public class ViewFilterList implements ViewFilter {

    final List<ViewFilter> filters;
    Pipeline pipeline;

    public ViewFilterList(Pipeline pipeline) {
        this.filters = new CopyOnWriteArrayList<>();
        this.pipeline = pipeline;
    }

    public ViewFilterList add(ViewFilter filter) {
        remove(filter.getName());
        filter.setPipeline(getPipeline());
        this.filters.add(filter);
        return this;
    }

    public void remove(ViewFilter filter) {
        filters.remove(filter);
    }

    private void remove(String name) {
        filters.removeIf(filter -> filter.getName().equals(name));
    }

    public ViewFilter get(String name) {
        return filters.stream().filter(filter -> filter.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void forEach(Consumer<ViewFilter> forEach) {
        this.filters.forEach(forEach::accept);
    }
    
    @Override
    public Stream<ByteTreeItem> stream() {
        return getPipeline().streamTerminals().peek(this::test);
    }

    @Override
    public Pipeline getPipeline() {
        return this.pipeline;
    }

    @Override
    public boolean test(ByteTreeItem t) {
        for (ViewFilter filter : filters) {
            if (filter.skip()) {
                continue;
            }
            t.setVisible(filter.test(t));
            if (!t.isVisible()) {
                break;
            }
        }
        return true;
    }

    @Override
    public int getFailCount() {
        return filters.stream().mapToInt(ViewFilter::getFailCount).sum();
    }

    @Override
    public void setFailCount(int failCount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getName() {
        return this.toString();
    }

    @Override
    public void update() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public boolean skip() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Icon getIcon() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
