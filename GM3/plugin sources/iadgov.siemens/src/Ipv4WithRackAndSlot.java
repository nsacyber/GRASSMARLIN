package iadgov.siemens;

import grassmarlin.session.LogicalAddress;
import grassmarlin.session.logicaladdresses.Ipv4;

//TODO: This should probably inherit from Ipv4WithPort.Ipv4WithTcpPort with a fixed port
public class Ipv4WithRackAndSlot extends Ipv4 {
    private final int rack;
    private final int slot;

    public Ipv4WithRackAndSlot(final Ipv4 baseAddress, final int rack, final int slot) {
        super(baseAddress.getBaseAddress());

        this.rack = rack;
        this.slot = slot;
    }

    @Override
    public boolean contains(final LogicalAddress other) {
        if(super.contains(other)) {
            final Ipv4WithRackAndSlot rhs = (Ipv4WithRackAndSlot)other;
            return (this.rack == rhs.rack) && (this.slot == rhs.slot);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode() ^ (this.rack << 16) ^ this.slot;
    }

    //Hack: These are given as 3 and 5 bits, but the S7 400 series allows 22 racks, 18 slots per rack, and 32 channels--this got a lot more complicated than previously understood.
    public int getRack() {
        return this.rack;
    }
    public int getSlot() {
        return this.slot;
    }

    //TODO: Serialization, etc.
}
