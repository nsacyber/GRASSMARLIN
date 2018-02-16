package grassmarlin.plugins.serialization;

import com.sun.istack.internal.NotNull;
import grassmarlin.RuntimeConfiguration;

import java.io.*;

public class PluginSerializableWrapper implements Serializable {
    private String pluginName;
    private Serializable wrapped;
    private String serializedString;


    public PluginSerializableWrapper(@NotNull String pluginName, @NotNull Serializable toWrap) {
        this.pluginName = pluginName;
        this.wrapped = toWrap;
    }

    public Serializable getWrapped() throws IllegalStateException{
        return this.wrapped;
    }
    
    private void writeObject(ObjectOutputStream stream) throws IOException{
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream wrappedOut = new ObjectOutputStream(bytesOut);

        wrappedOut.writeObject(wrapped);
        wrappedOut.close();
        bytesOut.close();

        String serializedString = javax.xml.bind.DatatypeConverter.printHexBinary(bytesOut.toByteArray());

        stream.writeUTF(pluginName != null ? pluginName : "");
        stream.writeObject(serializedString);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException{
        this.pluginName = stream.readUTF();
        this.serializedString = (String)stream.readObject();

        ClassLoader loader = RuntimeConfiguration.getLoader(pluginName);

        if (loader != null) {
            byte[] bytes = javax.xml.bind.DatatypeConverter.parseHexBinary(serializedString);
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
            CustomObjectInputStream objIn = new CustomObjectInputStream(bytesIn, loader);

            this.wrapped = (Serializable) objIn.readObject();
        } else {
            throw new ClassNotFoundException("ClassLoader for Plugin " + pluginName + " is null");
        }
    }

    @Override
    public String toString() {
        if (wrapped != null) {
            return this.wrapped.toString();
        } else {
            return null;
        }
    }
}
