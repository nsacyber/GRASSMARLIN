package grassmarlin.session;

import grassmarlin.Event;

public class Edge<TAddress extends IAddress> extends PropertyCloud {
    private final TAddress source;
    private final TAddress destination;

    public Edge(final Event.IAsyncExecutionProvider provider, final TAddress source, final TAddress destination) {
        super(provider);

        this.source = source;
        this.destination = destination;
    }

    public TAddress getSource() {
        return this.source;
    }
    public TAddress getDestination() {
        return this.destination;
    }

    @Override
    public int hashCode() {
        return source.hashCode() ^ destination.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof Edge<?>)) {
            return false;
        } else {
            return this.source.equals(((Edge<?>)other).source) && this.destination.equals(((Edge<?>)other).destination);
        }
    }

    @Override
    public String toString() {
        return String.format("Edge ([%s] -> [%s])\n%s", source, destination, super.toString());
    }
}
