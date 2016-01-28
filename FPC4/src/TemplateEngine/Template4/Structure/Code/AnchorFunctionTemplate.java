package TemplateEngine.Template4.Structure.Code;

import TemplateEngine.Template4.FunctionTemplate;
import TemplateEngine.Template4.RegularTemplate;
import TemplateEngine.Template4.Structure.ClassTemplate;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.Anchor;
import TemplateEngine.Fingerprint3.AndThen;
import org.stringtemplate.v4.ST;

/**
 * Created by BESTDOG on 11/18/2015.
 * <p>
 * Anchor is a method for users to delcare the location of a cursor.
 * <p>
 * This can be used to set the cursors around a portion or data field.
 * It may also be used to anchor the cursors some negative offset from the back of the payload.
 */
public class AnchorFunctionTemplate extends RegularTemplate implements FunctionTemplate {

    Integer offset;
    String cursor;
    String position;
    boolean relative;

    public AnchorFunctionTemplate() {
        super("Anchor");
        position = "";
        cursor = "";
        offset = 0;
        relative = false;
    }

    public void setOffset(Integer offset) {
        if( offset != null && offset != 0 ) {
            this.offset = offset;
        }
    }

    @Override
    public AndThen generateFunction(Object obj) {
        AndThen andThen = null;
        if (obj instanceof Anchor) {
            Anchor function = (Anchor) obj;
            //andThen = function.getAndThen();

            this.setOffset(function.getOffset());
            relative = function.isRelative();
            cursor = function.getCursor().value();
            position = function.getPosition() != null ? function.getPosition().value() : "";
        }
        return andThen;
    }

    @Override
    public void onAppend(ClassTemplate template) {

    }

    @Override
    public void render(ST st) {
        $.Cursor.add(st, getCursor(this.cursor));
        $.Position.add(st, getPosition(this.position));
        $.Relative.add(st, this.relative);
        $.Offset.add(st, this.offset);
        if (this.hasBody()) {
            $.Body.add(st, this.getBody().render());
        }
    }

    /**
     * @param position String from XML for the Fingerprint.Payload.Anchor.Position parameter.
     * @return Formatted source code which will render for that selected {@link AnchorFunctionTemplate.Position} enumeration value.
     */
    private String getPosition(String position) {
        String ret;
        try {
            ret = Position.valueOf(position).code;
        } catch (java.lang.IllegalArgumentException ex) {
            ret = null; /** often null or empty values are contained in xml, this is ok */
        }
        return ret;
    }

    /**
     * @param cursor String from XML for the Fingerprint.Payload.Anchor.Cursor parameter.
     * @return Formatted source code which will render for that selected {@link AnchorFunctionTemplate.Cursor } enumeration value.
     */
    private String getCursor(String cursor) {
        return Cursor.valueOf(cursor).code;
    }

    /**
     * This template accessor enumeration, each value is mapped to a specific Template4 parameter.
     */
    private enum $ implements TemplateAccessor {
        Cursor, Position, Relative, Offset, Body;
    }

    private enum Cursor {
        START("A"),
        MAIN(""),
        END("B");

        public final String code;

        Cursor(String code) {
            this.code = code;
        }
    }

    private enum Position {
        START_OF_PAYLOAD("0"),
        END_OF_PAYLOAD("payload.end()"),
        CURSOR_START("cursor.getA()"),
        CURSOR_MAIN("cursor.get()"),
        CURSOR_END("cursor.getB()");

        public final String code;

        Position(String code) {
            this.code = code;
        }
    }

}
