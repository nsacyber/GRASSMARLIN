/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter;

import core.Pipeline;
import java.util.stream.Stream;

/**
 *
 * @author BESTDOG - 10/15/15 - init
 */
public abstract class AbstractViewFilter implements ViewFilter {

    Pipeline pipeline;
    int failCount;
    
    public AbstractViewFilter() {
        this.resetFailCount();
        this.pipeline = null;
    }
    
    @Override
    public Stream stream() {
        resetFailCount();
        return getPipeline().streamTerminals().filter(this::test);
    }
    
    @Override
    public Pipeline getPipeline() {
        return this.pipeline;
    }

    @Override
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public int getFailCount() {
        return this.failCount;
    }

    @Override
    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

}
