package TemplateEngine.Template4;

import TemplateEngine.Template4.Structure.Variable;
import org.stringtemplate.v4.ST;

import java.util.List;
import java.util.Map;

/**
 * Created by BESTDOG on 11/18/2015.
 * Interface allows for ease-of-use with Enumerations for fulfilling ST4 templates (.st & .stg) required parameters
 * with properly formatted and content-specific values.
 *
 * Some unnecessary operations are omitted, like adding by zero, by not including the template parameter at all.
 *
 */
public interface TemplateAccessor {
    /**
     * Indented to be fulfilled by the erasure of {@link Enum#name()}.
     * @return Name of the object associated with some template parameter.
     */
    String name();

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param val Parameter set to the {@link Enum#name()} value or, false if null.
     */
    default void add(ST template, Enum val) {
        if( val == null ) {
            template.add(this.name(), false);
        } else {
            template.add(this.name(), val.name());
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param val Object array to add.
     */
    default void add(ST template, Object[] val) {
        template.add(this.name(), val);
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param val Adds Integer if not null or not zero.
     */
    default void add(ST template, Integer val) {
        if( val != null && val != 0 ) {
            template.add(this.name(), val);
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param val Adds String if not null or not empty.
     */
    default void add(ST template, String val) {
        if( val != null && !val.isEmpty() ) {
            template.add(this.name(), val);
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param t Adds Template4 by calling the {@link Template#render()} method.
     */
    default void add(ST template, Template t) {
        if( t != null ) {
            template.add(this.name(), t.render());
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param t Adds StringTemplate by calling the {@link ST#render()} method.
     */
    default void add(ST template, ST t) {
        if( t != null ) {
            template.add(this.name(), t.render());
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param c Adds class by its {@link Class#getSimpleName()} value.
     */
    default void add(ST template, Class c) {
        if( c != null ) {
            template.add(this.name(), c.getSimpleName());
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param val Adds Boolean if not null.
     */
    default void add(ST template, Boolean val) {
        if( val != null ) {
            template.add(this.name(), val);
        }
    }

    /**
     * @param template Template4 to append for the property-key returned by the {@link #name()} method.
     * @param map Map to be converted to a matrix of [ [k0, v0], [k1, v1], ... ]
     */
    default void add(ST template, Map<String,String> map) {
        Object[] obj = new Object[map.size()];
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            obj[i++] = new Object[] { e.getKey(), e.getValue() };
        }
        template.add(this.name(), obj);
    }

    default void add(ST template, List<Variable> list) {
        if( list != null && !list.isEmpty() ) {
            template.add(this.name(), list.toArray());
        }
    }
}
