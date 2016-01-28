package core.ui.widgets;

import javax.swing.*;
import java.util.ArrayList;

public class JListCell extends JPanel{
    
    public static final long serialVersionUID = 1001;

    private ArrayList<JButton> buttons = new ArrayList<JButton>();
    
    public JListCell(){
        this.setupLayout();
    }
    
    private void setupLayout(){
//        this.setBackground(Color.ORANGE);
//        this.setLayout(new GridBagLayout());
//        GridBagConstraints gbc = new GridBagConstraints();
        
        
    }
    
    public ArrayList<JButton> getButtons(){
        return this.buttons;
    }
    
    public void setButtons(ArrayList<JButton> buttons) {
        this.buttons = buttons;
        updateButtons();
    }

    private void updateButtons(){
        for(JButton button : this.buttons){
            this.add(button);
        }
    }
    
    public boolean equals(Object cell) {
        if (!(cell instanceof JListCell)) {
            return false;
        }
        return super.equals(cell);
    }
    
}
