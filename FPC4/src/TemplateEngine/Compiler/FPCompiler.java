package TemplateEngine.Compiler;

import TemplateEngine.Data.FunctionalOperation;

import javax.tools.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by BESTDOG on 11/23/2015.
 *
 * Manages compilation of SourceCode by ClassName.
 * Allows classes to be unloaded with {@link IsolatedClassLoader}.
 */
public class FPCompiler {

    final JavaCompiler javac;
    IsolatedClassLoader loader;

    private static FPCompiler self;

    public FPCompiler() {
        findTools();
        javac = ToolProvider.getSystemJavaCompiler();
        if (FPCompiler.self == null) {
            this.self = this;
        }
    }

    public static FPCompiler getCompiler() {
        return self;
    }

    /**
     * Compiles a {@link SourceCode} java-code file and loads it into an {@link IsolatedClassLoader} so it may be unloaded
     * from the JVM.
     * @param sourceCode SourceCode to compile.
     * @return true on successful compilation, else false.
     */
    public boolean compile(SourceCode sourceCode)  {
        boolean res = false;
        CompiledCode cc = null;

        if(javac == null) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "JavaCompiler is unavailable.");
        } else try {
            cc = new CompiledCode(sourceCode.getClassName());

            Iterable<JavaFileObject> units = Arrays.asList(sourceCode);

            IsolatedClassLoader cl = getLoader();

            StandardJavaFileManager sjfm = javac.getStandardFileManager(new DiagnosticListener() {
                @Override
                public void report(Diagnostic diagnostic) {
                    Logger.getLogger(FPCompiler.class.getName()).log(Level.SEVERE, diagnostic.getMessage(Locale.ENGLISH));
                }
            }, Locale.ENGLISH, Charset.defaultCharset());

            List<String> options = new ArrayList<>();
            //options.add("-Xlint:unchecked");

            IsolatedJavaFileManager jfm = new IsolatedJavaFileManager(sjfm, cc, cl);
            JavaCompiler.CompilationTask t = javac.getTask(null, jfm, null, options, null, units);
            res = t.call();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return res;
    }

    /**
     * Compiles and loads a single source file.
     *
     * @param className Unique name of the class.
     * @param rawCode Raw source code as a String.
     * @return true on successful compilation, else false.
     * @throws URISyntaxException Thrown when the JavaFileManagerEx cannot
     * resolve the name of the compiled code in the StandardJavaFileManager.
     */
    public boolean compile(String className, String rawCode) throws URISyntaxException {
        SourceCode sc = new SourceCode(className, rawCode);
        return this.compile(sc);
    }

    /**
     * Sets the internal class loader reference to null and returns the last
     * strong reference to it.
     *
     * @return The last strong reference to the class loader.
     */
    public IsolatedClassLoader deleteLoader() {
        IsolatedClassLoader l = loader;
        if (loader != null) {
            loader.clear();
        }
        loader = null;
        return l;
    }

    public IsolatedClassLoader getLoader() {
        if (loader == null) {
            loader = new IsolatedClassLoader();
        }
        return loader;
    }

    /**
     * Typically used to check if the loader persisted after being deleted.
     * @return IsolatedClassLoader if exists, may be null.
     */
    public IsolatedClassLoader getCurrentLoader() {
        return this.loader;
    }

    /***
     * Locates the JDK's tools.jar, typically jdk1.8.0_66 folder.
     */
    private void findTools() {
        /** this should be handled by implementing project. */
    }
}
