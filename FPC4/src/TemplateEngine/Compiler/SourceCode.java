package TemplateEngine.Compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/**
 * Created by BESTDOG on 11/23/2015.
 *
 * <pre>
 * Used to intermediatly buffer source code as a JavaFileObject before it is compiled into a CompiledCode object.
 * </pre>
 */
public class SourceCode extends SimpleJavaFileObject {
    final String source;
    private String className;

    /**
     * By creating a URI to the default-java String pool we load our source files into memory, vs on disk.
     * We can then read them as files with {@link ByteArrayInputStream}.
     * @param className Name of contained class.
     * @param source Contents of the source code.
     */
    public SourceCode(String className, String source) {
        super( URI.create("string:///"+className.replace('.','/')+Kind.SOURCE.extension), Kind.SOURCE );
        this.source = source;
        this.className = className;
    }

    public String getClassName() {
        return this.className;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return source;
    }
}
