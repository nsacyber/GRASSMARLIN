package grassmarlin.plugins.internal.metadata;

import com.sun.istack.internal.NotNull;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.hardwareaddresses.Mac;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MacManufacturer {
    protected static class Entry {
        private final long mask;
        private final long value;
        private final String manufacturer;

        Entry(final long mask, final long value, final String manufacturer) {
            this.mask = mask;
            this.value = value;
            this.manufacturer = manufacturer;
        }

        public boolean matches(final long id) {
            return (id & mask) == value;
        }

        public String getManufacturer() {
            return this.manufacturer;
        }

        public int getKey() {
            return (int)(value >>> 32);
        }
    }

    protected static long longFromText(final String text) {
        long value = 0;
        for(final String token : text.replace('-', ':').split(":")) {
            value <<= 8;
            value += Long.parseLong(token, 16);
        }
        return value;
    }
    protected static long longFromInts(final int[] input) {
        long value = 0;
        for(final int token : input) {
            value <<= 8;
            value += token;
        }
        return value;
    }

    private String lastUsedPath = null;
    protected HashMap<Integer, List<Entry>> entries;

    public MacManufacturer() {
        entries = new HashMap<>();

        parseFile(Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.PATH_MANUFACTURER_DB)));
    }

    public void parseFile(final Path file) {
        //If the file hasn't changed, then there is no reason to reparse--we don't support on-the-fly updates to the same file.
        final String pathNew = file.toAbsolutePath().toString();
        if(pathNew.equals(lastUsedPath)) {
            return;
        }
        lastUsedPath = pathNew;

        entries.clear();
        try(BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            String line;
            while((line = reader.readLine()) != null) {
                this.addEntryFromLine(line);
            }
        } catch(IOException ex) {
            Logger.log(Logger.Severity.WARNING, "There was an error loading the MAC Manufaturer Database: %s", ex.getMessage());
        }
    }

    protected void addEntryFromLine(@NotNull final String line) {
        String source = line.trim();
        if(source.isEmpty() || source.startsWith("#")) {
            return;
        }

        final String[] tokens = source.split("\t", 2);
        //If the manufacturer isn't listed, this entry serves no purpose.
        if(tokens.length != 2) {
            return;
        }
        final String[] names = tokens[1].split("#", 2);
        final String manufacturer = names[names.length - 1].trim(); //Remove trailing comment

        final String[] tokensValue = tokens[0].split("/", 2);
        final String txtValue = tokensValue[0];
        final String filter = txtValue.replaceAll("[^A-Fa-f0-9]", "").trim();
        final int bitsMask = tokensValue.length == 1 ? filter.length() * 4 : Integer.parseInt(tokensValue[1]);
        final long id = longFromText(filter);
        if(id == 0) {
            return;
        }

        final Entry entry = new Entry(0x0000FFFFFFFFFFFFL & (0x0000FFFFFFFFFFFFL << (48 - bitsMask)), id << (48 - bitsMask), manufacturer);
        final int key = entry.getKey();

        if(!entries.containsKey(key)) {
            entries.put(key, new ArrayList<>());
        }
        entries.get(key).add(entry);
    }

    public String forMac(final Mac mac) {
        final long address = longFromInts(mac.getAddress());
        final int key = (int)(address >>> 32);

        final List<Entry> potentials = entries.get(key);
        if(potentials == null) {
            return null;
        } else {
            for(final Entry entry : potentials) {
                if(entry.matches(address)) {
                    Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "%s has Manufacturer of '%s'", mac, entry.getManufacturer());
                    return entry.getManufacturer();
                }
            }
            return null;
        }
    }
}
