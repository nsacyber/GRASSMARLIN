package TemplateEngine.Template4.Structure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import TemplateEngine.Data.FunctionalFingerprint;
import TemplateEngine.Template4.Structure.Code.FingerprintGetMethod;
import TemplateEngine.Template4.TemplateAccessor;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.Template;

/**
 * @author BESTDOG
 *
 * Template4 for a Java-source file containing a single class; semi-specific to the needs of the
 * {@link FunctionalFingerprint} class.
 */
public class ClassTemplate implements Template {

    private enum $ implements TemplateAccessor {
        packageDrtv,
        importsDrtv,
        classDclr,
        globalDclr,
        interfaceDclr,
        interfaceGeneric,
        className,
        classBody,
        staticInit,
        methods;
    }

    String scopeName;
    String template;
    Package classPackage;
    String classDirectives;
    String className;
    String staticCodeBlock;
    Class implementingClass;
    Class interfaceGeneric;
    Class returnType;
    ImportsTemplate classImports;
    List<VariableDeclaration> globalVars;
    List<MethodTemplate> methods;
    List<Variable> initialVars;

    FingerprintGetMethod getMethod;

    public ClassTemplate(Class returnType) {
        this.methods = new ArrayList<>();
        this.globalVars = new ArrayList<>();
        this.initialVars = new ArrayList<>();
        this.classImports = new ImportsTemplate();
        this.getMethod = new FingerprintGetMethod(returnType);
        this.returnType = returnType;
        this.interfaceGeneric = returnType;
    }

    /**
     * Adds a variable to the initial scope of the method.
     * @param var Var to add to scope.
     * @return This reference is returned so that methods may be chained.
     */
    public ClassTemplate addInitialVariable(Variable var) {
        this.initialVars.add(var);
        return this;
    }

    public ClassTemplate addMethod(MethodTemplate method) {
        methods.add(method);
        method.setInitialVars(this.initialVars);
        method.setParent(this);
        getMethod.addMethodIdentifier(method.getMethodName());
        method.onAppend(this);
        return this;
    }

    public ClassTemplate setClassImports(ImportsTemplate template) {
        this.classImports = template;
        return this;
    }

    public ClassTemplate setStaticCodeBlock(String staticCodeBlock) {
        this.staticCodeBlock = staticCodeBlock;
        return this;
    }
    
    public ClassTemplate setClassPackage(Package classPackage) {
        this.classPackage = classPackage;
        return this;
    }

    public ClassTemplate setImplementingClass(Class implementingClass) {
        this.implementingClass = implementingClass;
        return this;
    }
    
    public ClassTemplate setClassName(String className) {
        this.className = className;
        return this;
    }

    public ClassTemplate setClassDirectives(String classDirectives) {
        this.classDirectives = classDirectives;
        return this;
    }

    public ClassTemplate addVariable( VariableDeclaration declr ) {
        globalVars.add(declr);
        return this;
    }

    public ImportsTemplate getClassImports() {
        return classImports;
    }

    public ClassTemplate setScopeName(String scopeName) {
        this.scopeName = scopeName;
        return this;
    }

    public List<MethodTemplate> getMethods() {
        return methods;
    }

    public String getClassName() {
        return className;
    }

    public Class getImplementingClass() {
        return implementingClass;
    }

    @Override
    public void render(ST st) {
        if( this.classPackage != null ) {
            $.packageDrtv.add(st, this.classPackage.getName());
        }
        if( this.classImports != null ) {
            $.importsDrtv.add(st, this.classImports.render());
        }
        $.className.add(st, this.className);
        $.classDclr.add(st, this.classDirectives);
        $.staticInit.add(st, this.staticCodeBlock);
        if( this.implementingClass != null ) {
            $.interfaceDclr.add(st, this.implementingClass.getSimpleName());
            $.interfaceGeneric.add(st, this.interfaceGeneric.getSimpleName());
        }
        $.globalDclr.add(st, this.globalVars.toArray());
        $.methods.add(st, toRenderedTemplateArray(this.methods));
        renderImpl(st);
    }

    public static Object[] toClassObjectArray(List<Class> classes) {
        return classes.stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.toList())
                .toArray();
    }

    public static Object[] toRenderedTemplateArray(Collection<MethodTemplate> templates) {
        Object[] array = new Object[templates.size()];
        int i = 0;
        for( Template t : templates ) {
            array[i++] = t.render();
        }
        return array;
    }

    protected void renderImpl(ST st) {
        $.methods.add(st, getMethod.render());
    }

    /**
     * A class template cannot have a parent set.
     * @param parent ignored.
     */
    @Override
    public void setParent(Template parent) {

    }

    @Override
    public Template getParent() {
        return null;
    }

    public String getScopeName() {
        return this.scopeName;
    }

    @Override
    public String getName() {
        return "Class";
    }

    @Override
    public void setTemplate(String template) {
        this.template = template;
    }

    @Override
    public String getTemplate() {
        return this.template;
    }

}
