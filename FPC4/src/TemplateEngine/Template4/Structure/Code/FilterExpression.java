package TemplateEngine.Template4.Structure.Code;

import TemplateEngine.Fingerprint3.Fingerprint;
import TemplateEngine.Template4.Structure.Variable;
import TemplateEngine.Template4.Structure.VariableDeclaration;
import org.stringtemplate.v4.ST;
import TemplateEngine.Template4.Expression;
import TemplateEngine.Template4.Template;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by BESTDOG on 11/12/2015.
 */
public class FilterExpression implements Template, Expression {
    /** used to delimit ranged values */
    private static final String RANGE_DELIMITER = "::";
    private enum $ {
        /** variable names must coincide with Grassmarlins LogicalDataImportTask variable names */
        Ack(new Variable(String.class, "getAck()")),
        Dsize(new Variable(int.class, "getDsize()")),
        DsizeWithin(new Variable(int.class, "getDsize()"), true),
        DstPort(new Variable(int.class, "getDst()")),
        Ethertype(new Variable(int.class, "getEth()")),
        Flags(new Variable(String.class, "getFlags()")),
        Seq(new Variable(String.class, "getSeq()")),
        SrcPort(new Variable(int.class, "getSrc()")),
        TransportProtocol(new Variable(int.class, "getProto()")),
        TTL(new Variable(int.class, "getTtl()")),
        TTLWithin(new Variable(int.class, "getTtl()"), true),
        Window(new Variable(int.class, "getWindow()"));

        Variable var;

        public final Function<VariableDeclaration,String> renderMethod;

        $(Variable var) {
            this(var, false);
        }

        $(Variable var, boolean isInequality) {
            this.var = var;
            if( isInequality ) {
                renderMethod = this::inqualityMethod;
            } else {
                renderMethod = this::equalMethod;
            }
        }

        public String equalMethod(VariableDeclaration var) {
            String expression = null;
            String name = this.var.name;
            String val = var.initialVal;

            if( this.var.isPrimitive() ) {
                expression = String.format("t.%s==%s", name, val);
            } else if( this.var.isString() ){
                expression = String.format("\"%s\".equals(t.%s)", val, name);
            } else {
                expression = String.format("Objects.equals(t.%s,%s)", name, val);
            }

            return expression;
        }

        public String inqualityMethod(VariableDeclaration var) {
            String name = this.var.name;
            String min = var.initialVal.split("::")[1];
            String max = var.initialVal.split("::")[0];
            String expression = String.format("t.%s<=%s && t.%s>=%s", name, max, name, min);
            return expression;
        }

        public static VariableDeclaration getVariable(JAXBElement<?> element) {
            String name = element.getName().toString();
            String value;

            if( element.getValue() instanceof Fingerprint.Filter.DsizeWithin ) {
                Fingerprint.Filter.DsizeWithin within = (Fingerprint.Filter.DsizeWithin) element.getValue();
                value = String.format("%s%s%s", within.getMax(), RANGE_DELIMITER, within.getMin());
            } else if( element.getValue() instanceof Fingerprint.Filter.TTLWithin ) {
                Fingerprint.Filter.TTLWithin within = (Fingerprint.Filter.TTLWithin) element.getValue();
                value = String.format("%s%s%s", within.getMax(), RANGE_DELIMITER, within.getMin());
            } else {
               value = element.getValue().toString();
            }

            $ code = $.valueOf(name);
            return new VariableDeclaration(code.var.clazz, code.name(), value);
        }

        public static String getExpression(VariableDeclaration var) {
            $ code = $.valueOf(var.name);
            return code.renderMethod.apply(var);
        }

    }

    List<List<VariableDeclaration>> expressions;

    Template parent;

    public FilterExpression() {
        this.expressions = new ArrayList<>();
    }

    public FilterExpression(List<Fingerprint.Filter> filters) {
        this();
        filters.forEach(this::addGroup);
    }

    public List<List<VariableDeclaration>> getExpressions() {
        return expressions;
    }

    private void addGroup(Fingerprint.Filter filter) {
        List<VariableDeclaration> expression = new ArrayList<>();
        for( JAXBElement<?> element : filter.getAckAndMSSAndDsize() ) {
            expression.add( $.getVariable(element) );
        }
        this.expressions.add(expression);
    }

    /**
     * <pre>
     * Generates the comparators for each of the filter operations.
     * Members checks are as follows,
     *     1. Window
     *     2. TTLWithin
     *     3. DsizeWithin
     *     4. Flags
     *     5. Seq
     *     6. TTL
     *     7. Dsize
     *     8. Dstport
     *     9. Srcport
     *     10. Ethertype
     *     11. TransportProtocol
     * </pre>
     * @return Formatted java expression string for checking the members of the LogicalDataImportTask object.
     */
    public String render() {
        final StringBuilder sb = new StringBuilder();
        final String AND_CODE = " && ";
        final String OR_CODE = " || ";
        Function<String,String> formatFunction;
        String AND = "";
        String OR = "";

        for (List<VariableDeclaration> expression : this.expressions) {
            if( !expression.isEmpty() ) {
                if( expression.size() == 1 ) {
                    formatFunction =  s -> s;
                } else {
                    formatFunction = s -> "("+s+")";
                }

                sb.append(OR);
                OR = OR_CODE;
                AND = "";

                for( VariableDeclaration var : expression ) {
                    String s = $.getExpression(var);
                    sb.append(AND).append( formatFunction.apply(s) );
                    AND = AND_CODE;
                }
            }
        }
        if( sb.length() == 0 ) {
            sb.append(false);
        }
        return sb.toString();
    }


    @Override
    public String getName() {
        return "";
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
        this.parent = parent;
    }

    @Override
    public Template getParent() {
        return parent;
    }

    @Override
    public List<VariableDeclaration> getVariables() {
        List<VariableDeclaration> variables = new ArrayList<>();
        this.expressions.forEach(variables::addAll);
        return variables;
    }

}
