package TemplateEngine.Template4.Structure;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.RegularTemplate;

/**
 *
 * @author BESTDOG
 *
 * Template4 for the list of Class imports at the top of a generated Fingerprint Java source code file.
 */
public class ImportsTemplate extends RegularTemplate {

    private enum $ {

        importList;

        public void add(ST template, Object[] val) {
            template.add(this.name(), val);
        }
    }

    ArrayList<Class> importList;

    public ImportsTemplate() {
        super("ClassImports");
        importList = new ArrayList<>();
    }

    public ImportsTemplate addClass(Class clazz) {
        this.importList.add(clazz);
        return this;
    }

    @Override
    public void render(ST st) {
        Object[] classNames = importList.stream()
                .map(Class::getCanonicalName)
                .collect(Collectors.toList())
                .toArray();
        $.importList.add(st, classNames);
    }


}
