package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.common.Confidence;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This differs from the PropertyContainer by hiding values of a lower confidence only if that value exists at a higher confidence.
 * This is used for edges, where the role of conduit means that one identified behavior does not invalidate any other identified behavior.
 *
 * Also, this exists so that we can claim to be leveraging the cloud when matters of buzzword compliance come up.
 */
public class PropertyCloud implements XmlSerializable {
    public class PropertyEventArgs {
        private final String name;
        private final Property<?> property;
        private final boolean added;

        public PropertyEventArgs(final String name, final Property<?> value, final boolean added) {
            this.name = name;
            this.property = value;
            this.added = added;
        }

        public String getName() {
            return this.name;
        }

        public Serializable getValue() {
            return this.property.getValue();
        }

        public Confidence getConfidence() {
            return this.property.getConfidence();
        }

        public Property<?> getProperty() {
            return this.property;
        }

        public boolean isAdded() {
            return this.added;
        }

        public PropertyCloud getCloud() {
            return PropertyCloud.this;
        }
    }

    public final Event<PropertyEventArgs> onPropertyChanged;

    private final Set<PropertyCloud> ancestors;
    private final Map<String, Map<String, Set<Property<?>>>> propertiesProvided;
    private final Map<String, Set<Property<?>>> propertiesComputed;

    protected final Event.IAsyncExecutionProvider uiProvider;

    public PropertyCloud() {
        this(Event.PROVIDER_IN_THREAD);
    }

    public PropertyCloud(final Event.IAsyncExecutionProvider eventThread) {
        this.uiProvider = eventThread;

        this.ancestors = new HashSet<>();
        this.propertiesProvided = new HashMap<>();
        this.propertiesComputed = new HashMap<>();

        this.onPropertyChanged = new Event<>(eventThread);
    }

    public Map<String, Set<Property<?>>> getProperties() {
        return propertiesComputed;
    }

    //Ancestor Management
    public void addAncestor(final PropertyCloud ancestor) {
        //We need to synchronize ancestors and may need to synchronize on computedProperties--if we need both, we have to do computedProperties first
        synchronized(propertiesComputed) {
            synchronized(ancestors) {
                if(ancestors.add(ancestor)) {
                    ancestor.onPropertyChanged.addHandler(this.handlerAncestorPropertyChanged);
                    //Process the initial state into the cache / fire events.
                    for(final Map.Entry<String, Set<Property<?>>> entry : ancestor.propertiesComputed.entrySet()) {
                        for(final Property<?> property : entry.getValue()) {
                            addPropertyToCache(entry.getKey(), property.getValue(), property.getConfidence());
                        }
                    }
                }
            }
        }
    }

    public void removeAncestor(final PropertyCloud ancestor) {
        //We need to synchronize ancestors and may need to synchronize on computedProperties--if we need both, we have to do computedProperties first
        synchronized(propertiesComputed) {
            if(ancestors.remove(ancestor)) {
                ancestor.onPropertyChanged.removeHandler(this.handlerAncestorPropertyChanged);

                for(final Map.Entry<String, Set<Property<?>>> entry : ancestor.propertiesComputed.entrySet()) {
                    for(final Property<?> property : entry.getValue()) {
                        removePropertyFromCache(entry.getKey(), property.getValue(), property.getConfidence());
                    }
                }
            }
        }
    }

    protected Collection<PropertyCloud> getAncestors() {
        synchronized(this.ancestors) {
            return new ArrayList<>(this.ancestors);
        }
    }

    private Event.EventListener<PropertyEventArgs> handlerAncestorPropertyChanged = this::handleAncestorPropertyChanged;
    private void handleAncestorPropertyChanged(final Event<PropertyEventArgs> event, final PropertyEventArgs args) {
        if(args.isAdded()) {
            addPropertyToCache(args.getName(), args.getValue(), args.getConfidence());
        } else {
            removePropertyFromCache(args.getName(), args.getValue(), args.getConfidence());
        }
    }

    //Property Management
    public void addProperties(final String source, final String name, final Collection<Property<?>> values) {
        synchronized (propertiesComputed) {
            synchronized (ancestors) {
                synchronized (propertiesProvided) {
                    if (!propertiesProvided.containsKey(source)) {
                        propertiesProvided.put(source, new HashMap<>());
                    }

                    final Map<String, Set<Property<?>>> propertiesOfSource = propertiesProvided.get(source);
                    if (!propertiesOfSource.containsKey(name)) {
                        propertiesOfSource.put(name, new HashSet<>());
                    }

                    final Set<Property<?>> properties = propertiesOfSource.get(name);
                    for (final Property<?> property : values) {
                        if (properties.add(property)) {
                            addPropertyToCache(name, property.getValue(), property.getConfidence());
                        }
                    }
                }
            }
        }
    }

    public void addProperties(final String source, final String name, final Property<?>... values) {
        addProperties(source, name, Arrays.asList(values));
    }

    // Cache Management
    protected void addPropertyToCache(final String name, final Serializable value, final Confidence confidence) {
        synchronized (propertiesComputed) {
            final Set<Property<?>> values = propertiesComputed.get(name);
            if (values == null) {
                //This is the first property for this name, so we can construct the list, add it, and announce
                final HashSet<Property<?>> valuesNew = new HashSet<>();
                final Property<?> propertyNew = new Property<>(value, confidence);
                valuesNew.add(propertyNew);
                propertiesComputed.put(name, valuesNew);
                onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
            } else if (values.isEmpty()) {
                //We don't need to create the set, we can just add and announce the property.
                final Property<?> propertyNew = new Property<>(value, confidence);
                values.add(propertyNew);
                onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
            } else {
                Confidence confidenceExisting = values.stream().filter(property -> property.getValue().equals(value)).map(Property::getConfidence).sorted().findFirst().orElse(Confidence.NONE);
                if(confidenceExisting == null) {
                    // It doesn't exist at any confidence, so add it
                    final Property<?> propertyNew = new Property<>(value, confidence);
                    values.add(propertyNew);
                    onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
                } else if(confidenceExisting.compareTo(confidence) > 0) {
                    // Remove the old and add the new
                    final Property<?> propertyOld = new Property<>(value, confidenceExisting);
                    values.remove(propertyOld);
                    onPropertyChanged.call(new PropertyEventArgs(name, propertyOld, false));
                    final Property<?> propertyNew = new Property<>(value, confidence);
                    values.add(propertyNew);
                    onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
                } else if(confidenceExisting == confidence) {
                    // Increment the reference count
                    values.stream().filter(property -> property.getValue().equals(value) && property.getConfidence() == confidence).forEach(Property::addReference);
                } else {
                    // Ignore the new
                }
            }
        }
    }

    protected void removePropertyFromCache(final String name, final Object value, final Confidence confidence) {
        synchronized(propertiesComputed) {
            //We're going to cut a few corners here and assume that, if we're removing a property from the cache, it is actually there.
            final Set<Property<?>> values = propertiesComputed.get(name);
            final Property<?> propExisting = values.stream().filter(property -> property.getValue().equals(value) && property.getConfidence() == confidence).findAny().orElse(null);
            if(propExisting.removeReference() < 1) {
                values.remove(propExisting);
                onPropertyChanged.call(new PropertyEventArgs(name, propExisting, false));
                //If the value set is not empty, then there is still at least one item at the same confidence, so nothing should be unmasked.
                //If it is empty, we need to re-evaluate the cache line for this property name.
                if(values.isEmpty()) {
                    rebuildCacheLine(name);
                }
            }
        }
    }

    protected void rebuildCacheLine(final String name) {
        //We don't lock on computedProperties here because you should only ever be doing this from within a lock on computedProperties.
        //We can lock on ancestors and providedProperties, however.
        synchronized(ancestors) {
            synchronized(propertiesProvided) {
                //noinspection OptionalGetWithoutIsPresent
                final List<Property<?>> propertiesForLine = Stream.concat(
                        ancestors.stream()
                            .map(ancestor -> ancestor.propertiesComputed.get(name))         // We're only looking at this one line from the cache
                            .filter(set -> set != null && !set.isEmpty())                   // Ignore sources that don't contribute anything meaningful--primarily, avoid the null reference
                            .flatMap(set -> set.stream()),                                  // Consider every property for this name
                        propertiesProvided.values().stream()
                                .map(entry -> entry.get(name))                              // For each source, get the set associated with the provided name
                                .filter(set -> set != null && !set.isEmpty())               // Remove null and empty sets
                                .flatMap(set -> set.stream())
                    )
                    .collect(Collectors.groupingBy(property -> property.getValue()))        // Since we're looking for the highest-confidence instance of each value, group by value
                    .entrySet().stream()
                    .map(entry -> entry.getValue().stream()
                            .sorted((p1, p2) -> -Confidence.COMPARATOR.compare(p1.getConfidence(),p2.getConfidence()))    // Sort by ascending confidence (highest confidence first, which is lowest numeric value)
                            .findFirst()
                            .get()) //As long as there is at least one item then this won't be null.  If there are 0 items it should have been removed from the collection.
                    .collect(Collectors.toList());

                // Find the changes from the current cache line
                final Set<Property<?>> cacheLine = propertiesComputed.get(name);
                final Iterator<Property<?>> iterator = cacheLine.iterator();
                for(Property<?> prop = iterator.next(); iterator.hasNext(); prop = iterator.next()) {
                    if(!propertiesForLine.contains(prop)) {
                        iterator.remove();
                        onPropertyChanged.call(new PropertyEventArgs(name, prop, false));
                    } else {
                        propertiesForLine.remove(prop);
                    }
                }
                cacheLine.addAll(propertiesForLine);
                for(final Property<?> prop : propertiesForLine) {
                    onPropertyChanged.call(new PropertyEventArgs(name, prop, true));
                }
            }
        }
    }

    public boolean hasProperties(final String... propertyNames) {
        synchronized(propertiesComputed) {
            for(final String property : propertyNames) {
                if(!propertiesComputed.containsKey(property)){
                    return false;
                }
            }
            return true;
        }
    }

    public Object getBestPropertyValue(final String propertyName) {
        synchronized(propertiesComputed) {
            final Set<Property<?>> properties = this.propertiesComputed.get(propertyName);
            if(properties == null || properties.isEmpty()) {
                return null;
            }
            //TODO: Verify the order is high-to-low...  is is probably low-to-high
            final Confidence confidence = properties.stream().map(Property::getConfidence).sorted(Confidence.COMPARATOR).findFirst().get();
            return properties.stream().filter(property -> property.getConfidence().equals(confidence)).map(Property::getValue).findAny().get();
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeStartElement("Properties");

        for(final Map.Entry<String, Map<String, Set<Property<?>>>> entry : propertiesProvided.entrySet()) {
            target.writeStartElement("Source");
            target.writeAttribute("id", entry.getKey());

            for(final Map.Entry<String, Set<Property<?>>> property : entry.getValue().entrySet()) {
                target.writeStartElement("Property");
                target.writeAttribute("name", property.getKey());

                for(final Property<?> prop : property.getValue()) {
                    target.writeStartElement("Value");
                    Writer.writeObject(target, prop);
                    target.writeEndElement();
                }
                target.writeEndElement();
            }
            target.writeEndElement();
        }
        target.writeEndElement();
    }

    @Override
    public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
        String source = null;
        String property = null;

        while(reader.hasNext()) {
            final int typeNext = reader.next();
            final String name;
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    name = reader.getLocalName();
                    if(name.equals("Value")) {
                        final Object value = Loader.readObject(reader);
                        if(source != null && property != null && value != null && value instanceof Property) {
                            this.addProperties(source, property, (Property<?>)value);
                        }
                    } else if(name.equals("Property")) {
                        property = reader.getAttributeValue(null, "name");
                    } else if(name.equals("Source")) {
                        source = reader.getAttributeValue(null, "id");
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    name = reader.getLocalName();
                    if(name.equals("Property")) {
                        property = null;
                    } else if(name.equals("Source")) {
                        source = null;
                    } else if(name.equals("Properties")) {
                        return;
                    }
                    break;
            }
        }
    }
}
