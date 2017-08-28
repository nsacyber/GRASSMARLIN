package grassmarlin.plugins.internal.fingerprint.processor;

import core.fingerprint3.Fingerprint;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 07.13.2015 - CC - New...
 */
public class FingerprintBuilder {
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    private static JAXBContext context;
    private static final String xsdFile = "/resources/xsd/fingerprint3.xsd";

    public static Fingerprint[] loadFingerprint(Path fingerprintPath) throws JAXBException, IOException {
        Fingerprint[] fingerprints = new Fingerprint[2];
        if(unmarshaller == null) {
            unmarshaller = getContext().createUnmarshaller();
        }
        try (BufferedReader reader = Files.newBufferedReader(fingerprintPath)) {
            StringWriter string = new StringWriter();
            BufferedWriter writer = new BufferedWriter(string);
            String line;
            while((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
            fingerprints[0] = (Fingerprint) unmarshaller.unmarshal(new StringReader(string.toString()));
            fingerprints[1] = (Fingerprint) unmarshaller.unmarshal(new StringReader(string.toString()));
        }

        return fingerprints;
    }

    public static Fingerprint saveFile(Fingerprint fingerprint, Path savePath) throws IOException, JAXBException {
        Fingerprint fp = null;
        if(marshaller == null) {
            marshaller = getContext().createMarshaller();
            marshaller.setProperty("jaxb.formatted.output", true);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL xsd = FingerprintBuilder.class.getResource(xsdFile);
            try {
                Schema schema = schemaFactory.newSchema(xsd);
                marshaller.setSchema(schema);
            } catch (SAXException se) {
                se.printStackTrace();
            }
        }

        try (BufferedWriter out = Files.newBufferedWriter(savePath)) {
            JAXBElement<Fingerprint> element = new JAXBElement<>(new QName("", "Fingerprint"), Fingerprint.class, fingerprint);
            StringWriter writer = new StringWriter();
            marshaller.marshal(element, writer);
            out.write(writer.toString());
            if(unmarshaller == null) {
                unmarshaller = getContext().createUnmarshaller();
            }
            fp = (Fingerprint)unmarshaller.unmarshal(new StringReader(writer.toString()));
        }

        return fp;
    }

    private static JAXBContext getContext() throws JAXBException{
        if (context == null) {
            context = JAXBContext.newInstance("core.fingerprint3");
        }

        return context;
    }

}
