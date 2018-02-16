package grassmarlin.session.pipeline;

import java.nio.ByteBuffer;

public interface IIcmpPacketData extends ILogicalPacketData, IIcmpPacketMetadata {
    ByteBuffer getIcmpContents();
}
