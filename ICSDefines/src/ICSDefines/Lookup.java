package ICSDefines;

import java.util.function.Function;

/**
 * Created by BESTDOG on 11/24/2015.
 */
public enum Lookup implements ByteFunctionSupplier {

    BACNET(),
    ENIPVENDOR(),
    ENIPDEVICE();

    private Function<byte[], String> byteFunction;

    Lookup() {
        this(Lookup::defaultFunction);
    }

    Lookup(Function<byte[], String> byteFunction) {
        this.setSupplier(byteFunction);
    }

    private static String defaultFunction(byte[] bytes) {
        return "No lookup function available.";
    }

    @Override
    public Function<byte[], String> getByteFunction() {
        return byteFunction;
    }

    @Override
    public void setSupplier(Function<byte[], String> byteFunction) {
        this.byteFunction = byteFunction;
    }


}
