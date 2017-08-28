package grassmarlin.plugins.internal.metadata;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.logicaladdresses.Ipv4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Ipv4GeoIp {
    private static class Location {
        public final long ipLow;
        public final long ipHigh;
        public final String name;
        public final String code;

        public Location(final String line) {
            final String[] tokens = line.split("^\"|\",\"|\"$", 8); //6 meaningful fields and blanks before/after
            //tokens[0] is blank
            //tokens[1] is the low IP in dotted-decimal form
            //tokens[2] is the high IP in dotted-decimal form
            this.ipLow = Long.parseLong(tokens[3]);
            this.ipHigh = Long.parseLong(tokens[4]);
            this.code = tokens[5];
            this.name = tokens[6];
        }
    }

    private final List<Location> locations;

    public Ipv4GeoIp() {
        locations = new ArrayList<>(143847); //The number of lines in the Ipv4 GeoIp CSV file. (or at least it was when this was written)

        loadFile(Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APPLICATION), "data", "geoip", "ipv4.csv"));
    }

    public void loadFile(final Path path) {
        try {
            Files.lines(path).filter(line -> !line.trim().isEmpty()).forEach(line -> locations.add(new Location(line)));
        } catch(IOException ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error loading the Ipv4 GeoIp Database: %s", ex.getMessage());
        }
    }

    public String match(final Ipv4 ip) {
        for(Location location : locations) {
            if(location.ipLow <= ip.getRawAddress() && location.ipHigh >= ip.getRawAddress()) {
                return location.name;
            }
        }
        return null;
    }
}