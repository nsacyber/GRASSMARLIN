package TemplateEngine.Template4.Structure;

import TemplateEngine.Template4.*;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author BESTDOG
 *         <p>
 *         Template4 for Java Class Method; specifically methods which meet the erasure of {@link core.fingerprint.Operation}.
 */
public class MethodTemplate extends RegularTemplate implements ClassAdapter {

    Supplier<List<List<VariableDeclaration>>> filterSupplier;
    Boolean override;
    Class returnType;
    Class genericReturnType;
    List<Variable> argumentList;
    List<Variable> initialVars;
    String methodName;
    String scopeName;
    Template body;

    public MethodTemplate() {
        super("ClassMethod");
        this.argumentList = new ArrayList<>();
        this.initialVars = new ArrayList<>();
    }

    /**
     * Sets generic arguments to the return type.
     * @param genericReturnType Class for the generic type of the return type.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setGenericReturnType(Class genericReturnType) {
        this.genericReturnType = genericReturnType;
        return this;
    }

    /**
     * If this method will contain a "Filter check" after being routed through the Filter object.
     *
     * @return true if present, else false.
     */
    public boolean hasFilterSupplier() {
        return this.filterSupplier != null;
    }

    /**
     * @return List of Groups of Variables within the filter check.
     */
    public List<List<VariableDeclaration>> getFilters() {
        return filterSupplier.get();
    }

    /**
     * Sets the conditional block which is ran when the filter-check predicates.
     *
     * This has the same funtionality of ... except this will not
     * caused the body to be set as the child of this template.
     *
     * @param conditionalBlock Block of code containing within the filter-check.
     */
    public void setMethodConditionalBody(ConditionalBlock conditionalBlock) {
        this.body = conditionalBlock;
        filterSupplier = conditionalBlock.getExpression()::getExpressions;
    }

    /**
     * Adds a variable to the initial scope of the method.
     *
     * @param var Var to add to scope.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate addInitialVariable(Variable var) {
        this.initialVars.add(var);
        return this;
    }

    /**
     * Sets the initial vars which are set in above the filter-check.
     * @param initialVars List of variables to declare at the top of this method.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setInitialVars(List<Variable> initialVars) {
        this.initialVars = initialVars;
        return this;
    }

    /**
     * Sets the argument list for this method.
     * @param argumentList List of variables rendered in this methods parameter list.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setArgumentList(List<Variable> argumentList) {
        this.argumentList = argumentList;
        return this;
    }

    /**
     * @param override If true will include the @Override annotation.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setOverride(Boolean override) {
        this.override = override;
        return this;
    }

    /**
     * @param body Sets the body of this method, adding this Template4 as the Body's parent Template4.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setMethodbody(Template body) {
        this.body = body;
        body.setParent(this);
        return this;
    }

    /**
     * @param returnType The return type of this method, null for void method.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setReturnType(Class returnType) {
        this.returnType = returnType;
        return this;
    }

    /**
     * @return The template within the body of this method.
     */
    public Template getMethodBody() {
        return body;
    }

    /**
     * Returns the name this method will have when generated.
     * @return Name of this method.
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param methodName Name which will be used in the generated code of this class-method.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    /**
     * @return Rendered {@link #getBody()} template if present, else empty String.
     */
    public String getRenderedBody() {
        String s;
        if (body == null) {
            s = "";
        } else {
            s = body.render();
        }
        return s;
    }

    /**
     * Adds a single argument to this method's parameter list.
     * @param var Variable to ass to parameter list.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate addArgument(Variable var) {
        this.argumentList.add(var);
        return this;
    }

    @Override
    public String getScopeName() {
        return this.scopeName;
    }

    /**
     * Allows the scope returned by this Template4 {@link Template#getScopeName()} method to be set. And is the
     * reason for ... not declaring this Template4 as the provided body's parent.
     * @param scopeName Name of this method's scope.
     * @return This reference is returned so that methods may be chained.
     */
    public MethodTemplate setScopeName(String scopeName) {
        this.scopeName = scopeName;
        return this;
    }

    @Override
    public void render(ST st) {
        $.name.add(st, methodName);
        $.body.add(st, getRenderedBody());
        $.initVars.add(st, initialVars);
        $.override.add(st, override);
        $.argList.add(st, argumentList);
        $.genericType.add(st, this.genericReturnType);
        if (returnType != null) {
            $.returnType.add(st, returnType.getSimpleName());
        } else {
            $.returnType.add(st, "void");
        }
    }

    @Override
    public void onAppend(ClassTemplate template) {
        if (this.body instanceof ClassAdapter) {
            ((ClassAdapter) this.body).onAppend(template);
        } else {
            if (this.body instanceof NestedBlock) {
                NestedBlock block = (NestedBlock) this.body;
                while (block.hasBody()) {
                    block = block.getBody();
                    if (block instanceof ClassAdapter) {
                        ((ClassAdapter) block).onAppend(template);
                    }
                }
            }
        }
    }

    /**
     * Parameters of the ClassMethod.st
     */
    private enum $ implements TemplateAccessor {
        name, body, initVars, argList, returnType, genericType, override;
    }

}
