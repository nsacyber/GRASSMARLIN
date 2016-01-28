package core.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/** 
 * Basic panel for selecting a subset of items from a list
 *  
 * 2007.04.08 - New
 */
public class SubsetSelectorPanel<E> extends JPanel {
    public static final long serialVersionUID = 10001;
    private Collection<E> pool;
    private Collection<E> subset;
    private JList poolList;
    private JList subsetList;
    private String poolTitle;
    private String subsetTitle;
    private ImageIcon leftToRightIcon = new ImageIcon(this.getClass().getResource("/images/navigate_right2.png"));
    private ImageIcon rightToLeftIcon = new ImageIcon(this.getClass().getResource("/images/navigate_left2.png"));

    /**
     * Creates a new instance of SubsetSelectorPanel
     * @param pool Pool of available items (expected to not include any subset members)
     * @param subset Subset of pool items
     * @param poolTitle Title for the available items
     * @param subsetTitle Title for the subsest items
     */
    public SubsetSelectorPanel(Collection<E> pool, Collection<E> subset, String poolTitle, String subsetTitle) {
        this.pool = pool;
        this.subset = subset;
        this.poolTitle = poolTitle;
        this.subsetTitle = subsetTitle;
        this.buildInterface();
    }
    
    /**
     * Builds the interface for this selector panel
     */
    private final void buildInterface() { 
        this.poolList = new JList(this.sortArray(this.pool.toArray()));
        this.subsetList = new JList(this.sortArray(this.subset.toArray()));
        Component leftPane = this.getListPanel(this.poolList, this.poolTitle);
        Component rightPane = this.getListPanel(this.subsetList, this.subsetTitle);

        JButton leftToRightButton = new JButton(this.getLeftToRightAction());
        JButton rightToLeftButton = new JButton(this.getRightToLeftAction());
        Component centerStrut = Box.createVerticalStrut(10);
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(3,1));
        centerPanel.add(leftToRightButton);
        centerPanel.add(centerStrut);
        centerPanel.add(rightToLeftButton);
        
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gbc.gridy = 0;
        gbc.weightx = 1d;
        gbc.weighty = 1d;
        gbc.fill = GridBagConstraints.BOTH;
        this.add(leftPane, gbc);
        gbc.gridx = 2;
        this.add(rightPane, gbc);
        gbc.gridx = 1;
        gbc.weightx = gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5,5,5,5); 
        this.add(centerPanel, gbc);
        this.setBorder(BorderFactory.createEmptyBorder(15,15,15,15));
    }
    

    /**
     * Creates the Panel with the list displayed
     */
    private final Component getListPanel(JList list, String title) {
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel(title);
        titlePanel.add(titleLabel);
        JScrollPane scrollPane = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(titlePanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    
    /** 
     * Creates the action for moving objects from the left list to the right list
     */
    private final AbstractAction getLeftToRightAction() { 
        AbstractAction action = new AbstractAction() {
            public static final long serialVersionUID = 10001;
            public void actionPerformed(ActionEvent ae) {
                try {
                    Collection<Object> objectsToMove = new ArrayList<Object>();
                    for (int index : poolList.getSelectedIndices()) { 
                        objectsToMove.add(poolList.getModel().getElementAt(index));
                    }
                    for (Object selected : objectsToMove) { 
                        if (selected != null) { 
                            pool.remove(selected);
                            subset.add((E)selected);
                            poolList.setListData(sortArray(pool.toArray()));
                            subsetList.setListData(sortArray(subset.toArray()));
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) { 
                    // must not be anything to select
                }
            }
        };
        action.putValue(Action.SMALL_ICON, this.leftToRightIcon);
        return action;
    }
    
    /**
     * Returns the action for moving items from the right list to the left list
     * @return
     */
    private final AbstractAction getRightToLeftAction() {
        AbstractAction action = new AbstractAction() { 
            public static final long serialVersionUID = 1001;
            public void actionPerformed(ActionEvent ae) {
                try { 
                    Collection<Object> objectsToMove = new ArrayList<Object>();
                    for (int index : subsetList.getSelectedIndices()) { 
                        objectsToMove.add(subsetList.getModel().getElementAt(index));
                    }
                    for (Object selected : objectsToMove) { 
                        if (selected != null) { 
                            subset.remove(selected);
                            pool.add((E)selected);
                            poolList.setListData(sortArray(pool.toArray()));
                            subsetList.setListData(sortArray(subset.toArray()));
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) { 
                    // must not be anything to select
                }
            }
        };
        action.putValue(Action.SMALL_ICON, this.rightToLeftIcon);
        return action;
    }

    /**
     * Updates the lists for the pool and subset
     */
    public void updateLists(Collection<E> pool, Collection<E> subset) { 
        this.pool = pool;
        this.subset = subset;
        this.poolList.setListData(this.pool.toArray());
        this.subsetList.setListData(this.subset.toArray());
    }

    /**
     * Sorts the objects by the natural order if they support Comparable 
     */
    private Object[] sortArray(Object[] items) {
        if (items.length > 0 && items[0] instanceof Comparable) { 
            Arrays.sort(items);
        }
        return items;
    }
    
        
    
}
