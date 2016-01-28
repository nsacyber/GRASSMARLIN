package core.ui.table;

/**
 * Allows a user to respond to actions performed during the
 * use of a SimpleAddDeleteComboBoxComponent
 * 
 * @param Type of item used in Component
 * 
 * 2007.04.23 - New
 */
public interface SimpleAddDeleteComboBoxComponentListener<T> {
    
    /**
     * User selected a new item
     * @param component source of action
     * @param selection new selection
     */
    public void selectionChanged(SimpleAddDeleteComboBoxComponent component, T selection);
    
    /**
     * User deleted an item
     * @param component source of action
     * @param deletedItem item that was deleted
     */
    public void itemDeleted(SimpleAddDeleteComboBoxComponent component, T deletedItem);
    
    /**
     * User added an item
     * @param component source of action
     * @param addedItem item that was added
     */
    public void itemAdded(SimpleAddDeleteComboBoxComponent component, T addedItem);

}
