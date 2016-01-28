package TemplateEngine.Template4.Structure.Code;

import TemplateEngine.Data.Filter;
import TemplateEngine.Data.FunctionalFingerprint;
import TemplateEngine.Data.FunctionalOperation;
import TemplateEngine.Template4.RegularTemplate;
import TemplateEngine.Template4.Structure.*;
import TemplateEngine.Template4.Template;
import TemplateEngine.Template4.TemplateAccessor;
import org.stringtemplate.v4.ST;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Created by BESTDOG on 11/19/2015.
 * <p>
 * The filter class is the mechanism GM uses to route packets to fingerprints which are likely to succeed in
 * identifying the devices communicating.
 * <p>
 * It contains references to each of the Fingerprint objects and {@link FunctionalOperation} lambdas paired with
 * their Fingerprint-Operation-Codes used with the {@link Filter#getOperation(int)} method.
 */
public class FilterClassTemplate extends RegularTemplate {
    public static final String DEFAULT_CLASS_NAME = "FilterImpl";
    final Class filterClass;
    Class compilerClass;
    final Class operationClass;
    final Class fingerprintClass;
    final ArrayList<String> imports;
    final ArrayList<Entry> entries;
    final Class returnType;
    String className;

    public FilterClassTemplate(Class compilerClass, Class returnType) {
        super("FilterClass");
        this.returnType = returnType;
        this.compilerClass = compilerClass;
        this.className = DEFAULT_CLASS_NAME;
        this.filterClass = Filter.class;
        this.operationClass = FunctionalOperation.class;
        this.fingerprintClass = FunctionalFingerprint.class;
        this.imports = new ArrayList<>();
        this.entries = new ArrayList<>();
        this.addImports(
                filterClass,
                fingerprintClass,
                operationClass,
                returnType,
                compilerClass,
                Logger.class,
                Level.class,
                ParameterizedType.class
        );
    }

    @Override
    public void render(ST st) {
        $.ClassName.add(st, this.className);
        $.ImplementingClass.add(st, this.filterClass);
        $.CompilerClass.add(st, this.compilerClass);
        $.Imports.add(st, this.imports.toArray());
        $.Entries.add(st, this.getEntries());
        $.OperationCodes.add(st, this.getOperationCodes());
        $.FingerprintClass.add(st, this.fingerprintClass);
        $.OperationClass.add(st, this.operationClass);
        $.ReturnType.add(st, this.returnType);
        /** the {@link #getFilterPackage(List)} method will also generate this list of variable names to initial values. */
        List<Object[]> codeMap = new ArrayList<>();
        $.FilterPackage.add(st, getFilterPackage(codeMap));
        $.FilterCodes.add(st, codeMap.toArray());
    }

    public void setCompilerClass(Class compilerClass) {
        this.compilerClass = compilerClass;
    }

    /**
     * @return The name of this JavaClass.
     */
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Generates the parameter vector responsible for populating the majority of the FilterClass.stg's fingerprint
     * specific filtering code.
     *
     * @param codeMap The filtering process returns Fingerprint-Operation-Codes, this is map of those codes.
     * @return The filter-package vector.
     */
    private Object[] getFilterPackage(List<Object[]> codeMap) {

        HashMap<String, HashMap<String, HashSet<Integer>>> map = new HashMap<>();

        List<FilterUnit> units = entries.stream()
                .map(Entry::getTemplate)
                .map(ClassTemplate::getMethods)
                .flatMap(List::stream)
                .filter(MethodTemplate::hasFilterSupplier)
                .map(FilterUnit::new)
                .collect(Collectors.toList());

        units.forEach(unit -> {
            unit.getVariables().forEach(filter -> {
                if (!map.containsKey(filter)) {
                    map.put(filter, new HashMap<>());
                }
            });
            int i = 0;
            for (List<VariableDeclaration> vars : unit.getGroups()) {
                Integer affine = unit.getResolvingCode();
                vars.forEach(var -> {
                    HashMap<String, HashSet<Integer>> set = map.get(var.getName());
                    if (!set.containsKey(var.getValue())) {
                        set.put(var.getValue(), new HashSet<>());
                    }
                    set.get(var.getValue()).add(affine);
                });
            }
        });

        // printMap(map);
        /** no within methods are used in the initial filter operation */
        map.entrySet().removeIf(e -> e.getKey().contains("Within"));

        reduceMapSpray(map);

        Object[] fpac = new Object[map.size()];
        int i = 0;
        for (Map.Entry<String, HashMap<String, HashSet<Integer>>> entry : map.entrySet()) {
            String filter = entry.getKey();
            Object[] function = new Object[entry.getValue().size() + 1];
            int x = 0;
            function[x] = filter;
            for (Map.Entry<String, HashSet<Integer>> values : entry.getValue().entrySet()) {
                String value = values.getKey();
                String var = Template.asJavaIdentifier(String.format("%s%s", filter, value));
                codeMap.add(new Object[]{var, values.getValue().toArray()});
                function[++x] = new Object[]{value, var};
            }
            fpac[i++] = function;
        }

        return fpac;
    }

    /**
     * Reduces the amount of criteria which are required to predicate for a Fingerprint to apply itself to a payload.
     *
     * @param map Map of [{ FilterName : [ { Value : [ Fingerprint-Operation-Code, ... ] }, {...}, ...]}, ...]
     */
    private void reduceMapSpray(HashMap<String, HashMap<String, HashSet<Integer>>> map) {
        Predicate<Integer> duplicate = p ->
                map.values().stream()
                        .map(Map::values)
                        .flatMap(Collection::stream)
                        .flatMap(Collection::stream)
                        .filter(p::equals)
                        .count() > 1L;

        map.entrySet().removeIf(e1 -> {
            e1.getValue().entrySet().removeIf(e2 -> {
                e2.getValue().removeIf(duplicate);
                return e2.getValue().isEmpty();
            });
            return e1.getValue().isEmpty();
        });
    }

    /***
     * Depub method which write the filter "value" map to std::out
     *
     * @param map Map to print.
     */
    public void printMap(HashMap<String, HashMap<String, HashSet<Integer>>> map) {
        map.forEach((k, v) -> {
            System.out.println(k);
            v.forEach((k0, v0) -> {
                System.out.println("\t" + k0);
                v0.forEach(v1 ->
                                System.out.println("\t\t" + v1)
                );
            });
        });
    }

    /**
     * Returns an array of all operation Codes mapped to the Fingerprint which will reflect the operation by name.
     *
     * @return Matrix of [[213091283, "MethodName1"], [ -21312481, "MethodName2" ]]
     */
    private Object[] getOperationCodes() {
        return this.entries.stream()
                .map(Entry::getMethods)
                .flatMap(List::stream)
                .map(method -> new Object[]{Integer.toString(method.hashCode()), method})
                .collect(Collectors.toList()).toArray();
    }

    /**
     * Generates the matrix of Fingerprints and Method within the each fingerprint
     * which are reflected and used in Filtering and processing.
     *
     * @return Matrix of [[ Fingerprint name, method1, method2, ... ], ... ]
     */
    private Object[][] getEntries() {
        Object[][] obj = new Object[this.entries.size()][];
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            Object[] methods = e.getMethods().toArray();
            Object[] entry = new Object[methods.length + 1];
            entry[0] = e.getName();
            System.arraycopy(methods, 0, entry, 1, methods.length);
            obj[i] = entry;
        }
        return obj;
    }

    /**
     * Sets the imports used by the class this template creates.
     *
     * @param classes Array of classes to be used.
     */
    private void addImports(Class... classes) {
        for (int i = 0; i < classes.length; ++i) {
            Class clazz = classes[i];
            this.imports.add(clazz.getCanonicalName());
        }
    }

    /**
     * Tries to add a fingerprint to this Filter.
     *
     * @param template Template4 to attempt to add.
     */
    public void addFingerprint(ClassTemplate template) {
        if (isFingerprint(template)) {
            this.entries.add(new Entry(template));
        }
    }

    /**
     * Tests if a template contains methods with signatures that are required by the filter object.
     * The Filter object is meant to be a "self reflecting" object. Meaning it can be cast to usable interfaces
     * without explicitly reflecting the classes.
     *
     * @param classTemplate Template4 to check if it implements the required interface.
     * @return True if this ClassTemplate is a fingerprint, else false.
     */
    public boolean isFingerprint(ClassTemplate classTemplate) {
        return fingerprintClass.equals(classTemplate.getImplementingClass());
    }

    private enum $ implements TemplateAccessor {
        ClassName,
        ImplementingClass,
        CompilerClass,
        Imports,
        Entries,
        OperationCodes,
        FingerprintClass,
        OperationClass,
        ReturnType,
        FilterPackage,
        FilterCodes;
    }

    /**
     * link of a resolving method code to the filters it will check
     */
    private class FilterUnit {
        private final String resolvingMethod;
        private final List<List<VariableDeclaration>> groups;

        public FilterUnit(MethodTemplate template) {
            this.resolvingMethod = template.getMethodName();
            this.groups = template.getFilters();
        }

        public String getResolvingMethod() {
            return resolvingMethod;
        }

        public Integer getResolvingCode() {
            return this.getResolvingMethod().hashCode();
        }

        public List<List<VariableDeclaration>> getGroups() {
            return groups;
        }

        public List<String> getVariables() {
            return this.groups.stream().flatMap(List::stream)
                    .map(Variable::getName)
                    .collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return String.format("%s %s", this.getResolvingCode(), this.getVariables());
        }
    }

    /***
     * Container class for each ClassTemplate (fingerprint) this filter Object will include in its Operation routes.
     */
    private class Entry {
        public final String name;
        public final ClassTemplate template;

        public Entry(ClassTemplate template) {
            this.template = template;
            this.name = template.getClassName();
        }

        public String getName() {
            return name;
        }

        public ClassTemplate getTemplate() {
            return this.template;
        }

        public List<String> getMethods() {
            return this.template.getMethods()
                    .stream()
                    .map(MethodTemplate::getMethodName)
                    .collect(Collectors.toList());
        }

    }

}