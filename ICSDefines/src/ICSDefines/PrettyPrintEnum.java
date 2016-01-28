package ICSDefines;

/**
 * Created by BESTDOG on 11/24/2015.
 */
public interface PrettyPrintEnum {

    String name();

    Enum get();

    default String getPrettyPrint() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }

}
