package grassmarlin.session.pipeline;

public interface IUdpPacketMetadata extends IPacketMetadata {
    int getSourcePort();
    int getDestinationPort();
}
