package grassmarlin.session.pipeline;


public interface IPacketMetadata extends IDeferredProgress {
    Long getFrame();    //Result is optional, so this is nullable.
    long getTime();
    short getTransportProtocol();
    int getTtl();
    int getEtherType();
    long getPacketSize();
}
