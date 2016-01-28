package core.fingerprint;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * 07.29.2015 - CC - New...
 */
public class XmlEnumeration {
    public static final ArrayList<String> POSITIONS = new ArrayList<>(Arrays.asList("START_OF_PAYLOAD", "END_OF_PAYLOAD", "CURSOR_START", "CURSOR_MAIN", "CURSOR_END"));
    public static final ArrayList<String> CATEGORIES = new ArrayList<>(Arrays.asList("PLC", "RTU", "MTU", "IED", "HMI", "UNKNOWN", "ICS_HOST", "FIREWALL", "NETWORK_DEVICE", "PROTOCOL_CONVERTER", "WORKSTATION", "OTHER"));
    public static final ArrayList<String> ROLES = new ArrayList<>(Arrays.asList("CLIENT", "SERVER", "MASTER", "SLAVE", "OPERATOR", "ENGINEER", "UNKNOWN", "OTHER"));
}
