package prefuse.data.expression;

/**
 * Expression sub-interface representing a function in the prefuse
 * expression language.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a> 
 */
public interface Function extends Expression {

    /** Constant indicating a vriable argument count */
    public static final int VARARGS = -1;
    
    /**
     * Get the name of this function.
     * @return the function name
     */
    public String getName();
    
    /**
     * Add a parameter value sub-expression to this function.
     * @param e the parameter sub-expression
     */
    public void addParameter(Expression e);
    
    /**
     * Get the maximum number of parameters accepted by this Function.
     * @return the maximum number of parametes accepted, or
     * {@link #VARARGS} is the number is variable.
     */
    public int getParameterCount();
    
} // end of interface Function
