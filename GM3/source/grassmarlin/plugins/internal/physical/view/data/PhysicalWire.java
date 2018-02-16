package grassmarlin.plugins.internal.physical.view.data;

public class PhysicalWire {
    private final PhysicalEndpoint source;
    private final PhysicalEndpoint destination;

    public PhysicalWire(final PhysicalEndpoint source, final PhysicalEndpoint destination) {
        this.source = source;
        this.destination = destination;
    }

    public PhysicalEndpoint getSource() {
        return this.source;
    }
    public PhysicalEndpoint getDestination() {
        return this.destination;
    }

    @Override
    public int hashCode() {
        return this.source.hashCode() ^ this.destination.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if(other != null && other instanceof PhysicalWire) {
            final PhysicalWire o = (PhysicalWire)other;
            return (this.source.equals(o.source) && this.destination.equals(o.destination)) || (this.source.equals(o.destination) && this.destination.equals(o.source));
        } else {
            return false;
        }
    }
}
