package core.ui.table;


/**
 * Used to create instances of an object.  See SimpleAddDeleteComboBoxComponent for example use.
 *
 * @param <T> Type of object this factory creates
 * 
 * 2007.04.23 - New
 */
public interface ObjectFactory<T> {

    /**
     * Returns a new object of type T
     * @param param String supplied by the user to create the new object
     */
    public T createNewObject(String param);
    
    
}
