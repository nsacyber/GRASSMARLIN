package core.ui.table;

import java.awt.event.MouseListener;

/**
 * A Row for use in tables where a user may which to add and delete rows
 * 
 * 2004.11.13 - New
 * 2007.04.21 - Removed createNewInstance - replaced by ObjectFactory
 */
public interface AddDeleteRow<T extends AddDeleteRow> extends Row {
     
    /**
     * Provides the Row the option of adding the add/delete mouse listener 
     * which displays the popup to its components.  This is especially 
     * usefull if the table has only one column and the editor expands to 
     * cover the entire cell.
     */
    public void addMouseListener(MouseListener listener);
    
    /** 
     * Notifies the Row that the user is trying to delete it.  
     * Will allow the user to delete the row if return value is true
     */
    public boolean delete();

     
    
}
