package grassmarlin.session;

import grassmarlin.Event;

public class PhysicalConnection extends PropertyCloud {
    private final HardwareVertex source;
    private final HardwareVertex destination;

    public PhysicalConnection(final Event.IAsyncExecutionProvider provider, final HardwareVertex source, final HardwareVertex destination) {
        super(provider);

        this.source = source;
        this.destination = destination;
    }

    public HardwareVertex getSource() {
        return this.source;
    }
    public HardwareVertex getDestination() {
        return this.destination;
    }

    public HardwareVertex other(final HardwareVertex endpoint) {
        return this.source == endpoint ? this.destination : this.source;
    }

    @Override
    public int hashCode() {
        return source.hashCode() ^ destination.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof PhysicalConnection)) {
            return false;
        } else {
            return this.source.equals(((PhysicalConnection)other).source) && this.destination.equals(((PhysicalConnection)other).destination);
        }
    }

    @Override
    public String toString() {
        return String.format("[PhysicalConnection ([%s] -> [%s])]", source.getAddress(), destination.getAddress());
    }
}
