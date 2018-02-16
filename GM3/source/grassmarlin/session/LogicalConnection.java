package grassmarlin.session;

import grassmarlin.Event;

/**
 * A LogicalConnection is a bidirectional link between two LogicalVertex elements.
 * The endpoints must not evaluate as equivalent via a .compareTo between the underlying LogicalAddressMapping structures.
 * Because this edge is bidirectional, the endpoints as stored do not necessarily match the order provided to the constructor--but the distinction of a source and destination are retained for legacy and other purposes.
 */
public class LogicalConnection extends PropertyCloud {
    private final LogicalVertex source;
    private final LogicalVertex destination;

    public LogicalConnection(final Event.IAsyncExecutionProvider provider, final LogicalVertex source, final LogicalVertex destination) {
        super(provider);

        final int comparison = source.getLogicalAddressMapping().compareTo(destination.getLogicalAddressMapping());
        if(comparison == 0) {
            throw new IllegalArgumentException("Source matches destination");
        } else if(comparison < 0) {
            this.source = source;
            this.destination = destination;
        } else {
            this.source = destination;
            this.destination = source;
        }
    }

    public LogicalVertex getSource() {
        return this.source;
    }
    public LogicalVertex getDestination() {
        return this.destination;
    }

    @Override
    public int hashCode() {
        return source.hashCode() ^ destination.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof LogicalConnection)) {
            return false;
        } else {
            return this.source.equals(((LogicalConnection)other).source) && this.destination.equals(((LogicalConnection)other).destination);
        }
    }

    @Override
    public String toString() {
        return String.format("LogicalEdge ([%s] -> [%s])\n%s", source, destination, super.toString());
    }
}
