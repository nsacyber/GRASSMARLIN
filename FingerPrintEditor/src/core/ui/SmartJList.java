package core.ui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

/**
 * Standard JList with a ListCellRenderer that delegates the job
 * of rendering to each SmartJLlistItem, and a MouseListener that 
 * delegates the job of handling MouseEvents to the SmartJListItem
 * that is at the point of the mouse event.
 * 
 * 2005.12.22 - Transitioned from Xware Legacy
 * 2006.07.07 - Added maybeSelectIndex method to adjust behavior so a selection is only processed if the current index is not selected, this made it play nicely with multiple selections when a right-click is needed
 */
public class SmartJList extends JList{
    public static final long serialVersionUID = 10001;
    
    /** 
     * Creates a new instance of SmartJList
     */
    public SmartJList(){ 
        this.init(); 
    }    
    
    /** 
     * Creates a new instance of SmartJList
     */
    public SmartJList(SmartJListItem[] listData){
        super(listData); 
        this.init();
    }
    
    /** 
     * Creates a new instance of SmartJList 
     */
    public SmartJList(Vector<SmartJListItem> listData){ 
        super(listData); 
        this.init();
    }        
    
    /**
     * Adds the primary MouseAdapter and ListCellRenderer to the JList
     */
    private final void init(){
        super.addMouseListener(listMouseAdapter);
        super.setCellRenderer(listCellRenderer);
    }
    
    /**
     * MouseAdapter for the SmartJList.  Retrieves the mouse adapter
     * from each SmartJListItem and passes on the event.
     */
    private MouseAdapter listMouseAdapter = new MouseAdapter(){
        public void mouseClicked(MouseEvent me){ 
            SmartJList.this.getMouseAdapter(me, true).mouseClicked(me); 
        }
        
        public void mouseEntered(MouseEvent me){ 
            SmartJList.this.getMouseAdapter(me, false).mouseEntered(me); 
        }
        
        public void mouseExited(MouseEvent me){ 
            SmartJList.this.getMouseAdapter(me, false).mouseExited(me); 
        }
        
        public void mousePressed(MouseEvent me){ 
            SmartJList.this.getMouseAdapter(me, true).mousePressed(me); 
        }
        
        public void mouseReleased(MouseEvent me){ 
            SmartJList.this.getMouseAdapter(me, false).mouseReleased(me); 
        }
    };
    
    
    /**
     * Returns the MouseAdapter for the SmartJListItem at the
     * point where the mouse event occured.  
     * @param me The MouseEvent that fired this action
     * @select Will change the list selection to the SmartJListItem at the point when true
     */
    private MouseAdapter getMouseAdapter(MouseEvent me, boolean select)
    {
         int index = super.locationToIndex(me.getPoint());
         if ( index == -1 ){ 
             // the mouse is not over an item
             return this.defaultMouseAdapter; 
         }         
         if ( select ){ 
             this.maybeSelectIndex(me, index);
         } 
         Object element = this.getModel().getElementAt(index);
         if ( element != null &&
              element instanceof SmartJListItem ){
             SmartJListItem item = (SmartJListItem)element;
             return item.getMouseAdapter();
         }
         else{
             // element is either null or not a SmartJListItem
             return this.defaultMouseAdapter;
         }
    }   

    /**
     * Will select the list item at the mouse pointer if it is not 
     * already part of the selection when the user right-clicks on 
     * an item
     * @param me
     * @param index
     */
    private void maybeSelectIndex(MouseEvent me, int index){
        if ( me.getButton() == MouseEvent.BUTTON3 ) {
            boolean isSelected = false;
            for ( int selectedIndex : this.getSelectedIndices() ){
                if ( selectedIndex == index) {
                    isSelected = true;
                }
            }
            if ( ! isSelected ){
                this.setSelectedIndex(index);
            }
        }
    }
    
    
    /** 
     * Delegates the task of rendering each SmartJListItem to each item 
     */ 
    private ListCellRenderer listCellRenderer = new ListCellRenderer() {
        public Component getListCellRendererComponent(JList list, 
                                                      Object value, 
                                                      int index, 
                                                      boolean isSelected, 
                                                      boolean cellHasFocus){
            if ( value != null &&
                 value instanceof SmartJListItem ){
                return ((SmartJListItem)value).getRenderedComponent(list,
                                                                    value,
                                                                    index,
                                                                    isSelected,
                                                                    cellHasFocus);
            }
            else{
                String type = "null";
                if ( value != null ){
                    type = value.getClass().toString();
                }
                return new JLabel("Invalid ListItem type [" + type + "]");
            }
        }
    };

    
    /**
     * Mouse adapter used when a valid SmartJListItem can not 
     * be identfied.  Takes no action.
     */
    private MouseAdapter defaultMouseAdapter = new MouseAdapter(){        
    };
    
    
    
}
