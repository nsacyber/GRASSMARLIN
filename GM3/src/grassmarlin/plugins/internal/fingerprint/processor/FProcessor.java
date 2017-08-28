package grassmarlin.plugins.internal.fingerprint.processor;

import core.fingerprint3.*;
import grassmarlin.plugins.internal.fingerprint.FingerprintEdgeProperties;
import grassmarlin.plugins.internal.fingerprint.FingerprintProperties;
import grassmarlin.plugins.internal.fingerprint.manager.filters.Filter;
import grassmarlin.plugins.internal.fingerprint.manager.payload.Endian;
import grassmarlin.plugins.internal.fingerprint.manager.payload.Test;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.*;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the class that will process fingerprints
 */
public class FProcessor {

    private static class UnpackedFilter<T> {
        private Filter.FilterType type;
        private T value;

        public UnpackedFilter (Filter.FilterType type, T value) {
            this.type = type;
            this.value = value;
        }

        public Filter.FilterType getType() {
            return this.type;
        }

        public T getValue() {
            return this.value;
        }
    }

    private static class UnpackedFilterGroup {
        private String payloadName;
        private List<UnpackedFilter<?>> filters;

        public UnpackedFilterGroup(String payloadName, List<UnpackedFilter<?>> filters) {
            this.payloadName = payloadName;

            this.filters = filters;
        }

        public String getFor() {
            return this.payloadName;
        }

        public List<UnpackedFilter<?>> getFilters() {
            return this.filters;
        }
    }

    List<Fingerprint> fingerprints;
    Map<Fingerprint, Map<String, List<UnpackedFilterGroup>>> filtersByPayload;

    public FProcessor(List<Fingerprint> runningFingerprints) {
        this.fingerprints = Collections.unmodifiableList(new ArrayList<>(runningFingerprints));
        this.filtersByPayload = unpackFilters(this.fingerprints);
    }

    private synchronized static Map<Fingerprint, Map<String, List<UnpackedFilterGroup>>> unpackFilters(List<Fingerprint> fingerprints) {
        Map<Fingerprint, Map<String, List<UnpackedFilterGroup>>> returnMap = new HashMap<>();
        for (Fingerprint fp : fingerprints) {
            Map<String, List<UnpackedFilterGroup>> groupByPayload = fp.getFilter().stream()
                    .map(group -> {
                        List<UnpackedFilter<?>> filters = group.getAckAndMSSAndDsize().stream()
                                .map(element -> {
                                    UnpackedFilter<?> filter = null;
                                    switch (Filter.FilterType.valueOf(element.getName().toString().replaceAll(" ", "").toUpperCase())) {
                                        case ACK:
                                            filter = new UnpackedFilter<>(Filter.FilterType.ACK, (Long) element.getValue());
                                            break;
                                        case DSIZE:
                                            filter = new UnpackedFilter<>(Filter.FilterType.DSIZE, (Integer)element.getValue());
                                            break;
                                        case DSIZEWITHIN:
                                            filter = new UnpackedFilter<>(Filter.FilterType.DSIZEWITHIN, (Fingerprint.Filter.DsizeWithin) element.getValue());
                                            break;
                                        case DSTPORT:
                                            filter = new UnpackedFilter<>(Filter.FilterType.DSTPORT, (Integer) element.getValue());
                                            break;
                                        case ETHERTYPE:
                                            filter = new UnpackedFilter<>(Filter.FilterType.ETHERTYPE, (Integer) element.getValue());
                                            break;
                                        case FLAGS:
                                            filter = new UnpackedFilter<>(Filter.FilterType.FLAGS, (String) element.getValue());
                                            break;
                                        case MSS:
                                            filter = new UnpackedFilter<>(Filter.FilterType.MSS, (Integer) element.getValue());
                                            break;
                                        case SEQ:
                                            filter = new UnpackedFilter<>(Filter.FilterType.SEQ, (Integer) element.getValue());
                                            break;
                                        case SRCPORT:
                                            filter = new UnpackedFilter<>(Filter.FilterType.SRCPORT, (Integer) element.getValue());
                                            break;
                                        case TRANSPORTPROTOCOL:
                                            filter = new UnpackedFilter<>(Filter.FilterType.TRANSPORTPROTOCOL, (Short) element.getValue());
                                            break;
                                        case TTL:
                                            filter = new UnpackedFilter<>(Filter.FilterType.TTL, (Integer) element.getValue());
                                            break;
                                        case TTLWITHIN:
                                            filter = new UnpackedFilter<>(Filter.FilterType.TTLWITHIN, (Fingerprint.Filter.TTLWithin) element.getValue());
                                            break;
                                        case WINDOW:
                                            filter = new UnpackedFilter<>(Filter.FilterType.WINDOW, (Integer) element.getValue());
                                            break;
                                    }

                                    return filter;
                                })
                                .filter(filter -> filter != null)
                                .collect(Collectors.toList());

                        return new UnpackedFilterGroup(group.getFor(), filters);
                    })
                    .collect(Collectors.groupingBy(UnpackedFilterGroup::getFor));


            returnMap.put(fp, groupByPayload);
        }

        return returnMap;
    }

    public List<Object> process(IPacketMetadata data) {
        List<Object> results = new CopyOnWriteArrayList<>();
        fingerprints.parallelStream()
            .forEach(fp -> {
                List<Object> props = Collections.emptyList();
                try {
                    props =  this.filter(fp, data)
                            .flatMap( pl -> {
                                return this.fingerprint(fp, pl, data).stream();
                            })
                            .collect(Collectors.toList());
                } catch(Exception ex) {
                    ex.printStackTrace();
                }

                results.addAll(props);
            });

        return results;
    }

    private Stream<Fingerprint.Payload> filter(Fingerprint fp, IPacketMetadata data) {
        List<String> payloadNames = new ArrayList<>();

        Map<String, List<UnpackedFilterGroup>> filterByPayload = this.filtersByPayload.get(fp);

        for (String payload : filterByPayload.keySet()) {
            groupLoop:
            for (UnpackedFilterGroup filterGroup : filterByPayload.get(payload)) {
                for(UnpackedFilter<?> filter : filterGroup.getFilters()) {
                    switch (filter.getType()) {
                        case ACK:
                            if (data instanceof ITcpPacketMetadata) {
                                if (((ITcpPacketMetadata)data).getAck() != (Long) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                        case DSIZE:
                            if (data.getImportProgress() != (Integer) filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case DSIZEWITHIN:
                            Fingerprint.Filter.DsizeWithin within = ((Fingerprint.Filter.DsizeWithin) filter.getValue());
                            if (data.getImportProgress() < within.getMin().longValue() || data.getImportProgress() > within.getMax().longValue()) {
                                continue groupLoop;
                            }
                            break;
                        case DSTPORT:
                            if (data instanceof ITcpPacketMetadata) {
                                if (((ITcpPacketMetadata)data).getDestinationPort() != (Integer) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else if (data instanceof IUdpPacketMetadata) {
                                if (((IUdpPacketMetadata)data).getDestinationPort() != (Integer) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                        case ETHERTYPE:
                            if (data.getEtherType() != (Integer) filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case FLAGS:
                            String flagList = (String) filter.getValue();
                            if (data instanceof ITcpPacketMetadata) {
                                for (String flag : flagList.split(" ")) {
                                    if (!((ITcpPacketMetadata) data).hasFlag(ITcpPacketMetadata.TcpFlags.valueOf(flag))) {
                                        continue groupLoop;
                                    }
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                        case MSS:
                            if (data instanceof ITcpPacketMetadata) {
                                if (((ITcpPacketMetadata) data).getMss() != (Integer) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                        case SEQ:
                            if (data instanceof  ITcpPacketMetadata) {
                                if (((ITcpPacketMetadata) data).getSeqNum() != (Integer) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                        case SRCPORT:
                            if (data instanceof ITcpPacketMetadata) {
                                if (((ITcpPacketMetadata) data).getSourcePort() != (Integer) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                        case TRANSPORTPROTOCOL:
                            if (data.getTransportProtocol() != (Short) filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case TTL:
                            if (data.getTtl() != (Integer) filter.getValue()) {
                                continue groupLoop;
                            }
                            break;
                        case TTLWITHIN:
                            Fingerprint.Filter.TTLWithin ttlWithin = (Fingerprint.Filter.TTLWithin) filter.getValue();
                            if (data.getTtl() < ttlWithin.getMin().longValue() || data.getTtl() > ttlWithin.getMax().longValue()) {
                                continue groupLoop;
                            }
                            break;
                        case WINDOW:
                            if (data instanceof ITcpPacketMetadata) {
                                if (((ITcpPacketMetadata) data).getWindowNum() != (Integer) filter.getValue()) {
                                    continue groupLoop;
                                }
                            } else {
                                continue groupLoop;
                            }
                            break;
                    }
                }

                // if we have made it this far than no filter has failed, we then add the payload and are done checking filters
                payloadNames.add(payload);
                break groupLoop;
            }
        }

        return fp.getPayload().stream()
                .filter(pl -> payloadNames.contains(pl.getFor()));
    }

    private List<Object> fingerprint(Fingerprint fp, Fingerprint.Payload pl, IPacketMetadata data) {
        List<Object> props = new ArrayList<>();
        String fpName = fp.getHeader().getName();
        CursorImpl cursor = new CursorImpl();
        if (pl.getAlways() != null) {
            for (Return ret : pl.getAlways().getReturn()) {
                props.add(processReturn(ret, data, cursor, fpName));
            }

        }

        props.add(executeOps(fpName, pl.getOperation(), data, cursor));

        //TODO: REMOVE THIS
        //THIS IS JUST TEMPORARY FOR PROOF OF CONCEPT
        if (!props.isEmpty()) {
            FingerprintEdgeProperties edgeProps = new FingerprintEdgeProperties(fp.getHeader().getName(), new Session.AddressPair(data.getSourceAddress(), data.getDestAddress()));
            edgeProps.addProperty("Fingerprint", new Property<>(fp.getHeader().getName(), 0));
            props.add(edgeProps);
        }

        return props;
    }

    private Object processReturn(Return ret, IPacketMetadata data, CursorImpl cursor, String fpName) {
        Map<String, Collection<Property<?>>> values = new HashMap<>();
        int confidence = ret.getConfidence();
        DetailGroup details = ret.getDetails();
        if (details != null) {
            if (details.getRole() != null && !details.getRole().isEmpty()) {
                String role = details.getRole();
                Collection<Property<?>> roles = values.get("Role");
                if (roles == null) {
                    roles = new ArrayList<>();
                    values.put("Role", roles);
                }
                roles.add(new Property<>(role, confidence));
            }
            if (details.getCategory() != null && !details.getCategory().isEmpty()) {
                String category = details.getCategory();
                Collection<Property<?>> categories = values.get("Category");
                if (categories == null) {
                    categories = new ArrayList<>();
                    values.put("Category", categories);
                }
                categories.add(new Property<>(category, confidence));
            }
            for (DetailGroup.Detail detail : details.getDetail()) {
                Collection<Property<?>> detailCollection = values.get(detail.getName());
                if (detailCollection == null) {
                    detailCollection = new ArrayList<>();
                    values.put(detail.getName(), detailCollection);
                }
                detailCollection.add(new Property<>(detail.getValue(), confidence));
            }
        }
        if (data instanceof IPacketData) {
            PayloadAccessor accessor = null;
            if (data instanceof ITcpPacketData) {
                accessor = new PayloadAccessor(((ITcpPacketData) data).getTcpContents());
            } else if (data instanceof IUdpPacketData) {
                accessor = new PayloadAccessor(((IUdpPacketData) data).getUdpContents());
            }

            if (accessor != null) {
                for (Extract extract : ret.getExtract()) {
                    Post post = extract.getPost();
                    ContentType convert = null;
                    Lookup lookup = null;
                    if (post != null) {
                        convert = post.getConvert();
                        lookup = post.getLookup() != null ? Lookup.valueOf(post.getLookup()) : null;
                    }
                    Endian endian = extract.getEndian() != null ? Endian.valueOf(extract.getEndian()) : Endian.getDefault();
                    Map.Entry<String, String> entry = PayloadFunctions.extractFunction(accessor, cursor, extract.getName(), extract.getFrom(), extract.getTo(),
                            extract.getMaxLength(), endian, convert, lookup);
                    if (entry != null) {
                        Collection<Property<?>> propertyCollection = values.get(entry.getKey());
                        if (propertyCollection == null) {
                            propertyCollection = new ArrayList<>();
                            values.put(entry.getKey(), propertyCollection);
                        }
                        propertyCollection.add(new Property<>(entry.getValue(), confidence));
                    }
                }
            }
        }

        FingerprintProperties properties = null;
        LogicalAddressMapping sourceAddress = null;
        LogicalAddressMapping destAddress = null;

        if (data.getSourceAddress() instanceof LogicalAddressMapping) {
            sourceAddress = ((LogicalAddressMapping) data.getSourceAddress());
        }

        if (data.getDestAddress() instanceof LogicalAddressMapping) {
           destAddress = ((LogicalAddressMapping) data.getDestAddress());
        }

        switch (ret.getDirection()) {
            case "SOURCE":
                if (sourceAddress != null) {
                    properties = new FingerprintProperties(fpName, sourceAddress);
                }
                break;
            case "DESTINATION":
                if (destAddress != null) {
                    properties = new FingerprintProperties(fpName, destAddress);
                }
                break;
        }

        if (properties != null) {
            properties.getProperties().putAll(values);
        }

        return properties;
    }

    private FingerprintProperties executeOps(String fpName, List<Serializable> opList, IPacketMetadata data, CursorImpl cursor) {
        for (Object op : opList) {
            if (op instanceof Return) {
                Return ret = ((Return) op);
                processReturn(ret, data, cursor, fpName);
            } else if (data instanceof IPacketData) {
                PayloadAccessor accessor = null;
                if (data instanceof ITcpPacketData) {
                    accessor = new PayloadAccessor(((ITcpPacketData) data).getTcpContents());
                } else if (data instanceof IUdpPacketData) {
                    accessor = new PayloadAccessor(((IUdpPacketData) data).getUdpContents());
                }

                if (accessor != null) {

                    if (op instanceof MatchFunction) {
                        MatchFunction match = ((MatchFunction) op);

                        byte[] content = null;
                        if (match.getContent() != null) {
                            content = getContent(match.getContent().getType(), match.getContent().getValue());
                        }

                        boolean matched = PayloadFunctions.matchFunction(accessor, cursor, match.getDepth(), match.getOffset(), match.isRelative(),
                                match.isNoCase(), match.getPattern(), content, match.isMoveCursors(), StandardCharsets.UTF_8);

                        if (matched) {
                            if (match.getAndThen() != null) {
                                executeOps(fpName, match.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                            }
                        }

                    } else if (op instanceof ByteTestFunction) {
                        ByteTestFunction testFunc = ((ByteTestFunction) op);

                        Test test = getTest(testFunc);
                        boolean passed = false;
                        if (test != null) {
                            BigInteger value = getTestValue(testFunc, test);
                            passed = PayloadFunctions.byteTestFunction(accessor, cursor, test, value.intValue(), testFunc.isRelative(),
                                    testFunc.getOffset(), testFunc.getPostOffset(), testFunc.getBytes(), Endian.valueOf(testFunc.getEndian()));
                        }

                        if (passed && testFunc.getAndThen() != null) {
                            executeOps(fpName, testFunc.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                        }
                    } else if (op instanceof ByteJumpFunction) {
                        ByteJumpFunction jump = ((ByteJumpFunction) op);

                        Endian endian = jump.getEndian() != null ? Endian.valueOf(jump.getEndian()) : Endian.getDefault();

                        int postOffset = jump.getPostOffset() != null ? jump.getPostOffset() : 0;
                        int offset = jump.getOffset() != null ? jump.getOffset() : 0;

                        PayloadFunctions.byteJumpFunction(accessor, cursor, offset, jump.isRelative(), jump.getBytes(),
                                endian, postOffset, jump.getCalc());

                        if (jump.getAndThen() != null) {
                            executeOps(fpName, jump.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                        }
                    } else if (op instanceof IsDataAtFunction) {
                        IsDataAtFunction at = ((IsDataAtFunction) op);

                        boolean isData = PayloadFunctions.isDataAtFunction(accessor, cursor, at.getOffset(), at.isRelative());

                        if (isData && at.getAndThen() != null) {
                            executeOps(fpName, at.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor);
                        }
                    } else if (op instanceof Anchor) {
                        Anchor anchor = ((Anchor) op);

                        int offset = anchor.getOffset() != null ? anchor.getOffset() : 0;

                        PayloadFunctions.anchorFunction(accessor, cursor, anchor.getCursor(), anchor.getPosition(), anchor.isRelative(), offset);
                    }
                }
            }
        }

        return null;
    }

    private Test getTest(ByteTestFunction func) {
        if (func.getAND() != null) {
            return Test.AND;
        } else if (func.getOR() != null) {
            return Test.OR;
        } else if (func.getGT() != null) {
            return Test.GT;
        } else if (func.getGTE() != null) {
            return Test.GTE;
        } else if (func.getLT() != null) {
            return Test.LT;
        } else if (func.getLTE() != null) {
            return Test.LTE;
        } else if (func.getEQ() != null) {
            return Test.EQ;
        } else {
            return null;
        }
    }

    private BigInteger getTestValue(ByteTestFunction func, Test test) {
        BigInteger ret = null;
        switch (test) {
            case GT:
                ret = func.getGT();
                break;
            case GTE:
                ret = func.getGTE();
                break;
            case LT:
                ret = func.getLT();
                break;
            case LTE:
                ret = func.getLTE();
                break;
            case AND:
                ret = func.getAND();
                break;
            case OR:
                ret = func.getOR();
                break;
            case EQ:
                ret = func.getEQ();
                break;
        }

        return ret;
    }

    private byte[] getContent(ContentType type, String value) {
        byte[] ret = new byte[0];

        try {
            switch (type) {
                case HEX:
                    ret = new BigInteger(value, 16).toByteArray();
                    break;
                case STRING:
                    ret = value.getBytes(StandardCharsets.UTF_8);
                    break;
                case RAW_BYTES:
                    value = value.replaceAll("\\s+", "");
                    ret = new byte[value.length() / 2];
                    for (int i = 0; i < value.length(); i += 2) {
                        int parsed = Integer.parseInt(value.substring(i, i + 2), 16);
                        ret[i / 2] = (byte) parsed;
                    }
                    break;
                case INTEGER:
                    ret = new BigInteger(value).toByteArray();
            }
        } catch (NumberFormatException e) {
            // returning empty array
        }

        return ret;
    }

}
