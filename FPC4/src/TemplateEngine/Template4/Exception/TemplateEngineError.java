package TemplateEngine.Template4.Exception;

/**
 * Created by BESTDOG on 11/18/2015.
 */
public abstract class TemplateEngineError extends Error {

    TemplateEngineError(String msg) {
        super(msg);
    }

}
