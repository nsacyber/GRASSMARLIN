package prefuse.util;

import java.awt.Font;

import prefuse.util.collections.IntObjectHashMap;

/**
 * Library maintaining a cache of fonts and other useful font computation
 * routines.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FontLib {

    private static final IntObjectHashMap fontMap = new IntObjectHashMap();
    private static int misses = 0;
    private static int lookups = 0;
    
    /**
     * Get a Font instance with the given font family name and size. A
     * plain font style is assumed.
     * @param name the font name. Any font installed on your system should
     * be valid. Common examples include "Arial", "Verdana", "Tahoma",
     * "Times New Roman", "Georgia", and "Courier New".
     * @param size the size, in points, of the font
     * @return the requested Font instance
     */
    public static Font getFont(String name, double size) {
        int isize = (int)Math.floor(size);
        return getFont(name, Font.PLAIN, isize);
    }
    
    /**
     * Get a Font instance with the given font family name, style, and size
     * @param name the font name. Any font installed on your system should
     * be valid. Common examples include "Arial", "Verdana", "Tahoma",
     * "Times New Roman", "Georgia", and "Courier New".
     * @param style the font style, such as bold or italics. This field
     * uses the same style values as the Java {@link java.awt.Font} class.
     * @param size the size, in points, of the font
     * @return the requested Font instance
     */
    public static Font getFont(String name, int style, double size) {
        int isize = (int)Math.floor(size);
        return getFont(name, style, isize);
    }
    
    /**
     * Get a Font instance with the given font family name, style, and size
     * @param name the font name. Any font installed on your system should
     * be valid. Common examples include "Arial", "Verdana", "Tahoma",
     * "Times New Roman", "Georgia", and "Courier New".
     * @param style the font style, such as bold or italics. This field
     * uses the same style values as the Java {@link java.awt.Font} class.
     * @param size the size, in points, of the font
     * @return the requested Font instance
     */
    public static Font getFont(String name, int style, int size) {
        int key = (name.hashCode()<<8)+(size<<2)+style;
        Font f = null;
        if ( (f=(Font)fontMap.get(key)) == null ) {
            f = new Font(name, style, size);
            fontMap.put(key, f);
            misses++;
        }
        lookups++;
        return f;
    }
    
    /**
     * Get the number of cache misses to the Font object cache.
     * @return the number of cache misses
     */
    public static int getCacheMissCount() {
        return misses;
    }
    
    /**
     * Get the number of cache lookups to the Font object cache.
     * @return the number of cache lookups
     */
    public static int getCacheLookupCount() {
        return lookups;
    }
    
    /**
     * Clear the Font object cache.
     */
    public static void clearCache() {
        fontMap.clear();
    }
    
    /**
     * Interpolate between two font instances. Font sizes are interpolated
     * linearly. If the interpolation fraction is under 0.5, the face and
     * style of the starting font are used, otherwise the face and style of
     * the second font are applied.
     * @param f1 the starting font
     * @param f2 the target font
     * @param frac a fraction between 0 and 1.0 controlling the interpolation
     * amount.
     * @return an interpolated Font instance
     */
    public static Font getIntermediateFont(Font f1, Font f2, double frac) {
        String name;
        int size, style;
        if ( frac < 0.5 ) {
            name  = f1.getName();
            style = f1.getStyle();
        } else {
            name  = f2.getName();
            style = f2.getStyle();
        }
        size = (int)Math.round(frac*f2.getSize()+(1-frac)*f1.getSize());
        return getFont(name,style,size);
    }
    
} // end of class FontLib
