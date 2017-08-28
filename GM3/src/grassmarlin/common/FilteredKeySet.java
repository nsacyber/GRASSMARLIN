package grassmarlin.common;

import com.sun.javafx.collections.ObservableSetWrapper;
import javafx.beans.Observable;
import javafx.collections.ObservableMap;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilteredKeySet<K, V> extends ObservableSetWrapper<K> {
    private final Map<K, V> map;
    private final Function<V, Boolean> fnInclude;

    public FilteredKeySet(ObservableMap<K, V> source, Function<V, Boolean> fnInclude) {
        super(new LinkedHashSet<>(source.entrySet().stream().filter(entry -> fnInclude.apply(entry.getValue())).map(entry -> entry.getKey()).collect(Collectors.toList())));

        this.map = source;
        this.fnInclude = fnInclude;

        source.addListener(this::Handle_MapInvalidated);
    }

    private void Handle_MapInvalidated(final Observable observable) {
        this.map.keySet().forEach(element -> {
            if(fnInclude.apply(map.get(element))) {
                this.add(element);
            } else {
                this.remove(element);
            }
        });
    }
}
