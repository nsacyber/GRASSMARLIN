/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package TemplateEngine.Template4.Structure;

/**
 *
 * @author BESTDOG
 *
 * A {@link Variable} with initial value, optional static directive, and optional canonical code generation for the
 * class of the declared variable.
 */
public class VariableDeclaration extends Variable {
    
    public String initialVal;
    boolean useCanonical;
    boolean isStatic;

    public VariableDeclaration(Variable var, String val) {
        this(var.clazz, var.name, val);
    }

    public VariableDeclaration(Class clazz, String var) {
        this(clazz, var, "");
    }
    public VariableDeclaration(Class clazz, String var, String val) {
        super(clazz, var);
        this.initialVal = val;
        this.isStatic = false;
        this.useCanonical = false;
    }
    
    public VariableDeclaration setIsStatic(boolean isStatic) {
        this.isStatic = isStatic;
        return this;
    }

    public String getValue() {
        return this.initialVal;
    }

    @Override
    public String toString() {
        String prefix = isStatic ? "public static final " : "";
        String valString = initialVal.isEmpty() ? "" : " = " + getValue();
        String clazzName = useCanonical ? clazz.getCanonicalName() : clazz.getSimpleName();
        return String.format("%s%s %s%s;", prefix, clazzName, name, valString);
    }

    public VariableDeclaration useCanonicalName(boolean useCanonical) {
        this.useCanonical = useCanonical;
        return this;
    }
}
