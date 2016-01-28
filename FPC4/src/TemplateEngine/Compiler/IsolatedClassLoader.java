package TemplateEngine.Compiler;

import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by BESTDOG on 11/23/2015.
 *
 * <pre>
 * Classloader for Fingerprints and Detect interfaces.
 * A classloader is created for each fingerprint so that it may be deleted.
 * Loading a class into the system class loader means that class cannot be unloaded without unloading every other class.
 * A class can only be unloaded by removing all references to it's classLoader, this is why we use {@link IsolatedClassLoader}.
 * </pre>
 */
public class IsolatedClassLoader extends ClassLoader {

    final HashMap<String, CompiledCode> units = new HashMap<>();
    final HashMap<String, Class> classes = new HashMap<>();

    public IsolatedClassLoader() {
        super(new ClassLoader() {});
    }

    /**
     * @return Map of all code units to their {@link CompiledCode#getName() } value.
     */
    public HashMap<String, CompiledCode> getUnits() {
        return units;
    }

    public void loadAll() throws ClassNotFoundException {
        loadAll(false);
    }

    /**
     * Loads all code units within this class loader ( typically just 1 )
     *
     * @param verbose logges errors if true, else false and silent.
     * @throws ClassNotFoundException If {@link #findClass(java.lang.String) }
     * is called on a class that has not been added.
     */
    public void loadAll(Boolean verbose) throws ClassNotFoundException {
        Set<String> names = getUnits().keySet();
        for (String s : names) {
            if (verbose) {
                Logger.getLogger(IsolatedClassLoader.class.getName()).log(Level.INFO, "Loading, " + s);
            }
            Class c = this.findClass(s);
            classes.put(s, c);
        }
    }

    /**
     * Adds a compiled code unit to this classloader.
     *
     * @param unit Compiled unit of code, its {@link CompiledCode#getName() }
     * will be the name used to load the class with {@link #findClass(java.lang.String)
     * }.
     */
    public void setCode(CompiledCode unit) {
        String name = unit.getName();
        units.put(name, unit);
    }

    /**
     * Clears all code units and loaded classes.
     */
    public void clear() {
        classes.clear();
        units.values().forEach(CompiledCode::delete);
        units.clear();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        Class c = classes.get(name);
        if (c != null) {
            return c;
        }
        CompiledCode unit = units.get(name);
        if (unit == null) {
            return super.findClass(name);
        }
        byte[] bytes = unit.getbytes();
        return defineClass(name, bytes, 0, bytes.length);
    }

    public boolean isPresent(String className) {
        return this.classes.containsKey(className);
    }
}
