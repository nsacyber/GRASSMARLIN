package grassmarlin.plugins.internal.fingerprint.processor;

import core.fingerprint3.ContentType;
import core.fingerprint3.Cursor;
import core.fingerprint3.Position;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.fingerprint.Reference;
import grassmarlin.plugins.internal.fingerprint.manager.payload.Endian;
import grassmarlin.plugins.internal.fingerprint.manager.payload.Test;
import grassmarlin.session.logicaladdresses.Ipv4;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import util.parser.CalcLexer;
import util.parser.CalcParser;

import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class containing all the payload function methods
 */
public class PayloadFunctions {

    private static final String workingDir = RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APPLICATION);
    private static final Path kbPath = Paths.get(workingDir, "data", "fingerprintingLookup");
    private static final Path bacnetPath = kbPath.resolve("BACnetVendors.htm");
    private static final Path enipDevicePath = kbPath.resolve("enipDevice.csv");
    private static final Path enipVendorPath = kbPath.resolve("enipVendors.csv");


    public static void anchorFunction(PayloadAccessor payload, CursorImpl cursor, Cursor cursorType, Position position, boolean relative, int offset) {
        if (relative) {
            offset += cursor.getMain();
        } else if (position != null) {
            switch (position) {
                case START_OF_PAYLOAD:
                    break;
                case END_OF_PAYLOAD:
                    offset = payload.size() + offset;
                    break;
                case CURSOR_START:
                    offset = cursor.getStart() + offset;
                    break;
                case CURSOR_MAIN:
                    offset = cursor.getMain() + offset;
                    break;
                case CURSOR_END:
                    offset = cursor.getEnd() + offset;
                    break;
            }
        }

        switch (cursorType) {
            case START:
                cursor.setStart(offset);
                break;
            case MAIN:
                cursor.setMain(offset);
                break;
            case END:
                cursor.setEnd(offset);
                break;
        }
    }

    public static void byteJumpFunction(PayloadAccessor payload, CursorImpl cursor, int offset, boolean relative, int bytes, Endian endian, int postOffset, String calc) {
        int location;
        if (payload.size() > offset) {
            if (bytes > 0) {
                location = payload.getInteger(offset, bytes, endian == Endian.getDefault()).intValue();
            } else {
                location = payload.getInt(offset, endian == Endian.getDefault());
            }

            if (null != calc && !calc.isEmpty()) {
                location = calculate(location, calc);
            }

            location += postOffset;

            if (payload.size() > location) {
                if (relative) {
                    cursor.forward(location);
                } else {
                    cursor.setMain(location);
                }
            }
        }
    }

    private static int calculate(int input, String expression) {
        CalcLexer lexer = new CalcLexer(new ANTLRInputStream(expression));
        CalcParser parser = new CalcParser(new CommonTokenStream(lexer));

        expression.replace("x", Integer.toString(input));

        return eval(parser.expr());
    }

    private static int eval(CalcParser.ExprContext context) {
        if (context.number() != null) {
            return Integer.parseInt(context.number().getText());
        } else if (context.BR_CLOSE() != null) {
            return eval(context.expr(0));
        } else if (context.MOD() != null) {
            return eval(context.expr(0)) % eval(context.expr(1));
        } else if (context.TIMES() != null) {
            return eval(context.expr(0)) * eval(context.expr(1));
        } else if (context.DIV() != null) {
            return eval(context.expr(0)) / eval(context.expr(1));
        } else if (context.PLUS() != null) {
            return eval(context.expr(0)) + eval(context.expr(1));
        } else if (context.MINUS() != null) {
            return eval(context.expr(0)) - eval(context.expr(1));
        } else {
            throw new IllegalStateException();
        }
    }

    public static boolean byteTestFunction(PayloadAccessor payload, CursorImpl cursor, Test operator, int testVal, boolean relative, int offset, int postOffset, int bytes, Endian endian) {
        if (relative) {
            offset = cursor.getMain() + offset;
        }

        boolean passes;
        if (bytes > 0) {
            passes = test(payload.getInteger(offset, bytes, endian == Endian.getDefault()).intValue(), operator, testVal);
        } else {
            passes = test(payload.getInt(offset, endian == Endian.getDefault()), operator, testVal);
        }
        if (postOffset != 0)
        cursor.forward(postOffset);

        return passes;
    }

    private static boolean test(int input, Test operator, int testVal) {
        switch (operator) {
            case GT:
                return input > testVal;
            case LT:
                return input < testVal;
            case GTE:
                return input >= testVal;
            case LTE:
                return input <= testVal;
            case AND:
                return (input & testVal) != 0;
            case OR:
                return (input | testVal) != 0;
            case EQ:
                return input == testVal;
            default:
                return false;
        }
    }


    public static Map.Entry<String, String> extractFunction(PayloadAccessor payload, CursorImpl cursor, String name, String fromString, String toString, int maxLength, Endian endian, ContentType convert, Lookup lookup) {

        Position fromPos;
        try {
            fromPos = fromString != null ? Position.valueOf(fromString) : null;
        } catch (IllegalArgumentException e) {
            fromPos = null;
        }
        int from;
        if (fromPos != null) {
           from = getIntegerPosition(payload, fromPos, cursor);
        } else {
            try {
                from = Integer.parseInt(fromString);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Position toPos;
        try {
         toPos = toString != null ? Position.valueOf(toString) : null;
        } catch (IllegalArgumentException e) {
            toPos = null;
        }
        int to;
        if (toPos != null) {
            to = getIntegerPosition(payload, toPos, cursor);
        } else {
            try {
                to = Integer.parseInt(toString);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        byte[] ext;
        if (endian == Endian.BIG) {
            ext = payload.extract(from, to, maxLength);
        } else {
            ext = payload.extractLittle(from, to, maxLength);
        }

        String value = null;
        if (convert != null) {
            if (ext.length > 0) {
                switch (convert) {
                    case HEX:
                        value = DatatypeConverter.printHexBinary(ext);
                        break;
                    case INTEGER:
                        value = new BigInteger(ext).toString();
                        break;
                    case RAW_BYTES:
                        value = Arrays.toString(ext);
                        break;
                    case STRING:
                        try {
                            value = new String(ext, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            value = null;
                        }
                        break;
                    case IP:
                        value = new Ipv4(new BigInteger(ext).longValue()).toString();
                        break;
                    case UUID:
                        ByteBuffer buffer;
                        if (endian == Endian.LITTLE) {
                            byte[] reverse = new byte[ext.length];
                            for (int i = 0; i < ext.length; i++) {
                                reverse[i] = ext[ext.length - (i + 1)];
                            }
                            buffer = ByteBuffer.wrap(reverse);
                            buffer.order(ByteOrder.LITTLE_ENDIAN);
                        } else {
                            buffer = ByteBuffer.wrap(ext);
                            buffer.order(ByteOrder.BIG_ENDIAN);
                        }
                        long high = 0L;
                        high |= ((long)buffer.getInt()) & 0x00000000FFFFFFFF;
                        high = high << Short.BYTES * 8;
                        high |= ((int)buffer.getShort()) & 0x0000FFFF;
                        high = high << Short.BYTES * 8;
                        high |= ((int)buffer.getShort()) & 0x0000FFFF;

                        long low = 0L;

                        for (int i = 0; i < 8; i++) {
                            low = low << Byte.BYTES * 8;
                            low |= ((short)buffer.get()) & 0x00FF;
                        }


                        UUID uuid = new UUID(high, low);
                        value = uuid.toString();
                        break;
                }
            }
        } else if (lookup != null) {
            int id = new BigInteger(ext).intValue();
            Reference ref = Reference.getInstance();
            switch (lookup) {
                case BACNET:
                    value = ref.getBacnetVendor(id);
                    break;
                case ENIPDEVICE:
                    value = ref.getEnipDevice(id);
                    break;
                case ENIPVENDOR:
                    value = ref.getEnipVendor(id);
                    break;
            }
        }

        Map.Entry<String, String> entry = null;
        if (value != null) {
            entry = new AbstractMap.SimpleEntry<>(name, value);
        }

        return entry;
    }

    private static int getIntegerPosition(PayloadAccessor payload, Position position, CursorImpl cursor) {
        int ret;
        switch (position) {
            case START_OF_PAYLOAD:
                ret = 0;
                break;
            case END_OF_PAYLOAD:
                ret = payload.size();
                break;
            case CURSOR_START:
                ret = cursor.getStart();
                break;
            case CURSOR_MAIN:
                ret = cursor.getMain();
                break;
            case CURSOR_END:
                ret = cursor.getEnd();
                break;
            default:
                ret = -1;
        }

        return ret;
    }

    public static boolean isDataAtFunction(PayloadAccessor payload, CursorImpl cursor, int offset, boolean relative) {
        if (relative) {
            offset += cursor.getMain();
        }

        return payload.size() > offset;
    }

    public static boolean matchFunction(PayloadAccessor payload, CursorImpl cursor, int depth, int offset, boolean relative,
                                        boolean noCase, String patternString, byte[] content, boolean move, Charset charset) {

        boolean matched = false;


        if (relative) {
            offset += cursor.getMain();
        }

        int length;
        // find the end point
        if (depth > 0) {
            length = Math.min(depth, payload.size() - offset);
        } else {
            length = payload.size() - offset;
        }

        if (patternString != null) {
            String string = new String(payload.getByteArray(offset, length), charset);
            Pattern pattern;
            if (noCase) {
                pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
            } else {
                pattern = Pattern.compile(patternString);
            }
            Matcher matcher;
            if (string != null && !string.isEmpty() && (matcher = pattern.matcher(string)).matches()) {
                if (move) {
                    cursor.setStart(matcher.start());
                    cursor.setEnd(matcher.end());
                }
                cursor.setMain(matcher.end());
                matched = true;
            }
        } else if (null != content && content.length > 0) {
            int location = payload.match(content, offset, length);
            if (location != -1) {
                if (move) {
                    cursor.setStart(location);
                    cursor.setEnd(location + content.length);
                }

                cursor.setMain(location);
                matched = true;
            }
        }

        return matched;
    }
}
