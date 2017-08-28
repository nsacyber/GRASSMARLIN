/**
 * Might never be used, and when I need it I will probably forget it is here, but it produces so much cleaner code in the few cases where it might be needed (and is remembered).
 */
public class Coalesce {
    public static <T> T of(final T arg1, final T arg2) {
        return arg1 == null ? arg2 : arg1;
    }
    public static <T> T of(final T... args) {
        for(final T arg : args) {
            if(arg != null) {
                return arg;
            }
        }
        return null;
    }
}
