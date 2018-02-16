package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface ITcpPacketData extends ILogicalPacketData, ITcpPacketMetadata {
    ByteBuffer getTcpContents();
}
