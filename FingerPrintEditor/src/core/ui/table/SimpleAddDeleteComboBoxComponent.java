package core.ui.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

/**
 * Allows a user to add and delete items from a JComboBox and
 * respond to selection, addition, and deletion actions resulting
 * from its use.
 * 
 * @param T Type of object used in ComboBox
 * 
 * 2007.04.23 - New (FrontMan)
 * 2007.05.11 - Added constructor param to set the text on the add new input dialog
 * 2007.09.04 - Imported to repository from Xware
 */
public class SimpleAddDeleteComboBoxComponent<T> extends JPanel {
    public static final long serialVersionUID = 10001;

    private SimpleAddDeleteComboBoxComponentListener<T> listener;
    /** Used to create new Objects **/
    private ObjectFactory<T> objectFactory;
    /** label for the Add link **/
    private JLabel addLabel;
    /** label for the Delete link **/
    private JLabel deleteLabel;
    /** combo box **/
    private JComboBox comboBox;
    /** The text displayed on the Input Dialog when adding a new item **/
    private String addNewDescription;
    
    /**
     * Creates a new instance of SimpleAddDelteComboBoxComponent
     * 
     * @param title Title for the component
     * @param addNewDescription The text to display to the user when prompting them 
     *        for input when adding a new item 
     * @param initialItems Ordered list of initial items to display in the combobox
     * @param objectFactory Factory for creating new object when a user requests one
     * @param listener Listens for add, delete, and selection changes from this panel
     */
    public SimpleAddDeleteComboBoxComponent(String title,
                                            String addNewDescription,
                                            Collection<T> initialItems,
                                            ObjectFactory<T> objectFactory, 
                                            SimpleAddDeleteComboBoxComponentListener<T> listener) {
        this.addNewDescription = addNewDescription;
        this.listener = listener;
        this.objectFactory = objectFactory;
        this.buildInterface(title, initialItems);
    }
    
    /**
     * Builds the interface for this component
     */
    private final void buildInterface(String title, Collection<T> initialItems) { 
        this.setLayout(new GridLayout(2,1));
        this.add(this.getTitlePane(title));
        this.add(this.getComboBox());
        for (T t : initialItems) { 
            this.comboBox.addItem(t);
        }
    }
    
    /** 
     * Returns the titel pane for this component
     */
    private final Component getTitlePane(String title) {
        this.addLabel = new JLabel("<html><u><font color=blue>Add</u></font></html>");
        this.addLabel.addMouseListener(this.addMouseAdapter);
        this.deleteLabel = new JLabel("<html><u><font color=blue>Delete</u></font></html>");
        this.deleteLabel.addMouseListener(this.deleteMouseAdapter);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel(title));
        panel.add(Box.createHorizontalStrut(3));
        panel.add(this.addLabel);
        panel.add(Box.createHorizontalStrut(3));
        panel.add(this.deleteLabel);
        return panel;
    }
    
    
    /**
     * Returns the combo box
     */
    private final Component getComboBox() {
        this.comboBox = new JComboBox();
        this.comboBox.addActionListener(this.comboBoxSelectionAction);
        return this.comboBox;
    }
    

    /**
     * Pops up a dialog for adding a new item to this component 
     */
    private void processAdd() { 
        String input = JOptionPane.showInputDialog(this, this.addNewDescription);
        if (input != null && input.trim().length() > 0) { 
            T newObject = this.objectFactory.createNewObject(input);
            this.comboBox.addItem(newObject);
            this.comboBox.setSelectedItem(newObject);
            this.listener.itemAdded(this, newObject);
        }
    }
    
    
    /**
     * Deletes the selected item from the component
     */
    private void processDelete() { 
        T deletedObject = (T)this.comboBox.getSelectedItem();
        if (deletedObject != null) { 
            this.comboBox.removeItem(deletedObject);
            this.listener.itemDeleted(this, deletedObject);
        }
    }
    
    /**
     * Action to fire when selection changes on combo box
     */
    private AbstractAction comboBoxSelectionAction = new AbstractAction() { 
        public static final long serialVersionUID = 10001;
        public void actionPerformed(ActionEvent ae) { 
            SimpleAddDeleteComboBoxComponent.this.listener.selectionChanged(SimpleAddDeleteComboBoxComponent.this,
                                                                            (T)SimpleAddDeleteComboBoxComponent.this.comboBox.getSelectedItem());
        }
    };
    
    /**
     * Listens for mouse events on the add label
     */
    private MouseListener addMouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            SimpleAddDeleteComboBoxComponent.this.processAdd();
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            SimpleAddDeleteComboBoxComponent.this.addLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override
        public void mouseExited(MouseEvent e) {
            SimpleAddDeleteComboBoxComponent.this.addLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } 
    };

    
    /**
     * Listens for mouse events on the delete label
     */
    private MouseListener deleteMouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            SimpleAddDeleteComboBoxComponent.this.processDelete();
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            SimpleAddDeleteComboBoxComponent.this.deleteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override
        public void mouseExited(MouseEvent e) {
            SimpleAddDeleteComboBoxComponent.this.deleteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } 
    };

    
}


















