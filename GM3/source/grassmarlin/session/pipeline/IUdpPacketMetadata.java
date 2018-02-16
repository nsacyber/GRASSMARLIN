package grassmarlin.session.pipeline;

public interface IUdpPacketMetadata extends ILogicalPacketMetadata {
    int getSourcePort();
    int getDestinationPort();
}
