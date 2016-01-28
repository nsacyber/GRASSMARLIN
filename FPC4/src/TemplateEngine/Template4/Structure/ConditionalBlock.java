package TemplateEngine.Template4.Structure;

import TemplateEngine.Template4.Structure.Code.FilterExpression;
import TemplateEngine.Template4.TemplateAccessor;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.NestedBlock;
import TemplateEngine.Template4.RegularTemplate;
import TemplateEngine.Template4.Template;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by BESTDOG on 11/12/2015.
 *
 * A Template4 for a block of source code which contains a nested block of code within a if expression.
 *
 * if( expression ) {
 *     nested-block
 * }
 *
 */
public class ConditionalBlock extends RegularTemplate implements NestedBlock {

    private enum $ implements TemplateAccessor {
        expression,
        body;
    }

    NestedBlock body;

    public ConditionalBlock() {
        super("ConditionalBlock");
    }

    private FilterExpression expression;

    public ConditionalBlock setExpression(FilterExpression expression) {
        this.expression = expression;
        return this;
    }

    public FilterExpression getExpression() {
        return this.expression;
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
    public Template getParent() {
        return null;
    }

    @Override
    public String getScopeName() {
        return null;
    }

    @Override
    public void render(ST st) {
        $.expression.add(st, expression);
        $.body.add(st, this.body);
        if( body == null ) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "WARNING!!! No body found for conditional block.");
        }
    }

}
