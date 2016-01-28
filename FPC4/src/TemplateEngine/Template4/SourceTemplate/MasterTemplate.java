package TemplateEngine.Template4.SourceTemplate;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroupFile;

import java.net.URL;

/**
 * Created by BESTDOG on 11/13/2015.
 */
public class MasterTemplate {

    final STGroupFile groupFile;
    final String charset = "UTF-8";
    final URL url = getClass().getResource("MasterTemplate.stg");
    final char start = STGroupFile.defaultGroup.delimiterStartChar;
    final char end = STGroupFile.defaultGroup.delimiterStopChar;

    public MasterTemplate() {
        this.groupFile = new STGroupFile(url, charset, start, end);
    }

    public ST getInstanceOf(String name) {
        return this.groupFile.getInstanceOf(name);
    }

    public void printInfo() {
        System.out.println(String.format("Delimiters : { start:%c, end:%c }", start, end));
        System.out.println( this.groupFile.getFileName() );
        System.out.println( this.groupFile.getImportedGroups() );
        System.out.println( this.groupFile.getTemplateNames() );
    }

}
