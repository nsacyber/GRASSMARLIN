package TemplateEngine.Template4;

import TemplateEngine.Data.*;
import TemplateEngine.Fingerprint3.AndThen;
import TemplateEngine.Fingerprint3.Fingerprint;
import TemplateEngine.Fingerprint3.Return;
import TemplateEngine.Template4.Exception.MissingTemplateError;
import TemplateEngine.Template4.Structure.*;
import TemplateEngine.Template4.Structure.Code.*;


import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author BESTDOG
 *
 * Template4 will convert Fingerpint XML files into optimal Exception safe Java-Source-Code.
 *
 * Once several Fingerprints are generated the Filter Class may be generated which provides access to
 * Fingerprint-Payload-Operation via its Filter - interface.
 */
public class TemplateEngine {

    final Map<String, Fingerprint> fingerprintMap;
    final Map<String, ClassTemplate> templateMap;
    final Map<String, File> sourcecodeMap;
    final Class compilerClass;
    final Class returnType;

    private final Map<String, Supplier<FunctionTemplate>> functionTemplates;
    private final List<Variable> methodParameters;
    private final ImportsTemplate imports;

    public TemplateEngine(Class compilerClass, Class returnType) throws IOException, URISyntaxException {
        this.fingerprintMap = new HashMap<>();
        this.templateMap = new HashMap<>();
        this.sourcecodeMap = new HashMap<>();
        this.compilerClass = compilerClass;
        this.returnType = returnType;

        this.functionTemplates = new HashMap<>();
        this.functionTemplates.put(new MatchFunctionTemplate().getName(), MatchFunctionTemplate::new);
        this.functionTemplates.put(new ReturnTemplate().getName(), ReturnTemplate::new);
        this.functionTemplates.put(new ByteJumpFunctionTemplate().getName(), ByteJumpFunctionTemplate::new);
        this.functionTemplates.put(new ByteTestFunctionTemplate().getName(), ByteTestFunctionTemplate::new);
        this.functionTemplates.put(new IsDataAtFunctionTemplate().getName(), IsDataAtFunctionTemplate::new);
        this.functionTemplates.put(new AnchorFunctionTemplate().getName(), AnchorFunctionTemplate::new);

        this.methodParameters = new ArrayList<>();
        this.methodParameters.add(new Variable(FilterData.class, "t"));
        this.methodParameters.add(new Variable(PseudoBuffer.class, "payload"));
        this.methodParameters.add(new Variable(Cursor.class, "cursor"));
        this.methodParameters.add(new GenericVariable(Consumer.class, "consumer", returnType));
        this.methodParameters.add(new GenericVariable(Supplier.class, "supplier", returnType));

        this.imports = new ImportsTemplate()
                .addClass(FunctionalFingerprint.class)
                .addClass(FunctionalOperation.class)
                .addClass(ICSDefines.Category.class)
                .addClass(ICSDefines.Role.class)
                .addClass(ICSDefines.Direction.class)
                .addClass(ICSDefines.Lookup.class)
                .addClass(ICSDefines.Content.class)
                .addClass(FilterData.class)
                .addClass(PseudoBuffer.class)
                .addClass(Cursor.class)
                .addClass(PseudoDetails.class)
                .addClass(Pattern.class)
                .addClass(Matcher.class)
                .addClass(Supplier.class)
                .addClass(Consumer.class)
                .addClass(returnType);
    }

    public void setMethodParameters(Variable... argumentList) {
        this.methodParameters.clear();
        this.methodParameters.addAll(Arrays.asList(argumentList));
    }

    /**
     * @return A new JAXBContext for the TemplateEngine.fingerprint3.xsd the TemplateEngine uses.
     */
    public static JAXBContext getContext() throws JAXBException {
        return JAXBContext.newInstance(Fingerprint.class);
    }

    /**
     * Reads a valid fingerprint XML file and renders the class name to be used in a class loader and a string
     * containing source code to a BiConsumer.
     * @param inFile XML fingerprint file.
     * @return Name of the Fingerprint used to register the fingerprint as globally unique, else null on failure.
     */
    public String generateSourceCode(File inFile, BiConsumer<String, String> callback) {
        Fingerprint fingerprint;
        String name = null;
        try {

            JAXBContext jcx = getContext();
            fingerprint = (Fingerprint) jcx.createUnmarshaller().unmarshal(inFile);
            name = fingerprint.getHeader().getName();

            putFingerprint(name, fingerprint);
            generateSourceCode(name);
            Template t = getClassTemplateByName(name);
            callback.accept(Template.asJavaIdentifier(name), t.render());
            name = Template.asJavaIdentifier(name);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return name;
    }

    /**
     * Reads a valid fingerprint XML file and writes the java-code equivalent to the provided outfile as source code.
     *
     * @param inFile  XML fingerprint file.
     * @param outFile File to write source code to.
     * @return Name of the Fingerprint used to register the fingerprint as globally unique, else null on failure.
     */
    public String generateSourceCode(File inFile, File outFile) {
        Fingerprint fingerprint;
        String name = null;
        try {

            JAXBContext jcx = JAXBContext.newInstance("TemplateEngine/fingerprint3");
            fingerprint = (Fingerprint) jcx.createUnmarshaller().unmarshal(inFile);
            name = fingerprint.getHeader().getName();

            putFingerprint(name, fingerprint);
            generateSourceCode(name);
            writeSourceCode(name, outFile);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return name;
    }

    public Collection<String> getFingerprintNames() {
        return this.fingerprintMap.keySet();
    }

    /**
     * Writes source to for the given named Fingerprint to the provided file.
     *
     * @param name    Name of the file used in the {@link #putFingerprint(String, Fingerprint)} method.
     * @param outfile File to write fingerprint source code to.
     */
    private void writeSourceCode(final String name, File outfile) {
        try (FileWriter fw = new FileWriter(outfile)) {
            Template t = getClassTemplateByName(name);
            if (t != null) {
                t.renderToFile(fw);
                this.sourcecodeMap.put(name, outfile);
            } else {
                throw new MissingTemplateError(String.format("Template4 \"%s\" does not exist.", name));
            }
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void generateFilterClass(BiConsumer<String,String> callback) {
        FilterClassTemplate template = new FilterClassTemplate(this.compilerClass, this.returnType);
        this.templateMap.values().forEach(template::addFingerprint);
        callback.accept(template.getClassName(), template.render());
    }

    /**
     * @param outfile File to write filter classes source code to.
     * @return Name of the Filter classes implementing class.
     */
    public String generateFilterClass(File outfile) {
        FilterClassTemplate template = new FilterClassTemplate(this.compilerClass, this.returnType);
        this.templateMap.values().forEach(template::addFingerprint);
        try (FileWriter fw = new FileWriter(outfile)) {
            template.renderToFile(fw);
            this.sourcecodeMap.put(template.getClassName(), outfile);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
        return template.getClassName();
    }

    /**
     * Generates source code for a Fingerprint which has already been added to the Template4.
     *
     * @param name Key for the fingerprint used in the {@link #putFingerprint(String, Fingerprint)} method.
     */
    private void generateSourceCode(final String name) {
        final Fingerprint fingerprint = getFingerprintByName(name);
        final ClassTemplate clazz = getClassTemplateByName(name);
        final List<Fingerprint.Filter> filters = fingerprint.getFilter();
        final List<Fingerprint.Payload> payloads = fingerprint.getPayload();
        final String javaName = Template.asJavaIdentifier(name);
        final Integer index = javaName.hashCode();
        final VariableDeclaration indexVariable = new VariableDeclaration(Integer.class, "INDEX", index.toString()).setIsStatic(true);

        //(String.format("Generating:\"%s\", Filters:%d, Payloads:%d", name, filters.size(), payloads.size()));
        //(String.format("\tConfiguring:\"%s\", hook:\"%s\", hashcode:%s", name, javaName, index));

        clazz.addVariable(indexVariable)
                .setImplementingClass(FunctionalFingerprint.class)
                .setClassPackage(getPackage())
                .setClassDirectives("public")
                .setClassImports(getImports())
                .setClassName(javaName);

        clazz.addInitialVariable(new Variable(int.class, "offset"))
                .addInitialVariable(new Variable(int.class, "length"))
                .addInitialVariable(new Variable(int.class, "location"))
                .addInitialVariable(new Variable(String.class, "string"))
                .addInitialVariable(new Variable(Matcher.class, "matcher"))
                .addInitialVariable(new Variable(this.returnType, "ret"))
        ;

        Function<String,String> mathodNameTransform = p -> javaName.concat(Template.asJavaIdentifier(p));

        final List<String> codePaths = payloads
                .stream()
                .map(Fingerprint.Payload::getFor)
                .map(Template::asJavaIdentifier)
                .map(javaName::concat)
                .distinct()
                .collect(Collectors.toList());

        codePaths.stream().map(codePath -> {
            List<Fingerprint.Filter> methodFilters = new ArrayList<>(filters);
            List<Fingerprint.Payload> methodPayloads = new ArrayList<>(payloads);

            methodFilters.removeIf(p -> !mathodNameTransform.apply(p.getFor()).equals(codePath));
            methodPayloads.removeIf(p -> !mathodNameTransform.apply(p.getFor()).equals(codePath));

            MethodTemplate method = generateCodePath(codePath, methodFilters, methodPayloads);

            method.setArgumentList(methodParameters);

            return method;
        }).forEach(clazz::addMethod);
    }

    /**
     * Generates a code path given the list of filters and list of payloads for the desired operations.
     *
     * @param codePath Name of the method to generate.
     * @param filters  List of filters which will be compiled into a single {@link FilterExpression}.
     * @param payloads List of payload operations that will be compiled into multiple nested {@link ConditionalBlock}s.
     * @return The method template created for this method.
     */
    private MethodTemplate generateCodePath(final String codePath, List<Fingerprint.Filter> filters, List<Fingerprint.Payload> payloads) {
        String methodName = Template.asJavaIdentifier(codePath);
        //(String.format("\tcodepath:\"%s\", hook:\"%s\", Filters:%d, Payloads:%d", codePath, methodName, filters.size(), payloads.size()));

        FilterExpression filter = new FilterExpression(filters);

        MethodTemplate method = new MethodTemplate()
                .setScopeName(codePath)
                .setArgumentList(getDefaultArguments())
                .setMethodName(methodName);

        ConditionalBlock block = new ConditionalBlock()
                .setExpression(filter);

        generateCodePath(methodName, block, payloads);

        method.setMethodConditionalBody(block);

        return method;
    }

    /**
     * Generates the body of Fingerprint-Source-Code.
     *
     * @param block    Block to begin generating source code in.
     * @param payloads List of payloads to be rendered into the provided block of code.
     */
    private void generateCodePath(String scopeName, ConditionalBlock block, List<Fingerprint.Payload> payloads) {
        if (payloads.isEmpty()) {
            return;
        }
        Fingerprint.Payload payload = payloads.remove(0);

        if (hasAlwaysBlock(payload)) {
            generateAlwaysBlock(payload, block, payload.getAlways());
        }

        List<Object> operationList = payload.getOperation();
        List<Object> nextOperations = new ArrayList<>();
        while (!operationList.isEmpty()) {
            operationList.forEach(op -> {
                String elementName = op.getClass().getSimpleName();
                FunctionTemplate ft = newFunction(elementName);
                if (ft != null) {
                    AndThen nextFunction = ft.generateFunction(op);
                    if (nextFunction != null) {
                        nextOperations.addAll(nextFunction.getMatchOrByteTestOrIsDataAt());
                    }
                    block.appendBody(ft);
                }
            });
            operationList.clear();
            operationList.addAll(nextOperations);
            nextOperations.clear();
        }

    }

    /**
     * Generate the "Always" blocks which have slightly different conditional needs then typical Fingerprint Operations.
     *
     * @param payload Payload Object containing the necessary {@link TemplateEngine.Fingerprint3.Fingerprint.Payload.Always} block.
     * @param block   Block of code to render the "Always" code into.
     * @param always  The actual {@link TemplateEngine.Fingerprint3.Fingerprint.Payload.Always} object being rendered.
     */
    private void generateAlwaysBlock(Fingerprint.Payload payload, ConditionalBlock block, Fingerprint.Payload.Always always) {
        LinearBlock linearBlock = null;
        for (Return return_ : always.getReturn()) {

            DetailBlockTemplate template = new DetailBlockTemplate()
                    .setFingerprintName(payload.getFor())
                    .setReturn(return_);

            if (linearBlock == null) {
                linearBlock = new LinearBlock(template);
            } else {
                linearBlock.appendBody(new LinearBlock(template));
            }
        }

        block.appendBody(linearBlock);
    }

    /**
     * @param payload Payload to check if child is present.
     * @return True of Payload contains an always block.
     */
    private boolean hasAlwaysBlock(Fingerprint.Payload payload) {
        return payload.getAlways() != null && !payload.getAlways().getReturn().isEmpty();
    }

    /**
     * Provides idyllic access to the "Funtions" of fingerprinting which may be nested for mu-completeness within a
     * fixed payload.
     *
     * @param name Name of the template within the {@link #functionTemplates} map.
     * @return Named function template, null if key not present.
     */
    private FunctionTemplate newFunction(String name) {
        FunctionTemplate template;
        if (functionTemplates.containsKey(name)) {
            template = functionTemplates.get(name).get();
        } else {
            System.out.println("No template " + name);
            template = null;
        }
        return template;
    }

    /**
     * @param name Key in the {@link #fingerprintMap}.
     * @return Value from the {@link #fingerprintMap}.
     */
    public Fingerprint getFingerprintByName(String name) {
        return fingerprintMap.get(name);
    }

    /**
     * @param name Key in the {@link #templateMap}.
     * @return Value from the {@link #templateMap}.
     */
    public ClassTemplate getClassTemplateByName(String name) {
        return templateMap.get(name);
    }

    /**
     * @param name Key in the {@link #sourcecodeMap}.
     * @return Value from the {@link #sourcecodeMap}.
     */
    public File getSourceFileByName(String name) {
        return sourcecodeMap.get(name);
    }

    /**
     * Adds a {@link Fingerprint} object, usually from un-marshalling xml to the Template4, making it a valid
     * candidate for code generation.
     *
     * @param name        GUID or name of the fingerprint used to request the fingerprint when loading it.
     * @param fingerprint Fingerprint to add.
     */
    private void putFingerprint(String name, Fingerprint fingerprint) {
        this.fingerprintMap.put(name, fingerprint);
        this.templateMap.put(name, new ClassTemplate(returnType).setScopeName(name));
    }

    /**
     * Gets the package all classes will be generated in.
     *
     * @return Package each java source will render in.
     */
    public Package getPackage() {
        return null;
    }

    /**
     * Gets the template which renders all imports Fingerprint source code files require.
     *
     * @return The template used to render the imports for each Fingerprint Class.
     */
    public ImportsTemplate getImports() {
        return this.imports;
    }

    /**
     * Clears all data created by the Template4, even deletes generates sources.
     */
    public void clear() {
        this.fingerprintMap.clear();
        this.templateMap.clear();
        this.sourcecodeMap.values().forEach(java.io.File::delete);
        this.sourcecodeMap.clear();
    }

    /**
     * @return List of default variables supplied to each fingerprint, this should match the parameter erasure of {@link TemplateEngine.Data.FunctionalOperation}.
     */
    public List<Variable> getDefaultArguments() {
        return Collections.EMPTY_LIST;
    }
}
