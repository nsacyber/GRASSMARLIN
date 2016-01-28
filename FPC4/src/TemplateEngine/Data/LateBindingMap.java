package TemplateEngine.Data;

import ICSDefines.ByteFunctionSupplier;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 *
 * @author BESTDOG Map which can be given a Key-Value pair or a
 * Key-Byte[]-ToString method tuple to store the data quickly and convert is
 * later.
 *
 * The default underlying map is a
 * {@link java.util.concurrent.ConcurrentHashMap}.
 */
public class LateBindingMap extends ConcurrentHashMap<String, String> {

    ConcurrentHashMap<String, byte[]> unboundMap;
    ConcurrentHashMap<String, ByteFunctionSupplier> byteFunctions;

    public LateBindingMap() {
        super();
        unboundMap = new ConcurrentHashMap<>();
        byteFunctions = new ConcurrentHashMap<>();
    }

    public void put(String key, byte[] rawData, ByteFunctionSupplier byteFunction) {
        this.byteFunctions.put(key, byteFunction);
        unboundMap.put(key, rawData);
    }

    /**
     * If this map contains unbound entries.
     *
     * @return True if unbound entries exist, else false.
     */
    public boolean isUnbound() {
        return !isBound();
    }

    /**
     * If this map contains no unbound entries.
     *
     * @return True is no unbound entries exist, else false.
     */
    public boolean isBound() {
        return unboundMap.isEmpty();
    }

    /**
     * Attempts to bind all unbound entries by their assigned binding function.
     */
    public void bindAll() {
        if (isUnbound()) {
            unboundMap.entrySet().removeIf(this::bindEntry);
        }
    }

    /**
     * Binds a single entry unbound entry by its {@link #byteFunctions}
     * ByteFunctionSupplier.
     *
     * @param e Entry to bind.
     * @return Always true.
     */
    private boolean bindEntry(Entry<String, byte[]> e) {
        String key = e.getKey();
        byte[] oldValue = e.getValue();
        String newValue = this.byteFunctions.get(key).toString(oldValue);
        this.put(key, newValue);
        return true;
    }

    public void putAll(LateBindingMap other) {
        super.putAll(other);
        this.byteFunctions.putAll(other.byteFunctions);
        this.unboundMap.putAll(other.unboundMap);
    }

    public Stream<Entry<String, String>> stream() {
        bindAll();
        return this.entrySet().stream();
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super String> action) {
        bindAll();
        super.forEach(action);
    }

    @Override
    public void clear() {
        super.clear();
        unboundMap.clear();
        byteFunctions.clear();
    }

}
