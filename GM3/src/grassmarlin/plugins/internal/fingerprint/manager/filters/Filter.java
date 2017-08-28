package grassmarlin.plugins.internal.fingerprint.manager.filters;


import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.HBox;

import javax.xml.bind.JAXBElement;

/**
 *
 * @param <T> The Type of the JAXBElement created by this Filter
 */
public interface Filter<T> {

    default HBox getInput(){
        return new HBox();
    }

    FilterType getType();

    ObjectProperty<JAXBElement<T>> elementProperty();

    enum PacketType {
        TCP,
        UDP,
        ANY,
        OTHER
    }

    enum FilterType {
        ACK("Ack", "Accepts TCP packets with the ACK flag set. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                PacketType.TCP, AckFilter.class),
        MSS("MSS", "Accepts a MSS value for TCP packets with the MSS optional flag set. \n" +
                "This will force the TransportProtocol to only check TCP packets. ",
                PacketType.TCP, MssFilter.class),
        DSIZE("Dsize", "Accepts a packet with a payload size equal to a number of bytes. \n" +
                "This does not include the size of a packet header. ",
                PacketType.ANY, DsizeFilter.class),
        DSIZEWITHIN("DsizeWithin", "Accepts a packet with a payload size within a range of bytes(s). \n" +
                "This does not include the size of a packet header.",
                PacketType.ANY, DsizeWithinFilter.class),
        DSTPORT("DstPort", "Accepts a TCP or UDP packet with the specified destination port. ",
                PacketType.ANY, DestPortFilter.class),
        DSTPORTRANGE("DstPortRange", "Accepts a packet with the destination port within the specified range",
                PacketType.ANY, DestPortRangeFilter.class),
        SRCPORT("SrcPort", "Accepts the source port of a TCP or UDP packet. ",
                PacketType.ANY, SourcePortFilter.class),
        SRCPORTRANGE("SrcPortRange", "Accepts a packet with the source port within the specified range",
                PacketType.ANY, SourcePortRangeFilter.class),
        ETHERTYPE("Ethertype", "Accepts a packet with the specified ethertype. [2048] = IPv4",
                PacketType.OTHER, EthertypeFilter.class),
        FLAGS("Flags", "This Filter will check for the presence of TCP flags. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                PacketType.TCP, FlagsFilter.class),
        SEQ("Seq", "Accepts TCP packets which contain a SEQ field equal to the indicated value. \n" +
                "This will force the TransportProtocol to only check TCP packets. ",
                PacketType.TCP, SeqFilter.class),
        TRANSPORTPROTOCOL("TransportProtocol", "Accepts a packet with the specified Internet Protocol Number. \n" +
                "GM only supports IPv4, UDP and TCP protocols. It is not suggested to use values other than TCP(6) and UDP(17). ",
                PacketType.OTHER, TransportProtoFilter.class),
        TTL("TTL", "Accepts TCP packets which contain a TTL(Time To Live) field equal to a value",
                PacketType.ANY, TtlFilter.class),
        TTLWITHIN("TTLWithin", "Accepts TCP packets which contain a TTL(Time To Live) field within a range of value(s)",
                PacketType.ANY, TtlWithinFilter.class),
        WINDOW("Window", "Accepts a TCP packet which contains a Window Size field equal to the indicated value. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                PacketType.TCP, WindowFilter.class);

        private String name;
        private String tooltip;
        private Filter.PacketType packetType;
        Class<? extends Filter> implementingClass;

        FilterType(String name, String tooltip, Filter.PacketType packetType, Class<? extends Filter> implementingClass) {
            this.name = name;
            this.tooltip = tooltip;
            this.packetType = packetType;
            this.implementingClass = implementingClass;
        }

        public String getName() {
            return this.name;
        }

        public String getTooltip() {
            return this.tooltip;
        }

        public Filter.PacketType getPacketType() {
            return this.packetType;
        }

        public Class<? extends Filter> getImplementingClass() {
            return this.implementingClass;
        }

    }
}
