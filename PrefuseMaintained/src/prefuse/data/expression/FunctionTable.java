package prefuse.data.expression;

import java.util.HashMap;

import prefuse.visual.expression.GroupSizeFunction;
import prefuse.visual.expression.HoverPredicate;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.expression.QueryExpression;
import prefuse.visual.expression.SearchPredicate;
import prefuse.visual.expression.ValidatedPredicate;
import prefuse.visual.expression.VisiblePredicate;

/**
 * Function table that allows lookup of registered FunctionExpressions
 * by their function name.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FunctionTable {
    
    private FunctionTable() {
        // prevent instantiation
    }
    
    private static HashMap s_functionTable;
    static {
        s_functionTable = new HashMap();
        // tuple functions
        addFunction("ROW", RowFunction.class);
        addFunction("ISNODE", IsNodeFunction.class);
        addFunction("ISEDGE", IsEdgeFunction.class);
        addFunction("DEGREE", DegreeFunction.class);
        addFunction("INDEGREE", InDegreeFunction.class);
        addFunction("OUTDEGREE", OutDegreeFunction.class);
        addFunction("CHILDCOUNT", ChildCountFunction.class);
        addFunction("TREEDEPTH", TreeDepthFunction.class);
        
        // numeric functions
        addFunction("ABS", AbsFunction.class);
        addFunction("ACOS", AcosFunction.class);
        addFunction("ASIN", AsinFunction.class);
        addFunction("ATAN", AtanFunction.class);
        addFunction("ATAN2", Atan2Function.class);
        addFunction("CEIL", CeilFunction.class);
        addFunction("CEILING", CeilFunction.class);
        addFunction("COS", CosFunction.class);
        addFunction("COT", CotFunction.class);
        addFunction("DEGREES", DegreesFunction.class);
        addFunction("E", EFunction.class);
        addFunction("EXP", ExpFunction.class);
        addFunction("FLOOR", FloorFunction.class);
        addFunction("LOG", LogFunction.class);
        addFunction("LOG2", Log2Function.class);
        addFunction("LOG10", Log10Function.class);
        addFunction("MAX", MaxFunction.class);
        addFunction("MIN", MaxFunction.class);
        addFunction("MOD", MaxFunction.class);
        addFunction("PI", PiFunction.class);
        addFunction("POW", PowFunction.class);
        addFunction("POWER", PowFunction.class);
        addFunction("RADIANS", RadiansFunction.class);
        addFunction("RAND", RandFunction.class);
        addFunction("ROUND", RoundFunction.class);
        addFunction("SIGN", SignFunction.class);
        addFunction("SIN", SinFunction.class);
        addFunction("SQRT", SqrtFunction.class);
        addFunction("SUM", SumFunction.class);
        addFunction("TAN", TanFunction.class);
        
        addFunction("SAFELOG10", SafeLog10Function.class);
        addFunction("SAFESQRT", SafeSqrtFunction.class);
        
        // string functions
        addFunction("CAP", CapFunction.class);
        addFunction("CONCAT", ConcatFunction.class);
        addFunction("CONCAT_WS", ConcatWsFunction.class);
        addFunction("FORMAT", FormatFunction.class);
        addFunction("INSERT", RPadFunction.class);
        addFunction("LENGTH", LengthFunction.class);
        addFunction("LOWER", LowerFunction.class);
        addFunction("LCASE", LowerFunction.class);
        addFunction("LEFT", LeftFunction.class);
        addFunction("LPAD", LPadFunction.class);
        addFunction("MID", SubstringFunction.class);
        addFunction("POSITION", PositionFunction.class);
        addFunction("REVERSE", ReverseFunction.class);
        addFunction("REPEAT", RepeatFunction.class);
        addFunction("REPLACE", ReplaceFunction.class);
        addFunction("RIGHT", RightFunction.class);
        addFunction("RPAD", RPadFunction.class);
        addFunction("SPACE", SpaceFunction.class);
        addFunction("SUBSTRING", SubstringFunction.class);
        addFunction("UPPER", UpperFunction.class);
        addFunction("UCASE", UpperFunction.class);
        
        // color functions
        addFunction("RGB", RGBFunction.class);
        addFunction("RGBA", RGBAFunction.class);
        addFunction("GRAY", GrayFunction.class);
        addFunction("HEX", HexFunction.class);
        addFunction("HSB", HSBFunction.class);
        addFunction("HSBA", HSBAFunction.class);
        addFunction("COLORINTERP", ColorInterpFunction.class);
        
        // visualization functions
        addFunction("GROUPSIZE", GroupSizeFunction.class);
        addFunction("HOVER", HoverPredicate.class);
        addFunction("INGROUP", InGroupPredicate.class);
        addFunction("MATCH", SearchPredicate.class);
        addFunction("QUERY", QueryExpression.class);
        addFunction("VISIBLE", VisiblePredicate.class);
        addFunction("VALIDATED", ValidatedPredicate.class);
    }
    
    /**
     * Indicates if a function of the given name is included in the function
     * table.
     * @param name the function name
     * @return true if the function is in the table, false otherwise
     */
    public static boolean hasFunction(String name) {
        return s_functionTable.containsKey(name);
    }
    
    /**
     * Add a function to the function table. It will then become available
     * for use with compiled statements of the prefuse expression language.
     * @param name the name of the function. This name must not already
     * be registered in the table, i.e. there is no function overloading.
     * @param type the Class instance of the function itself
     */
    public static void addFunction(String name, Class type) {
        if ( !Function.class.isAssignableFrom(type) ) {
            throw new IllegalArgumentException(
                "Type argument must be a subclass of FunctionExpression.");
        }
        if ( hasFunction(name) ) {
            throw new IllegalArgumentException(
                "Function with that name already exists");
        }
        String lo = name.toLowerCase();
        String hi = name.toUpperCase();
        if ( !name.equals(lo) && !name.equals(hi) )
            throw new IllegalArgumentException(
                "Name can't have mixed case, try \""+hi+"\" instead.");
        s_functionTable.put(lo, type);
        s_functionTable.put(hi, type);
    }
    
    /**
     * Get a new Function instance for the function with the given name.
     * @param name the name of the function to create
     * @return the instantiated Function
     */
    public static Function createFunction(String name) {
        Class type = (Class)s_functionTable.get(name);
        if ( type == null ) {
            throw new IllegalArgumentException(
                    "Unrecognized function name");
        }
        try {
            return (Function)type.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

} // end of class FunctionTable
