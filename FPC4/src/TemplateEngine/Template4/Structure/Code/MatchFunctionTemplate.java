package TemplateEngine.Template4.Structure.Code;

import ICSDefines.Content;
import TemplateEngine.Template4.Exception.TemplateRequirmentsError;
import TemplateEngine.Template4.Structure.ByteArrayDeclaration;
import TemplateEngine.Template4.Structure.ClassTemplate;
import TemplateEngine.Template4.Structure.PatternDeclaration;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.AndThen;
import TemplateEngine.Fingerprint3.MatchFunction;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.FunctionTemplate;
import TemplateEngine.Template4.NestedBlock;
import TemplateEngine.Template4.RegularTemplate;

/**
 * Created by BESTDOG on 11/13/2015.
 *
 * A method stub/template for the Match-Function.
 * Match will match a sequence of bytes or a regex on the string equivalent of the input.
 */
public class MatchFunctionTemplate extends RegularTemplate implements FunctionTemplate {

    private enum $ implements TemplateAccessor {
        Body,
        Depth,
        Offset,
        Relative,
        Within,
        MoveCursors,
        /* NoCase, not required in template */
        Pattern,
        Content;
    }

    private static int VARIABLE_INDEX = 0;

    private NestedBlock body;
    String pattern;
    private int depth;
    private int offset;
    private int within;
    private boolean move;
    private boolean relative;
    private boolean noCase;
    private PatternDeclaration patternDeclaration;
    private ByteArrayDeclaration contentDeclaration;

    public MatchFunctionTemplate() {
        super("MatchFunction");
    }

    /** depth can only be positive, its a forward only offset */
    public MatchFunctionTemplate setDepth(int depth) {
        this.depth = Math.max(0, depth);
        return this;
    }

    /** limit to max frame size */
    public MatchFunctionTemplate setOffset(int offset) {
        this.offset = Math.min(65535, offset);
        return this;
    }

    /** this is an abs value, its the range ahead or behind the location where a successful hit is accepted */
    public MatchFunctionTemplate setWithin(int within) {
        this.within = Math.abs(within);
        return this;
    }

    /**
     * If true both A and B cursors will be set upon succesful match.
     * @param move true to move A and B cursors in addition to MAIN, else false and just main will be set on success.
     * @return This reference is returned so that methods may be chained.
     */
    public MatchFunctionTemplate setMove(boolean move) {
        this.move = move;
        return this;
    }

    public MatchFunctionTemplate setRelative(boolean relative) {
        this.relative = relative;
        return this;
    }

    public MatchFunctionTemplate setIgnoreCase(boolean noCase) {
        this.noCase = noCase;
        if( this.patternDeclaration != null ) {
            this.setPattern(this.patternDeclaration.getPattern());
        }
        return this;
    }

    public MatchFunctionTemplate setPattern(String pattern) {
        this.patternDeclaration = getPattern(pattern, noCase);
        return this;
    }

    public MatchFunctionTemplate setContent(String string, Content type) {
        ByteArrayDeclaration.ByteAdapter byteAdapter;
        String varName = getContentVariable();

        switch( type ) {
            case HEX:
                byteAdapter = new ByteArrayDeclaration.HexAdapter(string);
                break;
            case INTEGER:
                byteAdapter = new ByteArrayDeclaration.IntAdapter(string);
                break;
            case RAW_BYTES:
                byteAdapter = new ByteArrayDeclaration.RawBytes(string);
                break;
            case STRING:
            default:
                byteAdapter = new ByteArrayDeclaration.StringAdapter(string);
        }

        this.contentDeclaration = new ByteArrayDeclaration(varName, byteAdapter);

        return this;
    }

    /**
     * Generates function when manually constructed through code, not through JAXB object.
     * @return This reference is returned so that methods may be chained.
     */
    public MatchFunctionTemplate generateFunction() {
        return this;
    }

    @Override
    public AndThen generateFunction(Object obj) {
        AndThen andThen = null;
        if( obj instanceof MatchFunction ) {
            MatchFunction mf = (MatchFunction) obj;
            MatchFunction.Content content = mf.getContent();
            pattern = mf.getPattern();

            this.setDepth(mf.getDepth())
                .setOffset(mf.getOffset())
                .setMove(mf.isMoveCursors())
                .setRelative(mf.isRelative())
                .setIgnoreCase(mf.isNoCase());

            if( content == null && (pattern == null || !patternValid(pattern)) ) {
                throw new TemplateRequirmentsError("Expected Content or valid Pattern in MatchFunction.");
            } else if( pattern != null ) {
                this.setPattern(this.pattern);
            } else {
                String data = content.getValue();
                String typeName = content.getType().value();
                Content ct = Content.valueOf(typeName);
                this.setContent(data, ct);
            }

            andThen = mf.getAndThen();
        }
        return andThen;
    }

    private boolean patternValid(String pattern) {
        return PatternDeclaration.patternValid(pattern);
    }

    public PatternDeclaration getPattern(String pattern, boolean ignoreCase) {
        return new PatternDeclaration(getPatternVariable(), pattern, ignoreCase);
    }

    private String getContentVariable() {
        int id = Math.abs(VARIABLE_INDEX++);
        return String.format("CONTENT%d", id);
    }

    private String getPatternVariable() {
        int id = Math.abs(VARIABLE_INDEX++);
        return String.format("PATTERN%d", id);
    }

    @Override
    public void onAppend(ClassTemplate template) {
        if( patternDeclaration != null ) {
            template.addVariable(this.patternDeclaration);
        } else if( contentDeclaration != null ) {
            template.addVariable(this.contentDeclaration);
        }
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
        $.Depth.add(st, this.depth);
        $.Offset.add(st, this.offset);
        $.Within.add(st, this.within);
        $.Relative.add(st, this.relative);
        $.MoveCursors.add(st, this.move);
        if( body != null ) {
            $.Body.add(st, this.body.render());
        }
        if( patternDeclaration != null ) {
            $.Pattern.add(st, this.patternDeclaration.name);
        } else if( contentDeclaration != null ) {
            $.Content.add(st, this.contentDeclaration.name);
        }
    }
}
