package grassmarlin.plugins.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;

public class CustomObjectInputStream extends ObjectInputStream {
    /** table mapping primitive type names to corresponding class objects */
    private static final HashMap<String, Class<?>> primClasses
            = new HashMap<>(8, 1.0F);
    static {
        primClasses.put("boolean", boolean.class);
        primClasses.put("byte", byte.class);
        primClasses.put("char", char.class);
        primClasses.put("short", short.class);
        primClasses.put("int", int.class);
        primClasses.put("long", long.class);
        primClasses.put("float", float.class);
        primClasses.put("double", double.class);
        primClasses.put("void", void.class);
    }

    private ClassLoader loader;

    public CustomObjectInputStream(InputStream in, ClassLoader loader) throws IOException {
        super(in);

        this.loader = loader;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {

        String name = desc.getName();

        try {
            return Class.forName(name, false, this.loader);
        } catch (ClassNotFoundException ex) {
            Class<?> cl = primClasses.get(name);
            if (cl != null) {
                return cl;
            } else {
                throw ex;
            }
        }
    }
}
