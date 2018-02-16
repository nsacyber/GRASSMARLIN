package grassmarlin.session.pipeline;

import grassmarlin.session.HardwareAddress;

public interface IHardwarePacketMetadata extends IPacketMetadata {
    HardwareAddress getSourceAddress();
    HardwareAddress getDestAddress();
}
