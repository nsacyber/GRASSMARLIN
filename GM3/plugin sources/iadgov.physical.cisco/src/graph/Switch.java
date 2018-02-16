package iadgov.physical.cisco.graph;

import grassmarlin.plugins.internal.physicalview.graph.ISwitch;

public class Switch implements ISwitch {
    private final String name;

    public Switch(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
}
