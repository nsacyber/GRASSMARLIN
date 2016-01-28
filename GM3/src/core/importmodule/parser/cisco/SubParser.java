/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.importmodule.parser.cisco;

import core.importmodule.Trait;
import core.importmodule.TraitMap;
import core.topology.PhysicalNode;
import core.topology.TopologySource;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @param <R> CiscoReader the SubParser is called from.
 * @param <F> The file the SubParser is parsing.
 * @param <M> Usually a TraitMap to be filled with data.
 */
@FunctionalInterface
public interface SubParser<R extends CiscoReader, F extends File, M extends PhysicalNode> {

    public static final int IP4_BYTES = 4;
    public static final int IP6_BYTES = 8;

    boolean apply(R r, F f, M m);

    /**
     * Parses Ip4 and Ip6 IP addresses and places the associated Trait in the
     * provided map.
     *
     * @param ipString IP string from raw input.
     * @param map Map to populate.
     */
    public static void parseIPAddress(String ipString, TraitMap map) {
        String[] s = ipString.split("[^\\p{XDigit}]");
        if (s.length == IP6_BYTES) {
            /*ipv6*/
            throw new java.lang.IllegalArgumentException("Cannot parse IPv6 address " + ipString);
        } else if (s.length <= IP4_BYTES + 1) {
            /*ipv4 with cidr*/
            if (s.length == IP4_BYTES + 1) {
                map.parseThenPut(Trait.CIDR, s[4]);
            }
            int val;
            val = intVal(s[0]);
            val <<= 8;
            val |= intVal(s[1]);
            val <<= 8;
            val |= intVal(s[2]);
            val <<= 8;
            val |= intVal(s[3]);
            map.put(Trait.IPv4, val);
        }
    }

    /**
     * Removed punctuation from the begging or end of input if it does not
     * appear to be a sentence ending with a period.
     *
     * @param string Source string.
     * @return Formated input string.
     */
    public static String tidyString(String string) {
        if (string.matches(".*?\\.$")) {
            return string;
        } else {
            return string.replaceAll("(\\p{Punct}+\\z)|(\\A\\p{Punct}+)", "");
        }
    }

    public static int intVal(String s) {
        if (s.matches("\\p{XDigit}+")) {
            return Integer.parseInt(s, 16);
        } else {
            return Integer.parseInt(s);
        }
    }

    /**
     * @param line Source String.
     * @return First non-whitespace character sequence.
     */
    public static String getFirstWord(String line) {
        line = line.trim();
        int pos = locate(line, Character::isWhitespace);
        return pos == -1 ? line : line.substring(0, pos);
    }

    /**
     * @param line Source String.
     * @return First non-whitespace character sequence.
     */
    public static String getLastWord(String line) {
        line = line.trim();
        int pos = locateFromEnd(line, Character::isWhitespace);
        return line.substring(pos, line.length());
    }

    public static int locate(String line, IntPredicate testFunction) {
        int start = 0;
        int end = line.length();
        int incr = +1;
        return locate(line, start, end, incr, testFunction);
    }

    /**
     * Adds {@link Trait#INTERFACE_ID}, {@link Trait#INTERFACE_SLOT} and optionally
     * {@link Trait#INTERFACE_MODULE}. This expects a popular port numbering syntax
     * seen across multiple vendors. The typical idiom is as follows,
     *
     * 0/0/0 = motherboard-module/interface-slot/port-number
     *
     * @param moduleConfig String of the form 0/0/0 or 0/0.
     * @param map Map to populate.
     */
    public static void parseModuleconfig(String moduleConfig, TraitMap map) {

        int slot = 0;
        String moduleSeparatorCharacter = "/";
        String[] modSlots = moduleConfig.split("\\D");

        if (modSlots.length >= 2) {
            if (modSlots.length >= 3) {
                /* module-slot-iface */
                map.parseThenPut(Trait.INTERFACE_MODULE, modSlots[slot++]);
            }
            map.parseThenPut(Trait.INTERFACE_SLOT, modSlots[slot++]);
            map.parseThenPut(Trait.INTERFACE_ID, modSlots[slot++]);
            /* module-slot-iface-sub'iface */
            if( modSlots.length == 4 ) {
                map.parseThenPut(Trait.SUB_INTERFACE_ID, modSlots[slot++]);
            }
        } else {
            throw new java.lang.IndexOutOfBoundsException("Expected 2 or more module slots.");
        }

    }

    /**
     * Returns the rest of the string after the charSequence.
     *
     * @param line Source line.
     * @param charSequence CharSequence before the desired return string.
     * @return A substring of {@code line} or the original if the charSequence
     * is not contained.
     */
    public static String after(String line, String charSequence) {
        int pos = line.indexOf(charSequence);
        if (pos != -1) {
            line = line.substring(pos + charSequence.length());
        }
        return line.trim();
    }

    /**
     * Cuts the string from the beginning until the testFunction predicates.
     *
     * @param line Source line.
     * @param testFunction function that predicates the return position.
     * @return A substring starting from index 0 - where the testFunction
     * predicates, else empty-string.
     */
    public static String cut(String line, IntPredicate testFunction) {
        String s = "";
        int pos = SubParser.locate(line, testFunction);
        if (pos != -1) {
            s = line.substring(0, pos);
        }
        return s;
    }

    /**
     * Locates from end of the string.
     *
     * @param line Source line to test.
     * @param testFunction function that predicates the return position.
     * @return The index+1 where the testFunction predicates else -1 if
     * condition is never met.
     */
    public static int locateFromEnd(String line, IntPredicate testFunction) {
        int start = line.length() - 1;
        int end = -1;
        int incr = -1;
        return locate(line, start, end, incr, testFunction) + 1;
    }

    /**
     * Locates the index in the line where the testFunction predicates true.
     *
     * @param line Source line.
     * @param start Starting index.
     * @param end Ending index.
     * @param incr increment to reposition the index.
     * @param testFunction function the predicates the return position.
     * @return the index where testFunction first predicates, else -1 if
     * condition is never met.
     */
    public static int locate(String line, int start, int end, int incr, IntPredicate testFunction) {
        int ret = -1;
        for (; start != end; start += incr) {
            if (testFunction.test(line.codePointAt(start))) {
                ret = start;
                break;
            }
        }
        return ret;
    }

    /**
     * Converts a natural language list of numerals into a List of Integers.
     *
     * Sample input of "10, 20,20,303,50,1012-1014. 1 - 2, 5 ---5" would produce
     * [1, 2, 5, 10, 20, 20, 50, 303, 1012, 1013, 1014].
     *
     * @param string String containing natural list of Integers.
     * @return Naturally ordered list of integers within input, or empty list.
     */
    public static List<Integer> parseNumericList(String string) {
        ArrayList<Integer> list = new ArrayList<>();

        Pattern ranges = Pattern.compile("(?<Range>(\\d+\\s*-\\s*\\d+))");
        Matcher m = ranges.matcher(string);

        while (m.find()) {
            String range = m.group("Range");
            string = string.replace(range, "");
            parseNumericRange(range, list);
        }

        String[] numbers = string.split("\\D+");

        for (int i = 0; i < numbers.length; ++i) {
            String number = numbers[i];
            if (!number.isEmpty()) {
                list.add(Integer.valueOf(number));
            }
        }

        Collections.sort(list);
        return list;
    }

    /**
     * Parses a string of the form Number -> Non-number -> Number, into a list
     * of integers between the two numbers.
     *
     * @param range Input string such as 1-1, or 10-1000.
     * @return List of each integer between the two given in the range.
     */
    public static List<Integer> parseNumericRange(String range) {
        return parseNumericRange(range, new ArrayList<>());
    }

    /**
     * Parses a string of the form Number -> Non-number -> Number, into a list
     * of integers between the two numbers.
     *
     * @param range Input string such as 1-1, or 10-1000.
     * @param list List to fill with parsed integers.
     * @return List of each integer between the two given in the range.
     */
    public static List<Integer> parseNumericRange(String range, List<Integer> list) {
        String[] parts = range.trim().split("\\D+");//split on all non-numeric
        if (parts.length != 2) {
            throw new java.lang.IllegalArgumentException(String.format("Cannot parse range expression '%s'.", range));
        }
        int t0 = Integer.valueOf(parts[0]);
        int t1 = Integer.valueOf(parts[1]);
        int min = Math.min(t0, t1);
        int max = Math.max(t0, t1);
        while (min != max + 1) {
            list.add(min++);
        }
        return list;
    }

    /**
     * For CISCO config files only.
     *
     * If the first word which is the host-name of the interface appears to
     * contain the port-module numbering we can safely say it is a port and not
     * a vlan. There is a standard naming convention, and naming a vlan like a
     * port would be invalid.
     *
     * This is do to ACE or "Application Control Engine" which allows VLAN's to
     * interface with physical bridging through a virtual interface. ACE allows
     * for VLAN's to appear as interfaces for this reason. Though we are mapping
     * physical topology so this type of bridging must be stored within a
     * separate Trait vector.
     *
     * ACE ports do have IP addresses used in InterVlan routing supported by EMI
     * & SMI in switches v12.1+
     *
     * TLDR, ACE ports are not physical - BESTDOG
     *
     * BESTDOG - added a negative lookahead at the end of the expression to
     * avoid ambiguity between this and the {@link #isSubInterface(java.lang.String)
     * }.
     *
     * @param line Line containing a prospective port name.
     * @return True if the port name appears valid.
     */
    public static boolean isPhysicalPort(String line) {
        String word = SubParser.getFirstWord(line);
        /**
         * pattern looks for three characters in a row, either a digit or a
         * forward slash '/' with at least one forward-slash. See above comment
         * for why this pattern is reliable.
         */
        return word.matches("^\\D+(?=(.*?/+))(\\d|/){3,}.*");
    }

    /**
     * Sub interfaces are virtual interfaces in addition to real interfaces.
     * They are seen as a dotted decimal notation at the end of interface
     * notation "module-slot-port.sub".
     *
     * @param line Line where the first word is a port identifier.
     * @return True if the port-identifier appears to contain sub-interface
     * notation.
     */
    public static boolean isSubInterface(String line) {
        String word = SubParser.getFirstWord(line);
        return word.matches("^\\D+(?=(.*?/+))(\\d|/){3,}\\.\\d+");
    }

}
