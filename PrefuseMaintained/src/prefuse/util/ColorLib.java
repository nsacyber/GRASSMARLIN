package prefuse.util;

import java.awt.Color;

import prefuse.util.collections.IntObjectHashMap;

/**
 * <p>Library routines for processing color values. The standard color
 * representation used by prefuse is to store each color as single
 * primitive integer value, using 32 bits to represent 4 8-bit color
 * channels: red, green, blue, and alpha (transparency). An alpha value
 * of 0 indicates complete transparency while a maximum value (255)
 * indicated complete opacity. The layout of the bit is as follows,
 * moving from most significant bit on the left to least significant
 * bit on the right:</p>
 * 
 * <pre>
 * AAAAAAAARRRRRRRRGGGGGGGBBBBBBBB
 * </pre>
 * 
 * <p>This class also maintains methods for mapping these values to actual
 * Java {@link java.awt.Color} instances; a cache is maintained for
 * quick-lookups, avoiding the need to continually allocate new Color
 * instances.</p>
 * 
 * <p>Finally, this class also contains routine for creating color
 * palettes for use in visualization.</p>
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ColorLib {

    public static final char HEX_PREFIX = '#';
    
    private static final IntObjectHashMap colorMap = new IntObjectHashMap();
    private static int misses = 0;
    private static int lookups = 0;
    
    // ------------------------------------------------------------------------
    // Color Code Methods
    
    /**
     * Get the color code for the given red, green, and blue values.
     * @param r the red color component (in the range 0-255)
     * @param g the green color component (in the range 0-255)
     * @param b the blue color component (in the range 0-255)
     * @return the integer color code
     */
    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    /**
     * Get the color code for the given grayscale value.
     * @param v the grayscale value (in the range 0-255, 0 is
     * black and 255 is white)
     * @return the integer color code
     */
    public static int gray(int v) {
        return rgba(v, v, v, 255);
    }

    /**
     * Get the color code for the given grayscale value.
     * @param v the grayscale value (in the range 0-255, 0 is
     * black and 255 is white)
     * @param a the alpha (transparency) value (in the range 0-255)
     * @return the integer color code
     */
    public static int gray(int v, int a) {
        return rgba(v, v, v, a);
    }
    
    /**
     * Parse a hexadecimal String as a color code. The color convention
     * is the same as that used in webpages, with two-decimal hexadecimal
     * numbers representing RGB color values in the range 0-255. A single
     * '#' character may be included at the beginning of the String, but
     * is not required. For example '#000000' is black, 'FFFFFF' is white,
     * '0000FF' is blue, and '#FFFF00' is orange. Color values may also
     * include transparency (alpha) values, ranging from 00 (fully transparent)
     * to FF (fully opaque). If included, alpha values should come first in
     * the string. For example, "#770000FF" is a translucent blue.
     * @param hex the color code value as a hexadecimal String
     * @return the integer color code for the input String
     */
    public static int hex(String hex) {
        if ( hex.charAt(0) == HEX_PREFIX )
            hex = hex.substring(1);
        
        if ( hex.length() > 6 ) {
			// break up number, as Integer will puke on a large unsigned int
			int rgb = Integer.parseInt(hex.substring(2), 16);
			int alpha = Integer.parseInt(hex.substring(0,2), 16);
			return ColorLib.setAlpha(rgb, alpha);
		} else {
			return setAlpha(Integer.parseInt(hex, 16), 255);
		}
    }
    
    /**
     * Get the color code for the given hue, saturation, and brightness
     * values, translating from HSB color space to RGB color space.
     * @param h the hue value (in the range 0-1.0). This represents the
     * actual color hue (blue, green, purple, etc). 
     * @param s the saturation value (in the range 0-1.0). This represents
     * "how much" of the color is included. Lower values can result in
     * more grayed out or pastel colors.
     * @param b the brightness value (in the range 0-1.0). This represents
     * how dark or light the color is.
     * @return the integer color code
     */
    public static int hsb(float h, float s, float b) {
        return Color.HSBtoRGB(h,s,b);
    }
    
    /**
     * Get the color code for the given hue, saturation, and brightness
     * values, translating from HSB color space to RGB color space.
     * @param h the hue value (in the range 0-1.0). This represents the
     * actual color hue (blue, green, purple, etc). 
     * @param s the saturation value (in the range 0-1.0). This represents
     * "how much" of the color is included. Lower values can result in
     * more grayed out or pastel colors.
     * @param b the brightness value (in the range 0-1.0). This represents
     * how dark or light the color is.
     * @param a the alpha value (in the range 0-1.0). This represents the
     * transparency of the color.
     * @return the integer color code
     */
    public static int hsba(float h, float s, float b, float a) {
        return setAlpha(Color.HSBtoRGB(h,s,b), (int)(a*255+0.5) & 0xFF);
    }
    
    /**
     * Get the color code for the given red, green, blue, and alpha values.
     * @param r the red color component (in the range 0-255)
     * @param g the green color component (in the range 0-255)
     * @param b the blue color component (in the range 0-255)
     * @param a the alpha (transparency) component (in the range 0-255)
     * @return the integer color code
     */
    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) |
               ((g & 0xFF) << 8)  | ((b & 0xFF) << 0);
    }
    
    /**
     * Get the color code for the given red, green, blue, and alpha values as
     * floating point numbers in the range 0-1.0.
     * @param r the red color component (in the range 0-1.0)
     * @param g the green color component (in the range 0-1.0)
     * @param b the blue color component (in the range 0-1.0)
     * @param a the alpha (transparency) component (in the range 0-1.0)
     * @return the integer color code
     */
    public static int rgba(float r, float g, float b, float a) {
        return ((((int)(a*255+0.5)) & 0xFF) << 24) |
               ((((int)(r*255+0.5)) & 0xFF) << 16) | 
               ((((int)(g*255+0.5)) & 0xFF) <<  8) |
               (((int)(b*255+0.5)) & 0xFF);
    }
    
    /**
     * Get the color code for the given Color instance.
     * @param c the Java Color instance
     * @return the integer color code
     */
    public static int color(Color c) {
        return c.getRGB();
    }
    
    /**
     * Get the red component of the given color.
     * @param color the color code
     * @return the red component of the color (in the range 0-255)
     */
    public static int red(int color) {
        return (color>>16) & 0xFF;
    }
    
    /**
     * Get the green component of the given color.
     * @param color the color code
     * @return the green component of the color (in the range 0-255)
     */
    public static int green(int color) {
        return (color>>8) & 0xFF;
    }
    
    /**
     * Get the blue component of the given color.
     * @param color the color code
     * @return the blue component of the color (in the range 0-255)
     */
    public static int blue(int color) {
        return color & 0xFF;
    }
    
    /**
     * Get the alpha component of the given color.
     * @param color the color code
     * @return the alpha component of the color (in the range 0-255)
     */
    public static int alpha(int color) {
        return (color>>24) & 0xFF;
    }
    
    /**
     * Set the alpha component of the given color.
     * @param c the color code
     * @param alpha the alpha value to set
     * @return the new color with updated alpha channel
     */
    public static int setAlpha(int c, int alpha) {
        return rgba(red(c), green(c), blue(c), alpha);
    }
    
    // ------------------------------------------------------------------------
    // java.awt.Color Lookup Methods
    
    /**
     * Get a Java Color object for the given red, green, blue, and alpha values
     * as floating point numbers in the range 0-1.0.
     * @param r the red color component (in the range 0-1.0)
     * @param g the green color component (in the range 0-1.0)
     * @param b the blue color component (in the range 0-1.0)
     * @param a the alpha (transparency) component (in the range 0-1.0)
     * @return a Java Color object
     */
    public static Color getColor(float r, float g, float b, float a) {
        return getColor(rgba(r,g,b,a));
    }

    /**
     * Get a Java Color object for the given red, green, and blue values
     * as floating point numbers in the range 0-1.0.
     * @param r the red color component (in the range 0-1.0)
     * @param g the green color component (in the range 0-1.0)
     * @param b the blue color component (in the range 0-1.0)
     * @return a Java Color object
     */
    public static Color getColor(float r, float g, float b) {
        return getColor(r,g,b,1.0f);
    }
    
    /**
     * Get a Java Color object for the given red, green, and blue values.
     * @param r the red color component (in the range 0-255)
     * @param g the green color component (in the range 0-255)
     * @param b the blue color component (in the range 0-255)
     * @param a the alpa (transparency) component (in the range 0-255)
     * @return a Java Color object
     */
    public static Color getColor(int r, int g, int b, int a) {
        return getColor(rgba(r,g,b,a));
    }
    
    /**
     * Get a Java Color object for the given red, green, and blue values.
     * @param r the red color component (in the range 0-255)
     * @param g the green color component (in the range 0-255)
     * @param b the blue color component (in the range 0-255)
     * @return a Java Color object
     */
    public static Color getColor(int r, int g, int b) {
        return getColor(r,g,b,255);
    }
    
    /**
     * Get a Java Color object for the given grayscale value.
     * @param v the grayscale value (in the range 0-255, 0 is
     * black and 255 is white)
     * @return a Java Color object
     */
    public static Color getGrayscale(int v) {
        return getColor(v,v,v,255);
    }
    
    /**
     * Get a Java Color object for the given color code value.
     * @param rgba the integer color code containing red, green,
     * blue, and alpha channel information
     * @return a Java Color object
     */
    public static Color getColor(int rgba) {
        Color c = null;
        if ( (c=(Color)colorMap.get(rgba)) == null ) {
            c = new Color(rgba,true);
            colorMap.put(rgba,c);
            misses++;
        }
        lookups++;
        return c;
    }
    
    // ------------------------------------------------------------------------
    // ColorLib Statistics and Cache Management
    
    /**
     * Get the number of cache misses to the Color object cache.
     * @return the number of cache misses
     */
    public static int getCacheMissCount() {
        return misses;
    }

    /**
     * Get the number of cache lookups to the Color object cache.
     * @return the number of cache lookups
     */
    public static int getCacheLookupCount() {
        return lookups;
    }
    
    /**
     * Clear the Color object cache.
     */
    public static void clearCache() {
        colorMap.clear();
    }
    
    
    // ------------------------------------------------------------------------
    // Color Calculations
    
    private static final float scale = 0.7f;
    
    /**
     * Interpolate between two color values by the given mixing proportion.
     * A mixing fraction of 0 will result in c1, a value of 1.0 will result
     * in c2, and value of 0.5 will result in the color mid-way between the
     * two in RGB color space.
     * @param c1 the starting color
     * @param c2 the target color
     * @param frac a fraction between 0 and 1.0 controlling the interpolation
     * amount.
     * @return the interpolated color code
     */
    public static int interp(int c1, int c2, double frac) {
        double ifrac = 1-frac;
        return rgba(
            (int)Math.round(frac*red(c2)   + ifrac*red(c1)),
            (int)Math.round(frac*green(c2) + ifrac*green(c1)),
            (int)Math.round(frac*blue(c2)  + ifrac*blue(c1)),
            (int)Math.round(frac*alpha(c2) + ifrac*alpha(c1)));
    }
    
    /**
     * Get a darker shade of an input color.
     * @param c a color code
     * @return a darkened color code
     */
    public static int darker(int c) {
        return rgba(Math.max(0, (int)(scale*red(c))),
                    Math.max(0, (int)(scale*green(c))),
                    Math.max(0, (int)(scale*blue(c))),
                    alpha(c));
    }

    /**
     * Get a brighter shade of an input color.
     * @param c a color code
     * @return a brighter color code
     */
    public static int brighter(int c) {
        int r = red(c), g = green(c), b = blue(c);
        int i = (int)(1.0/(1.0-scale));
        if ( r == 0 && g == 0 && b == 0) {
           return rgba(i, i, i, alpha(c));
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return rgba(Math.min(255, (int)(r/scale)),
                    Math.min(255, (int)(g/scale)),
                    Math.min(255, (int)(b/scale)),
                    alpha(c));
    }
    
    /**
     * Get a desaturated shade of an input color.
     * @param c a color code
     * @return a desaturated color code
     */
    public static int desaturate(int c) {
        int a = c & 0xff000000;
        float r = ((c & 0xff0000) >> 16);
        float g = ((c & 0x00ff00) >> 8);
        float b = (c & 0x0000ff);

        r *= 0.2125f; // red band weight
        g *= 0.7154f; // green band weight
        b *= 0.0721f; // blue band weight

        int gray = Math.min(((int)(r+g+b)),0xff) & 0xff;
        return a | (gray << 16) | (gray << 8) | gray;
    }
    
    /**
     * Set the saturation of an input color.
     * @param c a color code
     * @param saturation the new sautration value
     * @return a saturated color code
     */
    public static int saturate(int c, float saturation) {
        float[] hsb = Color.RGBtoHSB(red(c), green(c), blue(c), null);
        return ColorLib.hsb(hsb[0], saturation, hsb[2]);
    }
    
    // ------------------------------------------------------------------------
    // Color Palettes
    
    /**
     * Default palette of category hues.
     */
    public static final float[] CATEGORY_HUES = {
        0f, 1f/12f, 1f/6f, 1f/3f, 1f/2f, 7f/12f, 2f/3f, /*3f/4f,*/ 5f/6f, 11f/12f
    };
    
    /**
     * The default length of a color palette if its size
     * is not otherwise specified.
     */
    public static final int DEFAULT_MAP_SIZE = 64;

    /**
     * Returns a color palette that uses a "cool", blue-heavy color scheme.
     * @param size the size of the color palette
     * @return the color palette
     */
    public static int[] getCoolPalette(int size) {
        int[] cm = new int[size];
        for( int i=0; i<size; i++ ) {
            float r = i / Math.max(size-1,1.f);
            cm[i] = rgba(r,1-r,1.f,1.f);
        }
        return cm;
    }

    /**
     * Returns a color palette of default size that uses a "cool",
     * blue-heavy color scheme.
     * @return the color palette
     */
    public static int[] getCoolPalette() {
        return getCoolPalette(DEFAULT_MAP_SIZE);
    }

    /**
     * Returns a color map that moves from black to red to yellow
     * to white.
     * @param size the size of the color palette
     * @return the color palette
     */
    public static int[] getHotPalette(int size) {
        int[] cm = new int[size];
        for ( int i=0; i<size; i++ ) {
            int n = (3*size)/8;
            float r = ( i<n ? ((float)(i+1))/n : 1.f );
            float g = ( i<n ? 0.f : ( i<2*n ? ((float)(i-n))/n : 1.f ));
            float b = ( i<2*n ? 0.f : ((float)(i-2*n))/(size-2*n) );
            cm[i] = rgba(r,g,b,1.0f);
        }
        return cm;
    }

    /**
     * Returns a color map of default size that moves from black to
     * red to yellow to white.
     * @return the color palette
     */
    public static int[] getHotPalette() {
        return getHotPalette(DEFAULT_MAP_SIZE);
    }

    /**
     * Returns a color palette of given size tries to provide colors
     * appropriate as category labels. There are 12 basic color hues
     * (red, orange, yellow, olive, green, cyan, blue, purple, magenta,
     * and pink). If the size is greater than 12, these colors will be
     * continually repeated, but with varying saturation levels.
     * @param size the size of the color palette
     * @param s1 the initial saturation to use
     * @param s2 the final (most distant) saturation to use
     * @param b the brightness value to use
     * @param a the alpha value to use
     */
    public static int[] getCategoryPalette(int size, 
            float s1, float s2, float b, float a)
    {
        int[] cm = new int[size];
        float s = s1;
        for ( int i=0; i<size; i++ ) {
            int j = i % CATEGORY_HUES.length;
            if ( j == 0 )
                s = s1 + (((float)i)/size)*(s2-s1);
            cm[i] = hsba(CATEGORY_HUES[j],s,b,a);
        }
        return cm;
    }

    /**
     * Returns a color palette of given size tries to provide colors
     * appropriate as category labels. There are 12 basic color hues
     * (red, orange, yellow, olive, green, cyan, blue, purple, magenta,
     * and pink). If the size is greater than 12, these colors will be
     * continually repeated, but with varying saturation levels.
     * @param size the size of the color palette
     */
    public static int[] getCategoryPalette(int size) {
        return getCategoryPalette(size, 1.f, 0.4f, 1.f, 1.0f);
    }

    /**
     * Returns a color palette of given size that cycles through
     * the hues of the HSB (Hue/Saturation/Brightness) color space.
     * @param size the size of the color palette
     * @param s the saturation value to use
     * @param b the brightness value to use
     * @return the color palette
     */
    public static int[] getHSBPalette(int size, float s, float b) {
        int[] cm = new int[size];
        for ( int i=0; i<size; i++ ) {
            float h = ((float)i)/(size-1);
            cm[i] = hsb(h,s,b);
        }
        return cm;
    }

    /**
     * Returns a color palette of default size that cycles through
     * the hues of the HSB (Hue/Saturation/Brightness) color space at
     * full saturation and brightness.
     * @return the color palette
     */
    public static int[] getHSBPalette() {
        return getHSBPalette(DEFAULT_MAP_SIZE, 1.f, 1.f);
    }

    /**
     * Returns a color palette of given size that ranges from one
     * given color to the other.
     * @param size the size of the color palette
     * @param c1 the initial color in the color map
     * @param c2 the final color in the color map
     * @return the color palette
     */
    public static int[] getInterpolatedPalette(int size, int c1, int c2)
    {
        int[] cm = new int[size];
        for ( int i=0; i<size; i++ ) {
            float f = ((float)i)/(size-1);
            cm[i] = interp(c1,c2,f);
        }
        return cm;
    }

    /**
     * Returns a color palette of default size that ranges from one
     * given color to the other.
     * @param c1 the initial color in the color map
     * @param c2 the final color in the color map
     * @return the color palette
     */
    public static int[] getInterpolatedPalette(int c1, int c2) {
        return getInterpolatedPalette(DEFAULT_MAP_SIZE, c1, c2);
    }

    /**
     * Returns a color palette of specified size that ranges from white to
     * black through shades of gray.
     * @param size the size of the color palette
     * @return the color palette
     */
    public static int[] getGrayscalePalette(int size) {
        int[] cm = new int[size];
        for ( int i=0, g; i<size; i++ ) {
            g = (int)Math.round(255*(0.2f + 0.6f*((float)i)/(size-1)));
            cm[size-i-1] = gray(g);
        }
        return cm;
    }

    /**
     * Returns a color palette of default size that ranges from white to
     * black through shades of gray.
     * @return the color palette
     */
    public static int[] getGrayscalePalette() {
        return getGrayscalePalette(DEFAULT_MAP_SIZE);
    }
    
} // end of class ColorLib
