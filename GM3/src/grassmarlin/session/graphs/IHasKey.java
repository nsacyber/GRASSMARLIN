package grassmarlin.session.graphs;

public interface IHasKey {
    /**
     * Return a string representation derived from fixed constructor inputs that will be consistent across serialization/deserialization cycles.
     * @return
     */
    String getKey();
}
