package TemplateEngine.Data;

import java.util.regex.Pattern;

/**
 * Created by BESTDOG on 11/24/2015.
 * <p>
 * Interface to be used for the buffer parameter.
 */
public interface PseudoBuffer {
    /**
     * @param index Index to get a byte from.
     * @return Byte at the provided index.
     */
    byte getByte(int index);

    /**
     * Retrieves a byte array with a maximum length and optionally reversed order.
     * @param index Start index to copy from.
     * @param array byte[] to write to.
     * @param offset Offset from the start index.
     * @param length maximum length of the copy, actual copied size may be less.
     * @param flip Reverses bytes if true, else NBO.
     * @return Reference to the copied byte array.
     */
    byte[] getByteArray(int index, byte[] array, int offset, int length, boolean flip);

    /**
     * Retrieves a byte array with a maximum length and always in NBO.
     * @param index Start index to copy from.
     * @param array byte[] to write to.
     * @param offset Offset from the start index.
     * @param length maximum length of the copy, actual copied size may be less.
     * @return Reference to the copied byte array.
     */
    byte[] getByteArray(int index, byte[] array, int offset, int length);

    /**
     * Creates a copy at the offset with the length provided, or less if end of buffer is reached.
     * @param offset Start index to copy from.
     * @param length Length in bytes to copy.
     * @return A new byte[] with the copied bytes.
     */
    byte[] getByteArray(int offset, int length);

    /**
     * Gets an int at the offset and optionally reverses byte order, see {@link Integer#BYTES}.
     * @param offset Start index to copy from.
     * @param isNetByteOrder Reverses bytes if true, else NBO.
     * @return Integer at the offset.
     */
    int getInt(final int offset, boolean isNetByteOrder);

    /**
     * Gets an int at the offset and optionally reverses byte order, see {@link Integer#BYTES}.
     * @param offset Start index to copy from.
     * @param length Length of bytes to read, may be less then 4 and converted to an int.
     * @param isNetByteOrder Reverses bytes if true, else NBO.
     * @return Integer at the offset.
     */
    int getInt(final int offset, int length, boolean isNetByteOrder);

    /**
     * Gets a String constructed from the bytes at the given offset.
     * @param offset Start index to copy from.
     * @param length Length of bytes to read.
     * @return UTF-8 string equivalent.
     */
    String getString(int offset, int length);

    /**
     * Returns the first string from the buffer which matches the Pattern.
     * @param pattern Pattern to search for.
     * @return First matching sub-string.
     */
    String match(Pattern pattern);

    /**
     * Returns the first string from the buffer within the bounds of offset and length which matches the Pattern.
     * @param pattern Pattern to search for.
     * @param offset Start index to search from.
     * @param length Length of bytes to read.
     * @return First matching sub-string.
     */
    String match(Pattern pattern, int offset, int length);

    /**
     * Searches for the byte sequence within the payload within the bounds of offset and length.
     * @param search byte[] to match.
     * @param offset Start index to search from.
     * @param length Length of bytes to read.
     * @return Beginning index in the buffer where the search sequence was a complete match, else -1 on failure.
     */
    int match(byte[] search, int offset, int length);

    /**
     * Checks if an integer is at a given byte offset.
     * @param offset Start index to search from.
     * @param compareTo Integer to compare to.
     * @return
     */
    boolean match(int offset, int compareTo);

    /**
     * Copies bytes `from an index `to an index with a maximum `length.
     * @param from Start index to search from.
     * @param to End index to search To.
     * @param length Maximum length, actual returned byte[] may be less then or equal to this length.
     * @return Copy of the bytes within the provided bounds.
     */
    byte[] extract(final int from, final int to, int length);

    /**
     * Same as {@link #extract(int, int, int)} but will flip the byte order.
     * @param from Start index to search from.
     * @param to End index to search To.
     * @param length Maximum length, actual returned byte[] may be less then or equal to this length.
     * @return Copy of the bytes within the provided bounds.
     */
    byte[] extractLittle(final int from, final int to, final int length);

    /**
     * @return Last addressable index within the buffer. ({@link #size()} - 1)
     */
    int end();

    /**
     * @return Size as returned by the buffer.
     */
    int size();

    /**
     * @return Entire buffer dumped to a hex-string.
     */
    String toHexdump();

}
