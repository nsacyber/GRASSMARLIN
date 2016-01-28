package TemplateEngine.Template4.Structure.Code;

import TemplateEngine.Data.FunctionalFingerprint;
import TemplateEngine.Data.FunctionalOperation;
import TemplateEngine.Template4.Structure.MethodTemplate;
import TemplateEngine.Template4.Structure.Variable;
import TemplateEngine.Template4.TemplateAccessor;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.RegularTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BESTDOG on 11/13/2015.
 *
 * Creates the {@link FunctionalOperation} getMethod(String name) method
 * used in {@link FunctionalFingerprint} classes to expose their entry methods.
 */
public class FingerprintGetMethod extends RegularTemplate {

    private enum $ implements TemplateAccessor {
        methodNames, returnType;
    }

    List<String> methodIdentifiers;
    final Class returnType;

    public FingerprintGetMethod(Class returnType) {
        super("FingerprintGetMethod");
        this.methodIdentifiers = new ArrayList<>();
        this.returnType = returnType;
    }

    public FingerprintGetMethod addMethodIdentifier(String identifier) {
        this.methodIdentifiers.add(identifier);
        return this;
    }

    @Override
    public String render() {
        MethodTemplate template = new MethodTemplate() {
            @Override
            public String getRenderedBody() {
                ST st = FingerprintGetMethod.this.get();
                FingerprintGetMethod.this.render(st);
                return st.render();
            }
        }
                .addArgument(new Variable(String.class, "name"))
                .setReturnType(FunctionalOperation.class)
                .setGenericReturnType(this.returnType)
                .setMethodName("loadMethod")
                .setMethodbody(this)
                .setOverride(true)
                .setInitialVars(null);
        return template.render();
    }

    @Override
    public void render(ST st) {
        $.methodNames.add(st, methodIdentifiers.toArray());
        $.returnType.add(st, this.returnType);
    }
}
