package TemplateEngine.Template4.Structure.Code;

import TemplateEngine.Template4.Structure.ClassTemplate;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.AndThen;
import TemplateEngine.Fingerprint3.Extract;
import TemplateEngine.Fingerprint3.Return;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.FunctionTemplate;
import TemplateEngine.Template4.NestedBlock;
import TemplateEngine.Template4.Template;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BESTDOG on 11/16/2015.
 *
 * Creates the function which extracts and returns information based on testing data in a packet's payload.
 *
 */
public class ReturnTemplate extends DetailBlockTemplate implements FunctionTemplate {

    private enum $ implements TemplateAccessor {
        Body,
        FingerprintName;
    }

    NestedBlock body;
    String name;
    List<Template> extractTemplates;

    public ReturnTemplate() {
        super("Return");
        extractTemplates = new ArrayList<>();
    }

    public ReturnTemplate addExtract(Extract extract) {
        ExtractTemplate template = new ExtractTemplate(extract);
        template.setParent(this);
        extractTemplates.add(template);
        return this;
    }

    @Override
    public AndThen generateFunction(Object obj) {
        AndThen andThen = null;
        if( obj instanceof Return ) {
            Return return_ = (Return) obj;
            this.setReturn(return_);
            return_.getExtract().forEach(this::addExtract);
        }
        return andThen;
    }

    @Override
    public void onAppend(ClassTemplate template) {
        this.name = template.getScopeName();
    }

    @Override
    public NestedBlock getBody() {
        return body;
    }

    @Override
    public void setBody(NestedBlock body) {
        this.body = body;
        this.body.setParent(this);
    }

    @Override
    public void render(ST st) {
        super.render(st);
        $.FingerprintName.add(st, this.getScopeName());
        extractTemplates.forEach( template ->
                        $.Body.add(st, template.render())
        );
        if( this.body != null ) {
            $.Body.add(st, body.render());
        }
    }


}
