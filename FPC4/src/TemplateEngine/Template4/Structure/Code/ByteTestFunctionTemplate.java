package TemplateEngine.Template4.Structure.Code;

import ICSDefines.Endian;
import TemplateEngine.Template4.Structure.ClassTemplate;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.AndThen;
import TemplateEngine.Fingerprint3.ByteTestFunction;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.FunctionTemplate;
import TemplateEngine.Template4.RegularTemplate;

import java.math.BigInteger;

/**
 * Created by BESTDOG on 11/18/2015.
 *
 * Functional template which tests some integer value from the payload and continues if the inequality predicates.
 */
public class ByteTestFunctionTemplate extends RegularTemplate implements FunctionTemplate {

    boolean endian;
    boolean relative;
    Integer bytes;
    Integer offset;
    Integer postOffset;
    Test operator;
    String val;

    public ByteTestFunctionTemplate() {
        super("ByteTestFunction");
        this.operator = Test.EQ;
        this.val = "0";
    }

    @Override
    public AndThen generateFunction(Object obj) {
        AndThen andThen = null;
        if( obj instanceof ByteTestFunction ) {
            ByteTestFunction function = (ByteTestFunction) obj;
            andThen = function.getAndThen();

            bytes = function.getBytes();
            offset = function.getOffset();
            postOffset = function.getPostOffset();
            relative = function.isRelative();
            /** big is the default, to we omit the endian flag if false */
            endian = Endian.BIG.name().equals(function.getEndian());
            operator = getTest(function);
            val = getValue(function).toString();
        }
        return andThen;
    }

    private BigInteger getValue(ByteTestFunction function) {
        BigInteger value;
        if( function.getAND() != null ) {
            value =  function.getAND();
        } else if( function.getOR() != null   ) {
            value = function.getOR();
        } else if( function.getGT() != null   ) {
            value = function.getGT();
        } else if( function.getGTE() != null   ) {
            value = function.getGTE();
        } else if( function.getLT() != null   ) {
            value = function.getLT();
        } else if( function.getLTE() != null   ) {
            value = function.getLTE();
        } else {
            value = BigInteger.ZERO;
        }
        return value;
    }

    private Test getTest(ByteTestFunction function) {
        Test t = null;
        if( function.getAND() != null ) {
            t = Test.EQ; /** actual AND unsupported */
        } else if( function.getOR() != null   ) {
            t = Test.EQ; /** actual OR unsupported */
        } else if( function.getGT() != null   ) {
            t = Test.GT;
        } else if( function.getGTE() != null   ) {
            t = Test.GTE;
        } else if( function.getLT() != null   ) {
            t = Test.LT;
        } else if( function.getLTE() != null   ) {
            t = Test.LTE;
        } else {
            t = Test.EQ;
        }
        return t;
    }

    @Override
    public void onAppend(ClassTemplate template) {

    }

    @Override
    public void render(ST st) {
        $.PostOffset.add(st, this.postOffset);
        $.Relative.add(st, this.relative);
        $.Offset.add(st, this.offset);
        $.Bytes.add(st, this.bytes);
        $.Operator.add(st, this.operator.code );
        $.Endian.add(st, this.endian);
        $.Val.add(st, this.val);
        if( this.hasBody() ) {
            $.Body.add(st, this.getBody().render());
        }
    }

    private enum $ implements TemplateAccessor {
        PostOffset, Relative, Endian, Offset, Bytes, Operator, Val, Body;
    }

    private enum Test {
        GT(">"),
        LT("<"),
        GTE(">="),
        LTE("<="),
        EQ("=="),
        AND("=="),
        OR("==");
        public final String code;
        Test(String code) {
            this.code = code;
        }
    }

}
