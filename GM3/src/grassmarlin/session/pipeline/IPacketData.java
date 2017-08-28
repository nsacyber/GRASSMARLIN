package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface IPacketData extends IPacketMetadata {
    ByteBuffer getContents();
}
