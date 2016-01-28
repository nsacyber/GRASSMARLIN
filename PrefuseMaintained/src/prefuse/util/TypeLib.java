package prefuse.util;

/**
 * Library routines dealing with Java Class types.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TypeLib {

    private TypeLib() {
        // prevent instantiation
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Check if an object is an instance of a given class, or, if the class
     * is a primitive type, if the Object is an instance of the wrapper class
     * for that primitive (e.g., as Integer is a wrapper for int).
     * @param type the Class type
     * @param instance the Object instance
     * @return true if the object is an instance of the given class, or if
     * of the appropriate primitive wrapper type.
     */
    public static boolean typeCheck(Class type, Object instance) {
        return type.isAssignableFrom(instance.getClass()) ||
            isWrapperInstance(type, instance);
    }
    
    /**
     * Get the nearest shared ancestor class of two objects. Note: this
     * currently does not compute the actual least common ancestor, but
     * only looks up one level in the inheritance tree and quits if
     * it does not find a match.
     * @param o1 the first object
     * @param o2 the second object
     * @return the nearest class instance of which both objects
     * are instances
     */
    public static Class getSharedType(Object o1, Object o2) {
        return getSharedType(o1.getClass(), o2.getClass());
    }
    
    /**
     * Get the nearest shared ancestor class of two classes. Note: this
     * currently does not compute the actual least common ancestor, but
     * only looks up one level in the inheritance tree and quits if
     * it does not find a match.
     * @param type1 the first type
     * @param type2 the second type
     * @return the nearest class instance which is equal to or a
     * superclass of the two class instances
     */
    public static Class getSharedType(Class type1, Class type2) {
        if ( type1 == type2 ) {
            return type1;
        } else if ( type1.isAssignableFrom(type2) ) {
            return type1;
        } else if ( type2.isAssignableFrom(type1) ) {
            return type2;
        } else {
            return null;
        }
    }
    
    /**
     * Indicates if an object is an instance of a wrapper class for a given
     * primitive type.
     * @param type the primitive Class type
     * @param instance the object to test as wrapper (e.g., as Integer is a
     * wrapper type for int)
     * @return true if the object is a wrapper instance of the given
     * primitive type
     */
    public static boolean isWrapperInstance(Class type, Object instance) {
        if ( !type.isPrimitive() )
            throw new IllegalArgumentException("Input type must be a primitive");
        
        if ( int.class == type && instance instanceof Integer ) {
            return true;
        } else if ( long.class == type && instance instanceof Long ) {
            return true;
        } else if ( float.class == type && instance instanceof Float ) {
            return true;
        } else if ( double.class == type && instance instanceof Double ) {
            return true;
        } else if ( boolean.class == type && instance instanceof Boolean ) {
            return true;
        } else if ( short.class == type && instance instanceof Short ) {
            return true;
        } else if ( byte.class == type && instance instanceof Byte ) {
            return true;
        } else if ( char.class == type && instance instanceof Character ) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Given a numeric (byte, short, int, long, float, or double) class type or
     * associated wrapper class type, return the primitive class type
     * @param type the type to look up, must be a numerical type, but can be
     * either primitive or a wrapper.
     * @return the primitive class type
     */
    public static Class getPrimitiveType(Class type) {
        if ( Integer.class.equals(type) || type == int.class ) {
            return int.class;
        } else if ( Long.class.equals(type) || type == long.class ) {
            return long.class;
        } else if ( Float.class.equals(type) || type == float.class ) {
            return float.class;
        } else if ( Double.class.equals(type) || type == double.class ) {
            return double.class;
        } else if ( Byte.class.equals(type) || type == byte.class ) {
            return byte.class;
        } else if ( Short.class.equals(type) || type == short.class ) {
            return short.class;
        } else {
            throw new IllegalArgumentException(
                "Input class must be a numeric type");
        }
    }
    
    /**
     * Get the wrapper class type for a primitive class type.
     * @param type a class type
     * @return the wrapper class for the input type if it is a
     * primitive class type, otherwise returns the input type
     */
    public static Class getWrapperType(Class type) {
        if ( !type.isPrimitive() ) {
            return type;
        } else if ( int.class == type ) {
            return Integer.class;
        } else if ( long.class == type ) {
            return Long.class;
        } else if ( float.class == type ) {
            return Float.class;
        } else if ( double.class == type ) {
            return Double.class;
        } else if ( boolean.class == type ) {
            return Boolean.class;
        } else if ( short.class == type ) {
            return Short.class;
        } else if ( char.class == type ) {
            return Character.class;
        } else if ( byte.class == type ) {
            return Byte.class;
        } else if ( short.class == type ) {
            return Short.class;
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    /**
     * Indicates if a given class type is a primitive integer type
     * (one of byte, short, int, or long).
     * @param type the type to check
     * @return true if it is a primitive numeric type, false otherwise
     */
    public static boolean isIntegerType(Class type) {
        return ( type == byte.class || type == short.class ||
                 type == int.class  || type == long.class);
    }
    
    /**
     * Indicates if a given class type is a primitive numeric one type
     * (one of byte, short, int, long, float, or double).
     * @param type the type to check
     * @return true if it is a primitive numeric type, false otherwise
     */
    public static boolean isNumericType(Class type) {
        return ( type == byte.class   || type == short.class ||
                 type == int.class    || type == long.class  || 
                 type == double.class || type == float.class );
    }
    
    /**
     * Get a compatible numeric type for two primitive numeric
     * class types. Any of (byte, short, int) will resolve to int.
     * @param c1 a numeric primitive class type (int, long, float, or double)
     * @param c2 a numeric primitive class type (int, long, float, or double)
     * @return the compatible numeric type for binary operations involving
     * both types.
     */
    public static Class getNumericType(Class c1, Class c2) {
        if ( !isNumericType(c1) || !isNumericType(c2) ) {
            throw new IllegalArgumentException(
                "Input types must be primitive number types");
        }
        if ( c1 == double.class || c2 == double.class ) {
            return double.class;
        } else if ( c1 == float.class || c1 == float.class ) {
            return float.class;
        } else if ( c1 == long.class || c2 == long.class ) {
            return long.class;
        } else {
            return int.class;
        }
    }
    
} // end of class TypeLib
