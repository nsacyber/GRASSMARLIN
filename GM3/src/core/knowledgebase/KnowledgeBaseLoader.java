/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.knowledgebase;

import core.Core;
import core.Environment;
import core.types.LogEmitter;

import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.SerializationUtils;

/**
 * <p> The standard pattern here will be to have a "fetch" method for each flat file. 
 * The form of these methods should be "fetchFileName( String path, Consumer&lt;Exception&gt; callback )" </p>
 * 
 * <p> KnowledeBaseLoader is a BasicReportObject because the knowledgebase should be able to
 * load itself automatically. There should be a persistent file, containing name:value pairs of
 * the flat-files to load. This will be used by an auto-load method to populate the knowledgebade's 
 * providers. The <b>auto-load</b> method will use BasicReportObject's completeCB, errCB, and startCB. </p>
 * 
 * <p> <b>Public loaders</b> <br>
 *  Given the path to the original file each "load" method will,
 * <ol>
 * <li> Parse it </li>
 * <li> Convert it to generic structure </li>
 * <li> serialize it </li>
 * <li> put its entry in the SerialMap </li>
 * </ol>
 * 
 * <p>
 * Access to the KnowledgeBaseLoader WILL be thread safe.
 * </p>
 * 
 * <b>Flat-files</b>
 * <ol>
 * <li> OUI Vendor </li>
 * <li> ENIP Vendor </li>
 * <li> ENIP Device </li>
 * <li> EtherType </li>
 * <li> BACNET Vendor </li>
 * <li> Hardware Vendor </li>
 * <li> Protocol Number </li>
 * </ol>
 */
public final class KnowledgeBaseLoader {
    
    public static final LogEmitter log = LogEmitter.factory.get();
    
    private static Pattern ouiMatchPattern = Pattern.compile("\\s*(?<Mac>(\\p{XDigit}+))\\s*\\(base\\s16\\)\\s*(?<Vendor>(.*))");
    private static final Pattern csvFull = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
    private static final Pattern csvNVP = Pattern.compile("^(?<Name>(\\d+))\\s*,\\s*\"*(?<Value>([^\"]+))\"*.*?");
    private static final Pattern row = Pattern.compile("<td>(?<Content>(.*?))</td>");
    private static final Map<Target,File> serialMap = Collections.synchronizedMap(new EnumMap<>(Target.class));
    /* keep reference around as it's used in high-volume */
    
    /**
     * Properties in the KnowledgeBaseLoader.properties file will match this enumeration. Other keys are invalid.
     */
    public enum Target {
        BACNETVendor(false, "BACnetVendors.htm") {
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<Integer, String> results = new HashMap<>(); // Number,Device String
                ArrayList<String> buf = new ArrayList();
                try {
                    processByLine(path, (i,s)->{
                        Matcher m = row.matcher(s);
                        if( m.matches() ) {
                            buf.add(m.group("Content"));
                            if( buf.size() == 4 ) {
                                try {
                                    results.put(
                                            Integer.parseInt( buf.get(0) ),
                                            buf.get(1)
                                    );
                                } catch( NumberFormatException ex ) {
                                    log.emit(this, Core.ALERT.DANGER, "KB:"+ex.getMessage());
                                }
                                buf.clear();
                            }
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( results.isEmpty() ) return null;
                return createSerialObject((Serializable)results);
            }
        },
        ENIPDevice(false, "enipDevice.csv") {
            /**
             * @param path Path to the ENIPDevice.csv file.
             * @return The File with the serialized lookup map of {id->Device type}
             */
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<Integer, String> results = new HashMap<>(); // Number,Device String
                try {
                    processByLine(path, (i,s)->{
                        Matcher m = csvNVP.matcher(s);
                        if( m.matches() ) {
                            try {
                                results.put(
                                        Integer.parseInt(m.group("Name") ),
                                        m.group("Value")
                                );
                            } catch( NumberFormatException ex ) {
                                log.emit(this, Core.ALERT.DANGER, "KB:"+ex.getMessage());
                            }
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( results.isEmpty() ) return null;
                return createSerialObject((Serializable)results);
            }
        },
        ENIPVendor(false, "enipVendors.csv") {
            /**
             * Load the ENIP Vendor information from our local enipVendors.csv file.
             * If/when we move to having a copy of the Wireshark file enip-enumerate.nse
             * from which to pull ENIP Vendor and Device information, then this routine
             * should be merged with laodENIPDevice(String) as the canonical copy of
             * both lists live in one file.
             *
             * @param path Path to the ENIPVendors.csv
             * @return The File with the serialized lookup map of {id->Vendor Names}.
             */
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<Integer, String> results = new HashMap<>(); // Number,Vendor String
                try {
                    processByLine(path, (i,s)->{
                        Matcher m = csvNVP.matcher(s);
                        if( m.matches() ) {
                            try {
                                results.put(
                                        Integer.parseInt(m.group("Name") ),
                                        m.group("Value")
                                );
                            } catch( NumberFormatException ex ) {
                                log.emit(this, Core.ALERT.DANGER, ex.getMessage());
                            }
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( results.isEmpty() ) return null;
                return createSerialObject((Serializable)results);
            }
        },
        EtherType(false, "ieee-802-numbers.csv") {
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<Integer, String> results = new HashMap<>(); // Number|Vendor String
                try {
                    Scanner scanner = new Scanner(new File(path));
                    scanner.useDelimiter("\r\n");
                    if(scanner.hasNext()) {
                        scanner.next(); // skip the header line.
                    }
                    while(scanner.hasNext()) {
                        String line = scanner.next().replaceAll("\n", " ");
                        String[] fields = csvFull.split(line, 6);
                        if(fields.length < 6)
                            continue; // skip malformed lines.
                        Integer index = getEtherTypeIndex(fields);
                        if(index == null)
                            continue;

                        String value = (fields[5].startsWith("[RFC"))
                                ? (fields[4] + ' ' + fields[5]) : fields[4];

                        results.put(index, value);
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( results.isEmpty() ) return null;
                return createSerialObject((Serializable)results);
            }
        },
        HardwareVendor(false, "hardwareVendors.csv") {
            /**
             * Expects a csv format that supports quotations around fields to escape commas.
             * Will report malformed lines of with either of the following details,
             * <ul>
             * <li>The first field is not a proper hex value of the form 000000</li>
             * <li>The line is not a proper CSV format</li>
             * </ul>
             * @param path Path to the HardwareVendors.csv.
             * @return File object of the temporary serialized Map&lt;String,String[]&gt;.
             */
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<String,String[]> map = new HashMap<>();
                try {
                    processByLine(path, (i,s)->{
                        String[] split = s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                        try {
                            if( split.length >= 2 && Integer.parseInt(split[0], 16) > 0x000000 )
                                map.put(split[0], Arrays.copyOfRange(split, 1, split.length));
                            else
                                log.emit(this, Core.ALERT.WARNING, "line "+i+": Malformed format, found \""+split[0]+"\" expected (Hex)000000.");
                        } catch( NumberFormatException | IndexOutOfBoundsException ex ) {
                            Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.FINE, null, ex);
                            log.emit(this, Core.ALERT.WARNING, "line "+i+": Malformed format, "+s);
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( map.isEmpty() ) return null;
                return createSerialObject((Serializable)map);
            }

            @Override
            public boolean save(List<String> lines) {
                if(!validate()) {
                    return false;
                }
                File outputFile = new File(getDefaultPath());
                try(FileWriter fileWriter = new FileWriter( outputFile )) {
                    lines.stream().forEach((str) -> {
                        try {
                            fileWriter.write(str);
                        } catch (IOException e) {
                            Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, e);
                        }
                    });
                } catch (IOException e) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, e);
                }
                return true;
            }
        },
        OUIVendor(true,"oui.txt") {
            /**
             *
             * @param path Path to the resource to load.
             * @return File object containing the temp file created of serialized object for the OUI.txt resource.
             */
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<String,String> map = new HashMap<>();
                try {
                    processByLine(path, (i, s) -> {
                        Matcher m = ouiMatchPattern.matcher(s);
                        if (m.matches())
                            map.put(m.group("Mac"), m.group("Vendor"));
                    });
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( map.isEmpty() ) return null;
                return createSerialObject((Serializable)map);
            }
        },
        ProtocolNumber(false, "protocolNumbers.csv") {
            /**
             * @param path Path to the location of a raw protocol-number file.
             * @return File of the serialized Map&lt;Integer,String&gt; object this will create on success, else null;
             */
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                Map<Integer,String> map = new HashMap<>();
                try {
                    processByLine(path, (i,s)->{
                        // take the entire line, the new version has 5 fields, the two we need are in the same place
                        String[] parts = s.split(",");
                        Integer n = -1;
                        try {
                            // the latest version of the IANA protocols sheet has citations that make
                            // the CSV's nature useless, we no longer care about format issues, we will always see them
                            n = Integer.parseInt(parts[0]);
                        } catch( NumberFormatException | IndexOutOfBoundsException ex ) {}

                        // handle unassigned protocol IDs
                        if( n == -1 ) {
                            String[] parts2 = parts[0].split("-");
                            if( parts2.length == 2 ) {
                                try {
                                    Integer from  = Integer.parseInt(parts2[0]);
                                    Integer to    = Integer.parseInt(parts2[1]);
                                    if( from > to )
                                        from = to;
                                    for(; from<=to; from++ )
                                        map.put(from, parts[2]);
                                } catch( NullPointerException | NumberFormatException | IndexOutOfBoundsException ex ) {}
                            }
                        } else {
                            String text = parts[1].isEmpty() ? parts[2].isEmpty() ? "" : parts[2] : parts[1];
                            if( parts[ parts.length-1 ].contains("RFC") )
                                text = text.concat("\t"+parts[ parts.length-1 ]);
                            // use the first field like "TCP" or the second if its empty like "Transmission Control"
                            map.put( n, text );
                        }
                    });
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( map.isEmpty() ) return null;
                return createSerialObject( (Serializable)map );
            }
        },
        GeoIP(false, "cidr_to_geo_id.csv","geo_id_to_name.csv") {
            @Override
            public String getDefaultPath() {
                String path = Environment.DIR_DATA.getPath()+File.separator+"kb"+File.separator;
                return path + this.fileName[0] + ";" + path + this.fileName[1];
            }

            /**
             * Load the GeoIP data and return a
             * @param path Path to the cidr_to_geo_id.csv;geo_id_to_name.csv
             * @return File object of the temporary serialized Map&lt;CIDR,String&gt;.
             */
            @Override
            public File loadTarget(String path) {
                if((path == null) || path.isEmpty()) path = getDefaultPath();
                TreeMap<CIDR, String> results = new TreeMap<>(); // CIDR -> Country Name
                try {
                    String[] paths = path.split(";");
                    if((paths.length != 2) || paths[0].isEmpty() || paths[1].isEmpty()) {
                        throw new IllegalArgumentException("Expected GeoIP value to be semicolon-delimted pair of file names, saw: " + path);
                    }
                    Map<CIDR, Integer> cidr_geoId = collapse(csvToMap(paths[0], s -> {return new CIDR(s);},        s -> {return Integer.valueOf(s);}, "CIDR", "Integer", ","));
                    Map<Integer, String> geoId_countryName = csvToMap(paths[1], s -> {return Integer.valueOf(s);}, s -> {return s;},                  "Integer", "String", ",");

                    cidr_geoId.entrySet().stream().forEach(p -> {
                        Integer code = p.getValue();
                        results.put(p.getKey(), geoId_countryName.containsKey(code) ? geoId_countryName.get(code) : "UNKNOWN");
                    });

                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.SEVERE, null, ex);
                }
                if( results.isEmpty() ) return null;
                return createSerialObject((Serializable)results);
            }

            /**
             * Given a map of CIDR -> integers, combine any adjacent CIDRs that map
             * to the same integers.
             * @param m the original map
             * @return the minimized map
             */
            private Map<CIDR, Integer> collapse(Map<CIDR, Integer> m) {
                // Partition the CIDRs by country code
                Map<Integer, ArrayList<CIDR>> byCode = new HashMap<>();
                m.entrySet().stream().forEach((p) -> {
                    Integer code = p.getValue();
                    if(!byCode.containsKey(code))
                        byCode.put(code, new ArrayList<>());
                    byCode.get(code).add(p.getKey());
                });

                // Join adjacent CIDRs, then store the minimized CIDR->code map.
                Map<CIDR, Integer> result = new HashMap<>();
                byCode.entrySet().stream().forEach( p -> {
                    CIDR.coalesce(p.getValue()).stream().forEach( cidr -> {
                        result.put(cidr, p.getKey());
                    });
                });

                return result;
            }

            private <L, R> Map<L, R> csvToMap(String path, Function<String, L> toL, Function<String, R> toR, String lName, String rName, String delimiter) throws IOException {
                Map<L, R> results = new HashMap<>();
                processByLine(path, (Integer i, String line) -> {
                    String[] parts = line.split(delimiter, 2);
                    try {
                        if ((parts.length == 2) && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                            results.put(toL.apply(parts[0]), toR.apply(parts[1]));
                        }
                    } catch (IndexOutOfBoundsException ex) {
                        Logger.getLogger(KnowledgeBaseLoader.class.getName()).log(Level.WARNING, null, ex);
                        log.emit(this, Core.ALERT.WARNING, "line " + i + ": Malformed format, expected " + lName + "," + rName + " but found " + line);
                    } catch (NumberFormatException nfe) {
                        log.emit(this, Core.ALERT.WARNING, "line " + i + ": Malformed format, expected Number, but found " + line);
                    }
                });

                return results;
            }
        };

        protected final boolean retentionPolicy;
        protected final String[] fileName;

        Target( boolean retain , String ... fileName) {
            this.retentionPolicy = retain;
            this.fileName = fileName;
        }

        public boolean isRetained() {
            return retentionPolicy;
        }

        public String getDefaultPath() {
            String path = Environment.DIR_DATA.getPath()+File.separator+"kb"+File.separator;
            return path + this.fileName[0];
        }

        public boolean validate() {
            File file = new File(getDefaultPath());
            return (file.exists() && file.canRead());
        }

        protected void processByLine( String path, BiConsumer<Integer,String> lineCB) throws IOException {
            BufferedReader br = new BufferedReader( new FileReader(path) );
            String line;
            int i = 0;
            while( (line = br.readLine()) != null )
                lineCB.accept(++i, line);
            try{ br.close(); } catch(IOException ex) {}
        }

        /**
         * create a temp file the JVM will delete when it shuts down.
         * @param serializable object
         * @return file
         */
        protected File createSerialObject(Serializable serializable) {
            File file;
            try {
                file = File.createTempFile(this.name(), "gmdb.g3");
                byte[] obj_bytes = SerializationUtils.serialize( (Serializable) serializable);
                try (FileOutputStream fous = new FileOutputStream( file )) {
                    fous.write(obj_bytes);
                    KnowledgeBaseLoader.serialMap.put(this, file);
                    fous.close();
                    FileUtils.forceDeleteOnExit(file);
                }
            } catch (IOException ex) {
                KnowledgeBaseLoader.log.emit(this, Core.ALERT.WARNING, "KB"+ex.getMessage());
                file = null;
            }
            return file;
        }

        protected Integer getEtherTypeIndex(String[] fields) {
            Integer result = null;
            try {
                result = hasValue(fields[0])
                        ? Integer.parseInt(fields[0])
                        : (hasValue(fields[1]) ? Integer.parseInt(fields[1], 16) : null);
            } catch(NumberFormatException e) {
                // Eat this exception so we just return null.
            }
            return result;
        }

        private boolean hasValue(String s) {
            return (s != null) && !s.isEmpty();
        }

        /**
         * @param path Path to the resource to load.
         * @return File object containing the temp file created of serialized object for the Target indicated.
         * WILL return null if path is empty.
         */
        public abstract File loadTarget( String path );
        public boolean save(List<String> lines) {
            return false;
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /** Clears cache */
    public void unload() {
        serialMap.forEach((k,v)->{
//            serialMap.put(k, null);
        });
    }
    
    /**
     * WILL load all files contained in the properties file. If any fail they are likely missing.
     * Failure on this method likely indicates the user has an incorrect path in their .properties file
     * for this object or they deleted one of the resources the .properties file points to.
     * They must delete the .properties file to have the default restored.
     * @param path Path to list containing the files to include for each "service".
     * @throws java.io.IOException Thrown for any errors that occur when loading the auto-load file.
     */
    public void loadProperties( String path ) throws IOException, IllegalArgumentException {
        if( path == null ) {
            throw new IOException( "No properties file found." );
        }
        Properties resources;
        File f;
        if( (f = new File(path)).exists() ) {
            try (InputStream is = new FileInputStream(f)) {
                resources = new Properties();
                resources.load(is);
                is.close();
            }
        } else { 
            throw new IOException( path );
        }
        resources.forEach(this::loadByName);
    }
    
    /**
     *
     * @return Map of Property key to serialized KnowledgeBase File.
     */
    public Map<Target, File> getSerialMap() {
        return serialMap;
    }

    /**
     *
     * @param key Should be a KnowledgeBaseLoader.Target
     * @param path Path on disk of the desired target file.
     * @return The path to the new serialized datafile created by the internal load method.
     * <p>
     * A successful load from this or any subsequent loader WILL cause the user's property
     * file for this class to update. The default inside this class should only change from
     * different GM installations.
     * </p>
     */
    public File loadByName( Object key, Object path ) {
        if((key == null) || (path == null)) return null;
        File result = null;
        
        /* asserts do not make it to production */
        assert( key instanceof String );
        assert( path instanceof String );
        Target target;
        try {
            target = Target.valueOf(key.toString());
            result = target.loadTarget(path.toString());
        } catch( IllegalArgumentException ex ) {
            Logger.getLogger(Object.class.getName()).log(Level.SEVERE,
                "Key value ''{0}'' is not a supported property.", key);
            ex.printStackTrace();
        }
        return result;
    }
    
    /**
     * @param t Target to retrieve.
     * @return File object containing the serialized generic container for the KnowledgeBaseLoader Target requested.
     * @throws java.io.FileNotFoundException Thrown when the File specified by <b>path</b> cannot be accessed.
     */
    public File fetch(Target t) throws FileNotFoundException {
        File f = serialMap.get(t);
        if( f == null ) throw new FileNotFoundException(t.toString());
        if( !f.exists() || !f.canRead() ) throw new FileNotFoundException(t.toString()+"\t"+f.getAbsolutePath() );
        return f;
    }
    
    /**
     *
     * @param t Target to check.
     * @return true if Target t was successfully loaded, else false.
     */
    public Boolean loaded( Target t ) {
        if( !serialMap.containsKey(t) ) return false;
        File f = serialMap.get(t);
        return f == null ? false : f.exists();
    }
    
    /**
     * Will look for all leftover temp files created by GM3 and delete them. This will only be 
     * necessary if Grassmarlin closes abnormally.
     */
    public void cleanup() {
        File temp = FileUtils.getTempDirectory();
        
        IOFileFilter files = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return serialMap.keySet().stream().anyMatch(t->{
                    return file.getName().startsWith(t.name()) && file.getName().endsWith("gmdb.g3");
                });
            }

            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        };
        
        IOFileFilter dirs = new IOFileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }

            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        };
        
        FileUtils.listFiles(temp, files, dirs).forEach((file) -> {
            try {
                FileUtils.forceDelete(file);
            } catch (IOException ex) {}
        });
    }
    
}
