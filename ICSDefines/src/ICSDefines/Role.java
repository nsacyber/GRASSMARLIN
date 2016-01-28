package ICSDefines;

/**
 * Created by BESTDOG on 11/24/2015.
 */
public enum Role implements PrettyPrintEnum {
    CLIENT, SERVER, MASTER, SLAVE, OPERATOR, ENGINEER, UNKNOWN, OTHER;

    @Override
    public Enum get() {
        return this;
    }
}
