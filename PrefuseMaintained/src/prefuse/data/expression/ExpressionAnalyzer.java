package prefuse.data.expression;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Library class that computes some simple analyses of an expression. Each
 * analysis is computed using a visitor instance.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ExpressionAnalyzer {
    
    /**
     * Determine if an expression has a dependency on a data field.
     * @param expr the expression to analyze
     * @return true if the expression has at least one tuple data
     * field dependency.
     */
    public static boolean hasDependency(Expression expr) {
        ColumnCollector cc = new ColumnCollector(false);
        expr.visit(cc);
        return cc.getColumnCount() > 0;
    }
    
    /**
     * Get the set of data fields the expression is dependent upon.
     * @param expr the expression to analyze
     * @return a set of all data field names the expression references
     */
    public static Set getReferencedColumns(Expression expr) {
        ColumnCollector cc = new ColumnCollector(true);
        expr.visit(cc);
        return cc.getColumnSet();
    }
    
    /**
     * ExpressionVisitor that collects all referenced columns / data fields
     * in an Expression.
     */
    private static class ColumnCollector implements ExpressionVisitor {
        private boolean store;
        private Set m_cols;
        private int m_count;
        
        public ColumnCollector(boolean store) {
            this.store = store;
        }
        public int getColumnCount() {
            return m_count;
        }
        public Set getColumnSet() {
            if ( m_cols == null ) {
                return Collections.EMPTY_SET;
            } else {
                return m_cols;
            }
        }
        public void visitExpression(Expression expr) {
            if ( expr instanceof ColumnExpression ) {
                ++m_count;
                if ( store ) {
                    String field = ((ColumnExpression)expr).getColumnName();
                    if ( m_cols == null )
                        m_cols = new HashSet();
                    m_cols.add(field);
                }
                
            }
        }
        public void down() {
            // do nothing
        }
        public void up() {
            // do nothing
        }
    }
    
} // end of class ExpressionAnalyzer
