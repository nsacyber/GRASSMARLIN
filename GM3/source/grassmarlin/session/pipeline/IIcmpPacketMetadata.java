package grassmarlin.session.pipeline;

public interface IIcmpPacketMetadata extends ILogicalPacketMetadata {
    int getIcmpType();
    int getIcmpCode();
}
