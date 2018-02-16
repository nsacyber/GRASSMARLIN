package grassmarlin.plugins.internal.logicalview;

public abstract class Protocols {
    public static String toString(final short protocol) {
        switch(protocol) {
            case 1:
                return "ICMP";
            case 6:
                return "TCP";
            case 17:
                return "UDP";
            case 41:
                return "IPv6";
            default:
                return Short.toString(protocol);
        }
    }
}
