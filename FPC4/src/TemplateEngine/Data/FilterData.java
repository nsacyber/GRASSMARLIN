package TemplateEngine.Data;

/**
 * Created by BESTDOG on 11/24/2015.
 *
 * FilterData contains the data that filter must check.
 *
 * This contain mostly UDP and TCP Ethernet header fields.
 *
 */
public interface FilterData {

    /**
     * @return Source Port.
     */
    int getSrc();

    /**
     * @return Destination Port.
     */
    int getDst();

    /**
     * @return Packets Ethertype.
     */
    int getEth();

    /**
     * @return The Time-To-Live for a IPv4 packet.
     */
    int getTtl();

    /**
     * @return IANA protocol number, either 6(TCP) or 7(UDP) while implementing JNETPCAP.
     */
    int getProto();

    /**
     * @return The size of the datagram.
     */
    int getDsize();

    /**
     * @return The TCP window field.
     */
    int getWindow();

    /**
     * @return The packet size in bytes.
     */
    int size();

    /**
     * @return The frame count of this traffic.
     */
    Long getFrame();

    /**
     * @return ACK flag string.
     */
    String getFlags();

    /**
     * @return The Long value of the ACK field of a TCP packet as a String since Long cannot be used in a switch.
     */
    String getAck();

    /**
     * @return The Long value of the SEQ field of a TCP packet as a String since Long cannot be used in a switch.
     */
    String getSeq();

}
