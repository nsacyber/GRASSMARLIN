package ICSDefines;

/**
 * Created by BESTDOG on 11/24/2015.
 */
public enum Category implements PrettyPrintEnum {
    PLC                 (255, 200, 200, 200, "Original_ics_host", "PLC"),
    RTU                 (255, 200, 200, 200, "Original_ics_host", "RTU"),
    MTU                 (255, 200, 200, 200, "Original_ics_host", "MTU"),
    IED                 (255, 200, 200, 200, "Original_ics_host", "IED"),
    HMI                 (255, 200, 200, 200, "Original_ics_host", "HMI"),
    OTHER               (200, 191, 231, 200, "Original_other_host", "Other"),
    UNKNOWN             (199, 199, 199, 200, "Original_unknown", null),
    FIREWALL            (200, 255, 200, 200, "Original_network_device", "Firewall"),
    ICS_HOST            (255, 200, 200, 200, "Original_ics_host", "ICS Host"),
    WORKSTATION         (153, 217, 234, 200, "Original_host", "Workstation"),
    NETWORK_DEVICE      (200, 255, 200, 200, "Original_network_device", "Network Device"),
    PROTOCOL_CONVERTER  (200, 255, 200, 200, "Original_network_device", "Protocol Converter"),
    ;
    /** Color key for devices categorized as such */
    public final int rgba;
    /** UI-visible text */
    private final String displayText;
    /** Value from the Icons enum to get the icon from */
    public final String iconValue;

    Category(int r, int g, int b, int a, String iconValue, String displayText) {
        rgba = (a << 24) | (r << 16) | (g << 8)  | b;
        this.iconValue = iconValue;
        this.displayText = displayText;
    }

    public String getText() {
        return displayText == null ? name() : displayText;
    }

    public static Object[][] createVector() {
        Object[][] vector = new Object[Category.values().length][];
        for( int i = 0; i < Category.values().length; i++) {
            Category ct = Category.values()[i];
            vector[i] = new Object[] { ct.iconValue, ct.rgba, ct.name() };
        }
        return vector;
    }

    @Override
    public String getPrettyPrint() {
        return this.displayText == null ? PrettyPrintEnum.super.getPrettyPrint() : this.displayText;
    }

    @Override
    public String toString() {
        return this.getPrettyPrint();
    }

    @Override
    public Enum get() {
        return this;
    }
}
