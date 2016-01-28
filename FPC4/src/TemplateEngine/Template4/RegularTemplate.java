package TemplateEngine.Template4;

/**
 *
 * @author BESTDOG
 *
 * Regular code template which basic parental hiearchy achieved by setting all Template4 bodies as childen of the
 * parent template.
 *
 */
public abstract class RegularTemplate implements NestedBlock {

    NestedBlock body;
    Template parent;
    String template;
    String templateName;

    /**
     * The value used to construct a regular template is its return value for {@link #getName()}.
     * @param templateName Name of this Template4 from the MasterTemplate.stg file.
     */
    protected RegularTemplate(String templateName) {
        this.templateName = templateName;
    }

    @Override
    public void setParent(Template parent) {
        this.parent = parent;
    }

    @Override
    public Template getParent() {
        return parent;
    }

    public String getScopeName() {
        return parent.getScopeName();
    }

    @Override
    public String getName() {
        return this.templateName;
    }

    @Override
    public String getTemplate() {
        return this.template;
    }

    @Override
    public void setTemplate(String template) {
        this.template = template;
    }

    public NestedBlock getBody() {
        return body;
    }

    /**
     * Adds a inner Template4 the body of this Template4. Sets the parent of the added body to this Template4.
     * @param body Body to add and set this Template4 as its parent.
     */
    public void setBody(NestedBlock body) {
        this.body = body;
        this.body.setParent(this);
    }

}
