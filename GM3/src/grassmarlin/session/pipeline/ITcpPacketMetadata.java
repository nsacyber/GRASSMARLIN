package grassmarlin.session.pipeline;

public interface ITcpPacketMetadata extends IPacketMetadata {
    enum TcpFlags {
        NS(0x0100),
        CWR(0x0080),
        ECE(0x0040),
        URG(0x0020),
        ACK(0x0010),
        PSH(0x0008),
        RST(0x0004),
        SYN(0x0002),
        FIN(0x0001);

        TcpFlags(int value) {
            this.value = value;
        }

        private final int value;

        public int getValue() {
            return this.value;
        }
    }

    int getSourcePort();
    int getDestinationPort();

    long getAck();
    int getMss();
    long getSeqNum();
    int getWindowNum();
    boolean hasFlag(TcpFlags flag);
}
