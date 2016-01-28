package ICSDefines;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by BESTDOG on 11/24/2015.
 */
public enum Content implements ByteFunctionSupplier {
    HEX(javax.xml.bind.DatatypeConverter::printHexBinary),
    INTEGER(Content::simpleToInt),
    RAW_BYTES(Arrays::toString),
    STRING(String::new);

    private Function<byte[], String> bytefunction;

    private static String simpleToInt(byte[] intBytes) {
        return new BigInteger(1, intBytes).toString();
    }

    Content() {
        this(Content::defaultFunction);
    }

    Content(Function<byte[], String> bytefunction) {
        this.setSupplier(bytefunction);
    }

    private static String defaultFunction(byte[] bytes) {
        return "No format function available.";
    }

    @Override
    public Function<byte[], String> getByteFunction() {
        return bytefunction;
    }

    @Override
    public void setSupplier(Function<byte[], String> byteFunction) {
        this.bytefunction = byteFunction;
    }
}
