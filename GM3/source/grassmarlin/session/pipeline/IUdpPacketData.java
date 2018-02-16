package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface IUdpPacketData extends ILogicalPacketData, IUdpPacketMetadata {
    ByteBuffer getUdpContents();
}
