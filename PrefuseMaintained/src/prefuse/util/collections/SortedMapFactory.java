package prefuse.util.collections;

import java.util.Comparator;
import java.util.Date;

import prefuse.data.DataTypeException;


/**
 * Factory class that generates the appropriate IntSortedMap implementation
 * given a key data type.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class SortedMapFactory {

    public static IntSortedMap getMap(
            Class type, Comparator cmp, boolean unique)
        throws IncompatibleComparatorException
    {
        if ( !comparatorCheck(type, cmp) ) {
            throw new IncompatibleComparatorException();
        }
        
        if ( type.equals(int.class) || type.equals(byte.class) )
        {
            return new IntIntTreeMap((LiteralComparator)cmp, !unique);
        } 
        else if ( type.equals(long.class) || type.isAssignableFrom(Date.class) )
        {
            return new LongIntTreeMap((LiteralComparator)cmp, !unique);
        }
        else if ( type.equals(float.class) )
        {
            return new FloatIntTreeMap((LiteralComparator)cmp, !unique);
        }
        else if ( type.equals(double.class) )
        {
            return new DoubleIntTreeMap((LiteralComparator)cmp, !unique);
        }
        else if ( type.equals(boolean.class) )
        {
            return new BooleanIntBitSetMap();
        }
        else if ( Object.class.isAssignableFrom(type) )
        {
            return new ObjectIntTreeMap(cmp, !unique);
        }
        else {
            throw new DataTypeException(
                    "No map available for the provided type");
        }
    }
    
    public static boolean comparatorCheck(Class type, Comparator cmp) {
        if ( cmp == null )
        {
            return true;
        }
        else if ( type.equals(int.class) )
        {
            if ( !(cmp instanceof LiteralIterator) )
                return false;
            try {
                ((LiteralComparator)cmp).compare(0,0);
                return true;
            } catch ( Exception e ) {
                return false;
            }
        } 
        else if ( type.equals(long.class) )
        {
            if ( !(cmp instanceof LiteralIterator) )
                return false;
            try {
                ((LiteralComparator)cmp).compare(0L,0L);
                return true;
            } catch ( Exception e ) {
                return false;
            }
        }
        else if ( type.equals(float.class) )
        {
            if ( !(cmp instanceof LiteralIterator) )
                return false;
            try {
                ((LiteralComparator)cmp).compare(0.f,0.f);
                return true;
            } catch ( Exception e ) {
                return false;
            }
        }
        else if ( type.equals(double.class) )
        {
            if ( !(cmp instanceof LiteralIterator) )
                return false;
            try {
                ((LiteralComparator)cmp).compare(0.0,0.0);
                return true;
            } catch ( Exception e ) {
                return false;
            }
        }
        else if ( type.equals(boolean.class) )
        {
            if ( !(cmp instanceof LiteralIterator) )
                return false;
            try {
                ((LiteralComparator)cmp).compare(false,false);
                return true;
            } catch ( Exception e ) {
                return false;
            }
        }
        else if ( Object.class.isAssignableFrom(type) )
        {
            return true;
        }
        else {
            throw new DataTypeException(
                    "No comparator available for the provided type");
        }
    }
    
} // end of class SortedMapFactory
