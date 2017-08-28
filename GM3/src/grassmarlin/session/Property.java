package grassmarlin.session;

import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

public class Property<T extends Serializable> implements XmlSerializable, Comparable<Property<?>> {
    private final T value;
    private final int confidence;
    private final AtomicInteger referenceCount;

    /**
     *
     * @param value
     * @param confidence The numeric confidence should fall somewhere on the following scale:
     *                   0: The user said it is true.
     *                   1: Multiple sources report this is true.
     *                   2: A single source reports this is true about another entity.
     *                   3: A source claims this about itself.
     *                   4: The value has been observed, but is easily faked / misinterpreted
     *                   5: The value has not been directly observed.
     */
    public Property(final T value, final int confidence) {
        this.value = value;
        this.confidence = confidence;
        this.referenceCount = new AtomicInteger(1);
    }

    public Property(final XMLStreamReader source) throws XMLStreamException {
        this.confidence = Integer.parseInt(source.getAttributeValue(null, "confidence"));
        while(source.next() != 1 && source.hasNext());  //Advance into Value tag
        if(source.getAttributeValue(null, "XmlSerializable") != null) {
            try {
                this.value = (T) Loader.readSerializableObject(source);
            } catch(IOException | ClassNotFoundException ex) {
                throw new XMLStreamException("Unable to read object from XML", ex);
            }
        } else {
            this.value = (T) Loader.readObject(source);
        }

        source.next();
        //TODO: Save/load reference count
        this.referenceCount = new AtomicInteger(1);
    }

    public T getValue() {
        return value;
    }
    public int getConfidence() {
        return confidence;
    }

    void addReference() {
        this.referenceCount.incrementAndGet();
    }
    int removeReference() {
        return this.referenceCount.decrementAndGet();
    }

    @Override
    public int compareTo(final Property<?> other) {
        //Consider first the ordering of the values, then use confidence as a tiebreaker.
        Integer valueMatch = null;
        if(other.value.getClass().equals(this.value.getClass())) {
            Class<?> clazz = this.value.getClass();
            while(valueMatch == null && clazz != Object.class) {
                try {
                    //Since the classes match, try to call compareTo
                    valueMatch = (Integer) clazz.getMethod("compareTo", clazz).invoke(this.value, other.value);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                    clazz = clazz.getSuperclass();
                }
            }
            if(valueMatch == null) {
                //There is no matching compareTo, so fall back to string compare
                valueMatch = this.value.toString().compareTo(other.value.toString());
            }
        } else {
            valueMatch = this.value.getClass().getName().compareTo(other.value.getClass().getName());
        }
        if(valueMatch == 0) {
            return this.confidence - other.confidence;
        } else {
            return valueMatch;
        }
    }
    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("confidence", Integer.toString(this.confidence));
        target.writeStartElement("Value");
        try {
            if(this.value instanceof XmlSerializable || this.value.getClass().getName().startsWith("java.lang.")) {
                Writer.writeObject(target, this.value);
            } else {
                Writer.writeSerializableObject(target, this.value);
            }
        } catch(IOException ex) {
            throw new XMLStreamException("Unable to write Serializable", ex);
        }
        target.writeEndElement();
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof Property)) {
            return false;
        }

        final Property<?> propOther = (Property<?>)other;

        if(propOther.confidence != this.confidence) {
            return false;
        }
        if(propOther.value == null || this.value == null) {
            return propOther.value == this.value;
        } else {
            return propOther.value.getClass().equals(this.value.getClass()) && propOther.value.equals(this.value);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%d)", value, confidence);
    }
}
