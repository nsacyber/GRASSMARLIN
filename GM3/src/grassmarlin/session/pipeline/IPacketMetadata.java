package grassmarlin.session.pipeline;

import grassmarlin.session.IAddress;

public interface IPacketMetadata extends IDeferredProgress {
    Long getFrame();    //Result is optional, so this is nullable.
    long getTime();
    short getTransportProtocol();
    IAddress getSourceAddress();
    IAddress getDestAddress();
    int getTtl();
    int getEtherType();
    long getPacketSize();
}
