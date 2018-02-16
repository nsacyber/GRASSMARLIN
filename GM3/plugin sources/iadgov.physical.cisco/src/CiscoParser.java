package iadgov.physical.cisco;

import grassmarlin.plugins.internal.physicalview.graph.Groupings;
import grassmarlin.plugins.internal.physicalview.graph.Router;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Cidr;
import grassmarlin.session.logicaladdresses.Ipv4;
import iadgov.physical.cisco.graph.Switch;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses the results of various Cisco Show commands to build a Device object.
 * All the commands have to be in the same file, but order doesn't matter.
 *
 * The following data is extracted:
 *
 * SHOW RUNNING-CONFIG (MANDATORY)
 *  - The device name
 *  - list of interfaces
 *  - list of vlans
 *  - firmware version
 *
 * SHOW INTERFACES
 *  - list of interfaces
 *  - list of vlans
 *  - interface/vlan statuses
 *
 * SHOW MAC ADDRESS-TABLE (for SWITCHES)
 *  - used to map how devices interconnect
 *
 * SHOW IP ARP (for ROUTERS)
 *  - used to map how devices interconnect
 *
 *  This is not the most efficient way to process this data:
 *   - Multi-line detail blocks are better parsed as a block than line-by-line
 *   - Context from one line is often needed in later lines
 *   - Regex overhead compared to simple string comparisons (e.g. .startsWith)
 *     is costly, both in terms of code clarity and performance.
 *
 *  However, it is written this way because, for over a year now, we've been
 *  asked if we can parse other formats.  We don't have the resources to do
 *  that, so we can't.  We would love for other people to fill this gap and
 *  provide the results to the community at large, but this doesn't happen.
 *  Ever.  This means one of two things (possibly both) is true:
 *  1) Writing log parsers is hard.
 *  2) If anybody ever sits down to write one, it will be me.
 *
 *  It is easier to replace a regex that spots a specific detail than it is to
 *  replace a stateful block.
 *
 *  That's the reason why we have this sub-optimal arrangement.  In 3.2 we
 *  used a stateful block processor, but porting it to the 3.3 architecture was
 *  cumbersome compared to writing a bunch of regular expressions.  Sadly, most
 *  design decisions like this pivot on the singular concern of developer time;
 *  the quickest solution wins, because there are always more tasks than
 *  developers.  Optimal file parsing is far less important than the ability to
 *  infer a physical layout from a logical graph, or manually modify the graph
 *  contents, or build out Visio support, or support watch-floor deployments,
 *  or write documentation, or... we have a lot to do--please send caffeine (or
 *  interns(?: with caffeine){0,1}).
 */
public class CiscoParser {
    public static class InterfaceName implements Comparable<InterfaceName> {
        private static final Pattern reName = Pattern.compile("^(\\D+)(\\d+(/\\d+)*)$");
        private String name;
        private final int[] indices;

        public InterfaceName(final String text) {
            final Matcher matcher = reName.matcher(text);
            matcher.find();
            this.name = matcher.group(1);
            this.indices = Arrays.stream(matcher.group(2).split("/")).mapToInt(token -> Integer.parseInt(token)).toArray();
        }

        public void addAlias(final InterfaceName other) {
            //Assume numbers match
            if(other.name.length() > this.name.length()) {
                this.name = other.name;
            }
        }

        public String getName() {
            String name = this.name;
            for(int idx = 0; idx < this.indices.length - 1; idx++) {
                if(idx != 0) {
                    name += '/';
                }
                name += this.indices[idx];
            }
            return name;
        }
        public int getIndex() {
            return this.indices[this.indices.length - 1];
        }
        public int[] getIndices() {
            return this.indices;
        }

        @Override
        public int compareTo(final InterfaceName other) {
            final int nameComparison = this.name.compareTo(other.name);
            if(nameComparison != 0) {
                return nameComparison;
            }

            for(int idx = 0; idx < indices.length; idx++) {
                if(other.indices.length <= idx) {
                    return 1;
                } else {
                    final int indexComparison = Integer.compare(this.indices[idx], other.indices[idx]);
                    if(indexComparison != 0) {
                        return indexComparison;
                    }
                }
            }

            if(other.indices.length > this.indices.length) {
                return -1;
            } else {
                return 0;
            }
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(indices);
        }

        @Override
        public boolean equals(final Object other) {
            if(other instanceof InterfaceName) {
                final InterfaceName o = (InterfaceName)other;

                if(o.name.startsWith(this.name) || this.name.startsWith(o.name)) {
                    return Arrays.equals(o.indices, this.indices);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return Arrays.stream(indices).mapToObj(value -> Integer.toString(value)).collect(Collectors.joining("/", this.name, ""));
        }
    }
    protected static class Interface {
        public final InterfaceName name;  //Back-reference for ease of resolving aliases.
        public boolean trunk = false;   //Device-switch connection.  Sometimes the config lies.
        public boolean gateway = false; //Router port.
        public HardwareVertex address = null; // Probably supplied as a MAC in the form xxxx.xxxx.xxxx
        public String status = null; // (up|down|administratively down)
        public String lineStatus = null; // (up|down)
        public String lineDetail = null; // (looped|disabled) TODO: Continue enumerating possibilities (not sure this is a full list).
        private final Set<HardwareVertex> connections;

        public Set<Integer> vlans = new HashSet<>();

        public Interface(final InterfaceName name) {
            this.name = name;

            this.connections = new HashSet<>();
        }

        public void addConnection(final HardwareVertex target) {
            this.connections.add(target);
        }
        public Collection<HardwareVertex> getConnections() {
            return this.connections;
        }
    }
    protected static class Vlan {
        public Ipv4 ip = null;
        public Cidr network = null;
    }
    protected static class Device {
        public String name = null;
        public int mtu = -1;
        public String version = null;
        public static String model = null;

        public final Map<Integer, Vlan> vlans = new HashMap<>();
        public final Map<InterfaceName, Interface> interfaces = new HashMap<>();
        public final List<String> warnings = new LinkedList<>();
        public final Set<String> features = new HashSet<>();
    }
    //  Certain test files contain backspaces.  This regex will match 1 or more backspaces at the start of a line as well as 1 or more backspaces and an equal number of preceding characters.
    private static Pattern reTrimWhitespace = Pattern.compile("\\s+$");

    // show running config
    private static Pattern reName = Pattern.compile("^hostname\\s+(?<Name>\\S+)\\s*$", Pattern.CASE_INSENSITIVE);
    private static Pattern reHttpServer = Pattern.compile("^ip http (?<Feature>(?:secure-)?server)$");
    private static Pattern reMtu = Pattern.compile("^system mtu routing (?<Mtu>\\d+)$");
    private static Pattern reVersion = Pattern.compile("^version (?<Version>\\S+)$");
    private static Pattern reDefineInterface = Pattern.compile("^interface (?!Vlan)(?<Name>\\S+)$");
    private static Pattern reSwitchPortVlan = Pattern.compile("^\\s+switchport (?:access|trunk native|trunk allowed) vlan (?<Vlan>\\d+(?:,)?)*$");
    private static Pattern reSwitchPortTrunk = Pattern.compile("^\\s+switchport mode trunk$");
    private static Pattern reDefineVlan = Pattern.compile("^interface Vlan(?<Vlan>\\d+)$");
    private static Pattern reDefineVlanAddress = Pattern.compile("^\\s+ip address (?<Ip>\\S+)\\s+(?<NetMask>\\S+)$");

    // show interfaces
    //TODO: This line also reports up/down and line protocol up/down [Vlan1 is up, line protocol is up ]
    private static Pattern reStartVlanInterfaceDescription = Pattern.compile("^Vlan(?<Vlan>\\d+) .*$");
    //TODO: track interface errors
    //TODO: Extract Hardware, hardware address, MTU, bandwidth

    private static Pattern reStartPortDescription = Pattern.compile("^(?!Vlan)(?<Name>\\S+) is (?<Status>up|down|administratively down), line protocol is (?<LineStatus>up|down)( \\((?<LineDetail>[^\\)]+)\\))?\\s*$");
    //  Hardware is Gigabit Ethernet, address is 1ce6.c783.e58a (bia 1ce6.c783.e58a)
    private static Pattern rePortMac = Pattern.compile("^\\s+Hardware is [^,]+, address is (?<Mac>[0-9a-f]{4}\\.[0-9a-f]{4}\\.[0-9a-f]{4}).*$");

    //12.2 format:
    // Static entries are not necessarily valid.  They also tend to contain internal stuff we want to ignore.  Looking solely at dynamic means we may miss something but we will also reduce the false positive rate tremendously.
    private static Pattern reMacTableEntry = Pattern.compile("^\\s+(?<Vlan>All|\\d+)\\s+(?<Mac>(?:[0-9a-f]{4}\\.?){3})\\s+DYNAMIC\\s+(?<Interface>\\S+)$");

    // show version
    private static Pattern reModel = Pattern.compile("^Cisco IOS Software, (?<Model>\\S+) Sofware .*$");

    // Pattern groups
    private static List<Pattern> vlanPatterns = Arrays.asList(
            reDefineVlan,
            reStartVlanInterfaceDescription
    );
    private static List<Pattern> featurePatterns = Arrays.asList(
            reHttpServer
    );
    private static List<Pattern> interfacePatterns = Arrays.asList(
            reDefineInterface,
            reStartPortDescription
    );


    protected static String cleanLine(final String line) {
        String result = line;

        // Process backspaces
        while(result.contains("\b")) {
            final int lengthBefore = result.length();
            while(result.contains("\b")) {
                result = result.replaceFirst("(^|[^\b])\b", "");
            }

            if(result.length() == lengthBefore) {
                //We didn't remove any characters; just remove remaining backspaces
                result = result.replace("\b", "");
                break;
            }
        }

        result = reTrimWhitespace.matcher(result).replaceAll("");

        if(result.startsWith("!") || result.isEmpty()) {
            return null;
        } else {
            return result;
        }
    }

    protected static boolean isValid(final Device device) {
        //TODO: Check for completeness of device
        if(device.name == null) {
            return false;
        }
        return !device.interfaces.isEmpty();

    }

    public static Object parseFile(final BufferedReader reader) throws IOException {
        final Device result = new Device();
        String line;
        InterfaceName currentInterface = null;
        Integer currentVlan = null;

        final Set<Pattern> unusedPatterns = new HashSet<>();
        unusedPatterns.addAll(Arrays.asList(
            reName,
            reHttpServer,
            reMtu,
            reVersion,
            reDefineInterface,
            reSwitchPortVlan,
            reSwitchPortTrunk,
            reDefineVlan,
            reDefineVlanAddress,
            reStartVlanInterfaceDescription,
            reStartPortDescription,
            rePortMac,
            reMacTableEntry
        ));

        parseNextLine:
        while((line = reader.readLine()) != null) {
            // cleanLine will return null for
            line = cleanLine(line);
            if(line == null) {
                currentInterface = null;
                currentVlan = null;
                continue;
            }

            // Test for running config data:
            final Matcher matcherName = reName.matcher(line);
            if(matcherName.matches()) {
                unusedPatterns.remove(reName);
                result.name = matcherName.group("Name");
                continue;
            }
            final Matcher matcherMtu = reMtu.matcher(line);
            if(matcherMtu.matches()) {
                unusedPatterns.remove(reMtu);
                result.mtu = Integer.parseInt(matcherMtu.group("Mtu"));
                continue;
            }
            final Matcher matcherVersion = reVersion.matcher(line);
            if(matcherVersion.matches()) {
                unusedPatterns.remove(reVersion);
                result.version = matcherVersion.group("Version");
                continue;
            }

            if(currentInterface != null && reSwitchPortTrunk.matcher(line).matches()) {
                unusedPatterns.remove(reSwitchPortTrunk);
                result.interfaces.get(currentInterface).trunk = true;
                continue;
            }

            final Matcher matcherVlans = reSwitchPortVlan.matcher(line);
            if(currentInterface != null && matcherVlans.matches()) {
                unusedPatterns.remove(reSwitchPortVlan);
                result.interfaces.get(currentInterface).vlans.add(Integer.parseInt(matcherVlans.group("Vlan")));
                while(matcherVlans.find()) {
                    result.interfaces.get(currentInterface).vlans.add(Integer.parseInt(matcherVlans.group("Vlan")));
                }
                continue;
            }

            final Matcher matcherVlanAddress = reDefineVlanAddress.matcher(line);
            if(currentVlan != null && matcherVlanAddress.matches()) {
                unusedPatterns.remove(reDefineVlanAddress);
                final Ipv4 ip = Ipv4.fromString(matcherVlanAddress.group("Ip"));
                final Ipv4 net = Ipv4.fromString(matcherVlanAddress.group("NetMask"));
                result.vlans.get(currentVlan).ip = ip;
                result.vlans.get(currentVlan).network = new Cidr(ip.getRawAddress(), net.getNetmaskSize());
            }

            final Matcher matcherMacTableEntry = reMacTableEntry.matcher(line);
            if(matcherMacTableEntry.matches()) {
                unusedPatterns.remove(reMacTableEntry);
                // private static Pattern reMacTableEntry = Pattern.compile("^\\s+(?<Vlan>All|\\d+)\\s+(?<Mac>(?:[0-9a-f]{4}\\.?){3})\\s+DYNAMIC\\s+(?<Interface>\\S+)$");

                final InterfaceName name = new InterfaceName(matcherMacTableEntry.group("Interface"));

                if(result.interfaces.containsKey(name)) {
                    result.interfaces.get(name).name.addAlias(name);
                } else {
                    result.interfaces.put(name, new Interface(name));
                }

                result.interfaces.get(name).addConnection(new Mac(matcherMacTableEntry.group("Mac").replace(".", "").replaceAll("..(?=[0-9a-f])", "$0:")));
            }

            final Matcher matcherPortMac = rePortMac.matcher(line);
            if(currentInterface != null && matcherPortMac.matches()) {
                unusedPatterns.remove(rePortMac);
                final String mac = matcherPortMac.group("Mac");
                final HardwareAddress address = new Mac(mac.replace(".", "").replaceAll("..(?=[0-9a-f])", "$0:"));
                result.interfaces.get(currentInterface).address = address;
            }

            for(final Pattern pattern : vlanPatterns) {
                final Matcher matcher = pattern.matcher(line);
                if(matcher.matches()) {
                    unusedPatterns.remove(pattern);
                    currentVlan = Integer.parseInt(matcher.group("Vlan"));
                    if(!result.vlans.containsKey(currentVlan)) {
                        result.vlans.put(currentVlan, new Vlan());
                    }
                    continue parseNextLine;
                }
            }

            for(final Pattern pattern : interfacePatterns) {
                final Matcher matcher = pattern.matcher(line);
                if(matcher.matches()) {
                    unusedPatterns.remove(pattern);
                    currentInterface = new InterfaceName(matcher.group("Name"));
                    if(!result.interfaces.containsKey(currentInterface)) {
                        result.interfaces.put(currentInterface, new Interface(currentInterface));
                    } else {
                        result.interfaces.get(currentInterface).name.addAlias(currentInterface);
                    }

                    try {
                        result.interfaces.get(currentInterface).status = matcher.group("Status");
                    } catch(IllegalArgumentException ex) {
                        //Apparently there wasn't a status.
                    }
                    try {
                        result.interfaces.get(currentInterface).lineStatus = matcher.group("LineStatus");
                    } catch(IllegalArgumentException ex) {
                        //Apparently there wasn't a line status.
                    }
                    try {
                        result.interfaces.get(currentInterface).lineDetail = matcher.group("LineDetail");
                    } catch(IllegalArgumentException ex) {
                        //Apparently there wasn't a line detail.
                    }

                    continue parseNextLine;
                }
            }

            for(final Pattern pattern : featurePatterns) {
                final Matcher matcher = pattern.matcher(line);
                if(matcher.matches()) {
                    unusedPatterns.remove(pattern);
                    result.features.add(matcher.group("Feature").intern());
                    continue parseNextLine;
                }
            }
        }

        if(isValid(result)) {
            return construct(result);
        } else {
            return null;
        }
    }

    private static Object construct(final Device source) {
        if(source.interfaces.values().stream().anyMatch(iface -> iface.gateway)) {
            //Router
            return constructRouter(source);
        } else {
            //Device
            return constructSwitch(source);
        }
    }

    private static Switch constructSwitch(final Device dev) {
        final Switch result = new Switch(dev.name);

        for(final String nameGroup : dev.interfaces.keySet().stream().map(item -> item.getName()).distinct().sorted().collect(Collectors.toList())) {
            final Switch.PortGroup group = new Switch.PortGroup(nameGroup);

            final long cnt = dev.interfaces.values().stream().filter(value -> value.name.getName().equals(nameGroup)).count();
            if(cnt % 12 == 0) {
                group.setMapper(Groupings.Grouping6x2);
            } else {
                //EXTEND: Add additional logic for port layouts.
                group.setMapper(Groupings.GroupingLow);
            }

            dev.interfaces.entrySet().stream()
                    .filter(entry -> entry.getKey().getName().equals(nameGroup))
                    .sorted((lhs, rhs) -> lhs.getKey().compareTo(rhs.getKey()))
                    .map(entry -> entry.getValue())
                    .forEach(i -> {
                        final Switch.Port port = new Switch.Port(i.address, i.name.toString());
                        //TODO: Fill in fields for port (errors, warnings, enabled)
                        port.setTrunk(i.trunk);
                        //We ignore gateway; it is really just used to determine if this is a switch or router.

                        //public String status = null; // (up|down|administratively down)
                        //public String lineStatus = null; // (up|down)
                        //public String lineDetail = null; // (looped|disabled) TODO: Continue enumerating possibilities (not sure this is a full list).
                        //public Set<Integer> vlans = new HashSet<>();

                        port.getConnectedVertices().addAll(i.getConnections());

                        group.addPort(port, i.name.getIndex());
                    });

            result.addGroup(group);
        }

        return result;
    }

    private static Router constructRouter(final Device dev) {
        //TODO: Build router
        return null;
    }
}