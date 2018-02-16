package grassmarlin.session.pipeline;

public interface ILogicalPacketMetadata extends IPacketMetadata {
    LogicalAddressMapping getSourceAddress();
    LogicalAddressMapping getDestAddress();
}
