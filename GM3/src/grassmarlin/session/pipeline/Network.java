package grassmarlin.session.pipeline;

import com.sun.istack.internal.NotNull;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.Property;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Networks are identified with a confidence value.  Unlike properties,
 * where only the highest-confidence values for a given key are accepted,
 * confidence on a network only affects conflict resolution--in case of
 * overlapping networks, only the highest-confidence network(s) will be
 * used.
 *
 * This means that every network must be tested for a conflict with each
 * other network.  This is accomplished by ordering the reported networks
 * by confidence, from highest (lowest numeric confidence) to lowest
 * (highest numeric value).  All of the networks at the highest confidence
 * are added to the set of effective networks.  All of the next-highest
 * confidence networks that do not conflict with an effective network are
 * then added to the set of effective networks.  This is a batch operation
 * so that conflicting second-tier networks don't prevent each others'
 * inclusion.  Ultimately this has O(N^2) as an upper bound, which should
 * be acceptable within the expected number of networks (N <<= 1000).
 * The conflict test is bidirectional--that is A and B conflict if A contains
 * B or B contains A
 */
public class Network extends Property<LogicalAddress<?>> {
    public Network(@NotNull final LogicalAddress<?> address, final int confidence) {
        super(address, confidence);
    }
    public Network(final XMLStreamReader source) throws XMLStreamException {
        super(source);
    }
}
