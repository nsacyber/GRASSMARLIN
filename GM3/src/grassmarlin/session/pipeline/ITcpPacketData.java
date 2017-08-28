package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface ITcpPacketData extends IPacketData, ITcpPacketMetadata {
    ByteBuffer getTcpContents();
}
