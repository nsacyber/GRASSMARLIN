package TemplateEngine.Template4.Structure;

/**
 * Created by BESTDOG on 11/13/2015.
 *
 * A {@link Variable} which renders a class with generic types.
 */
public class GenericVariable extends Variable {

    Class[] generics;

    public GenericVariable(Class clazz, String name, Class... generics) {
        super(clazz, name);
        this.generics = generics;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final String separator = ",";
        String sep = "";

        sb.append(this.clazz.getSimpleName()).append("<");
        for( int i = 0; i < generics.length; ++i ) {
            sb.append(sep).append(generics[i].getSimpleName());
            sep = separator;
        }
        sb.append("> ").append(this.name);
        return sb.toString();
    }
}
