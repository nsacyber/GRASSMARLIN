package iadgov.fingerprint.manager.filters;


import core.fingerprint3.Fingerprint;
import grassmarlin.session.pipeline.ILogicalPacketMetadata;
import grassmarlin.session.pipeline.ITcpPacketMetadata;
import grassmarlin.session.pipeline.IUdpPacketMetadata;
import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.HBox;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

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
                PacketType.TCP, AckFilter.class, (element) -> Collections.singletonList(((BigInteger) element.getValue()).intValue()),
                packet -> {
                    if (packet instanceof ITcpPacketMetadata) {
                        int retVal = (int) ((ITcpPacketMetadata) packet).getAck();
                        return retVal;
                    } else {
                        return -1;
                    }
                }),
        MSS("MSS", "Accepts a MSS value for TCP packets with the MSS optional flag set. \n" +
                "This will force the TransportProtocol to only check TCP packets. ",
                PacketType.TCP, MssFilter.class, (element) -> Collections.singletonList(((BigInteger) element.getValue()).intValue()),
                (packet) -> {
                    if (packet instanceof ITcpPacketMetadata) {
                        return ((ITcpPacketMetadata)packet).getMss();
                    } else {
                        return -1;
                    }
                }),
        DSIZE("Dsize", "Accepts a packet with a payload size equal to a number of bytes. \n" +
                "This does not include the size of a packet header. ",
                PacketType.ANY, DsizeFilter.class, (element) -> Collections.singletonList(((BigInteger) element.getValue()).intValue()),
                (packet) -> Integer.parseInt(Long.toString(packet.getPacketSize()))),
        DSIZEWITHIN("DsizeWithin", "Accepts a packet with a payload size within a range of bytes. \n" +
                "This does not include the size of a packet header.",
                PacketType.ANY, DsizeWithinFilter.class, (element) -> {
            Fingerprint.Filter.DsizeWithin within = ((Fingerprint.Filter.DsizeWithin) element.getValue());
            int min = within.getMin().intValue();
            int max = within.getMax().intValue();
            Integer[] nums = new Integer[max - min + 1];
            Arrays.setAll(nums, index -> index + min);

            return Arrays.asList(nums);
        }, (packet) -> Integer.parseInt(Long.toString(packet.getPacketSize()))),
        DSTPORT("DstPort", "Accepts a TCP or UDP packet with the specified destination port. ",
                PacketType.ANY, DestPortFilter.class, element -> Collections.singletonList((Integer) element.getValue()),
                (packet) -> {
                    if (packet instanceof ITcpPacketMetadata) {
                        return ((ITcpPacketMetadata) packet).getDestinationPort();
                    } else if (packet instanceof IUdpPacketMetadata) {
                        return ((IUdpPacketMetadata) packet).getDestinationPort();
                    } else {
                        return -1;
                    }
                }),
        DSTPORTRANGE("DstPortRange", "Accepts a packet with the destination port within the specified range",
                PacketType.ANY, DestPortRangeFilter.class, element -> {
            Fingerprint.Filter.DstPortRange range = ((Fingerprint.Filter.DstPortRange) element.getValue());
            int min = range.getMin().intValue();
            int max = range.getMax().intValue();
            Integer[] nums = new Integer[max - min + 1];
            Arrays.setAll(nums, index -> index + min);

            return Arrays.asList(nums);
        }, (packet) -> {
            if (packet instanceof ITcpPacketMetadata) {
                return ((ITcpPacketMetadata) packet).getDestinationPort();
            } else if (packet instanceof IUdpPacketMetadata) {
                return ((IUdpPacketMetadata) packet).getDestinationPort();
            } else {
                return -1;
            }
        }),
        SRCPORT("SrcPort", "Accepts the source port of a TCP or UDP packet. ",
                PacketType.ANY, SourcePortFilter.class, element -> Collections.singletonList((Integer) element.getValue()),
                (packet) -> {
                    if (packet instanceof ITcpPacketMetadata) {
                        return ((ITcpPacketMetadata) packet).getSourcePort();
                    } else if (packet instanceof IUdpPacketMetadata) {
                        return ((IUdpPacketMetadata) packet).getSourcePort();
                    } else {
                        return -1;
                    }
                }),
        SRCPORTRANGE("SrcPortRange", "Accepts a packet with the source port within the specified range",
                PacketType.ANY, SourcePortRangeFilter.class, element -> {
            Fingerprint.Filter.SrcPortRange range = ((Fingerprint.Filter.SrcPortRange) element.getValue());
            int min = range.getMin().intValue();
            int max = range.getMax().intValue();
            Integer[] nums = new Integer[max - min + 1];
            Arrays.setAll(nums, index -> index + min);

            return Arrays.asList(nums);
        }, (packet) -> {
            if (packet instanceof ITcpPacketMetadata) {
                return ((ITcpPacketMetadata) packet).getSourcePort();
            } else if (packet instanceof IUdpPacketMetadata) {
                return ((IUdpPacketMetadata) packet).getSourcePort();
            } else {
                return -1;
            }
        }),
        ETHERTYPE("Ethertype", "Accepts a packet with the specified ethertype. [2048] = IPv4",
                PacketType.OTHER, EthertypeFilter.class, element -> Collections.singletonList((Integer) element.getValue()),
                packet -> packet.getEtherType()),
        FLAGS("Flags", "This Filter will check for the presence of TCP flags. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                PacketType.TCP, FlagsFilter.class, element -> {
            String[] flags = ((String) element.getValue()).split(" ");
            Integer flagMask = 0;
            for (String flagName : flags) {
                flagMask = flagMask | ITcpPacketMetadata.TcpFlags.valueOf(flagName).getValue();
            }

            return Collections.singletonList(flagMask);
        }, packet -> {
            if (packet instanceof ITcpPacketMetadata) {
                return ((ITcpPacketMetadata) packet).getFlags();
            } else {
                return -1;
            }
        }),
        SEQ("Seq", "Accepts TCP packets with the indicated sequence number. \n" +
                "This will force the TransportProtocol to only check TCP packets. ",
                PacketType.TCP, SeqFilter.class, element -> Collections.singletonList(((BigInteger)element.getValue()).intValue()),
                packet -> {
                    if (packet instanceof ITcpPacketMetadata) {
                        int retVal = (int)((ITcpPacketMetadata) packet).getSeqNum();
                        return retVal;
                    } else {
                        return -1;
                    }
                }),
        TRANSPORTPROTOCOL("TransportProtocol", "Accepts a packet with the specified Internet Protocol Number. \n" +
                "GM only supports IPv4, UDP and TCP protocols. It is not suggested to use values other than TCP(6) and UDP(17). ",
                PacketType.OTHER, TransportProtoFilter.class, element -> Collections.singletonList(((Short)element.getValue()).intValue()),
                packet -> {
                    int retVal = packet.getTransportProtocol();
                    return retVal;
                }),
        TTL("TTL", "Accepts TCP packets which contain a TTL(Time To Live) field equal to a value",
                PacketType.ANY, TtlFilter.class, element -> Collections.singletonList(((BigInteger) element.getValue()).intValue()),
                packet -> packet.getTtl()),
        TTLWITHIN("TTLWithin", "Accepts TCP packets which contain a TTL(Time To Live) field within a range of value(s)",
                PacketType.ANY, TtlWithinFilter.class, element -> {
            Fingerprint.Filter.TTLWithin within = (Fingerprint.Filter.TTLWithin) element.getValue();
            int min = within.getMin().intValue();
            int max = within.getMax().intValue();
            Integer[] nums = new Integer[max - min + 1];
            Arrays.setAll(nums, index -> index + min);

            return Arrays.asList(nums);
        }, packet -> packet.getTtl()),
        WINDOW("Window", "Accepts a TCP packet which contains a Window Size field equal to the indicated value. \n" +
                "This will force the TransportProtocol to only check TCP packets.",
                PacketType.TCP, WindowFilter.class, element -> Collections.singletonList(((BigInteger) element.getValue()).intValue()),
                packet -> {
                    if (packet instanceof ITcpPacketMetadata) {
                        return ((ITcpPacketMetadata) packet).getWindowNum();
                    } else {
                        return -1;
                    }
                });

        private String name;
        private String tooltip;
        private Filter.PacketType packetType;
        Class<? extends Filter> implementingClass;
        Function<JAXBElement<? extends Serializable>, List<Integer>> parseValue;
        Function<ILogicalPacketMetadata, Integer> extractValue;

        FilterType(String name, String tooltip, Filter.PacketType packetType, Class<? extends Filter> implementingClass,
                   Function<JAXBElement<? extends Serializable>, List<Integer>> parseValueFunction,
                   Function<ILogicalPacketMetadata, Integer> extractValueFunction) {
            this.name = name;
            this.tooltip = tooltip;
            this.packetType = packetType;
            this.implementingClass = implementingClass;
            this.parseValue = parseValueFunction;
            this.extractValue = extractValueFunction;
        }

        public static FilterType getType(JAXBElement<?> element) {
            return FilterType.valueOf(element.getName().toString().replaceAll(" ", "").toUpperCase());
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

        public int getPacketValue(ILogicalPacketMetadata packet) {
            return this.extractValue.apply(packet);
        }

        public static List<Integer> getValue(JAXBElement<? extends Serializable> element) {
            FilterType type = FilterType.valueOf(element.getName().toString().replaceAll(" ","").toUpperCase());
            if (type != null) {
                return type.parseValue.apply(element);
            } else {
                return Collections.emptyList();
            }
        }
    }
}
