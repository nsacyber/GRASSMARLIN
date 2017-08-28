package iadgov.ping;

import grassmarlin.Logger;
import grassmarlin.session.logicaladdresses.Ipv4;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;

public abstract class Ping {
    public static boolean ping(final Ipv4 ip, final int timeout) {
        final long addr = ip.getRawAddress();
        final byte[] tokensIp = new byte[]{(byte) (addr >> 24 & 0xFF), (byte) (addr >> 16 & 0xFF), (byte) (addr >> 8 & 0xFF), (byte) (addr & 0xFF)};
        try {
            return Inet4Address.getByAddress(tokensIp).isReachable(timeout);
        } catch(UnknownHostException ex) {
            Logger.log(Logger.Severity.ERROR, "Unknown host: %s", tokensIp);
            return false;
        } catch(IOException ex) {
            Logger.log(Logger.Severity.ERROR, "Error attempting to ping %s: %s", tokensIp, ex.getMessage());
            return false;
        }

    }
}
