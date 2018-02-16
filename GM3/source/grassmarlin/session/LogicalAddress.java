package grassmarlin.session;

import grassmarlin.session.serialization.XmlSerializable;

import java.io.Serializable;

public abstract class LogicalAddress implements XmlSerializable, Comparable<LogicalAddress>, Serializable {
    protected LogicalAddress() {
    }

    public boolean contains(final LogicalAddress other) {
        return this.getClass().isAssignableFrom(other.getClass());
    }

    @Override
    public int compareTo(final LogicalAddress other) {
        if(other == null) {
            return 1;
        }
        if(this.contains(other) && other.contains(this)) {
            return 0;
        }
        if(this.contains(other)) {
            return 1;
        }
        if(other.contains(this)) {
            return -1;
        }
        int classComparison = this.getClass().hashCode() - other.getClass().hashCode();
        if(classComparison == 0) {
            return this.hashCode() - other.hashCode();
        } else {
            return classComparison;
        }
    }

    @Override
    public boolean equals(final Object other) {
        if(other == null) {
            return false;
        }

        if(this.getClass() == other.getClass()) {
            return this.contains((LogicalAddress)other) && ((LogicalAddress)other).contains(this);
        } else {
            return false;
        }
    }

    @Override
    public abstract String toString();
}
