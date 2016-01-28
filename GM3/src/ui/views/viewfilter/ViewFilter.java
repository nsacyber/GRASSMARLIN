/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter;

import core.Pipeline;
import core.types.ByteTreeItem;

import javax.swing.*;
import java.util.stream.Stream;

/**
 *
 * View filters reduce the amount of data tuples that leave the pipeline and reach the view.
 * 
 * @author BESTDOG - 10/15/2015 - init view filter
 * @param <T> The object to filter
 */
public interface ViewFilter<T extends ByteTreeItem> {
    /**
     * @return Pipeline this ViewFilter is filtering.
     */
    Pipeline getPipeline();
    /**
     * Sets the source of data to filter.
     * @param pipeline The new pipeline to use.
     */
    void setPipeline(Pipeline pipeline);
    /**
     * Tests a single item;
     * @param t Item to test.
     * @return True if the item passes the filter.
     */
    boolean test(T t);
    /**
     * @return Stream of all items from this filter's source which meet its criteria.
     */
    Stream<T> stream();
    /**
     * @return Gets the count of items which did not meet this filter's criteria.
     */
    int getFailCount();
    /**
     * @param failCount Sets the fail count.
     */
    void setFailCount(int failCount);
    /**
     * @return Name used to identify this filter.
     */
    String getName();
    /**
     * Should launch a window or other procedure to update this Filter's criteria.
     */
    void update();
    /**
     * @return True if this filter should be checked at all. Usually a filter is skipped when its criteria are empty.
     */
    boolean skip();
    /**
     * Increase fail count by one.
     */
    default void increaseFailCount() {
        setFailCount(getFailCount()+1);
    }
    /**
     * Resets fail count to zero;
     */
    default void resetFailCount() {
        setFailCount(0);
    }

    Icon getIcon();
}
