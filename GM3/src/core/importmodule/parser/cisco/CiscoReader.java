/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.importmodule.parser.cisco;

import core.Core;
import core.importmodule.CiscoImport;
import core.topology.PhysicalNode;
import core.types.LogEmitter;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An import module for Cisco show-commands.
 *
 * We support, 1. show interfaces (CiscoInterfaceParse.java) 2. show
 * running-config (CiscoRCParse.java) 3. show arp (CiscoArpParse.java)
 *
 * No sub-commands are accepted since they are radically different and
 * impossible for a parser to accommodate. Example: see the difference between,
 * 1. 'running-config' -
 * www.cisco.com/c/en/us/td/docs/security/asa/asa82/command/reference/cmd_ref/s5.html#wp1398263
 * 2. 'running-config aaa' -
 * www.cisco.com/c/en/us/td/docs/security/asa/asa82/command/reference/cmd_ref/s5.html#wp1398501
 *
 * Parsers ARP table parser {@link CiscoArpParse}, parses a table of ARP data.
 * Command splitter {@link CiscoCommandSplitter}, splits a single file into
 * several valid parse-able and discrete parts. Interface parser
 * {@link CiscoInterfaceParse}, parses the 'show interfaces' command, a list of
 * interfaces, vlans, and sub-interfaces. Running-config parser
 * {@link CiscoRCParse}, parses the running config with similar data to the
 * {@link CiscoInterfaceParse}. V12 MAT parser
 * {@link CiscoV12MacAddressTableParse}, which handles v12.0-4 mac address
 * tables (MAT). V15 MAT parser {@link CiscoV15MacAddressTableParse}, which
 * extends the v12 parser with different table information. Version parser
 * {@link CiscoVersionParse}, which parses version, software-image, model,
 * serial numbers for all versions.
 *
 * {@link SubParser} is an interface with very useful and efficient parse-helper
 * methods.
 *
 * BESTDOG - 9/15/15 - init BESTDOG - 9/17/15 - encountered issues in test 4.
 * Functionality is temporarily unavailable.
 */
public class CiscoReader implements BiFunction<CiscoImport, List<PhysicalNode>, Boolean> {

    /**
     * Enables debug logging if true.
     */
    public static boolean debug = false;
    /**
     * A Map of complete commands to their associated methods. A method will be
     * selected by the longest matching string of a key to the command parsed
     * from the input file. Example: 'SwitchName1#show interf'(raw) =
     * 'interf'(parsed) = 'interfaces'(matched)
     */
    private static final Map<String, SubParser> SUPPORT_MAP;
    /**
     * Key for a dummy parse method, given to unsupported commands This is a key
     * for an associated parse method, see {@link #SUPPORT_MAP}.
     */
    private static final String DUMMY_KEY = "DUMMY_KEY";
    /**
     * 'interfaces' key, IFA is a supported show command. This is a key for an
     * associated parse method, see {@link #SUPPORT_MAP}.
     */
    private static final String IFA_KEY = "interfaces";
    /** key also used to parse interfaces command */
    private static final String IFA_STATIC_KEY = "interfaces static";
    /**
     * 'arp' key, ARP is a supported show command. This is a key for an
     * associated parse method, see {@link #SUPPORT_MAP}.
     */
    private static final String ARP_KEY = "arp";
    /**
     * An older version of the {@link #ARP_KEY} command, is parse-able by the
     * same sub-parser.
     */
    private static final String IP_ARP_KEY = "ip arp";
    /**
     * 'running-config' RCONF is a supported show command. This is a key for an
     * associated parse method, see {@link #SUPPORT_MAP}.
     */
    private static final String RCONF_KEY = "running-config";
    /**
     * 'mac address-table' or MAT table parser. Should support versions 12.1+
     */
    private static final String MAT_V15_KEY = "mac address-table";
    /**
     * 'mac-address-tabke' is a pre 12.1 MAT.
     */
    private static final String MAT_V12_KEY = "mac-address-table";
    /**
     * 'version' a command that has software version information.
     */
    private static final String VERSION_KEY = "version";

    static {
        HashMap<String, SubParser> map = new HashMap<>();
        map.put(RCONF_KEY, new CiscoRCParse()::apply);
        map.put(ARP_KEY, new CiscoArpParse()::apply);
        map.put(IP_ARP_KEY, new CiscoArpParse()::apply);
        map.put(IFA_KEY, new CiscoInterfaceParse()::apply);
        map.put(IFA_STATIC_KEY, new CiscoInterfaceParse()::apply);
        map.put(MAT_V15_KEY, new CiscoV15MacAddressTableParse()::apply);
        map.put(MAT_V12_KEY, new CiscoV12MacAddressTableParse()::apply);
        map.put(VERSION_KEY, new CiscoVersionParse()::apply);
        map.put(DUMMY_KEY, CiscoReader::dummyParse);
        SUPPORT_MAP = Collections.unmodifiableMap(map);
    }

    public static LogEmitter emitter = LogEmitter.factory.get();

    /**
     * When the command string from the file is longer then the key of
     * {@link #SUPPORT_MAP} is means that is is a sub-command. We cannot process
     * it, and return this method from
     * {@link #getParseMethod(File, String)}  }.
     *
     * @param splitFile File containing a split-command from a successful {@link CiscoCommandSplitter#getSplitMap()
     * }.
     * @param root A TraitMap root which will be ignored in this dummy.
     * @return false, this method will log an error.
     */
    static final Boolean dummyParse(CiscoReader reader, File splitFile, PhysicalNode root) {
        return false;
    }

    @Override
    public Boolean apply(final CiscoImport item, final List<PhysicalNode> data) {
        Objects.nonNull(item);
        Objects.nonNull(data);

        try (CiscoCommandSplitter splitter = new CiscoCommandSplitter(item)) {

            String deviceName = splitter.getDeviceName();
            Map<String, File> commandFiles = splitter.getSplitMap();

            item.setMaxEntry(commandFiles.size());
            checkDeviceName(deviceName, item);
            checkCommandMap(commandFiles, item);

            PhysicalNode node = new PhysicalNode( deviceName );
            node.setSourceFile(item);
            data.add(node);
            
            commandFiles.forEach((command, splitFile) -> {
                SubParser parseMethod = getParseMethod(item, command);

                if (parseMethod != null) {
                    try {
                        
                        if (!parseMethod.apply(this, splitFile, node)) {
                            logMalformedCommand(command, item.getName());
                            item.log(item, Core.ALERT.DANGER, String.format("Issue parsing %s in %s.", command, item.getName()));
                        } else {
                            node.addCommand(command);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(CiscoReader.class.getName()).log(Level.SEVERE, null, ex);
                        log(Core.ALERT.DANGER, "Command '", command, "' in '", item.getName(), "' failed to parse.");
                    }
                } else {
                    logUnsupportedCommand(command);
                }

                item.reportEntryComplete();
            });

        } catch (Exception ex) {
            Logger.getLogger(CiscoReader.class.getName()).log(Level.SEVERE, null, ex);
            log(Core.ALERT.DANGER, "Unknown error occured", item.getName(), ". Import Failed.");
        }

        return !data.isEmpty();
    }

    /**
     * Located the parse method for a command based on the longest matching
     * string in {@link #SUPPORT_MAP}.
     *
     * @param source Source file of the command text.
     * @param commandText The parsed text of a user-entered command from the
     * file when split.
     * @return The parse method to run, this SHOULD never be null.
     */
    private SubParser getParseMethod(File source, String commandText) {
        SubParser method = CiscoReader::dummyParse;

        String commandKey = longestMatchingSubstring(source, commandText, CiscoReader.SUPPORT_MAP.keySet());

        if (DUMMY_KEY.equals(commandKey) || commandKey == null) {
            logUnsupportedCommand(commandText);
        } else if (commandText.length() > commandKey.length()) {
            logUnsupportedSubCommand(source.getPath(), commandText, commandKey);
        } else {
            method = CiscoReader.SUPPORT_MAP.get(commandKey);
        }

        return method;
    }

    private void checkDeviceName(String deviceName, File source) throws Exception {
        if (deviceName.isEmpty()) {
            throw new Exception(String.format("Could not determine device-name within file \"%s\"", source.getName()));
        }
    }

    private void checkCommandMap(Map commands, File source) throws Exception {
        if (commands.isEmpty()) {
            throw new Exception(String.format("Could not recognize commands within \"%s\"", source.getName()));
        }
    }

    protected void subParserLog(Exception ex) {
        Logger.getLogger(CiscoReader.class.getName()).log(Level.SEVERE, null, ex);
        log(Core.ALERT.DANGER, "Import Failed.");
    }

    protected void logUnsupportedCommand(String commandText) {
        log(String.format("Unsupported command, \"%s\".", commandText), Core.ALERT.DANGER);
    }

    protected void log(Core.ALERT alert, String... argv) {
        if (argv.length == 0) {
            log(argv[0], alert);
        } else {
            StringBuilder b = new StringBuilder();
            b.append(argv[0]);
            for (int i = 1; i < argv.length; ++i) {
                b.append(" ").append(argv[i]).append(" ");
            }
            log(b.toString(), alert);
        }
    }

    protected void log(String msg, Core.ALERT alert) {
        if (debug) {
            System.out.println(alert + " : " + msg);
        }
        CiscoReader.emitter.emit(this, alert, msg);
    }

    protected void logUnsupportedSubCommand(String path, String found, String expected) {
        log(Core.ALERT.DANGER, "Cannot process subcommand in '", path, "'. Found '", found, "' expected '", expected, "'.");
    }

    /**
     * Log when a command parses successfully but no data was extract.
     *
     * @param command Command of the parsed split-file.
     * @param path Path to the original input file.
     */
    protected void logMalformedCommand(String command, String path) {
        log(Core.ALERT.WARNING, String.format("Command \"%s\" in \"%s\" has no useable data.", command, path));
    }

    /**
     * Finds the most likely key in {@link CiscoReader#SUPPORT_MAP} for the
     * provided command (arg 0).
     *
     * @param str Command text found from the original input file.
     * @param choices The key set from the {@link #SUPPORT_MAP}.
     * @return The most similar key of {@link #SUPPORT_MAP}.
     */
    private String longestMatchingSubstring(File source, String str, Set<String> choices) {
        //<editor-fold defaultstate="collapsed" desc="long method body">
        /**
         * the valid key for which the method will be chosen
         */
        String match = DUMMY_KEY;
        int longest = -1;

        Iterator<String> it = choices.iterator();
        while (it.hasNext()) {
            String other = it.next();

            int sl = str.length();
            int ol = other.length();

            int[][] table = new int[sl + 1][ol + 1];

            for (int s = 0; s <= sl; s++) {
                table[s][0] = 0;
            }
            for (int o = 0; o <= ol; o++) {
                table[0][o] = 0;
            }

            for (int s = 1; s <= sl; s++) {
                for (int o = 1; o <= ol; o++) {
                    if (str.charAt(s - 1) == other.charAt(o - 1)) {
                        if (s == 1 || o == 1) {
                            table[s][o] = 1;
                        } else {
                            table[s][o] = table[s - 1][o - 1] + 1;
                        }
                        if (table[s][o] > longest) {
                            longest = table[s][o];
                            match = other;
                        }
                    }
                }
            }

        }

        if (debug) {
            System.out.println(String.format("longestMatchingSubstring: %s = %s | %s", str, match, source.getName()));
        }

        return match;
        //</editor-fold>
    }

}
