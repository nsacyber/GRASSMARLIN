package TemplateEngine.Template4.Structure;

import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.NestedBlock;
import TemplateEngine.Template4.Template;

/**
 * Created by BESTDOG on 11/13/2015.
 *
 * A block of code that follows the linear pattern of,
 * { this block }
 * { next block/body }
 *
 */
public class LinearBlock implements NestedBlock {

    Template template;
    NestedBlock nextBlock;

    public LinearBlock(Template template) {
        this.template = template;
    }

    @Override
    public NestedBlock getBody() {
        return nextBlock;
    }

    @Override
    public void setBody(NestedBlock body) {
        this.nextBlock = body;
        this.nextBlock.setParent(this);
    }

    @Override
    public String render() {
        String string;
        if( template == null ) {
            string = nextBlock.render();
        } else if( nextBlock == null ) {
            string = template.render();
        } else {
            string = template.render().concat("\n").concat(nextBlock.render());
        }
        return string;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public void setTemplate(String template) {
    }

    @Override
    public void render(ST st) {
    }

    @Override
    public void setParent(Template parent) {
        template.setParent(parent);
    }

    @Override
    public Template getParent() {
        return template.getParent();
    }

}
