package grassmarlin.session.pipeline;

public interface IIcmpPacketMetadata extends IPacketMetadata {
    int getIcmpType();
    int getIcmpCode();
}
