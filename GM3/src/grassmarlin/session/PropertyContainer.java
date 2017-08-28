package grassmarlin.session;

import grassmarlin.Event;
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

public class PropertyContainer implements XmlSerializable {

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
        public int getConfidence() {
            return this.property.getConfidence();
        }
        public Property<?> getProperty() {
            return this.property;
        }
        public boolean isAdded() {
            return this.added;
        }

        public PropertyContainer getContainer() {
            return PropertyContainer.this;
        }
    }

    // Events
    /**
     * This event is called whenever a property value is added or removed.
     */
    public final Event<PropertyEventArgs> onPropertyChanged;

    //Data Members
    /**
     * This is the cache of properties that are presently the dominant properties; the highest priority (lowest number) for each named field across all provided properties and ancestor containers.
     */
    private final Map<String, Set<Property<?>>> computedProperties;
    /**
     * The tree of source-property-value tuples applied to this PropertyContainer.
     */
    private final Map<String, Map<String, Set<Property<?>>>> providedProperties;
    /**
     * A PropertyContainer contains all the properties of all its ancestors.
     */
    private final Set<PropertyContainer> ancestors;

    // Constructors
    protected PropertyContainer(final Event.IAsyncExecutionProvider eventThread) {
        this.computedProperties = new HashMap<>();
        this.providedProperties = new HashMap<>();
        this.ancestors = new HashSet<>();

        this.onPropertyChanged = new Event<>(eventThread);
    }
    public PropertyContainer() {
        this(Event.PROVIDER_IN_THREAD);
    }

    /**
     * This overload should only be used as part of serialization, where we need to extract a PropertyContainer and then integrate it into another object.
     * @param eventThread
     * @param base
     */
    protected PropertyContainer(final Event.IAsyncExecutionProvider eventThread, final PropertyContainer base) {
        this.computedProperties = base.computedProperties;
        this.providedProperties = base.providedProperties;
        //TODO: ancestors isn't part of serialization yet, so this may not be correct
        this.ancestors = new HashSet<>(base.ancestors);

        this.onPropertyChanged = new Event<>(eventThread);
    }

    /**
     * Makes the property change announcement for each property, as if all properties were just set. (Used when loading, since that bypasses the normal event hooks)
     */
    public void reannounceProperties() {
        for(final Map.Entry<String, Set<Property<?>>> entry : this.computedProperties.entrySet()) {
            for(final Property<?> property : entry.getValue()) {
                this.onPropertyChanged.call(new PropertyEventArgs(entry.getKey(), property, true));
            }
        }
    }

    //Accessors
    public Map<String, Set<Property<?>>> getProperties() {
        return computedProperties;
    }
    public Map<String, Set<Property<?>>> getPropertiesForSource(final String source) {
        return this.providedProperties.get(source);
    }

    //Ancestor Management
    public void addAncestor(final PropertyContainer ancestor) {
        //We need to synchronize ancestors and may need to synchronize on computedProperties--if we need both, we have to do computedProperties first
        synchronized(computedProperties) {
            synchronized(ancestors) {
                if(ancestors.add(ancestor)) {
                    ancestor.onPropertyChanged.addHandler(this.handlerAncestorPropertyChanged);
                    //Process the initial state into the cache / fire events.
                    for(final Map.Entry<String, Set<Property<?>>> entry : ancestor.computedProperties.entrySet()) {
                        for(final Property<?> property : entry.getValue()) {
                            addPropertyToCache(entry.getKey(), property.getValue(), property.getConfidence());
                        }
                    }
                }
            }
        }
    }

    public void removeAncestor(final PropertyContainer ancestor) {
        //We need to synchronize ancestors and may need to synchronize on computedProperties--if we need both, we have to do computedProperties first
        synchronized(computedProperties) {
            if(ancestors.remove(ancestor)) {
                ancestor.onPropertyChanged.removeHandler(this.handlerAncestorPropertyChanged);

                for(final Map.Entry<String, Set<Property<?>>> entry : ancestor.computedProperties.entrySet()) {
                    for(final Property<?> property : entry.getValue()) {
                        removePropertyFromCache(entry.getKey(), property.getValue(), property.getConfidence());
                    }
                }
            }
        }
    }

    //Assignment
    protected void addPropertyToCache(final String name, final Serializable value, final int confidence) {
        synchronized(computedProperties) {
            final Set<Property<?>> values = computedProperties.get(name);
            if(values == null) {
                //We can safely add the new set with this property and announce it.
                final HashSet<Property<?>> valuesNew = new HashSet<>();
                final Property<?> propertyNew = new Property<>(value, confidence);
                valuesNew.add(propertyNew);
                computedProperties.put(name, valuesNew);
                onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
            } else  if(values.isEmpty()) {
                //We don't need to create the set, we can just add and announce the property.
                final Property<?> propertyNew = new Property<>(value, confidence);
                values.add(propertyNew);
                onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
            } else {
                //Iterate over the properties and...
                //  If it is a lower confidence, remove it, announcing the change
                //  If it is a higher confidence, terminate early (we won't be adding and removal should be unnecessary).
                //  If it is the same confidence and value, increment the reference count and abort early (again, no further action should be necessary.
                // If we complete the loop, we can add and announce this property.
                final Iterator<Property<?>> iterator = values.iterator();
                while(iterator.hasNext()) {
                    final Property<?> prop = iterator.next();
                    if(prop.getConfidence() > confidence) {
                        iterator.remove();
                        onPropertyChanged.call(new PropertyEventArgs(name, prop, false));
                    } else if(prop.getConfidence() < confidence) {
                        return;
                    } else {
                        if(prop.getValue().equals(value)) {
                            prop.addReference();
                            return;
                        }
                    }
                }
                final Property<?> propertyNew = new Property<>(value, confidence);
                values.add(propertyNew);
                onPropertyChanged.call(new PropertyEventArgs(name, propertyNew, true));
            }
        }
    }
    protected void removePropertyFromCache(final String name, final Object value, final int confidence) {
        synchronized(computedProperties) {
            //We're going to cut a few corners here and assume that, if we're removing a property from the cache, it is actually there.
            final Set<Property<?>> values = computedProperties.get(name);
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
            synchronized(providedProperties) {
                final int confidenceTarget = Math.min(
                        providedProperties.values().stream()
                            .map(map -> map.get(name))
                            .filter(set -> set != null)
                            .flatMap(set -> set.stream())
                            .mapToInt(property -> property.getConfidence())
                            .min().orElse(10),
                        ancestors.stream()
                                .map(container -> container.computedProperties.get(name))
                                .filter(set -> set != null)
                                .flatMap(set -> set.stream())
                                .mapToInt(property -> property.getConfidence())
                                .min().orElse(10)
                );
                if(confidenceTarget == 10) {
                    //There are no relevant properties, so we're done--the cache list should already be correct.
                    return;
                } else {
                    final HashSet<Property<?>> properties = new HashSet<>();
                    properties.addAll(providedProperties.values().stream()
                            .map(map -> map.get(name))
                            .filter(set -> set != null)
                            .flatMap(set -> set.stream())
                            .filter(property -> property.getConfidence() == confidenceTarget)
                            .collect(Collectors.toList()));
                    properties.addAll(ancestors.stream()
                            .map(container -> container.computedProperties.get(name))
                            .filter(set -> set != null)
                            .flatMap(set -> set.stream())
                            .filter(property -> property.getConfidence() == confidenceTarget)
                            .collect(Collectors.toList())
                    );
                    //Find the changes from the current cache line
                    final Set<Property<?>> cacheLine = computedProperties.get(name);
                    final Iterator<Property<?>> iterator = cacheLine.iterator();
                    while(iterator.hasNext()) {
                        final Property<?> prop = iterator.next();
                        if(!properties.contains(prop)) {
                            iterator.remove();
                            onPropertyChanged.call(new PropertyEventArgs(name, prop, false));
                        } else {
                            properties.remove(prop);
                        }
                    }
                    cacheLine.addAll(properties);
                    for(final Property<?> prop : properties) {
                        onPropertyChanged.call(new PropertyEventArgs(name, prop, true));
                    }
                }
            }
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

    public void addProperties(final String source, final String name, final Collection<Property<?>> values) {
        synchronized(computedProperties) {
            synchronized(ancestors) {
                synchronized (providedProperties) {
                    if (!providedProperties.containsKey(source)) {
                        providedProperties.put(source, new HashMap<>());
                    }

                    final Map<String, Set<Property<?>>> propertiesOfSource = providedProperties.get(source);
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

    public void removeProperties(final String source, final String name, final Property<?>... values) {
        removeProperties(source, name, Arrays.asList(values));
    }
    public void removeProperties(final String source, final String name, final Collection<Property<?>> values) {
        synchronized(computedProperties) {
            synchronized (ancestors) {
                synchronized (providedProperties) {
                    if (!providedProperties.containsKey(source)) {
                        //If there is no collection of properties, removing will have no effect anyway.
                        return;
                    }

                    final Map<String, Set<Property<?>>> propertiesOfSource = providedProperties.get(source);
                    if (!propertiesOfSource.containsKey(name)) {
                        //Again, there will be no effect from removal.
                        return;
                    }

                    final Set<Property<?>> properties = propertiesOfSource.get(name);
                    for (final Property<?> property : values) {
                        if (properties.remove(property)) {
                            removePropertyFromCache(name, property.getValue(), property.getConfidence());
                        }
                    }
                }
            }
        }
    }

    public void setProperties(final String source, final String name, final Property<?>... values) {
        setProperties(source, name, Arrays.asList(values));
    }
    public void setProperties(final String source, final String name, final Collection<? extends Property<?>> values) {
        synchronized (computedProperties) {
            synchronized (ancestors) {
                synchronized (providedProperties) {
                    if (!providedProperties.containsKey(source)) {
                        providedProperties.put(source, new HashMap<>());
                    }

                    final Map<String, Set<Property<?>>> propertiesOfSource = providedProperties.get(source);
                    if (!propertiesOfSource.containsKey(name)) {
                        propertiesOfSource.put(name, new HashSet<>());
                    }

                    final Set<Property<?>> properties = propertiesOfSource.get(name);

                    final Collection<Property<?>> newProperties = new LinkedList<>(values);
                    newProperties.removeAll(properties);
                    final Collection<Property<?>> removedProperties = new LinkedList<>(properties);
                    removedProperties.removeAll(values);

                    for (final Property<?> property : newProperties) {
                        if (properties.add(property)) {
                            addPropertyToCache(name, property.getValue(), property.getConfidence());
                        }
                    }
                    for (final Property<?> property : removedProperties) {
                        if (properties.remove(property)) {
                            removePropertyFromCache(name, property.getValue(), property.getConfidence());
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        if(computedProperties != null) {
            for (Map.Entry<String, Set<Property<?>>> entry : computedProperties.entrySet()) {
                if(!entry.getValue().isEmpty()) {
                    //Do not replace the "property -> property.toString()" with a method reference; Property may be used as a base class in future implementations and that sort of reference has caused issues before.
                    final String txtProperties = entry.getValue().stream().map(property -> property.toString()).collect(Collectors.joining("\n    ", "\n    ", ""));
                    result.append("  ").append(entry.getKey()).append(txtProperties);
                } else {
                    result.append("  ").append(entry.getKey()).append(" (empty)");
                }
            }
        }

        return result.toString();
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        for(final Map.Entry<String, Map<String, Set<Property<?>>>> entry : providedProperties.entrySet()) {
            target.writeStartElement("Source");
            target.writeAttribute("id", entry.getKey());

            for(final Map.Entry<String, Set<Property<?>>> property : entry.getValue().entrySet()) {
                target.writeStartElement("PropertySet");
                target.writeAttribute("name", property.getKey());

                for(final Property<?> prop : property.getValue()) {
                    target.writeStartElement("Property");
                    Writer.writeObject(target, prop);
                    target.writeEndElement();
                }
                target.writeEndElement();
            }
            target.writeEndElement();
        }
    }

    @Override
    public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
        String source = null;
        String property = null;

        final String tagRoot = reader.getLocalName();

        while(reader.hasNext()) {
            final int typeNext = reader.next();
            final String name;
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    name = reader.getLocalName();
                    if(name.equals("Property")) {
                        final Object value = Loader.readObject(reader);
                        if(source != null && property != null && value != null && value instanceof Property) {
                            this.addProperties(source, property, (Property<?>)value);
                        }
                    } else if(name.equals("PropertySet")) {
                        property = reader.getAttributeValue(null, "name");
                    } else if(name.equals("Source")) {
                        source = reader.getAttributeValue(null, "id");
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    name = reader.getLocalName();
                    if(name.equals("PropertySet")) {
                        property = null;
                    } else if(name.equals("Source")) {
                        source = null;
                    } else if(name.equals(tagRoot)) {
                        return;
                    }
                    break;
            }
        }
    }
}
