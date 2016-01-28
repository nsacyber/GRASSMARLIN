package TemplateEngine.Util;

import TemplateEngine.Compiler.FPCompiler;
import TemplateEngine.Compiler.SourceCode;
import TemplateEngine.Data.Filter;
import TemplateEngine.Data.FunctionalFingerprint;
import TemplateEngine.Data.PseudoDetails;
import TemplateEngine.Template4.Exception.SourceCodeError;
import TemplateEngine.Template4.Exception.TemplateEngineError;
import TemplateEngine.Template4.Structure.Code.FilterClassTemplate;
import TemplateEngine.Template4.Structure.Variable;
import TemplateEngine.Template4.TemplateEngine;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.ParameterizedType;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by BESTDOG on 11/12/2015.
 * <p>
 * The complete FPC package implementation for compiling and providing the necessary Objects required for
 * fingerprinting in GM3.
 * <p>
 * FPC4 is the 4th generation of Grassmarlin Fingerprinting procedures.
 * <p>
 * Changes from 3 include the following,
 * 1. More OOP.
 * 2. Cleaner templates.
 * 3. More code-gen moved to template code.
 * 4. Improved Minimal source code generation.
 * 5. Improved Filter-Spray reduction.
 * 6. Less coupling with Grassmarlin proper.
 * 7. Use of JAXB vs DOM walking.
 */
public class FPC {

    public static final String MOTD = "FingerprintCompiler v4.0 11/12/15 ";
    /** flag controls debug mode, enabled if true, else disabled. */
    public static boolean debug = false;
    /** The Templateengine which generates all source code for Fingerprints and Filters. */
    final TemplateEngine template;
    /** The actual JavaCompiler implementation. */
    final FPCompiler compiler;
    /** List of object which require the Filter object to be supplied to them. */
    final ArrayList<Consumer<Filter>> onFilterChange;
    /** All Fingerprint source code */
    final HashMap<String,SourceCode> sources;
    /** Source code for the filter object. */
    SourceCode filterCode;
    /** Output directory for all source files if debugging. */
    private File outputFolder;

    public FPC() throws IOException, URISyntaxException {
        this(null);
    }

    /**
     * See {@link PseudoDetails} for Object contract.
     *
     * @param returnType The dataType to be generated in source code which contains all data set in when fingerprinting.
     * @throws IOException Thrown if the MasterTemplate.stg or its imports cannot be loaded.
     * @throws URISyntaxException Thrown if the {@link TemplateEngine.Template4.SourceTemplate.MasterTemplate} object fails to load a resource.
     */
    public FPC(Class returnType) throws IOException, URISyntaxException {
        Class r = returnType == null ? PseudoDetails.class : returnType;
        this.compiler = new FPCompiler();
        this.template = new TemplateEngine(this.compiler.getClass(), r);
        this.sources = new HashMap<>();
        this.onFilterChange = new ArrayList<>();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            String inputFolder = args[0];
            List<File> files = Arrays.asList(new File(inputFolder).listFiles());
            int size = files.size();

            System.out.println("Compiling " + size + " files.");

            try {
                FPC fpc = new FPC();

                if (args.length > 1) {
                    FPC.debug = true;
                    String outputFolder = args[1];
                    fpc.setOutputFolder(new File(outputFolder));
                }

                fpc.onFilterChange(filter -> System.out.println("... done\n" + filter + " was successfully instantiated."));
                fpc.compileAll(files);

                if( fpc.clear() ) {
                    fpc.compileAll(files);
                } else {
                    System.err.println("Could not clear Filter. RECOMPILATION FAILED");
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Supply path to fingerprint.xml directory as argument 1.");
            System.out.println("Supply path to output directory as argument 2.");
        }
    }

    public void setDefaultMethodParameters(Variable... parameters) {
        this.template.setMethodParameters(parameters);
    }

    /**
     * Add a callback to consume the Filter once it is compiled. No references to the Filter Object linger here
     * for memory management reasons.
     *
     * @param filterChange Consumer which will be called once filter is compiled and reflected.
     */
    public void onFilterChange(Consumer<Filter> filterChange) {
        this.onFilterChange.add(filterChange);
    }

    /**
     * Removes all consumers of the filter upon Filter change.
     */
    public void clearFilterChangeEvents() {
        this.onFilterChange.clear();
    }

    /**
     * @param filter Will supply the provided Filter to all the Filter consumers in the list.
     */
    public void setFilter(final Filter filter) {
        this.onFilterChange.forEach(c -> c.accept(filter));
    }

    /**
     * Compiles a list of files and generates the Filter object, calling {@link #updateFilter()} on success.
     *
     * @param files Fingerprint xml files to compile.
     * @throws ClassNotFoundException A fingerprint xml file contains a invalid or conflicting identifier (Name, or For tag).
     * @throws InstantiationException The generated code from a Fingerprint or Filter does not fulfil the implementing classes contract.
     * @throws IllegalAccessException A Compiled class is reflected with lower access then public.
     */
    public void compileAll(List<File> files) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        files.forEach(this::generate);
        this.sources.values().forEach(this::compile);
        loadAll();
        generateFilter();
        compileFilter();
        updateFilter();
    }

    public void compileAll() {
        this.sources.values().forEach(this::compile);
    }

    public Collection<String> getFingerprintNames() {
        return this.template.getFingerprintNames();
    }

    /**
     * Compiles the provided SourceCode.
     *
     * @param sourceCode Sourcecode to compile.
     */
    public void compile(SourceCode sourceCode) {
        this.compiler.compile(sourceCode);
    }

    /**
     * Generates the filter, requires no fingerprints to be loaded, but at least one to be effectual.
     */
    public void generateFilter() {
        if (debug) {
            debugnl(String.format("Generating source code '%s'", FilterClassTemplate.DEFAULT_CLASS_NAME));
        }
        this.template.generateFilterClass(this::addFilterCode);
    }

    /**
     * Called when the filter code is to be set.
     *
     * @param name Class name, usually {@link FilterClassTemplate#DEFAULT_CLASS_NAME}.
     * @param code Entire filter class code.
     */
    public void addFilterCode(String name, String code) {
        this.filterCode = new SourceCode(name, code);
        newSourceCode(name, code);
    }

    /**
     * Called to catch the creation of each SourceCode object before being available to the compiler, post code-generation.
     *
     * @param name Class name of the generated code.
     * @param code Class body of the generated code.
     */
    public void newSourceCode(String name, String code) {
        if (debug) {
            if (this.outputFolder != null && this.outputFolder.canWrite()) {
                File f = new File(this.outputFolder + File.separator + name + ".java");
                try (FileWriter writer = new FileWriter(f)) {
                    writer.write(code);
                    writer.flush();
                    writer.close();
                } catch (Exception ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "failed to write source file", ex);
                }
            }
        }
    }

    /**
     * Compiles the filter code as successful result of {@link #generateFilter()}.
     */
    public void compileFilter() {
        compile(this.filterCode);
    }

    public Filter getFilter() throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        String className = FilterClassTemplate.DEFAULT_CLASS_NAME;
        Class filterClass = this.compiler.getLoader().findClass(className);
        Filter filter = (Filter) filterClass.newInstance();
        return filter;
    }

    /**
     * Reflects the Filter class and supplies it to all the {@link #onFilterChange} consumers.
     *
     * @throws ClassNotFoundException If the filter class cannot be found by unique class name.
     * @throws IllegalAccessException If the filter class has a access level other then public.
     * @throws InstantiationException If the Filter class does not have a 0-parameter constructor.
     * @throws ClassCastException     If the Filter class does not in fact implement {@link Filter}.
     */
    public void updateFilter() throws ClassNotFoundException, IllegalAccessException, InstantiationException, ClassCastException {
        Filter filter = getFilter();
        this.setFilter(filter);
    }

    /**
     * @return True if the Filter class may be reflected.
     */
    public boolean filterPresent() {
        boolean present = false;
        try {
            Filter filter = getFilter();
            present = Objects.nonNull(filter);
        } catch( Throwable t ) {}
        return present;
    }

    /**
     * Loads all Fingerprint xml files which have to added to the FPC.
     *
     * @throws ClassNotFoundException Thrown when a qualifier within the {@link FPCompiler} is invalid or conflicting.
     */
    public void loadAll() throws ClassNotFoundException {
        if (debug) {
            int units = this.compiler.getLoader().getUnits().size();
            debugnl(String.format("Loading all classes. (%d)", units));
            this.compiler.getLoader().loadAll();
            debug(String.format("... done"));
        } else {
            this.compiler.getLoader().loadAll();
        }
    }

    /**
     * Generates source code for the provided file and adds the resulting class-name and code to the {@link #addSourceCode(String, String)} method.
     *
     * @param file Fingerprint xml file to read and generate source code for.
     */
    public void generate(File file) {
        if (debug) {
            debugnl(String.format("Generating source code '%s'", file.getName()));
            this.template.generateSourceCode(file, this::addSourceCode);
            debug("... done");
        } else {
            this.template.generateSourceCode(file, this::addSourceCode);
        }
    }

    /**
     * Generates source code for the provided file and adds the resulting class-name and code to the {@link #addSourceCode(String, String)} method.
     * OnComplete - When code generation completes the callback will be supplied the new SourceCode.
     * @param file Fingerprint xml file to read and generate source code for.
     * @param callback OnComplete callback to accept the new SourceCode.
     */
    public void generateOnComplete(File file, Consumer<SourceCode> callback) {
        String name = this.template.generateSourceCode(file, this::addSourceCode);
        SourceCode code = this.sources.get(name);
        if(code != null) {
            callback.accept(code);
        } else {
            throw new SourceCodeError(String.format("Cannot find source code for Class '%s'", name));
        }
    }

    /**
     * Adds source code to the list of sources.
     *
     * @param name Class name for the source code provided.
     * @param code Source code for a the fingerprint to be compiled.
     */
    public void addSourceCode(String name, String code) {
        if (debug) {
            debugnl(String.format("... adding code '%s' ", name));
        }
        newSourceCode(name, code);
        this.sources.put(name, new SourceCode(name, code));
    }

    /**
     * @param msg Writes debug with new line.
     */
    private void debug(String msg) {
        System.out.println(msg);
        System.out.flush();
    }

    /**
     * @param msg Writes debug without new line.
     */
    private void debugnl(String msg) {
        System.out.print(msg);
        System.out.flush();
    }

    /**
     * If debugging is on and this is set, each generated source file will be written to the provided directory.
     *
     * @param outputFolder Directory to write each source file to.
     */
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    /**
     * Delete all classes and cleans the class loader for the reflected code.
     * Also provides `null to all Filter Consumers via {@link #setFilter(Filter)}
     */
    public synchronized boolean clear() {
        this.setFilter(null);
        this.sources.clear();
        this.template.clear();
        this.filterCode = null;
        Reference reference = new PhantomReference(
                this.compiler.deleteLoader(),
                new ReferenceQueue()
        );
        reference.enqueue();
        System.gc();
        System.gc();

        return this.compiler.getCurrentLoader() == null;
    }

    /**
     * @return A new JAXBContext for the TemplateEngine.fingerprint3.xsd the TemplateEngine uses.
     */
    public static JAXBContext getContext() throws JAXBException {
        return TemplateEngine.getContext();
    }

}
