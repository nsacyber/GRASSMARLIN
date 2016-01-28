package TemplateEngine.Data;

/**
 * Created by BESTDOG on 11/24/2015.
 */
public interface FunctionalFingerprint<T> {

    FunctionalOperation<T> loadMethod(String methodName);

}
