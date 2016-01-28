package TemplateEngine.Template4.Structure.Code;

import ICSDefines.Content;
import ICSDefines.Endian;
import ICSDefines.Lookup;
import TemplateEngine.Template4.TemplateAccessor;
import TemplateEngine.Fingerprint3.ContentType;
import TemplateEngine.Fingerprint3.Extract;
import TemplateEngine.Fingerprint3.Post;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.NestedBlock;
import TemplateEngine.Template4.RegularTemplate;

import java.util.Scanner;

/**
 * Created by BESTDOG on 11/17/2015.
 *
 * Tempalte for the code block responsible for copying bytes from payloads.
 *
 * The position enumeration from the fingerprints maps to a few macro-defines of code snippets,
 * START_OF_PAYLOAD - 0
 * END_OF_PAYLOAD - payload.end()
 * CURSOR_START - cursor.getA()
 * CURSOR_MAIN - cursor.get()
 * CURSOR_END - cursor.getB()
 *
 */
public class ExtractTemplate extends RegularTemplate {

    private enum $ implements TemplateAccessor {
        Name, From, To, MaxLength, Endian, PostProcess, Body;
    }

    private enum POSITION {
        START_OF_PAYLOAD("0"),
        END_OF_PAYLOAD("payload.end()"),
        CURSOR_START("cursor.getA()"),
        CURSOR_MAIN("cursor.get()"),
        CURSOR_END("cursor.getB()");

        String code;

        POSITION(String code) {
            this.code = code;
        }

        public static String getCodeSnippet(String string) {
            String ret;
            if( isInt(string) ) {
                ret = string;
            } else {
                ret = POSITION.valueOf(string).code;
            }
            return ret;
        }

        private static boolean isInt(String string) {
            return new Scanner(string).hasNextInt();
        }
    }

    String name;
    String from;
    String to;
    String maxLength;
    String postProcess;
    boolean endian;

    public ExtractTemplate(Extract extract) {
        this();
        setExtract(extract);
    }

    public ExtractTemplate() {
        super("Extract");
    }

    public ExtractTemplate setExtract(Extract extract) {
        String endianVal;

        name = extract.getName();
        maxLength = Integer.toString(extract.getMaxLength());

        from   = POSITION.getCodeSnippet(extract.getFrom());
        to     = POSITION.getCodeSnippet(extract.getTo());
        endianVal = extract.getEndian();

        /** big is the default, to we omit the endian flag if false */
        endian = Endian.LITTLE.name().equals(endianVal);

        Post p = extract.getPost();
        if( p != null ) {
            String lookup = p.getLookup();
            ContentType convert = p.getConvert();
            if( convert != null ) {
                postProcess = getConvertPostProcess(convert.value());
            } else if( lookup != null ) {
                postProcess = getLookupProcess(lookup);
            }
        } else {
            postProcess = getConvertPostProcess(Content.STRING.name());
        }
        return this;
    }

    public String getLookupProcess(String string) {
        String code;
        code = String.format("%s.%s", Lookup.class.getSimpleName(), string);
        return code;
    }

    public String getConvertPostProcess(String string) {
        String code;
        code = String.format("%s.%s", Content.class.getSimpleName(), string);
        return code;
    }

    public void setBody(NestedBlock body) {
        throw new UnsupportedOperationException("Body not accepted.");
    }

    @Override
    public void render(ST st) {
        $.Name.add(st, this.name);
        $.MaxLength.add(st, this.maxLength);
        $.From.add(st, this.from);
        $.To.add(st, this.to);
        $.PostProcess.add(st, this.postProcess);
        if(this.endian) {
            $.Endian.add(st, this.endian);
        }
    }
}
