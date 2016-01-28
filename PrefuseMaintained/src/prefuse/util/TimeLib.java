package prefuse.util;

import java.lang.reflect.Constructor;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Library routines for dealing with times and time spans. All time values
 * are given as long values, indicating the number of milliseconds since
 * the epoch (January 1, 1970). This is the same time format returned
 * by the {@link java.lang.System#currentTimeMillis()} method.
 * 
 * @author jeffrey heer
 */
public class TimeLib {

    /** Represents a millenium, 1000 years. */
    public static final int MILLENIUM = -1000;
    /** Represents a century, 100 years */
    public static final int CENTURY   = -100;
    /** Represents a decade, 10 years */
    public static final int DECADE    = -10;
    
    private static final double SECOND_MILLIS    = 1000;
    private static final double MINUTE_MILLIS    = SECOND_MILLIS*60;
    private static final double HOUR_MILLIS      = MINUTE_MILLIS*60;
    private static final double DAY_MILLIS       = HOUR_MILLIS * 24.0015;
    private static final double WEEK_MILLIS      = DAY_MILLIS * 7;
    private static final double MONTH_MILLIS     = DAY_MILLIS * 30.43675;
    private static final double YEAR_MILLIS      = WEEK_MILLIS * 52.2;
    private static final double DECADE_MILLIS    = YEAR_MILLIS * 10;
    private static final double CENTURY_MILLIS   = DECADE_MILLIS * 10;
    private static final double MILLENIUM_MILLIS = CENTURY_MILLIS * 10;
    
    private static final int[] CALENDAR_FIELDS = {
        Calendar.YEAR, Calendar.MONTH, Calendar.DATE, Calendar.HOUR_OF_DAY,
        Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND    
    };
    
    private TimeLib() {
        // prevent instantiation
    }
    
    /**
     * Get the number of time units between the two given timestamps.
     * @param t0 the first timestamp (as a long)
     * @param t1 the second timestamp (as a long)
     * @param field the time unit to use, one of the {@link java.util.Calendar}
     * fields, or one of the extended fields provided by this class
     * (MILLENIUM, CENTURY, or DECADE).
     * @return the number of time units between the two timestamps
     */
    public static int getUnitsBetween(long t0, long t1, int field) {
        boolean negative = false;
        if ( t1 < t0 ) {
            long tmp = t1; t1 = t0; t0 = tmp; // swap
            negative = true;
        }
        GregorianCalendar gc1 = new GregorianCalendar();
        GregorianCalendar gc2 = new GregorianCalendar();
        gc1.setTimeInMillis(t0);
        gc2.setTimeInMillis(t1);
        
        // add 2 units less than the estimate to 1st date,
        // then serially add units till we exceed 2nd date
        int est = estimateUnitsBetween(t0, t1, field);
        boolean multiYear = isMultiYear(field);
        if ( multiYear ) {
            gc1.add(Calendar.YEAR, -field*(est-2));
            est = -field*est;
        } else {
            gc1.add(field, est-2);
        }
        int f = multiYear ? Calendar.YEAR : field;
        int inc = multiYear ? -field : 1;
        for( int i=est-inc; ; i+=inc ) {
            gc1.add(f, inc);
            if( gc1.after(gc2) ) {
                return negative ? inc-i : i-inc;
            }
        }
    }
    
    /**
     * Based on code posted at
     *  http://forum.java.sun.com/thread.jspa?threadID=488676&messageID=2292012
     */
    private static int estimateUnitsBetween(long t0, long t1, int field) {
        long d = t1-t0;
        switch (field) {
        case Calendar.MILLISECOND:
            return (int)d; // this could be very inaccurate. TODO: use long instead of int?
        case Calendar.SECOND:
            return (int)(d / SECOND_MILLIS + .5);
        case Calendar.MINUTE:
            return (int)(d / MINUTE_MILLIS + .5);
        case Calendar.HOUR_OF_DAY:
        case Calendar.HOUR:
            return (int)(d / HOUR_MILLIS + .5);
        case Calendar.DAY_OF_WEEK_IN_MONTH :
        case Calendar.DAY_OF_MONTH :
        // case Calendar.DATE : // codes to same int as DAY_OF_MONTH
            return (int) (d / DAY_MILLIS + .5);
        case Calendar.WEEK_OF_YEAR :
            return (int) (d / WEEK_MILLIS + .5);
        case Calendar.MONTH :
            return (int) (d / MONTH_MILLIS + .5);
        case Calendar.YEAR :
            return (int) (d / YEAR_MILLIS + .5);
        case DECADE:
            return (int) (d / DECADE_MILLIS + .5);
        case CENTURY:
            return (int) (d / CENTURY_MILLIS + .5);
        case MILLENIUM:
            return (int) (d / MILLENIUM_MILLIS + .5);
        default:
            return 0;
        }
    }
        
    /**
     * Increment a calendar by a given number of time units.
     * @param c the calendar to increment
     * @param field the time unit to increment, one of the
     * {@link java.util.Calendar} fields, or one of the extended fields
     * provided by this class (MILLENIUM, CENTURY, or DECADE).
     * @param val the number of time units to increment by
     */
    public static void increment(Calendar c, int field, int val) {
        if ( isMultiYear(field) ) {
            c.add(Calendar.YEAR, -field*val);
        } else {
            c.add(field, val);
        }
    }
    
    /**
     * Get the value of the given time field for a Calendar. Just like the
     * {@link java.util.Calendar#get(int)} method, but include support for
     * the extended fields provided by this class (MILLENIUM, CENTURY, or
     * DECADE).
     * @param c the Calendar
     * @param field the time field
     * @return the value of the time field for the given calendar
     */
    public static int get(Calendar c, int field) {
        if ( isMultiYear(field) ) {
            int y = c.get(Calendar.YEAR);
            return -field * (y/-field);
        } else {
            return c.get(field);
        }
    }
    
    // ------------------------------------------------------------------------
    // Date Access
    
    /**
     * Get the timestamp for the given year, month, and, day.
     * @param c a Calendar to use to help compute the result. The state of the
     * Calendar will be overwritten.
     * @param year the year to look up
     * @param month the month to look up (months start at 0==January)
     * @param day the day to look up
     * @return the timestamp for the given date
     */
    public static long getDate(Calendar c, int year, int month, int day) {
        c.clear(Calendar.MILLISECOND);
        c.set(year, month, day, 0, 0, 0);
        return c.getTimeInMillis();
    }

    /**
     * Get a timestamp for the given hour, minute, and second. The date will
     * be assumed to be January 1, 1970.
     * @param c a Calendar to use to help compute the result. The state of the
     * Calendar will be overwritten.
     * @param hour the hour, on a 24 hour clock
     * @param minute the minute value
     * @param second the seconds value
     * @return the timestamp for the given date
     */
    public static long getTime(Calendar c, int hour, int minute, int second) {
        c.clear(Calendar.MILLISECOND);
        c.set(1970, 0, 1, hour, minute, second);
        return c.getTimeInMillis();
    }
    
    /**
     * Get a new Date instance of the specified subclass and given long value.
     * @param type the concrete subclass of the Date instance, must be an
     * instance of subclass of java.util.Date
     * @param d the date/time value as a long
     * @return the new Date instance, or null if the class type is not valid
     */
    public static Date getDate(Class type, long d) {
        try {
            Constructor c = type.getConstructor(new Class[] {long.class});
            return (Date)c.newInstance(new Object[] {new Long(d)});
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
    }
    
    // ------------------------------------------------------------------------
    // Date Normalization
    
    /**
     * Get the timestamp resulting from clearing (setting to zero) all time
     * values less than or equal to that of the given field. For example,
     * clearing to {@link Calendar#HOUR} will floor the time to nearest
     * hour which occurred before or at the given time (e.g., 1:32
     * --> 1:30).
     * @param t the reference time
     * @param c a Calendar instance used to help compute the value, the
     * state of the Calendar will be overwritten.
     * @param field the time field to clear to, one of the
     * {@link java.util.Calendar} fields, or one of the extended fields
     * provided by this class (MILLENIUM, CENTURY, or DECADE).
     * @return the cleared time
     */
    public static long getClearedTime(long t, Calendar c, int field) {
        c.setTimeInMillis(t);
        TimeLib.clearTo(c, field);
        return c.getTimeInMillis();
    }
    
    /**
     * Clear the given calendar, setting to zero all time
     * values less than or equal to that of the given field. For example,
     * clearing to {@link Calendar#HOUR} will floor the time to nearest
     * hour which occurred before or at the given time (e.g., 1:32
     * --> 1:30).
     * @param c the Calendar to clear
     * @param field the time field to clear to, one of the
     * {@link java.util.Calendar} fields, or one of the extended fields
     * provided by this class (MILLENIUM, CENTURY, or DECADE).
     * @return the original Calendar reference, now set to the cleared time
     */
    public static Calendar clearTo(Calendar c, int field) {
        int i = CALENDAR_FIELDS.length-1;
        for ( ; i>=1 && field != CALENDAR_FIELDS[i]; i-- ) {
            int val = (CALENDAR_FIELDS[i]==Calendar.DATE?1:0);
            c.set(CALENDAR_FIELDS[i],val);
        }
        if ( isMultiYear(field) ) {
            int y = c.get(Calendar.YEAR);
            y = -field * (y/-field);
            c.set(Calendar.YEAR, y);
        }
        return c;
    }
    
    /**
     * Indicates if a field value indicates a timespan greater than one
     * year. These multi-year spans are the extended fields introduced by
     * this class (MILLENIUM, CENTURY, and DECADE).
     * @param field the time field
     * @return true if the field is multi-year, false otherwise
     */
    public static boolean isMultiYear(int field) {
        return ( field == DECADE || field == CENTURY || field == MILLENIUM );
    }
    
} // end of class TimeLib
