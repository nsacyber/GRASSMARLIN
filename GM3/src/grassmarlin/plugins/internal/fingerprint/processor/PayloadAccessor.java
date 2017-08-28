package grassmarlin.plugins.internal.fingerprint.processor;

import com.sun.istack.internal.NotNull;

import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class PayloadAccessor {

    private final ByteBuffer payload;
    private final int payloadStart;

    public PayloadAccessor(@NotNull ByteBuffer payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload can not be null");
        }
        this.payload = payload;
        this.payloadStart = payload.position();
    }

    public byte getByte(int i) {
        return payload.get(i);
    }

    /**
     *
     * @param index The starting point in the payload to start reading
     * @param bytes The array to write the data into
     * @param offset The starting point within the array to start writing
     * @param length The length of the data to be read
     * @param bigEndian The endianness of the data
     * @throws IndexOutOfBoundsException When the requested write would pass the end of the provided array
     * @throws BufferUnderflowException When the requested read would pass the end of the payload
     */
    public void getByteArray(int index, byte[] bytes, int offset, int length, boolean bigEndian) throws IndexOutOfBoundsException, BufferUnderflowException{
        if (offset + length >= bytes.length) {
            throw new IndexOutOfBoundsException("Provided array is not big enough");
        }
        if (this.payloadStart + index + length >= this.payload.limit()) {
            throw new BufferUnderflowException();
        }
        byte[] ret = new byte[length];
        if (payload != null) {
            for (int i = 0; i < length; i++) {
                ret[i] = this.payload.get(this.payloadStart + index + i);
            }

            if (bigEndian) {
                for (int i = 0; i < length; i++) {
                   bytes[offset + i] = ret[i];
                }
            } else {
                for (int i = 0; i < length; i++) {
                    bytes[offset + i] = ret[length - (i + 1)];
                }
            }
        }
    }

    /**
     * {@code bigEndian} defaults to {@code true}
     *
     * @see PayloadAccessor#getByteArray(int, byte[], int, int, boolean)
     */
    public void getByteArray(int index, byte[] bytes, int offset, int length) throws IndexOutOfBoundsException, BufferUnderflowException{
        this.getByteArray(index, bytes, offset, length, true);
    }

    /**
     * Create a new byte array and fill from the payload buffer
     *
     * @param index The offset within the payload to start reading
     * @param length The number of bytes to read
     * @return A byte array containing the data
     * @throws BufferUnderflowException When the read would pass the end of the payload buffer
     */
    public byte[] getByteArray(int index, int length) {
        if (this.payloadStart + index + length > this.payload.limit()) {
            throw new BufferUnderflowException();
        }
        byte[] ret = new byte[length];
        for (int i = 0; i < length; i++) {
            ret[i] = this.payload.get(this.payloadStart + index + i);
        }

        return ret;
    }


    public int getInt(int index, boolean bigEndian) throws BufferUnderflowException{
        return this.getInteger(index, Integer.BYTES, bigEndian).intValue();
    }


    /**
     * Reads specified number of bytes from the payload and converts to a numeric value
     *
     * @param index The offset within the payload buffer to start reading
     * @param bigEndian If the data is Big Endian
     * @return The integer read from the payload
     * @throws BufferUnderflowException When the read would pass the end of the payload buffer
     */
    public BigInteger getInteger(int index, int length, boolean bigEndian) throws BufferUnderflowException{
        BigInteger ret;
        byte[] bytes = this.getByteArray(index, length);
        if (!bigEndian) {
            reverseArray(bytes);
        }
        ret = new BigInteger(1, bytes);

        return ret;
    }

    /**
     * Locates the bytes within given length from the offset in the buffer.
     * @param search Bytes to search for.
     * @param offset Offset to start search at.
     * @param length Length to search within.
     * @return Location of the start of the matched sequence on success, else -1 on failure.
     */
    public int match(byte[] search, int offset, int length) {
        int ret = -1;
        if (payload != null) {
            int searchLength = search.length;
            if (searchLength > 0) {
                int limit = Math.min(offset + length, payload.limit()) - searchLength - offset;
                byte byte0 = search[0];
                for (int start = offset; start <= limit; ++start) {
                    if (getByte(start) == byte0) {
                        int i = 0;
                        for (; i < searchLength; ++i) {
                            if (search[i] != getByte(start + i)) {
                                break;
                            }
                        }
                        if (i == searchLength) {
                            ret = start;
                            break;
                        }
                    }
                }
            }
        }
        return ret;
    }

    public byte[] extract(int from, int to, int length) {
        byte[] ret = new byte[0];

        if (from >= 0 && from < payload.limit() - payloadStart && to >= 0 && to < payload.limit() - payloadStart) {
            int start = Math.min(from, to);
            int end = Math.min(start + length, Math.max(to, from));

            ret = getByteArray(start, end - start);
        }

        return ret;
    }

    public byte[] extractLittle(final int from, final int to, final int length) {
        byte[] bytes = extract(from, to, length);
        reverseArray(bytes);
        return bytes;
    }

    public int size() {
        if (this.payload != null) {
            return payload.limit() - this.payloadStart;
        } else {
            return 0;
        }
    }

    private void reverseArray (byte[] array) {
        for (int i = 0; i < array.length / 2; i++) {
            byte temp = array[array.length - i - 1];
            array[array.length - i - 1] = array[i];
            array[i] = temp;
        }
    }

}
