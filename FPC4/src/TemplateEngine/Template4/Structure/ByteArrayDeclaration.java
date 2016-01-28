package TemplateEngine.Template4.Structure;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by BESTDOG on 11/16/2015.
 * A class scope variable declaration for any type which is to be compared as bytes.
 *
 * A class scope variable means it is append to a ClassTemplate outside of any method's body.
 */
public class ByteArrayDeclaration extends VariableDeclaration {

    ByteAdapter data;

    public ByteArrayDeclaration(String name, ByteAdapter data) {
        super(byte[].class, name, data.toString());
        this.data = data;
        this.setIsStatic(true);
    }

    public String getValue() {
        String string = data.getByteString();
        int len = string.length();
        return String.format("new byte[] { %s }", string.substring(1, len-1));
    }

    public interface ByteAdapter {
        byte[] getByteArray();
        String getString();
        default String getByteString() {
            return Arrays.toString(getByteArray());
        }
    }

    public static class HexAdapter implements ByteAdapter {
        String string;
        public HexAdapter( String string ) {
            this.string = string;
        }
        @Override
        public byte[] getByteArray() {
            int len = string.length();
            byte[] data = new byte[len /2];
            for(int i = 0; i < len; i+= 2) {
                data[i/2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                                    +Character.digit(string.charAt(i+1), 16));
            }
            return data;
        }
        @Override
        public String getString() {
            return string;
        }
    }
    public static class RawBytes extends HexAdapter {
        public RawBytes( String string ) {
            super( string.replace("[^\\p{XDigit}]","") );
        }
    }
    public static class StringAdapter implements ByteAdapter {
        String string;
        public StringAdapter( String string ) {
            this.string = string;
        }
        @Override
        public byte[] getByteArray() {
            return string.getBytes();
        }
        @Override
        public String getString() {
            return string;
        }
    }
    public static class IntAdapter implements ByteAdapter {
        String string;
        int value;
        public IntAdapter( String string ) {
            this.string = string;
            value = Integer.valueOf(this.string);
        }
        @Override
        public byte[] getByteArray() {
            return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
        }
        @Override
        public String getString() {
            return string;
        }
    }
}
