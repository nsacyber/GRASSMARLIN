package TemplateEngine.Template4.Structure.Code;

import TemplateEngine.Template4.Structure.ClassTemplate;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.AndThen;
import TemplateEngine.Fingerprint3.IsDataAtFunction;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.FunctionTemplate;
import TemplateEngine.Template4.RegularTemplate;

/**
 * Created by BESTDOG on 11/18/2015.
 *
 * IsDataAt tests the remaining length before the end of the payload.
 *
 *
 */
public class IsDataAtFunctionTemplate extends RegularTemplate implements FunctionTemplate {

    public IsDataAtFunctionTemplate() {
        super("IsDataAtFunction");
    }

    Integer offset;
    boolean relative;

    @Override
    public AndThen generateFunction(Object obj) {
        AndThen andThen = null;
        if( obj instanceof IsDataAtFunction) {
            IsDataAtFunction function = (IsDataAtFunction) obj;
            andThen = function.getAndThen();

            offset = function.getOffset();
            relative = function.isRelative();
        }
        return andThen;
    }

    @Override
    public void onAppend(ClassTemplate template) {

    }

    @Override
    public void render(ST st) {
        $.Offset.add(st, this.offset);
        $.Relative.add(st, this.relative);
        if( this.hasBody() ) {
            $.Body.add(st, this.getBody().render());
        }
    }

    private enum $ implements TemplateAccessor {
        Offset, Relative, Body;
    }

}
