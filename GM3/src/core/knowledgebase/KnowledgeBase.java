/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.knowledgebase;

// core
import core.Environment;
import core.knowledgebase.KnowledgeBaseLoader.Target;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.apache.commons.io.EndianUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SerializationUtils;
import ui.icon.Icons;
import static ui.icon.Icons.cache;

/**
 * KnowledgeBase fulfills the role of a lookup-service provider.
 * It provides multiple services to reference data that remains on disk in many files. Each file is a different of service.
 * 
 * Data returned by the KnowledgeBase should be in the form of <b>Future&lt;Object&gt;</b>.
 * 
 * The actual <b>records</b> maintained should be kept in soft (or weak) references that will load on the 
 * production of <b>Future::get;</b>.
 * 
 * Keep in mind other JVM constraints on memory. JNetPcap requires us to use medium to low amounts of memory;
 * we garbage collect often. SoftReferences will be collected fast, WeakReferences will be collected faster.
 * 
 * Note, knowledgebase should be final wherever declared.
 */
public class KnowledgeBase {
    public static final String NULL_TEXT = "Unknown";
    public static final String NOT_AVAILABLE = "Unknown";
    
    private final String geoIpFlagDir;
    
    final KnowledgeBaseLoader loader;
    final Map<Target,Object> map;
    
    public KnowledgeBase() {
        loader = new KnowledgeBaseLoader();
        // will not sync the GC
        map = Collections.synchronizedMap(new EnumMap<Target,Object>(Target.class));
        Arrays.asList(Target.values()).forEach(t->map.put(t, null));
        geoIpFlagDir = Environment.DIR_IMAGES.getPath() + File.separator + "geoip2" + File.separator;
    }
    
    /**
     * May return null if the KB cannot load its required resources.
     * @param propertiesFile A properties file containing the location of each Knowledgebase file.
     * @return Successfully constructed instance of the knowledge base, not a singleton, may return null.
     */
    public static KnowledgeBase newInstance(String propertiesFile) {
        KnowledgeBase kb = new KnowledgeBase();
        try {
            kb.getLoader().loadProperties(propertiesFile+File.separator+KnowledgeBaseLoader.class.getSimpleName()+".properties");
        } catch (IOException | IllegalArgumentException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return kb;
    }
    
    /**
     *
     * @return KnowledgeBaseLoader responsible for loading all services.
     */
    public KnowledgeBaseLoader getLoader() {
        return loader;
    }
    
    // If we want a Future<String> from a Map<K, String>, then use this function
    // to avoid more copy-paste programming. The first argument is the key type
    // of the Map, while the second argument is the appropriate enum value from
    // the KnowledgeBaseLoader.Target enumeration. The 3rd and 4th args are
    // values to use if there's no map, or if the key isn't found.
    private <K, V> Future<V> mkCompletableFuture(K key, KnowledgeBaseLoader.Target tag, V noMap, V noSuchKey) {
        return new CompletableFuture<V>() {
            @Override
            public V get() {
                Map<K, V> data = null;
                try {
                    data = (Map<K, V>) getObject( tag );
                } catch (IOException ex) {
                    Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
                }
                return data == null ? noMap : data.getOrDefault(key, noSuchKey);
            }
        };
    }
    
    public String getOuiSync(String mac) {
        String value = null;
        try {
            value = mkCompletableFuture(mac, Target.OUIVendor, NOT_AVAILABLE, NULL_TEXT).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return value;
    }
    
    /**
     * Returns values from the OUI.txt file. This is retrieved from <a href="standards-oui.ieee.org/oui.txt">standards-oui.ieee.org/oui.txt</a>.
     * @param mac String expected to be a 3 byte long hex format such as "000000".
     * @return String[] array containing the fields discovered in the CSV file.
     */
    public Future<String> getOUI( Byte[] mac ) {
        StringBuilder b = new StringBuilder();
        for( int i = 0; i <3; i++ )
           b.append( String.format("%02X", mac[i]) );
        return mkCompletableFuture(b.toString().toUpperCase(), Target.OUIVendor, NOT_AVAILABLE, NULL_TEXT);
    }
    /**
     * Returns values from the OUI.txt file. This is retrieved from <a href="standards-oui.ieee.org/oui.txt">standards-oui.ieee.org/oui.txt</a>.
     * @param macOUI String expected to be a 3 byte long hex format such as "000000".
     * @return String[] array containing the fields discovered in the CSV file.
     */
    public Future<String> getOUI( String macOUI ) {
        return mkCompletableFuture(macOUI.toUpperCase(), Target.OUIVendor, NOT_AVAILABLE, NULL_TEXT);
    }
    
    public String getBacnetVendor( byte[] intBytes ) {
        int id = 0;
        int len = intBytes.length;
        for( int i = 0; i < len; ++i ) {
            id <<= 8;
            id |= intBytes[i];
        }
        String ret = "...";
        try {
            ret = String.format("%s (%d)", getBacnetVendor(id).get(), id);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    /**
     * Returns values from the bacnetVendors.txt file. This is retrieved from <a href="www.bacnet.org/VendorID/BACnet Vendor IDs.htm">www.bacnet.org/VendorID/BACnet Vendor IDs.htm</a>.
     * @param id Integer vendor id value
     * @return String the vendor name, or "Unknown" if there is no such vendor id.
     */
    public Future<String> getBacnetVendor( Integer id ) {
        return mkCompletableFuture(id, Target.BACNETVendor, NOT_AVAILABLE, NULL_TEXT);
    }
    
    public String getEnipVendor(byte[] intBytes) {
        int id = 0;
        int len = intBytes.length;
        for( int i = 0; i < len; ++i ) {
            id <<= 8;
            id |= intBytes[i];
        }
        String ret = "...";
        try {
            ret = String.format("%s (%d)", getEnipVendor(id).get(), id);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    /**
     * Returns ENIP Vendor values from our copy of the vendor list.
     * @param id Integer vendor id value
     * @return String the vendor name, or "Unknown" if there is no such vendor id.
     */
    public Future<String> getEnipVendor( Integer id ) {
        return mkCompletableFuture(id, Target.ENIPVendor, NOT_AVAILABLE, NULL_TEXT);
    }
    
    public String getEnipDevice( byte[] intBytes ) {
        int id = 0;
        int len = intBytes.length;
        for( int i = 0; i < len; ++i ) {
            id <<= 8;
            id |= intBytes[i];
        }
        String ret = "...";
        try {
            ret = String.format("%s (%d)", getEnipDevice(id).get(), id);
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    
    /**
     * Returns ENIP Vendor values from our copy of the vendor list.
     * @param id Integer vendor id value
     * @return String the vendor name, or "Unknown" if there is no such vendor id.
     */
    public Future<String> getEnipDevice( Integer id ) {
        return mkCompletableFuture(id, Target.ENIPDevice, NOT_AVAILABLE, NULL_TEXT);
    }
    
    /**
     * Returns EtherType strings corresponding to the EtherType integer value.
     * @param id EtherType id value
     * @return String the EtherType value, or "Unknown" if there is no such id.
     */
    public Future<String> getEtherType( Integer id ) {
        return mkCompletableFuture(id, Target.EtherType, NOT_AVAILABLE, NULL_TEXT);
    }
    
    /**
     * Returns values from the HardwareVendor.csv file. We store proprietary Grassmarlin relevant data in this file.
     * The format is generally as follows <i>(ignore italicized comments)</i>,
     * <i>(3 byte hex)</i>000000, "Vendor Name, Inc", VENDOR_TYPE, <i>(0-5 confidence level)</i>5
     * @param mac String expected to be a 3 byte long hex format such as "000000".
     * @return String[] array containing the fields discovered in the CSV file.
     */
    public Future<String[]> getHardwareVendor(String mac) {
        return mkCompletableFuture(mac, Target.HardwareVendor, ArrayUtils.EMPTY_STRING_ARRAY, ArrayUtils.EMPTY_STRING_ARRAY);
    }
    
    /**
     *
     * @param n Integer value to retrieve from the ProtocolNumber HashMap.
     * @return String containing the protocol abbreviation.
     */
    public Future<String> getProtocol(Integer n) {
        return mkCompletableFuture(n, Target.ProtocolNumber, NOT_AVAILABLE, NULL_TEXT);
    }
    
    public String getCountryNameSync(String ipStr) {
        String country = null;
        try {
            country = this.getCountryName(ipStr).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return country;
    }
    
    /**
     * Get the country name associated with the given IP.
     * @param ipStr the host IP in question.
     * @return The country name, or "Not Found" if the IP is not in our mapping.
     */
    public Future<String> getCountryName(String ipStr) {
        return new CompletableFuture<String>() {
            @Override
            public String get() {
                String result = NULL_TEXT;
                TreeMap<CIDR, String> data = null;
                try {
                    data = (TreeMap<CIDR, String>)getObject(Target.GeoIP);
                    CIDR ip = new CIDR(ipStr);
                    CIDR keyCidr = data.floorKey(ip);
                    if((keyCidr != null) && keyCidr.contains(ip))
                        result = data.get(keyCidr);
                    else {
                        keyCidr = data.higherKey(ip);
                        if((keyCidr != null) && keyCidr.contains(ip))
                            result = data.get(keyCidr);
                    }
                } catch(IOException e) {
                   Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, e); 
                }
                if((result == null) || result.isEmpty())
                    result = NULL_TEXT;
                
                return result;
            }
        };
    }
    
    /**
     * Attempts to get the country flag in a blocking operation.
     * @param ipSstr String of the address to fetch a country flag for.
     * @return Image, or null on failure.
     */
    public Image getCountryFlagSync(String ipSstr) {
        Image image = null;
        try {
            image = this.getCountryFlag(ipSstr).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(KnowledgeBase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return image;
    }
    
    /**
     * Get the flag PNG filename associated with the given IP.
     * @param ipStr the host IP in question.
     * @return The File for the country's flag PNG, or a File for a "Not Found" flag..
     */
    public Future<Image> getCountryFlag(String ipStr) {
        return new CompletableFuture<Image>() {
            @Override
            public Image get() throws InterruptedException, ExecutionException {
                File f = new File(geoIpFlagDir + getCountryName(ipStr).get().replaceAll(" ", "_") + ".png"); 
                ImageIcon ii;
                try {
                    ii = new ImageIcon(f.toURI().toURL());
                } catch (MalformedURLException ex) {
                    return null;
                }
                if( ii.getIconWidth() < 2 )
                    return null;
                return ii.getImage();
            }
        };
    }
    
    /**
     * Retrieve the Serializable Object's File and marshals it into an Object.
     * @param t Target to retrieve.
     * @return Object marshaled from the 
     * @throws FileNotFoundException The temp file cannot be found for the given Target.
     * @throws IOException Some IO error occurred besides a missing temp File.
     */
    final synchronized Object getObject( Target t ) throws FileNotFoundException, IOException {
        // reference is atomic so we have to synchronize(this)
        Object ref = map.get(t);
        if( ref == null ) {
            File f = loader.fetch(t);
//            System.err.println("Loading "+t);
//            if( Runtime.getRuntime().freeMemory() <= f.length() ) return null;
            byte[] obj_bytes;
            try (FileInputStream fins = new FileInputStream(f)) {
                obj_bytes = new byte[(int)f.length()];
                fins.read(obj_bytes);
                fins.close();
            }
            Object o = SerializationUtils.deserialize(obj_bytes);
            map.put(t, o);
            return o;
        } else {
            return ref;
        }
    }
}