package TemplateEngine.Template4.Structure;

/**
 * Created by BESTDOG on 11/19/2015.
 * <p>
 * A {@link Variable} with an initial value.
 * </p>
 */
public class VariableInitialization extends Variable {

    String value;

    public VariableInitialization(Class clazz, String name) {
        this(clazz, name, "");
    }

    public VariableInitialization(Class clazz, String name, String value) {
        super(clazz, name);
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("%s = %s", this.name, this.value);
    }

}
