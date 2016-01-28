package ICSDefines;

import java.util.function.Function;

/**
 *
 * @author BESTDOG
 */
public interface ByteFunctionSupplier {

    Function<byte[],String> getByteFunction();

    void setSupplier(Function<byte[], String> byteFunction);

    default String toString(byte[] bytes) {
        return this.getByteFunction().apply(bytes);
    }
}

