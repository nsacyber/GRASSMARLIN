package grassmarlin.common;

import grassmarlin.plugins.internal.logicalview.Protocols;
import javafx.beans.binding.StringBinding;
import javafx.collections.ObservableSet;

import java.util.Set;
import java.util.stream.Collectors;

public class ProtocolSetExpression extends StringBinding {
    private final Set<Short> protocols;

    public ProtocolSetExpression(final ObservableSet<Short> protocols) {
        this.protocols = protocols;

        super.bind(protocols);
    }

    public String computeValue() {
        return this.protocols.stream().sorted().map(Protocols::toString).collect(Collectors.joining(", ", "", ""));
    }
}
