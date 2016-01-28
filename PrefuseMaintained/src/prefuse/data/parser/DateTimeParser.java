package prefuse.data.parser;

import java.text.DateFormat;

/**
 * DataParser instance that parses Date values as java.util.Date instances,
 * representing a particular date and time.
 * This class uses a backing {@link java.text.DateFormat} instance to
 * perform parsing. The DateFormat instance to use can be passed in to the
 * constructor, or by default the DateFormat returned by
 * {@link java.text.DateFormat#getDateTimeInstance(int, int)} with both
 * arguments being {@link java.text.DateFormat#SHORT} is used.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DateTimeParser extends DateParser {
    
    /**
     * Create a new DateTimeParser.
     */
    public DateTimeParser() {
        this(DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT));
    }
    
    /**
     * Create a new DateTimeParser.
     * @param dateFormat the DateFormat instance to use for parsing
     */
    public DateTimeParser(DateFormat dateFormat) {
        super(dateFormat);
    }

} // end of class DateTimeParser
