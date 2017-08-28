package grassmarlin.plugins.internal.physicalview.graph;

/*
 This could be an enum, but that is less extensible.
 */
public abstract class Groupings {
    public final static Switch.IPortGroupMapper Grouping6x2 = new Switch.IPortGroupMapper() {
        @Override
        public Switch.PortVisualSettings settingsFor(final int index) {
            return new Switch.PortVisualSettings(((index - 1) / 2), ((index - 1) % 2), (index % 2) == 0);
        }
    };

    public final static Switch.IPortGroupMapper GroupingLow = new Switch.IPortGroupMapper() {
        @Override
        public Switch.PortVisualSettings settingsFor(final int index) {
            return new Switch.PortVisualSettings(index, 1, false);
        }
    };
}
