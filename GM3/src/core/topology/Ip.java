/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

/**
 * L3 IP4 address.
 * @deprecated see {@link Ip4} for preferred behavior.
 */
public class Ip {
    public static final int IP6_BYTES = 8;
    public static final int IP4_BYTES = 4;
    
    /**
     * Preparing for move to ipv6 support
     */
    public static class Ip4 extends Ip {

        public Ip4(String ipText) {
            super(Ip4.intHash(ipText));
        }
        
        public Ip4(int hash) {
            super(hash);
        }
        /**
         * This should be suitable for ipv6 as well but we will have to accommodate
         * for the extra bytes later. Expects four integers 0-255 separated by some
         * non-numeric character.
         *
         * @param ipString String of the format (\\d\\D){4,}
         * @return Integer of a 'n >= 4' byte IP4 address.
         */
        public static int intHash(String ipString) {
            int val = 0;
            String[] strs = ipString.split("[^\\p{XDigit}]");
            int len = strs.length;
            if (len >= 4) {
                val = intVal(strs[0]);
                val <<= 8;
                val |= intVal(strs[1]);
                val <<= 8;
                val |= intVal(strs[2]);
                val <<= 8;
                val |= intVal(strs[3]);
            }
            return val;
        }
    }
    
    public static class MissingIp extends Ip {

        public MissingIp() {
            super(0);
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public String toString() {
            return "Unavailable";
        }
    }
    
    public static final Ip MISSING_IP = new MissingIp();
    static char OCTET_SEPARATOR = '.';
    int hash;

    public Ip(int hash) {
        this.hash = hash;
    }

    public boolean isPresent() {
        return true;
    }

    public static int[] toInts(int val) {
        int[] bytes = new int[4];
        for (int i = 0; i < 4; i++) {
            bytes[3 - i] = 0b11111111 & val;
            val >>= 8;
        }
        return bytes;
    }

    public static String ipString(int[] b, Integer CIDR) {
        StringBuilder sb = new StringBuilder();
        sb.append(b[0] & 0xFF);
        sb.append(OCTET_SEPARATOR);
        sb.append(b[1] & 0xFF);
        sb.append(OCTET_SEPARATOR);
        sb.append(b[2] & 0xFF);
        sb.append(OCTET_SEPARATOR);
        sb.append(b[3] & 0xFF);
        if (CIDR != null) {
            sb.append('/');
            sb.append(CIDR);
        }
        return sb.toString();
    }

    public static int getCIDR(int hash) {
        int cidr = 0;
        hash = Integer.reverseBytes(hash);
        while ((hash & 0b1) == 1) {
            cidr++;
            hash >>= 1;
        }
        return cidr;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Ip other = (Ip) obj;
        if (this.hash != other.hash) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Ip.ipString(Ip.toInts(this.hash), Ip.getCIDR(this.hash));
    }

    /**
     * This should be suitable for ipv6 as well but we will have to accommodate
     * for the extra bytes later. Expects four integers 0-255 separated by some
     * non-numeric character.
     *
     * @param ipString String of the format (\\d\\D){4,}
     * @return Integer of a 'n >= 4' byte IP4 address.
     */
    public static Integer parse(String ipString) {
        return Ip4.intHash(ipString);
    }
//    public static Integer parse(String ipString) {
//        int val = 0;
//        String[] strs = ipString.split("[^\\p{XDigit}]");
//        int len = strs.length;
//        if (len >= 4) {
//            val = intVal(strs[0]);
//            for (int i = 1; i < 4; ++i) {
//                val <<= 8;
//                val |= intVal(strs[1]);
//            }
//        }
//        return val;
//    }

    private static int intVal(String s) {
        if (s.matches("\\p{XDigit}+")) {
            return Integer.parseInt(s, 16);
        } else {
            return Integer.parseInt(s);
        }
    }

}
