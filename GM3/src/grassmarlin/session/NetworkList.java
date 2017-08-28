package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.pipeline.Network;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class NetworkList implements XmlSerializable {
    private final Event.IAsyncExecutionProvider computeExecutor;

    private final Map<String, Set<Network>> reportedNetworks;
    private final List<Network> calculatedNetworks;

    public final Event<List<Network>> onNetworksUpdated;
    public final Event<NetworkReportedEventArgs> onNetworkReported;

    public static class NetworkReportedEventArgs {
        private final String source;
        private final Network network;

        public NetworkReportedEventArgs(final String source, final Network network) {
            this.source = source;
            this.network = network;
        }

        public String getSource() {
            return this.source;
        }
        public Network getNetwork() {
            return this.network;
        }
    }

    private class NetworkListThreadManagedState extends ThreadManagedState {
        public NetworkListThreadManagedState(final Event.IAsyncExecutionProvider provider) {
            super(RuntimeConfiguration.UPDATE_INTERVAL_MS, "NetworkList", provider);
        }

        @Override
        public void validate() {
            boolean bChanged = false;
            // We're going to simplify each set of networks by source, then process the simplified networks by priority
            final HashMap<Integer, List<Network>> intermediateMapping = new HashMap<>();

            synchronized(reportedNetworks) {
                for (Set<Network> set : reportedNetworks.values()) {
                    //Find every distinct confidence value for this source...
                    List<Integer> confidences = set.stream().map(network -> network.getConfidence()).distinct().sorted().collect(Collectors.toList());
                    final LinkedList<Network> networksFromSource = new LinkedList<>();
                    for (int confidence : confidences) {
                        //Make sure we have the array list for this confidence in intermediateMapping...
                        if (!intermediateMapping.containsKey(confidence)) {
                            intermediateMapping.put(confidence, new ArrayList<>());
                        }

                        //Get the sublist of networks at this confidence, then remove any networks contained in higher-confidence networks...
                        List<Network> networksForConfidence = set.stream().filter(network -> network.getConfidence() == confidence).collect(Collectors.toList());
                        networksForConfidence = networksForConfidence.stream().filter(network -> !networksFromSource.stream().anyMatch(existing -> existing.getValue().contains(network.getValue()))).collect(Collectors.toList());
                        //Add the sublist to the networks that override lower-confidence networks, and add to the result at the appropriate confidence level.
                        networksFromSource.addAll(networksForConfidence);
                        intermediateMapping.get(confidence).addAll(networksForConfidence);
                    }
                }
            }

            //We now repeat the above method on intermediateMapping, resulting in a single list of Networks:
            final LinkedList<Network> networksFinal = new LinkedList<>();
            for(int confidence : intermediateMapping.keySet().stream().sorted().collect(Collectors.toList())) {
                final List<Network> networks = intermediateMapping.get(confidence);
                networksFinal.addAll(networks.stream().distinct().filter(network -> !networksFinal.stream().anyMatch(existing -> existing.getValue().contains(network.getValue()))).collect(Collectors.toList()));
            }

            if(NetworkList.this.calculatedNetworks.size() != networksFinal.size() || !NetworkList.this.calculatedNetworks.containsAll(networksFinal)) {
                NetworkList.this.calculatedNetworks.clear();
                NetworkList.this.calculatedNetworks.addAll(networksFinal);
                onNetworksUpdated.call(NetworkList.this.calculatedNetworks);
            }
        }
    }
    private final ThreadManagedState validator;

    public NetworkList(final Event.IAsyncExecutionProvider eventExecutor) {
        this(eventExecutor, eventExecutor);
    }
    public NetworkList(final Event.IAsyncExecutionProvider computeExecutor, final Event.IAsyncExecutionProvider eventExecutor) {
        this.computeExecutor = computeExecutor;

        this.onNetworksUpdated = new Event<>(eventExecutor);
        this.onNetworkReported = new Event<>(eventExecutor);

        this.reportedNetworks = new HashMap<>();
        this.calculatedNetworks = new ArrayList<>();

        this.validator = new NetworkListThreadManagedState(computeExecutor);
    }

    public void addNetwork(final String source, final Network network) {
        final AtomicBoolean added = new AtomicBoolean(false);
        this.validator.invalidate(this.reportedNetworks, () -> {
            Set<Network> setSource = this.reportedNetworks.get(source);
            if(setSource == null) {
                setSource = new HashSet<>();
                this.reportedNetworks.put(source, setSource);
            }
            added.set(setSource.add(network));
            return added.get();
        });
        if(added.get()) {
            onNetworkReported.call(new NetworkReportedEventArgs(source, network));
        }
    }

    @Deprecated
    /**
     * This is written for the generic case, but if there is ever a reason to
     * remove a network that was not manually defined by the user, please stop
     * to consider that you are probably about to do something really bad.
     *
     * This method is flagged as deprecated for this very reason.  If data
     * suggested that a network should exist, then the network should exist.
     * We may be able to draw a better conclusion at a higher confidence level,
     * but that is what this class is intended to resolve--you should not be
     * removing Networks just because a better assessment was reached.
     *
     * The one obvious exception to this is user-entered data.  Users are
     * expected to enter all valid values and can make mistakes, change their
     * minds, etc.  This method exists only to support that use case.  If you
     * are using it for any other purpose, you are probably not using the
     * confidence field correctly.
     *
     * The only other conceivable (yes, that word means what I think it means)
     * valid use of this method is for a state-based assessment that needs to
     * invalidate a prior assessment. (e.g. the old topology mapper).  We
     * jettisoned the old topology mapper from an airlock, nuked it from orbit,
     * then killed it again with fire.  Please don't clone it; nobody wants to
     * go down that path.
     * @param source The source reporting the network.
     * @param network The network to define.
     * @param confidence A positive Integer indicating the confidence level at which this network is defined.  A value of 0 indicates explicit user data entry.
     */
    public void removeNetwork(final String source, final Network network) {
        this.validator.invalidate(this.reportedNetworks, () -> {
            Set<Network> setSource = this.reportedNetworks.get(source);
            if(setSource == null) {
                return false;
            }
            return setSource.remove(network);
        });
        onNetworkReported.call(null);
    }

    /**
     * @return Returns a copy (produced with appropriate locks) of the internally used Network list.
     */
    public List<Network> getCalculatedNetworksCopy() {
        synchronized(reportedNetworks) {
            return new ArrayList(this.calculatedNetworks);
        }
    }

    public Map<String, Set<Network>> getAllReportedNetworks() {
        //TODO: This should return a new map that contains different Set references with the same Networks, but as long as we don't make changes we'll be fine with the lazy approach.  Please don't ruin the honor system for everybody.
        return reportedNetworks;
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        //We should be starting in the Networks element
        while(source.hasNext()) {
            final int typeNext = source.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    if(source.getLocalName().equals("Set")) {
                        final String sourceNetworks = source.getAttributeValue(null, "Source");
                        final Set<Network> networks = readNetworksFromXml(source);

                        this.validator.invalidate(this.reportedNetworks, () -> this.reportedNetworks.put(sourceNetworks, networks));
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if(source.getLocalName().equals("Networks")) {
                        return;
                    }
                    break;
            }
        }
    }

    protected Set<Network> readNetworksFromXml(final XMLStreamReader source) throws XMLStreamException {
        final Set<Network> networks = new HashSet<>();
        while (source.hasNext()) {
            final int typeNext = source.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    final Object obj = Loader.readObject(source);
                    if(obj instanceof Network) {
                        networks.add((Network)obj);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if(source.getLocalName().equals("Set")) {
                        return networks;
                    }
                    break;
            }
        }

        return networks;
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        for(final Map.Entry<String, Set<Network>> entry : this.reportedNetworks.entrySet()) {
            target.writeStartElement("Set");
            target.writeAttribute("Source", entry.getKey());

            for(final Network network : entry.getValue()) {
                target.writeStartElement("Network");
                Writer.writeObject(target, network);
                target.writeEndElement();
            }

            target.writeEndElement();
        }
    }
}
