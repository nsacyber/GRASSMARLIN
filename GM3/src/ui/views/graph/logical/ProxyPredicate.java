/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.util.function.Predicate;
import prefuse.data.Tuple;
import prefuse.visual.expression.InGroupPredicate;

/**
 *
 * @author BESTDOG
 */
public class ProxyPredicate extends InGroupPredicate {

    Predicate predicate;
    
    public ProxyPredicate(Predicate<Tuple> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean getBoolean(Tuple t) {
        return predicate.test(t);
    }
    
}
