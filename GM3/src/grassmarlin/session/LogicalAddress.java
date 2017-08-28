package grassmarlin.session;

import grassmarlin.session.serialization.XmlSerializable;

import java.io.Serializable;

public abstract class LogicalAddress<T extends Serializable> implements XmlSerializable, Comparable<LogicalAddress<?>>, Serializable {
    private final T address;

    protected LogicalAddress(final T address) {
        this.address = address;
    }

    public boolean contains(final LogicalAddress<?> child) {
        return this.equals(child);
    }

    public final T getRawAddress() {
        return address;
    }

    @Override
    public int compareTo(final LogicalAddress<?> other) {
        if(other.address.getClass().equals(this.address.getClass())) {
            if(this.address instanceof Comparable) {
                return ((Comparable) this.address).compareTo(other.address);
            } else {
                return this.address.hashCode() - other.address.hashCode();
            }
        } else {
            return this.hashCode() - other.hashCode();
        }
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other == null) {
            return false;
        }

        if(this.getClass().equals(other.getClass())) {
            return this.address.equals(((LogicalAddress<?>)other).address);
        } else {
            return false;
        }
    }

    @Override
    public abstract String toString();
}
