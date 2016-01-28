package prefuse.data.util;

import java.util.Comparator;
import java.util.Iterator;

import prefuse.data.Table;
import prefuse.data.expression.AndPredicate;
import prefuse.data.expression.ColumnExpression;
import prefuse.data.expression.ComparisonPredicate;
import prefuse.data.expression.Expression;
import prefuse.data.expression.ExpressionAnalyzer;
import prefuse.data.expression.NotPredicate;
import prefuse.data.expression.OrPredicate;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.RangePredicate;
import prefuse.data.tuple.TupleSet;
import prefuse.util.PrefuseConfig;
import prefuse.util.collections.CompositeIntIterator;
import prefuse.util.collections.IntIterator;

/**
 * Factory class that creates optimized filter iterators. When possible,
 * this factory will attempt to create an optimized query plan by using
 * available indexes, in many incrasing performance by only visiting
 * the tuples which will pass the filter condition.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FilterIteratorFactory {

    private static final int OPTIMIZATION_THRESHOLD
        = PrefuseConfig.getInt("data.filter.optimizeThreshold");
    
    // we can stash our query plan generation and optimization here to deal 
    // with it all in one spot, and keep the rest of the classes clean
    
    /**
     * Get a filtered iterator over the tuples in the given set,
     * filtered by the given predicate.
     * @param ts the TupleSet to iterate over
     * @param p the filter predicate
     * @return a filtered iterator over the tuples
     */
    public static Iterator tuples(TupleSet ts, Predicate p) {
        // no predicate means no filtering
        if ( p == null )
            return ts.tuples();
        
        // attempt to generate an optimized query plan
        Iterator iter = null;
        if ( ts instanceof Table ) {
            Table t = (Table)ts;
            IntIterator ii = getOptimizedIterator(t,p);
            if ( ii != null )
                iter = t.tuples(ii);
        }
        
        // optimization fails, scan the entire table
        if ( iter == null ) {
            iter = new FilterIterator(ts.tuples(), p);
        }
        
        return iter;
    }
    
    /**
     * Get a filtered iterator over the rows in the given table,
     * filtered by the given predicate.
     * @param t the Table to iterate over
     * @param p the filter predicate
     * @return a filtered iterator over the table rows
     */
    public static IntIterator rows(Table t, Predicate p) {
        // attempt to generate an optimized query plan
        IntIterator iter = null;
        iter = getOptimizedIterator(t, p);
        
        // optimization fails, scan the entire table
        if ( iter == null ) {
            iter = new FilterRowIterator(t.rows(), t, p);
        }
        return iter;
    }
    
    /**
     * Get an optimized iterator over the rows of a table, if possible.
     * @param t the Table to iterator over
     * @param p the filter predicate
     * @return an optimized iterator, or null if no optimization was found
     */
    protected static IntIterator getOptimizedIterator(Table t, Predicate p) {
        if ( t.getRowCount() < OPTIMIZATION_THRESHOLD ) 
            return null; // avoid overhead for small tables
        
        if ( p instanceof ColumnExpression ) {
            // try to optimize a boolean column
            return getColumnIterator(t,
                    ((ColumnExpression)p).getColumnName(), true);
        }
        else if ( p instanceof NotPredicate )
        {
            // try to optimize the negation a boolean column
            Predicate pp = ((NotPredicate)p).getPredicate();
            if ( pp instanceof ColumnExpression ) {
                return getColumnIterator(t,
                        ((ColumnExpression)pp).getColumnName(), false);
            }
        }
        else if ( p instanceof AndPredicate )
        {
            // try to optimize an and clause
            return getAndIterator(t, (AndPredicate)p);
        }
        else if ( p instanceof OrPredicate )
        {
            // try to optimize an or clause
            return getOrIterator(t, (OrPredicate)p);
        }
        else if ( p instanceof ComparisonPredicate )
        {
            // try to optimize a comparison (=, !=, <, > ,etc)
            return getComparisonIterator(t,(ComparisonPredicate)p);
        }
        else if ( p instanceof RangePredicate )
        {
            // try to optimize a bounded range of values
            return getRangeIterator(t, (RangePredicate)p); 
        }
        
        return null;
    }
    
    protected static IntIterator getColumnIterator(
            Table t, String field, boolean val)
    {
        if ( t.getColumnType(field) != boolean.class )
            return null; // only works for boolean-valued columns
        
        Index index = t.getIndex(field);
        if ( index == null ) {
            return null;
        } else {
            return index.rows(val);
        }
    }
    
    protected static IntIterator getOrIterator(Table t, OrPredicate op) {
        int size = op.size();
        if ( size > 1 ) {
            // if all subclauses can be optimized, we can optimize the query
            IntIterator[] rows = new IntIterator[size];
            for ( int i=0; i<rows.length; ++i ) {
                rows[i] = getOptimizedIterator(t, op.get(i));
                
                // all clauses must be optimized to avoid linear scan
                if ( rows[i] == null ) return null;
            }
            // group iterators, and filter for uniqueness
            return new UniqueRowIterator(new CompositeIntIterator(rows));
        } else if ( size == 1 ) {
            // only one clause, optimize for that
            return getOptimizedIterator(t, op.get(0));
        } else {
            // no woman, no cry
            return null;
        }
    }
    
    protected static IntIterator getAndIterator(Table t, AndPredicate ap) {
        // possible TODO: add scoring to select best optimized iterator
        // for now just work from the end backwards and take the first
        // optimized iterator we find
        IntIterator rows = null;
        Predicate clause = null;
        for ( int i=ap.size(); --i >= 0; ) {
            clause = ap.get(i);
            if ( (rows=getOptimizedIterator(t,clause)) != null )
                break;
        }
        
        // exit if we didn't optimize
        if ( rows == null ) return null;
        
        // if only one clause, no extras needed
        if ( ap.size() == 1 ) return rows;
        
        // otherwise get optimized source, run through other clauses
        return new FilterRowIterator(rows, t, ap.getSubPredicate(clause));
    }
    
    protected static IntIterator getComparisonIterator(Table t, 
                                           ComparisonPredicate cp)
    {
        Expression l = cp.getLeftExpression();
        Expression r = cp.getRightExpression();
        int operation = cp.getOperation();
        
        // not equals operations aren't handled by the index
        if ( operation == ComparisonPredicate.NEQ )
            return null;
        
        ColumnExpression col;
        Expression lit;
        
        // make sure columns are of the right type
        if (l instanceof ColumnExpression && 
                !ExpressionAnalyzer.hasDependency(r))
        {
            col = (ColumnExpression)l;
            lit = r;
        } else if (r instanceof ColumnExpression &&
                !ExpressionAnalyzer.hasDependency(l))
        {
            col = (ColumnExpression)r;
            lit = l;
        } else {
            return null;
        }
        
        // if table has index of the right type, use it
        Comparator cmp = cp.getComparator();
        Index index = t.getIndex(col.getColumnName());
        
        if ( index == null || !cmp.equals(index.getComparator()) )
            return null;
        
        Class ltype = lit.getClass();
        if ( ltype == int.class ) {
            int val = lit.getInt(null); // literal value, so null is safe
            switch ( operation ) {
            case ComparisonPredicate.LT:
                return index.rows(Integer.MIN_VALUE, val, Index.TYPE_AIE);
            case ComparisonPredicate.GT:
                return index.rows(val, Integer.MAX_VALUE, Index.TYPE_AEI);
            case ComparisonPredicate.EQ:
                return index.rows(val, val, Index.TYPE_AII);
            case ComparisonPredicate.LTEQ:
                return index.rows(Integer.MIN_VALUE, val, Index.TYPE_AII);
            case ComparisonPredicate.GTEQ:
                return index.rows(val, Integer.MAX_VALUE, Index.TYPE_AII);
            default:
                throw new IllegalStateException(); // should never occur
            }
        } else if ( ltype == long.class ) {
            long val = lit.getLong(null); // literal value, so null is safe
            switch ( operation ) {
            case ComparisonPredicate.LT:
                return index.rows(Long.MIN_VALUE, val, Index.TYPE_AIE);
            case ComparisonPredicate.GT:
                return index.rows(val, Long.MAX_VALUE, Index.TYPE_AEI);
            case ComparisonPredicate.EQ:
                return index.rows(val, val, Index.TYPE_AII);
            case ComparisonPredicate.LTEQ:
                return index.rows(Long.MIN_VALUE, val, Index.TYPE_AII);
            case ComparisonPredicate.GTEQ:
                return index.rows(val, Long.MAX_VALUE, Index.TYPE_AII);
            default:
                throw new IllegalStateException(); // should never occur
            }
        } else if ( ltype == float.class ) {
            float val = lit.getFloat(null); // literal value, so null is safe
            switch ( operation ) {
            case ComparisonPredicate.LT:
                return index.rows(Float.MIN_VALUE, val, Index.TYPE_AIE);
            case ComparisonPredicate.GT:
                return index.rows(val, Float.MAX_VALUE, Index.TYPE_AEI);
            case ComparisonPredicate.EQ:
                return index.rows(val, val, Index.TYPE_AII);
            case ComparisonPredicate.LTEQ:
                return index.rows(Float.MIN_VALUE, val, Index.TYPE_AII);
            case ComparisonPredicate.GTEQ:
                return index.rows(val, Float.MAX_VALUE, Index.TYPE_AII);
            default:
                throw new IllegalStateException(); // should never occur
            }
        } else if ( ltype == double.class ) {
            double val = lit.getDouble(null); // literal value, so null is safe
            switch ( operation ) {
            case ComparisonPredicate.LT:
                return index.rows(Double.MIN_VALUE, val, Index.TYPE_AIE);
            case ComparisonPredicate.GT:
                return index.rows(val, Double.MAX_VALUE, Index.TYPE_AEI);
            case ComparisonPredicate.EQ:
                return index.rows(val, val, Index.TYPE_AII);
            case ComparisonPredicate.LTEQ:
                return index.rows(Double.MIN_VALUE, val, Index.TYPE_AII);
            case ComparisonPredicate.GTEQ:
                return index.rows(val, Double.MAX_VALUE, Index.TYPE_AII);
            default:
                throw new IllegalStateException(); // should never occur
            }
        } else {
            Object val = lit.get(null); // literal value, so null is safe
            switch ( operation ) {
            case ComparisonPredicate.LT:
                return index.rows(null, val, Index.TYPE_AIE);
            case ComparisonPredicate.GT:
                return index.rows(val, null, Index.TYPE_AEI);
            case ComparisonPredicate.EQ:
                return index.rows(val, val, Index.TYPE_AII);
            case ComparisonPredicate.LTEQ:
                return index.rows(null, val, Index.TYPE_AII);
            case ComparisonPredicate.GTEQ:
                return index.rows(val, null, Index.TYPE_AII);
            default:
                throw new IllegalStateException(); // should never occur
            }
        }        
    }
    
    protected static IntIterator getRangeIterator(Table t, RangePredicate rp) {
        ColumnExpression col;
        Expression l, r;
        
        // make sure columns are of the right type
        if ( !(rp.getMiddleExpression() instanceof ColumnExpression) ||
                ExpressionAnalyzer.hasDependency(rp.getLeftExpression()) ||
                ExpressionAnalyzer.hasDependency(rp.getRightExpression()) )
        {
            return null;
        }
        
        // assign variables
        col = (ColumnExpression)rp.getMiddleExpression();
        l = rp.getLeftExpression();
        r = rp.getRightExpression();
        
        // if table has index of the right type, use it
        Comparator cmp = rp.getComparator();
        Index index = t.getIndex(col.getColumnName());
        
        if ( index == null || !cmp.equals(index.getComparator()) )
            return null;
        
        int operation = rp.getOperation();
        Class ltype = t.getColumnType(col.getColumnName());
        
        // TODO safety check literal types
        
        // get the index type
        int indexType;
        switch ( operation ) {
        case RangePredicate.IN_IN:
            indexType = Index.TYPE_AII;
            break;
        case RangePredicate.IN_EX:
            indexType = Index.TYPE_AIE;
            break;
        case RangePredicate.EX_IN:
            indexType = Index.TYPE_AEI;
            break;
        case RangePredicate.EX_EX:
            indexType = Index.TYPE_AEE;
            break;
        default:
            throw new IllegalStateException(); // should never occur
        }
        
        // get the indexed rows
        if ( ltype == int.class ) {
            return index.rows(l.getInt(null), r.getInt(null), indexType);
        } else if ( ltype == long.class ) {
            return index.rows(l.getLong(null), r.getLong(null), indexType);
        } else if ( ltype == float.class ) {
            return index.rows(l.getFloat(null), r.getFloat(null), indexType);
        } else if ( ltype == double.class ) {
            return index.rows(l.getDouble(null), r.getDouble(null), indexType);
        } else {
            return index.rows(l.get(null), r.get(null), indexType);
        }
    }
    
} // end of class FilterIteratorFactory
