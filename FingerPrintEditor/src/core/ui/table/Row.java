package core.ui.table;

/**
 * A Row for use in a SmartTable architecture
 * 
 * 2004.11.11 - New
 */
public interface Row {

    /**
     * Sets value at column to value
     */
    public void setValueAt(int column, Object value);

    /**
     * Returns the object for column
     */
    public Object getValueAt(int column);

    /**
     * Sets this Row Enabled or Disabled
     */
    public void setEnabled(boolean bv);

    /** 
     * Returns true if this row is enabled
     */
    public boolean isEnabled();
    
}
