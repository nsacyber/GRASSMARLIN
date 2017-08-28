package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface IUdpPacketData extends IPacketData, IUdpPacketMetadata {
    ByteBuffer getUdpContents();
}
