package grassmarlin.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

//TODO: Justify the creation of a ConfidenceManager class so that we can make ConMan references.
public enum Confidence {
    NONE(10, "None", "The confidence could not be assessed"),
    LOW(5, "Low", "Specifying Data Easily Spoofed, Decent Chance of False Positive"),
    MEDIUM_LOW(4, "Medium Low", "Single Specifying Self Reported Data Point, Chance of False Positive"),
    MEDIUM(3, "Medium", "Multiple Agreeing Self Reported Data Points, Some Chance of False Positive"),
    MEDIUM_HIGH(2, "Medium High", "Single Specifying Data Point, Low Probability of False Positive"),
    HIGH(1, "High", "Multiple Agreeing Data Points, Very Low Probability of False Positive"),
    USER(0, "User Defined", "Manually Entered by a User, or known with 100% certainty.");

    private final int value;
    private final String pretty;
    private final String description;
    private static final Collection<Confidence> assignableConfidences = Collections.unmodifiableCollection(Arrays.asList(HIGH, MEDIUM_HIGH, MEDIUM, MEDIUM_LOW, LOW));

    Confidence(int value, String pretty, String description) {
        this.value = value;
        this.pretty = pretty;
        this.description = description;
    }

    public static Collection<Confidence> getAssignableConfidenceList() {
        return assignableConfidences;
    }

    public int asNumber() {
        return this.value;
    }

    public String asString() {
        return this.pretty;
    }

    public String getDescription() {
        return this.description;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    public static Confidence fromNumber(final int value) {
        return Arrays.stream(Confidence.values()).filter(confidence -> confidence.value == value).findAny().orElse(null);
    }

    public static Confidence fromString(final String value) {
        return Arrays.stream(Confidence.values())
                .filter(confidence -> confidence.asString().equals(value))
                .findAny()
                .orElse(
                        Arrays.stream(Confidence.values())
                                .filter(confidence -> Integer.toString(confidence.asNumber()).equals(value))
                                .findAny()
                                .orElse(null)
                );
    }

    public static Comparator<Confidence> COMPARATOR = (o1, o2) -> {
        if(o1 == null) {
            return o2 == null ? 0 : -1;
        } else if(o2 == null) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    };
    public static Confidence max(final Confidence o1, final Confidence o2) {
        if(COMPARATOR.compare(o1, o2) == 1) {
            return o2;
        } else {
            return o1;
        }
    }
}
