package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface IIcmpPacketData extends IPacketData, IIcmpPacketMetadata {
    ByteBuffer getIcmpContents();
}
