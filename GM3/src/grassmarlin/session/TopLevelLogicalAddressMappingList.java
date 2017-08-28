package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LogicalAddressMappings are only ever added to the list--they don't disappear once created, but the list of top-level addresses can have elements removed in response to added mappings.
 */
public class TopLevelLogicalAddressMappingList {
    private final Map<HardwareAddress, List<LogicalAddressMapping>> mappingsByHardwareAddress;
    private final Map<HardwareAddress, List<LogicalAddressMapping>> topMappingsByHardwareAddress;

    public class LogicalAddressMappingEventArgs {
        private final LogicalAddressMapping mapping;

        protected LogicalAddressMappingEventArgs(final LogicalAddressMapping mapping) {
            this.mapping = mapping;
        }

        public LogicalAddressMapping getMapping() {
            return this.mapping;
        }
        public TopLevelLogicalAddressMappingList getTopLevelList() {
            return TopLevelLogicalAddressMappingList.this;
        }
    }

    public final Event<LogicalAddressMappingEventArgs> onNewTopLevelAddress;
    public final Event<LogicalAddressMappingEventArgs> onRemovedTopLevelAddress;

    public TopLevelLogicalAddressMappingList(final Event.IAsyncExecutionProvider executionProvider) {
        this.mappingsByHardwareAddress = new HashMap<>();
        this.topMappingsByHardwareAddress = new HashMap<>();

        this.onNewTopLevelAddress = new Event<>(executionProvider);
        this.onRemovedTopLevelAddress = new Event<>(executionProvider);
    }

    public synchronized void addMapping(final LogicalAddressMapping mappingNew) {
        List<LogicalAddressMapping> mappings = mappingsByHardwareAddress.get(mappingNew.getHardwareAddress());
        if(mappings == null) {
            mappings = new ArrayList<>();
            mappingsByHardwareAddress.put(mappingNew.getHardwareAddress(), mappings);
            topMappingsByHardwareAddress.put(mappingNew.getHardwareAddress(), new LinkedList<>());
        }
        //Check to see if this is top level or not, fire appropriate events
        final List<LogicalAddressMapping> contained = new LinkedList<>();

        for(final LogicalAddressMapping existing : mappings) {
            if(existing.contains(mappingNew)) {
                //mappingNew cannot be a top level
                mappings.add(mappingNew);
                return;
            }
            if(mappingNew.contains(existing)) {
                contained.add(existing);
            }
        }
        mappings.add(mappingNew);

        //If it is top level, check for no-longer-top-level mappings
        final List<LogicalAddressMapping> existingTopLevels = topMappingsByHardwareAddress.get(mappingNew.getHardwareAddress());
        contained.retainAll(existingTopLevels);

        topMappingsByHardwareAddress.get(mappingNew.getHardwareAddress()).removeAll(contained);
        for(final LogicalAddressMapping mapping : contained) {
            onRemovedTopLevelAddress.call(new LogicalAddressMappingEventArgs(mapping));
        }

        topMappingsByHardwareAddress.get(mappingNew.getHardwareAddress()).add(mappingNew);
        onNewTopLevelAddress.call(new LogicalAddressMappingEventArgs(mappingNew));
    }

    public List<LogicalAddressMapping> topLevelMappingsFor(final HardwareAddress hw) {
        return new ArrayList<>(topMappingsByHardwareAddress.get(hw));
    }
    public List<LogicalAddressMapping> getAllMappings() {
        return mappingsByHardwareAddress.values().stream().flatMap(list -> list.stream()).collect(Collectors.toList());
    }
}
