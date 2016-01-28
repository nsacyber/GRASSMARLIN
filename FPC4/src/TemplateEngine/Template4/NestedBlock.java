package TemplateEngine.Template4;

/**
 * Created by BESTDOG on 11/13/2015.
 * An interface for getting and setting nested blocks of code templates.
 */
public interface NestedBlock extends Template {
    /**
     * Get the body of this code block, which is also considered this Template4 child.
     *
     * @return
     */
    NestedBlock getBody();

    /**
     * Set the inner-body, or child, of this Template4.
     *
     * @param body
     */
    void setBody(NestedBlock body);

    /**
     * @return True if this Template4 has a child, else false.
     */
    default boolean hasBody() {
        return getBody() != null;
    }

    /**
     * Descends the child hierarchy of this code block, adding the provided body to the first block without a child.
     *
     * @param body Template4 to add to the last descendant of this Template4.
     */
    default void appendBody(NestedBlock body) {
        NestedBlock nestedBlock = this;
        while (nestedBlock.hasBody()) {
            nestedBlock = nestedBlock.getBody();
        }
        nestedBlock.setBody(body);
    }

}
