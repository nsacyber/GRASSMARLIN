package iadgov.fingerprint.processor;

import core.fingerprint3.*;
import grassmarlin.Logger;
import grassmarlin.common.Confidence;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.*;
import iadgov.fingerprint.FingerprintEdgeProperties;
import iadgov.fingerprint.FingerprintVertexProperties;
import iadgov.fingerprint.IHasFingerprintProperties;
import iadgov.fingerprint.manager.filters.Filter;
import iadgov.fingerprint.manager.payload.Endian;

import javax.xml.bind.JAXBElement;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is the class that will process fingerprints
 */
public class FProcessor {

    private static class FilterTreeNode {
        /**
         * The filter type of this node
         */
        private Filter.FilterType type;
        /**
         * The list of payloads that are valid by virtue of making it to this node
         */
        private List<PayloadTuple> payloads;
        /**
         * an array of filter values to follow on nodes. This is, in essence, the actual filter
         */
        private ArrayList<FilterTreeNode> nodeArray;
        /**
         * List of follow on nodes that are not filtered by this node
         */
        private FilterTreeNode passThroughNode;
        /**
         * Not all filter value ranges start at 0
         */
        private int arrayOffset;

        /**
         * A node iplementing a filter
         * @param arraySize the size of the array containing the possible filter values(max value - min value)
         * @param arrayOffset the distance from 0 of the min filter value
         */
        public FilterTreeNode(Filter.FilterType type, int arraySize, int arrayOffset) {
            this.type = type;
            this.nodeArray = new ArrayList<>(arraySize);
            this.arrayOffset = arrayOffset;
            this.payloads = new ArrayList<>();
        }

        public void connect(FilterTreeNode node, int filterValue) {
            while (filterValue > this.nodeArray.size()) {
                this.nodeArray.add(null);
            }
            this.nodeArray.add(filterValue, node);
        }

        public FilterTreeNode getPassThrough() {
            return this.passThroughNode;
        }

        public FilterTreeNode getFilteredNode(int value) {
            int adjustedValue = value - arrayOffset;
            if (adjustedValue < 0 || adjustedValue >= this.nodeArray.size()) {
                return null;
            } else {
                return this.nodeArray.get(value - arrayOffset);
            }
        }

        public void addPayload(PayloadTuple payload) {
            this.payloads.add(payload);
        }

        public Collection<PayloadTuple> getPayloads() {
            return Collections.unmodifiableCollection(this.payloads);
        }
    }

    private static class GroupTuple {
        public final String fingerprint;
        public final Fingerprint.Filter group;

        public GroupTuple(String fpName, Fingerprint.Filter group) {
            this.fingerprint = fpName;
            this.group = group;
        }
    }

    private static class PayloadTuple {
        public final String fingerprint;
        public final Fingerprint.Payload payload;

        public PayloadTuple(String fpName, Fingerprint.Payload payload) {
            this.fingerprint = fpName;
            this.payload = payload;
        }
    }


    List<Fingerprint> fingerprints;
    FilterTreeNode rootNode;

    public FProcessor(List<Fingerprint> runningFingerprints) {
        this.fingerprints = Collections.unmodifiableList(new ArrayList<>(runningFingerprints));
        this.configure();
    }

    private void configure() {
        Map<Filter.FilterType, Integer> countMap = new HashMap<>();
        for (Fingerprint fp : fingerprints) {
            for (Fingerprint.Filter group : fp.getFilter()) {
                for (JAXBElement element : group.getAckAndMSSAndDsize()) {
                    Filter.FilterType type = Filter.FilterType.valueOf(element.getName().toString().replaceAll(" ", "").toUpperCase());
                    int count = countMap.get(type) != null ? countMap.get(type) + 1 : 1;
                    countMap.put(type, count);
                }
            }
        }

        List<Filter.FilterType> typeByCount = countMap.entrySet().stream()
                .sorted((entry1, entry2) -> entry1.getValue().compareTo(entry2.getValue()) * -1)
                .map(entry -> entry.getKey())
                .collect(Collectors.toList());

        List<GroupTuple> groups = this.fingerprints.parallelStream()
                .flatMap(fp -> fp.getFilter().stream()
                    .map(group -> new GroupTuple(fp.getHeader().getName(), group)))
                .collect(Collectors.toList());
        Map<String, PayloadTuple> payloadByName = this.fingerprints.parallelStream()
                .flatMap(fp -> fp.getPayload().stream()
                    .map(payload -> new PayloadTuple(fp.getHeader().getName(), payload)))
                .collect(Collectors.toMap(tuple -> tuple.fingerprint + ":" + tuple.payload.getFor(), Function.identity()));

        rootNode = buildTree(groups, typeByCount, payloadByName);

    }

    private FilterTreeNode buildTree (List<GroupTuple> groups, List<Filter.FilterType> filterStack, Map<String, PayloadTuple> payloadMap) {
        if (filterStack.isEmpty()) {
            FilterTreeNode node = new FilterTreeNode(null, 0, 0);
            for (GroupTuple tuple : groups) {
                PayloadTuple payload = payloadMap.get(tuple.fingerprint + ":" + tuple.group.getFor());
                node.addPayload(payload);
            }
            return node;
        }
        Filter.FilterType type = filterStack.get(0);
        if (type != null) {
            filterStack.remove(0);
        }
        List<GroupTuple> groupsWithFilter = Collections.emptyList();
        List<GroupTuple> doneGroups = Collections.emptyList();
        List<Integer> possibleValues = Collections.emptyList();
        int min = 0;
        int max = 0;
        if (type != null) {
            groupsWithFilter = groups.parallelStream()
                    .filter(tuple -> tuple.group.getAckAndMSSAndDsize().stream()
                            .anyMatch(element -> Filter.FilterType.getType(element) == type)
                    )
                    .collect(Collectors.toList());

            groups.removeAll(groupsWithFilter);

            doneGroups = groups.parallelStream()
                    .filter(tuple -> tuple.group.getAckAndMSSAndDsize().stream()
                        .noneMatch(element -> filterStack.contains(Filter.FilterType.getType(element))))
                    .collect(Collectors.toList());

            groups.removeAll(doneGroups);

            if (!groupsWithFilter.isEmpty()) {
                possibleValues = groupsWithFilter.stream()
                        .flatMap(tuple -> tuple.group.getAckAndMSSAndDsize().stream()
                            .filter(element -> Filter.FilterType.getType(element) == type))
                        .flatMap(element -> Filter.FilterType.getValue(element).stream())
                        .distinct()
                        .sorted(Integer::compare)
                        .collect(Collectors.toList());
                min = possibleValues.get(0);
                max = possibleValues.get(possibleValues.size() - 1);
            }
        }

        FilterTreeNode node = new FilterTreeNode(type, max - min + 1, min);

        for (int value : possibleValues) {
            List<GroupTuple> nextGroups = groupsWithFilter.stream()
                    .filter(tuple -> tuple.group.getAckAndMSSAndDsize().stream()
                                        .filter(element -> Filter.FilterType.getType(element) == type)
                                        .flatMap(element -> Filter.FilterType.getValue(element).stream())
                                        .anyMatch(filterValue -> filterValue == value)
                    )
                    .collect(Collectors.toList());
            node.connect(buildTree(nextGroups, new ArrayList<>(filterStack), payloadMap), value - min);
        }

        if (groups.size() > 0) {
            node.passThroughNode = buildTree(groups, new ArrayList<>(filterStack), payloadMap);
        }

        doneGroups.stream()
                .map(tuple -> tuple.fingerprint + ":" + tuple.group.getFor())
                .forEach(plName -> {
                    PayloadTuple pl = payloadMap.get(plName);
                    if (pl != null) {
                        node.addPayload(payloadMap.get(plName));
                    }
                });

        return node;
    }

    public List<Object> process(ILogicalPacketMetadata data) {
        List<Object> results = new CopyOnWriteArrayList<>();

        List<Object> props = Collections.emptyList();
        try {
            props =  this.filter(this.rootNode, data)
                    .flatMap( pl -> {
                        try {
                            return this.fingerprint(pl, data).stream();
                        } catch (Exception e) {
                            Logger.log(Logger.Severity.ERROR, "Error in Fingerprinting: %s", e.getMessage());
                            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Error in Fingerprinting: %s", Arrays.toString(e.getStackTrace()));
                            return Stream.empty();
                        }

                    })
                    .collect(Collectors.toList());
        } catch(Exception ex) {
            Logger.log(Logger.Severity.ERROR, "Error in Fingerprinting: %s", ex.getMessage());
            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Error in Fingerprinting: %s", Arrays.toString(ex.getStackTrace()));
        }

            results.addAll(props);

        return results;
    }

    private Stream<PayloadTuple> filter(FilterTreeNode node, ILogicalPacketMetadata data) {
        if (node == null) {
            return Stream.empty();
        }

        int filterValue = node.type != null ? node.type.getPacketValue(data) : -1;

        Stream.Builder<PayloadTuple> builder = Stream.builder();

        node.getPayloads().forEach(builder::accept);
        filter(node.getFilteredNode(filterValue), data).forEach(builder::accept);
        filter(node.getPassThrough(), data).forEach(builder::accept);

        return builder.build();
    }

    private List<Object> fingerprint(PayloadTuple pl, ILogicalPacketMetadata data) {
        String fpName = pl.fingerprint;
        Fingerprint.Payload payload = pl.payload;
        List<Object> props = new ArrayList<>();
        CursorImpl cursor = new CursorImpl();
        if (payload.getAlways() != null) {
            for (Return ret : payload.getAlways().getReturn()) {
                props.add(processReturn(fpName, ret, data, cursor));
            }

        }

        props.addAll(executeOps(fpName, payload.getOperation(), data, cursor));

        if (!props.isEmpty()) {
            FingerprintEdgeProperties edgeProps = new FingerprintEdgeProperties(fpName, new Session.LogicalAddressPair(data.getSourceAddress(), data.getDestAddress()));
            edgeProps.addProperty("Fingerprint", new Property<>(fpName, Confidence.USER));
            props.add(edgeProps);
        }

        return props;
    }

    private Object processReturn(String fpName, Return ret, ILogicalPacketMetadata data, CursorImpl cursor) {
        Map<String, Collection<Property<?>>> values = new HashMap<>();
        Confidence confidence = Confidence.fromNumber(ret.getConfidence());
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

        IHasFingerprintProperties properties = null;
        LogicalAddressMapping sourceAddress = data.getSourceAddress();
        LogicalAddressMapping destAddress = data.getDestAddress();

        switch (ret.getDirection()) {
            case SOURCE:
                if (sourceAddress != null) {
                    properties = new FingerprintVertexProperties(fpName, sourceAddress);
                }
                break;
            case DESTINATION:
                if (destAddress != null) {
                    properties = new FingerprintVertexProperties(fpName, destAddress);
                }
                break;
            case CONNECTION:
                if (sourceAddress != null && destAddress != null) {
                    properties = new FingerprintEdgeProperties(fpName, new Session.LogicalAddressPair(sourceAddress, destAddress));
                }
        }

        if (properties != null) {
            properties.putProperties(values);
        }

        return properties;
    }

    private List<Object> executeOps(String fpName, List<Serializable> opList, ILogicalPacketMetadata data, CursorImpl cursor) {
        ArrayList<Object> props = new ArrayList<>();
        for (Object op : opList) {
            if (op instanceof Return) {
                Return ret = ((Return) op);
                props.add(processReturn(fpName, ret, data, cursor));
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
                                match.isNoCase(), Endian.valueOf(match.getEndian()), match.getPattern(), content, match.isMoveCursors(), StandardCharsets.UTF_8);

                        if (matched) {
                            if (match.getAndThen() != null) {
                                props.addAll(executeOps(fpName, match.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor));
                            }
                        }

                    } else if (op instanceof ByteTestFunction) {
                        ByteTestFunction testFunc = ((ByteTestFunction) op);

                        Boolean passed = PayloadFunctions.byteTestFunction(accessor, cursor, testFunc.getTest(), testFunc.getValue().intValue(), testFunc.isRelative(),
                                testFunc.getOffset(), testFunc.getPostOffset(), testFunc.getBytes(), Endian.valueOf(testFunc.getEndian()));

                        if (passed && testFunc.getAndThen() != null) {
                            props.addAll(executeOps(fpName, testFunc.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor));
                        }
                    } else if (op instanceof ByteJumpFunction) {
                        ByteJumpFunction jump = ((ByteJumpFunction) op);

                        Endian endian = jump.getEndian() != null ? Endian.valueOf(jump.getEndian()) : Endian.getDefault();

                        int offset = jump.getOffset() != null ? jump.getOffset() : 0;

                        PayloadFunctions.byteJumpFunction(accessor, cursor, offset, jump.isRelative(), jump.getBytes(),
                                endian, jump.getCalc());

                        if (jump.getAndThen() != null) {
                            props.addAll(executeOps(fpName, jump.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor));
                        }
                    } else if (op instanceof IsDataAtFunction) {
                        IsDataAtFunction at = ((IsDataAtFunction) op);

                        boolean isData = PayloadFunctions.isDataAtFunction(accessor, cursor, at.getOffset(), at.isRelative());

                        if (isData && at.getAndThen() != null) {
                            props.addAll(executeOps(fpName, at.getAndThen().getMatchOrByteTestOrIsDataAt(), data, cursor));
                        }
                    } else if (op instanceof Anchor) {
                        Anchor anchor = ((Anchor) op);

                        int offset = anchor.getOffset() != null ? anchor.getOffset() : 0;

                        PayloadFunctions.anchorFunction(accessor, cursor, anchor.getCursor(), anchor.getPosition(), offset);
                    }
                }
            }
        }

        return props;
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
