package TemplateEngine.Compiler;

import java.io.IOException;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
/**
 * Created by BESTDOG on 11/23/2015.
 *
 * <pre>
 * Used by the JavaCompiler to proxy the transition of SourceCode to CompiledCode objects.
 * Helps the determine (when used by javac) to locate the compiled-code files in memory, vs on disk.
 * </pre>
 */
public class IsolatedJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {

    CompiledCode unit;
    IsolatedClassLoader loader;

    /**
     * Constructs this file-manager from the manager it will forward files from.
     * @param fileman FileManager to forward or get from when necessary.
     * @param unit Code unit to supply when requested.
     * @param loader Class loader used to load the CompiledCodeUnit within this FileManager.
     */
    protected IsolatedJavaFileManager(JavaFileManager fileman, CompiledCode unit, IsolatedClassLoader loader) {
        super(fileman);
        this.unit = unit;
        this.loader = loader;
        this.loader.setCode(unit);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
        return unit;
    }

    @Override
    public ClassLoader getClassLoader(JavaFileManager.Location location) {
        return loader;
    }
}
