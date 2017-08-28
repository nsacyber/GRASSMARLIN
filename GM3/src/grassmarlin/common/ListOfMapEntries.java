package grassmarlin.common;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public class ListOfMapEntries<K, V> extends ObservableListWrapper<Map.Entry<K, V>> {
    private final ObservableMap<K, V> map;

    public ListOfMapEntries(final ObservableMap<K, V> map) {
        super(new ArrayList<>());

        this.map = map;

        map.addListener(this::Handle_MapInvalidated);
    }

    private final Map.Entry<K, V> mapEntryFor(final K key) {
        final Optional<Map.Entry<K, V>> result = map.entrySet().stream().filter(entry -> entry.getKey().equals(key)).findAny();
        if(result.isPresent()) {
            return result.get();
        } else {
            return null;
        }
    }
    private final Map.Entry<K, V> listEntryFor(final K key) {
        final Optional<Map.Entry<K, V>> result = stream().filter(entry -> entry.getKey().equals(key)).findAny();
        if(result.isPresent()) {
            return result.get();
        } else {
            return null;
        }
    }

    private final void Handle_MapInvalidated(MapChangeListener.Change<? extends K, ? extends V> change) {
        if(change.wasAdded() && change.wasRemoved()) {
            //Replace the existing element
            //Look up the key in the current list for position and in the already-modified map for the value.
            this.set(this.indexOf(listEntryFor(change.getKey())), mapEntryFor(change.getKey()));
        } else if(change.wasAdded()) {
            //Treat as insertion; we don't care about order
            this.add(mapEntryFor(change.getKey()));
        } else if(change.wasRemoved()) {
            //Deletion; find the entry in the list.
            this.remove(listEntryFor(change.getKey()));
        }
    }
}
