package TemplateEngine.Data;

/**
 * Created by BESTDOG on 11/24/2015.
 * <p>
 * Interface for filter ethernet header data.
 */
public interface Filter<T> {

    static final Integer[] NILL = new Integer[0];

    default Integer[] getTransportProtocol(int proto) {
        return NILL;
    }

    default Integer[] getSrcPort(int src) {
        return NILL;
    }

    default Integer[] getDstPort(int dst) {
        return NILL;
    }

    default Integer[] getEthertype(int eth) {
        return NILL;
    }

    default Integer[] getAck(String ack) {
        return NILL;
    }

    default Integer[] getFlags(String flags) {
        return NILL;
    }

    default Integer[] getSeq(String seq) {
        return NILL;
    }

    default Integer[] getWindow(int window) {
        return NILL;
    }

    default Integer[] getTTL(int ttl) {
        return NILL;
    }

    default Integer[] getDsize(int dsize) {
        return NILL;
    }

    /**
     * @deprecated
     */
    default Integer[] DsizeInRange(int dsize) {
        return NILL;
    }

    /**
     * @deprecated
     */
    default Integer[] TTLInRange(int ttl) {
        return NILL;
    }

    FunctionalOperation<T> getOperation(int index);


}
