package TemplateEngine.Compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import javax.tools.SimpleJavaFileObject;

/**
 * Created by BESTDOG on 11/23/2015.
 *
 * <pre>
 * Buffers compiled code in memory while behaving like a FileObject to the JavaCompiler.
 * </pre>
 */
public class CompiledCode extends SimpleJavaFileObject {

    ByteArrayOutputStream os;

    public CompiledCode(String className) throws URISyntaxException {
        super( new URI(className), Kind.CLASS );
        os = new ByteArrayOutputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return os;
    }

    @Override
    public boolean delete() {
        try {
            os.close();
        } catch( Exception ex ) {}
        os = null;
        return true;
    }

    public byte[] getbytes() {
        return os.toByteArray();
    }
}