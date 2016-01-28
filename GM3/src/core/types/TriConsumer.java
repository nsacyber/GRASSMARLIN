/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.types;

/**
 *
 */
@FunctionalInterface
public interface TriConsumer<A, B, C> {
    public void accept(A a, B b, C c);
}
