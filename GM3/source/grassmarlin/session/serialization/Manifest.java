package grassmarlin.session.serialization;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.Version;
import grassmarlin.plugins.IPlugin;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class Manifest {
    private final HashMap<String, Boolean> plugins;
    private String version;
    private boolean isValid;

    private Manifest() {
        this.plugins = new HashMap<>();
        this.isValid = true;
    }

    public Manifest(final RuntimeConfiguration config) {
        this();

        this.version = Version.FILE_FORMAT_VERSION;

        for(IPlugin plugin : config.enumeratePlugins(IPlugin.class)) {
            this.plugins.put(RuntimeConfiguration.pluginNameFor(plugin.getClass()), plugin instanceof IPlugin.SessionEventHooks);
        }
    }

    public Manifest(final InputStream stream) {
        this();

        try {
            final XMLStreamReader readerXml = XMLInputFactory.newInstance().createXMLStreamReader(stream);
            while (readerXml.hasNext()) {
                if (readerXml.next() == XMLEvent.START_ELEMENT) {
                    if(readerXml.getLocalName().equals("plugin")) {
                        final String namePlugin = readerXml.getAttributeValue(null, "name");
                        final Boolean hasData = Boolean.parseBoolean(readerXml.getAttributeValue(null, "hasContent"));
                        this.plugins.put(namePlugin, hasData);
                    } else if(readerXml.getLocalName().equals("manifest")) {
                        this.version = readerXml.getAttributeValue(null, "ver");
                    }
                }
            }
        } catch(XMLStreamException ex) {
            this.isValid = false;
        }
    }

    public boolean satisfies(final Manifest requirements) {
        for(Map.Entry<String, Boolean> requirement : requirements.plugins.entrySet()) {
            if(requirement.getValue()) {
                if(!this.plugins.get(requirement.getKey())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean write(OutputStream out) {
        try {
            final XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(out);

            writer.writeStartDocument();

            writer.writeStartElement("manifest");
            writer.writeAttribute("ver", this.version);

            for(Map.Entry<String, Boolean> entry : this.plugins.entrySet()) {
                writer.writeStartElement("plugin");
                writer.writeAttribute("name", entry.getKey());
                writer.writeAttribute("hasContent", entry.getValue().toString());
                writer.writeEndElement();
            }

            writer.writeEndElement();

            writer.writeEndDocument();

            return true;
        } catch(XMLStreamException ex) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.plugins.hashCode() ^ this.version.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        if(other == null || !(other instanceof Manifest)) {
            return false;
        }

        final Manifest rhs = (Manifest)other;
        //If either is not valid, they are not equal (invalid manifest is never equal to anything, even itself)
        if(!isValid || !rhs.isValid) {
            return false;
        }

        return this.version.equals(rhs.version) && this.plugins.equals(rhs.plugins);
    }
}
