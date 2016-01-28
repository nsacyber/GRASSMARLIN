package TemplateEngine.Template4.Structure.Code;

import ICSDefines.Endian;
import TemplateEngine.Template4.Structure.ClassTemplate;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.AndThen;
import TemplateEngine.Fingerprint3.ByteJumpFunction;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.FunctionTemplate;
import TemplateEngine.Template4.RegularTemplate;

/**
 * Created by BESTDOG on 11/18/2015.
 *
 * A method/code block template for the ByteJump operation.
 * Steps,
 * 1. Reads some number of bytes from the payload
 * 2. Converts it to an integer
 * 3. (optional) Does follow up arithmatic ( number = x / 2 + 5 )
 * 4. Moves the cursor based on that converted number
 *
 */
public class ByteJumpFunctionTemplate extends RegularTemplate implements FunctionTemplate {

    boolean endian;
    boolean relative;
    String calc;
    Integer bytes;
    Integer offset;
    Integer postOffset;

    public ByteJumpFunctionTemplate() {
        super("ByteJumpFunction");
    }

    @Override
    public AndThen generateFunction(Object obj) {
        AndThen andThen = null;
        if( obj instanceof ByteJumpFunction ) {
            ByteJumpFunction function = (ByteJumpFunction) obj;
            andThen = function.getAndThen();

            calc = function.getCalc();
            bytes = function.getBytes();
            offset = function.getOffset();
            postOffset = function.getPostOffset();
            relative = function.isRelative();
            /** big is the default, to we omit the endian flag if false */
            endian = Endian.BIG.name().equals(function.getEndian());

        }
        return andThen;
    }

    @Override
    public void onAppend(ClassTemplate template) {

    }

    @Override
    public void render(ST st) {
        $.PostOffset.add(st, this.postOffset);
        $.Relative.add(st, this.relative);
        $.Endian.add(st, this.endian);
        $.Offset.add(st, this.offset);
        $.Bytes.add(st, this.bytes);
        $.Calc.add(st, this.calc);
        if( this.hasBody() ) {
            $.Body.add(st, this.getBody().render());
        }
    }

    private enum $ implements TemplateAccessor {
        PostOffset, Relative, Endian, Offset, Bytes, Calc, Body;
    }
}
