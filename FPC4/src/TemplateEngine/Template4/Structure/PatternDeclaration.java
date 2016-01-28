package TemplateEngine.Template4.Structure;

import java.util.regex.Pattern;

/**
 * Created by BESTDOG on 11/16/2015.
 *
 * A special {@link VariableDeclaration} for {@link java.util.regex.Pattern}.
 *
 * Has an optional 'ignore case' declaration.
 */
public class PatternDeclaration extends VariableDeclaration {

    final String pattern;

    public PatternDeclaration(String name, String pattern) {
        this(name, String.format("Pattern.compile(\"%s\")", pattern), false);
    }

    public PatternDeclaration(String name, String pattern, boolean ignoreCase) {
        super(Pattern.class, name, String.format("Pattern.compile(\"%s\"%s)", pattern, ignoreCase ? ", Pattern.CASE_INSENSITIVE" : ""));
        this.setIsStatic(true);
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public static boolean patternValid(String pattern) {
        boolean valid = false;
        try {
            Pattern.compile(pattern);
            valid = true;
        } catch(Exception | Error ex) {
            /** Test pattern - do nothing */
        }
        return valid;
    }

}
