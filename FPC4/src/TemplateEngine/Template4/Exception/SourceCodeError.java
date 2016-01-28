package TemplateEngine.Template4.Exception;

/**
 * Created by BESTDOG on 11/25/2015.
 *
 * Errors meant to be thrown dealing with pre-compilation source code.
 */
public class SourceCodeError extends TemplateEngineError {
    public SourceCodeError(String msg) {
        super(msg);
    }
}
