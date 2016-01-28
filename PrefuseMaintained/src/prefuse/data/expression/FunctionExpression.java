package prefuse.data.expression;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.util.ColorLib;
import prefuse.util.MathLib;
import prefuse.util.StringLib;
import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Abstract base class for FunctionExpression implementations. Provides
 * parameter handling support.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class FunctionExpression extends AbstractExpression
    implements Function
{
    protected CopyOnWriteArrayList m_params;
    protected final int m_pcount; 
    
    /**
     * Protected constructor.
     * @param parameterCount the max parameter count
     */
    protected FunctionExpression(int parameterCount) {
        m_pcount = parameterCount;
    }
    
    /**
     * @see prefuse.data.expression.Function#getName()
     */
    public abstract String getName();
    
    /**
     * @see prefuse.data.expression.Function#addParameter(prefuse.data.expression.Expression)
     */
    public void addParameter(Expression e) {
        int pc = getParameterCount();
        if ( pc!=VARARGS && paramCount()+1 > pc ) {
            throw new IllegalStateException(
                "This function takes only "+pc+" parameters.");
        }
        if ( m_params == null )
            m_params = new CopyOnWriteArrayList();
        m_params.add(e);
    }
    
    /**
     * An internal-only method that returns the current number of
     * parameters collected.
     */
    protected int paramCount() {
        return m_params==null ? 0 : m_params.size();
    }
    
    /**
     * Return the parameter expression at the given index.
     * @param idx the parameter index
     * @return the parameter value Expression at the given index
     */
    protected final Expression param(int idx) {
        return (Expression)m_params.get(idx);
    }
    
    /**
     * @see prefuse.data.expression.Function#getParameterCount()
     */
    public int getParameterCount() {
        return m_pcount;
    }
    
    /**
     * Throw an exception when needed parameters are missing.
     */
    protected void missingParams() {
        throw new IllegalStateException(
            "Function is missing parameters: " + getName());
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.expression.Expression#visit(prefuse.data.expression.ExpressionVisitor)
     */
    public void visit(ExpressionVisitor v) {
        v.visitExpression(this);
        if ( paramCount() > 0 ) {
            Object[] params = m_params.getArray();
            for ( int i=0; i<params.length; ++i ) {
                v.down();
                ((Expression)params[i]).visit(v);
                v.up();
            }
        }
    }
    
    /**
     * @see prefuse.data.expression.AbstractExpression#addChildListeners()
     */
    protected void addChildListeners() {
        if ( paramCount() > 0 ) {
            Object[] params = m_params.getArray();
            for ( int i=0; i<params.length; ++i )
                ((Expression)params[i]).addExpressionListener(this);
        }
    }
    
    /**
     * @see prefuse.data.expression.AbstractExpression#removeChildListeners()
     */
    protected void removeChildListeners() {
        if ( paramCount() > 0 ) {
            Object[] params = m_params.getArray();
            for ( int i=0; i<params.length; ++i )
                ((Expression)params[i]).removeExpressionListener(this);
        }
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append(getName()).append('(');
        for ( int i=0; i<paramCount(); ++i ) {
            if ( i > 0 ) sbuf.append(", ");
            sbuf.append(param(i).toString());
        }
        sbuf.append(')');
        return sbuf.toString();
    }

} // end of class FunctionExpression

/**
 * Default Function Implementations
 */

// ----------------------------------------------------------------------------
// Mathematical Functions

abstract class DoubleFunction extends FunctionExpression {
    protected DoubleFunction(int parameterCount) {
        super(parameterCount);
    }
    public Class getType(Schema s) {
        return double.class;
    }
    public Object get(Tuple t) {
        return new Double(getDouble(t));
    }
    public int getInt(Tuple t) {
        return (int)getDouble(t);
    }
    public long getLong(Tuple t) {
        return (long)getDouble(t);
    }
    public float getFloat(Tuple t) {
        return (float)getDouble(t);
    }
}
abstract class IntFunction extends FunctionExpression {
    protected IntFunction(int parameterCount) {
        super(parameterCount);
    }
    public Class getType(Schema s) {
        return int.class;
    }
    public Object get(Tuple t) {
        return new Integer(getInt(t));
    }
    public long getLong(Tuple t) {
        return (long)getInt(t);
    }
    public float getFloat(Tuple t) {
        return (float)getFloat(t);
    }
    public double getDouble(Tuple t) {
        return (double)getInt(t);
    }
}
abstract class BooleanFunction extends FunctionExpression
    implements Predicate
{
    protected BooleanFunction(int parameterCount) {
        super(parameterCount);
    }
    public Class getType(Schema s) {
        return boolean.class;
    }
    public Object get(Tuple t) {
        return getBoolean(t) ? Boolean.TRUE : Boolean.FALSE;
    }
}

//ROW()
class RowFunction extends IntFunction {
    public RowFunction() { super(0); }
    public String getName() { return "ROW"; }
    public int getInt(Tuple t) {
        return t.getRow();
    }
}
//ISNODE()
class IsNodeFunction extends BooleanFunction {
    public IsNodeFunction() { super(0); }
    public String getName() { return "ISNODE"; }
    public boolean getBoolean(Tuple t) {
        return (t instanceof Node);
    }
}
//ISEDGE()
class IsEdgeFunction extends BooleanFunction {
    public IsEdgeFunction() { super(0); }
    public String getName() { return "ISEDGE"; }
    public boolean getBoolean(Tuple t) {
        return (t instanceof Edge);
    }
}
//DEGREE()
class DegreeFunction extends IntFunction {
    public DegreeFunction() { super(0); }
    public String getName() { return "DEGREE"; }
    public int getInt(Tuple t) {
        return (t instanceof Node ? ((Node)t).getDegree() : 0 );
    }
}
//INDEGREE()
class InDegreeFunction extends IntFunction {
    public InDegreeFunction() { super(0); }
    public String getName() { return "INDEGREE"; }
    public int getInt(Tuple t) {
        return (t instanceof Node ? ((Node)t).getInDegree() : 0 );
    }
}
//OUTDEGREE()
class OutDegreeFunction extends IntFunction {
    public OutDegreeFunction() { super(0); }
    public String getName() { return "OUTDEGREE"; }
    public int getInt(Tuple t) {
        return (t instanceof Node ? ((Node)t).getOutDegree() : 0 );
    }
}
//CHILDCOUNT()
class ChildCountFunction extends IntFunction {
    public ChildCountFunction() { super(0); }
    public String getName() { return "CHILDCOUNT"; }
    public int getInt(Tuple t) {
        return (t instanceof Node ? ((Node)t).getChildCount() : 0 );
    }
}
//TREEDEPTH()
class TreeDepthFunction extends IntFunction {
    public TreeDepthFunction() { super(0); }
    public String getName() { return "TREEDEPTH"; }
    public int getInt(Tuple t) {
        return (t instanceof Node ? ((Node)t).getDepth() : 0 );
    }
}

//ABS(X)
class AbsFunction extends DoubleFunction {
    public AbsFunction() { super(1); }
    public String getName() { return "ABS"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.abs(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}

//ACOS(X)
class AcosFunction extends DoubleFunction {
    public AcosFunction() { super(1); }
    public String getName() { return "ACOS"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.acos(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}

//ASIN(X)
class AsinFunction extends DoubleFunction {
    public AsinFunction() { super(1); }
    public String getName() { return "ASIN"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.asin(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}

//ATAN(X)
class AtanFunction extends DoubleFunction {
    public AtanFunction() { super(1); }
    public String getName() { return "ATAN"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.atan(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}

//ATAN2(Y,X)
class Atan2Function extends DoubleFunction {
    public Atan2Function() { super(2); }
    public String getName() { return "ATAN2"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 2 ) {
            return Math.atan2(param(0).getDouble(t), param(1).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//CEILING(X), CEIL(X)
class CeilFunction extends DoubleFunction {
    public CeilFunction() { super(1); }
    public String getName() { return "CEIL"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.ceil(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
    public int getInt(Tuple t) {
        return (int)getDouble(t);
    }
}
//COS(X)
class CosFunction extends DoubleFunction {
    public CosFunction() { super(1); }
    public String getName() { return "COS"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.cos(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//COT(X)
class CotFunction extends DoubleFunction {
    public CotFunction() { super(1); }
    public String getName() { return "COT"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return 1/Math.tan(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//DEGREES(X)
class DegreesFunction extends DoubleFunction {
    public DegreesFunction() { super(1); }
    public String getName() { return "DEGREES"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.toDegrees(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//E()
class EFunction extends DoubleFunction {
    public EFunction() { super(0); }
    public String getName() { return "E"; }
    public double getDouble(Tuple t) {
        return Math.E;
    }
}
//EXP(X)
class ExpFunction extends DoubleFunction {
    public ExpFunction() { super(1); }
    public String getName() { return "EXP"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.exp(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//FLOOR(X)
class FloorFunction extends DoubleFunction {
    public FloorFunction() { super(1); }
    public String getName() { return "FLOOR"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.floor(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
    public int getInt(Tuple t) {
        return (int)getDouble(t);
    }
}
//LOG(X), LOG(B,X)
class LogFunction extends DoubleFunction {
    public LogFunction() { super(2); }
    public String getName() { return "LOG"; }
    public double getDouble(Tuple t) {
        int pc = paramCount();
        if ( pc == 2 ) {
            double b = param(0).getDouble(t);
            double x = param(1).getDouble(t);
            return Math.log(x)/Math.log(b);
        } else if ( pc == 1 ) {
            return Math.log(param(0).getDouble(t));
        } else {
            missingParams();
            return Double.NaN;
        }
    }
}
//LOG2(X)
class Log2Function extends DoubleFunction {
    public Log2Function() { super(1); }
    public String getName() { return "LOG2"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return MathLib.log2(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//LOG10(X)
class Log10Function extends DoubleFunction {
    public Log10Function() { super(1); }
    public String getName() { return "LOG10"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return MathLib.log10(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//SAFELOG10(X)
class SafeLog10Function extends DoubleFunction {
    public SafeLog10Function() { super(2); }
    public String getName() { return "SAFELOG10"; }
    public double getDouble(Tuple t) {
        int pc = paramCount();
        if ( pc == 1 ) {
            double x = param(0).getDouble(t);
            return MathLib.safeLog10(x);
        } else {
            missingParams();
            return Double.NaN;
        }
    }
}
//MAX(X1,X2,...)
class MaxFunction extends DoubleFunction {
    public MaxFunction() { super(Function.VARARGS); }
    public String getName() { return "MAX"; }
    public double getDouble(Tuple t) {
        double x, v = param(0).getDouble(t);
        for ( int i=1; i<paramCount(); ++i ) {
            x = param(i).getDouble(t);
            if ( x > v ) v = x;
        }
        return v;
    }
}
//MIN(X1,X2,...)
class MinFunction extends DoubleFunction {
    public MinFunction() { super(Function.VARARGS); }
    public String getName() { return "MIN"; }
    public double getDouble(Tuple t) {
        double x, v = param(0).getDouble(t);
        for ( int i=1; i<paramCount(); ++i ) {
            x = param(i).getDouble(t);
            if ( x < v ) v = x;
        }
        return v;
    }
}
//MOD(N,M)
class ModFunction extends DoubleFunction {
    public ModFunction() { super(2); }
    public String getName() { return "MOD"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 2 ) {
            double x = param(0).getDouble(t);
            double y = param(1).getDouble(t);
            return x % y;
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//PI()
class PiFunction extends DoubleFunction {
    public PiFunction() { super(0); }
    public String getName() { return "PI"; }
    public double getDouble(Tuple t) {
        return Math.PI;
    }
}
//POW(X,Y), POWER(X,Y)
class PowFunction extends DoubleFunction {
    public PowFunction() { super(1); }
    public String getName() { return "POW"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 2 ) {
            return Math.pow(param(0).getDouble(t), param(1).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//RADIANS(X)
class RadiansFunction extends DoubleFunction {
    public RadiansFunction() { super(1); }
    public String getName() { return "RADIANS"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.toRadians(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//RAND(), RAND(N)
class RandFunction extends DoubleFunction {
    public RandFunction() { super(0); }
    public String getName() { return "RAND"; }
    public double getDouble(Tuple t) {
        return Math.random();
    }
}
//ROUND(X) // --> ROUND(X,D) TODO
class RoundFunction extends DoubleFunction {
    public RoundFunction() { super(1); }
    public String getName() { return "ROUND"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.round(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
    public int getInt(Tuple t) {
        return (int)getDouble(t);
    }
}
//SIGN(X)
class SignFunction extends DoubleFunction {
    public SignFunction() { super(1); }
    public String getName() { return "SIGN"; }
    public Class getType(Schema s) {
        return int.class;
    }
    public double getDouble(Tuple t) {
        return getInt(t);
    }
    public int getInt(Tuple t) {
        if ( paramCount() == 1 ) {
            double d = param(0).getDouble(t);
            return d<0 ? -1 : d==0 ? 0 : 1;
        } else {
            missingParams(); return Integer.MIN_VALUE;
        }
    }
}
//SIN(X)
class SinFunction extends DoubleFunction {
    public SinFunction() { super(1); }
    public String getName() { return "SIN"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.sin(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//SQRT(X)
class SqrtFunction extends DoubleFunction {
    public SqrtFunction() { super(1); }
    public String getName() { return "SQRT"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.sqrt(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//SUM(X1,X2,...)
class SumFunction extends DoubleFunction {
    public SumFunction() { super(Function.VARARGS); }
    public String getName() { return "SUM"; }
    public double getDouble(Tuple t) {
        double v = param(0).getDouble(t);
        for ( int i=1; i<paramCount(); ++i )
            v += param(i).getDouble(t);
        return v;
    }
}
//SAFESQRT(X)
class SafeSqrtFunction extends DoubleFunction {
    public SafeSqrtFunction() { super(2); }
    public String getName() { return "SAFESQRT"; }
    public double getDouble(Tuple t) {
        int pc = paramCount();
        if ( pc == 1 ) {
            double x = param(0).getDouble(t);
            return MathLib.safeSqrt(x);
        } else {
            missingParams();
            return Double.NaN;
        }
    }
}
//TAN(X)
class TanFunction extends DoubleFunction {
    public TanFunction() { super(1); }
    public String getName() { return "TAN"; }
    public double getDouble(Tuple t) {
        if ( paramCount() == 1 ) {
            return Math.tan(param(0).getDouble(t));
        } else {
            missingParams(); return Double.NaN;
        }
    }
}
//TRUNCATE(X,D)
// TODO

//----------------------------------------------------------------------------
// String Functions

abstract class StringFunction extends FunctionExpression {
    protected StringFunction(int parameterCount) {
        super(parameterCount);
    }
    public Class getType(Schema s) {
        return String.class;
    }
    protected StringBuffer getBuffer() {
        return new StringBuffer();
    }
}
//CAP(str1)
class CapFunction extends StringFunction {
    public CapFunction() { super(1); }
    public String getName() { return "CAP"; }
    public Object get(Tuple t) {
        String str = param(0).get(t).toString();
        return StringLib.capitalizeFirstOnly(str);
    }
}
//CONCAT(str1,str2,...)
class ConcatFunction extends StringFunction {
    public ConcatFunction() { super(Function.VARARGS); }
    public String getName() { return "CONCAT"; }
    public Object get(Tuple t) {
        StringBuffer sbuf = getBuffer();
        for ( int i=0; i<paramCount(); ++i ) {
            Object o = param(i).get(t);
            if ( o != null )
                sbuf.append(o.toString());
        }
        return sbuf.toString();
    }
}
//CONCAT_WS(separator,str1,str2,...)
class ConcatWsFunction extends StringFunction {
    public ConcatWsFunction() { super(Function.VARARGS); }
    public String getName() { return "CONCAT_WS"; }
    public Object get(Tuple t) {
        StringBuffer sbuf = getBuffer();
        String sep = param(0).get(t).toString();
        for ( int i=1; i<paramCount(); ++i ) {
            sbuf.append(param(i).get(t).toString());
            sbuf.append(sep);
        }
        return sbuf.toString();
    }
}
//FORMAT(X,D)
class FormatFunction extends StringFunction {
    public FormatFunction() { super(2); }
    public String getName() { return "FORMAT"; }
    public Object get(Tuple t) {
        double x = param(0).getDouble(t);
        int d = param(1).getInt(t);
        DecimalFormat df = (DecimalFormat)NumberFormat.getInstance();
        df.setMinimumFractionDigits(d);
        df.setMaximumFractionDigits(d);
        return df.format(x);
    }
}
//INSERT(str,pos,len,newstr)
class InsertFunction extends StringFunction {
    public InsertFunction() { super(4); }
    public String getName() { return "INSERT"; }
    public Object get(Tuple t) {
        String str = param(0).get(t).toString();
        int strlen = str.length();
        int pos = param(1).getInt(t);
        int len = pos+param(2).getInt(t);
        String newstr = param(1).get(t).toString();
        if ( pos < 0 || pos > strlen )
            return str;
        if ( len < 0 || len > strlen )
            return str.substring(0,pos)+newstr;
        else
            return str.substring(0,pos)+newstr+str.substring(len);
    }
}
//INSTR(str,substr)
//LEFT(str,len)
class LeftFunction extends StringFunction {
    public LeftFunction() { super(2); }
    public String getName() { return "LEFT"; }
    public Object get(Tuple t) {
        String src = param(0).get(t).toString();
        int len = param(1).getInt(t);
        return src.substring(0, len);
    }
    
}
//LENGTH(str)
class LengthFunction extends IntFunction {
    public LengthFunction() { super(1); }
    public String getName() { return "LENGTH"; }
    public int getInt(Tuple t) {
        return param(0).get(t).toString().length();
    }
}
//LOCATE(substr,str), LOCATE(substr,str,pos)
//LOWER(str) | LCASE(str)
class LowerFunction extends StringFunction {
    public LowerFunction() { super(1); }
    public String getName() { return "LOWER"; }
    public Object get(Tuple t) {
        return param(0).get(t).toString().toLowerCase();
    }
}
//LPAD(str,len,padstr)
class LPadFunction extends StringFunction {
    public LPadFunction() { super(3); }
    public String getName() { return "LPAD"; }
    public Object get(Tuple t) {
        String str = param(0).get(t).toString();
        int len = param(1).getInt(t);
        String pad = param(2).get(t).toString();
        int strlen = str.length();
        if ( strlen > len ) {
            return str.substring(0,len);
        } else if ( strlen == len ) {
            return str;
        } else {
            StringBuffer sbuf = getBuffer();
            int padlen = pad.length();
            int diff = len-strlen;
            for ( int i=0; i<diff; i+=padlen)
                sbuf.append(pad);
            if ( sbuf.length() > diff )
                sbuf.delete(diff, sbuf.length());
            sbuf.append(str);
            return sbuf.toString();
        }
    }
}
//LTRIM(str)
//MID(str,pos,len) -- same as substring
//POSITION(substr, str)
class PositionFunction extends IntFunction {
    public PositionFunction() { super(2); }
    public String getName() { return "POSITION"; }
    public int getInt(Tuple t) {
        String substr = param(0).get(t).toString();
        String src = param(1).get(t).toString();
        return src.indexOf(substr);
    } 
}
//QUOTE(str)
//REPEAT(str,count)
class RepeatFunction extends StringFunction {
    public RepeatFunction() { super(2); }
    public String getName() { return "REPEAT"; }
    public Object get(Tuple t) {
        String src = param(0).get(t).toString();
        int count = param(1).getInt(t);
        StringBuffer sbuf = new StringBuffer();
        for ( int i=0; i<count; ++i ) {
            sbuf.append(src);
        }
        return sbuf.toString();
    } 
}
//REPLACE(str,from_str,to_str)
class ReplaceFunction extends StringFunction {
    public ReplaceFunction() { super(3); }
    public String getName() { return "REPLACE"; }
    public Object get(Tuple t) {
        String src = param(0).get(t).toString();
        String from = param(1).get(t).toString();
        String to = param(2).get(t).toString();
        return src.replaceAll(from, to);
    }
    
}
//REVERSE(str)
class ReverseFunction extends StringFunction {
    public ReverseFunction() { super(1); }
    public String getName() { return "REVERSE"; }
    public Object get(Tuple t) {
        String str = param(0).get(t).toString();
        StringBuffer sbuf = getBuffer();
        for ( int i=str.length()-1; --i>=0; ) {
            sbuf.append(str.charAt(i));
        }
        return sbuf.toString();
    }
}
//RIGHT(str,len)
class RightFunction extends StringFunction {
    public RightFunction() { super(2); }
    public String getName() { return "RIGHT"; }
    public Object get(Tuple t) {
        String src = param(0).get(t).toString();
        int len = param(1).getInt(t);
        return src.substring(src.length()-len);
    }
    
}
//RPAD(str,len,padstr)
class RPadFunction extends StringFunction {
    public RPadFunction() { super(3); }
    public String getName() { return "RPAD"; }
    public Object get(Tuple t) {
        String str = param(0).get(t).toString();
        int len = param(1).getInt(t);
        String pad = param(2).get(t).toString();
        int strlen = str.length();
        if ( strlen > len ) {
            return str.substring(0,len);
        } else if ( strlen == len ) {
            return str;
        } else {
            StringBuffer sbuf = getBuffer();
            sbuf.append(str);
            int padlen = pad.length();
            int diff = len-strlen;
            for ( int i=0; i<diff; i+=padlen)
                sbuf.append(pad);
            if ( sbuf.length() > len )
                sbuf.delete(len, sbuf.length());
            return sbuf.toString();
        }
    }
}
//RTRIM(str)
//// SOUNDEX(str)
//SPACE(N)
class SpaceFunction extends StringFunction {
    public SpaceFunction() { super(1); }
    public String getName() { return "SPACE"; }
    public Object get(Tuple t) {
        int n = param(0).getInt(t);
        StringBuffer sbuf = new StringBuffer();
        for ( int i=0; i<n; ++i )
            sbuf.append(' ');
        return sbuf.toString();
    }
    
}
//SUBSTRING(str,pos), SUBSTRING(str,pos,len)
class SubstringFunction extends StringFunction {
    public SubstringFunction() { super(3); }
    public String getName() { return "SUBSTRING"; }
    public Object get(Tuple t) {
        String src = param(0).get(t).toString();
        int pos = param(1).getInt(t);
        if ( paramCount() == 3 ) {
            int len = param(2).getInt(t);
            return src.substring(pos, pos+len);
        } else {
            return src.substring(pos);
        }
    }
}
//SUBSTRING_INDEX(str,delim,count)
//TRIM([{BOTH | LEADING | TRAILING} [remstr] FROM] str), TRIM(remstr FROM] str)
//UPPER(str) | UCASE(str)
class UpperFunction extends StringFunction {
    public UpperFunction() { super(1); }
    public String getName() { return "UPPER"; }
    public Object get(Tuple t) {
        return param(0).get(t).toString().toUpperCase();
    }
}

// ----------------------------------------------------------------------------

//RGB(r,g,b)
class RGBFunction extends IntFunction {
    public RGBFunction() { super(3); }
    public String getName() { return "RGB"; }
    public int getInt(Tuple t) {
        int r = param(0).getInt(t);
        int g = param(1).getInt(t);
        int b = param(2).getInt(t);
        return ColorLib.rgb(r,g,b);
    }
}
//HEX(hex)
class HexFunction extends IntFunction {
    public HexFunction() { super(1); }
    public String getName() { return "RGB"; }
    public int getInt(Tuple t) {
        String hex = (String)param(0).get(t);
        return ColorLib.hex(hex);
    }
}
//RGBA(r,g,b,a)
class RGBAFunction extends IntFunction {
    public RGBAFunction() { super(4); }
    public String getName() { return "RGBA"; }
    public int getInt(Tuple t) {
        int r = param(0).getInt(t);
        int g = param(1).getInt(t);
        int b = param(2).getInt(t);
        int a = param(3).getInt(t);
        return ColorLib.rgba(r,g,b,a);
    }
}
//GRAY(v) | GRAY(v, a)
class GrayFunction extends IntFunction {
    public GrayFunction() { super(2); }
    public String getName() { return "GRAY"; }
    public int getInt(Tuple t) {
        int g = param(0).getInt(t);
        if ( paramCount() == 2 ) {
            int a = param(1).getInt(t);
            return ColorLib.gray(g, a);
        } else {
            return ColorLib.gray(g);
        }
    }
}
//HSB(h,s,b)
class HSBFunction extends IntFunction {
    public HSBFunction() { super(3); }
    public String getName() { return "HSB"; }
    public int getInt(Tuple t) {
        float h = param(0).getFloat(t);
        float s = param(1).getFloat(t);
        float b = param(2).getFloat(t);
        return ColorLib.hsb(h,s,b);
    }
}
//HSBA(h,s,b,a)
class HSBAFunction extends IntFunction {
    public HSBAFunction() { super(4); }
    public String getName() { return "HSBA"; }
    public int getInt(Tuple t) {
        float h = param(0).getFloat(t);
        float s = param(1).getFloat(t);
        float b = param(2).getFloat(t);
        float a = param(3).getFloat(t);
        return ColorLib.hsba(h,s,b,a);
    }
}
//COLORINTERP(c1, c2, f)
class ColorInterpFunction extends IntFunction {
    public ColorInterpFunction() { super(3); }
    public String getName() { return "COLORINTERP"; }
    public int getInt(Tuple t) {
        int c1 = param(0).getInt(t);
        int c2 = param(1).getInt(t);
        double f = param(2).getDouble(t);
        return ColorLib.interp(c1, c2, f);
    }
}
