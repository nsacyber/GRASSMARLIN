package prefuse.util;

/**
 * A color map provides a mapping from numeric values to specific colors.
 * This useful for assigning colors to visualized items. The numeric values
 * may represent different categories (i.e. nominal variables) or run along
 * a spectrum of values (i.e. quantitative variables).
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ColorMap {

    private int[] palette;
    private double minValue, maxValue;
    
    /**
     * Creates a new ColorMap instance using the given internal color map
     * array and minimum and maximum index values.
     * @param map the color palette, an int array of color values
     * @param min the minimum value in the color map
     * @param max the maximum value in the color map
     */
    public ColorMap(int[] map, double min, double max) {
        palette = map;
        minValue = min;
        maxValue = max;
    }
    
    /**
     * Returns the color associated with the given value. If the value
     * is outside the range defined by this map's minimum or maximum
     * values, a endpoint value is returned (i.e. the first entry
     * in the color map for values below the minimum, the last enty
     * for value above the maximum).
     * @param val the value for which to retrieve the color
     * @return the color corresponding the given value
     */
    public int getColor(double val) {
        if ( val < minValue ) {
            return palette[0];
        } else if ( val >= maxValue ) {
            return palette[palette.length-1];
        } else {
            int idx = (int)(palette.length *
                            (val-minValue)/(maxValue-minValue));
            return palette[idx];
        }
    }

    /**
     * Gets the internal color palette, an int array of color values.
     * @return returns the color palette.
     */
    public int[] getColorPalette() {
        return palette;
    }

    /**
     * Sets the internal color palette, an int array of color values.
     * @param palette the new palette.
     */
    public void setColorPalette(int[] palette) {
        this.palette = palette;
    }

    /**
     * Gets the maximum value that corresponds to the last
     * color in the color map.
     * @return returns the max index value into the color map.
     */
    public double getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the maximum value that corresponds to the last
     * color in the color map.
     * @param maxValue the new max index value.
     */
    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the minimum value that corresponds to the first
     * color in the color map.
     * @return Returns the min index value.
     */
    public double getMinValue() {
        return minValue;
    }

    /**
     * Sets the minimum value that corresponds to the first
     * color in the color map.
     * @param minValue the new min index value.
     */
    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }
    
} // end of class ColorMap
