package core.fingerprint;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 07.23.2015 - CC - New...
 */
public class XmlElementTreeItem {
    private String helpText = "";
    private HashMap<XmlElementAttribute,String> attributeMap = new HashMap<>();
    private Instruction instruction;

    public XmlElementTreeItem(Instruction intruction) {
        this.instruction = intruction;
    }

    public void addAttribute(XmlElementAttribute attribute, String value) {
        this.attributeMap.put(attribute,value);
    }

    public ArrayList<XmlElementAttribute> getAttributes() {
        ArrayList<XmlElementAttribute> returnList = new ArrayList<>();
        returnList.addAll(this.attributeMap.keySet());
        return returnList;
    }

    public String getAttributeValue(XmlElementAttribute attribute) {
        return this.attributeMap.get(attribute);
    }

    public String getAttributeValueByName(String attributeName) {
        StringBuilder mutableReturn = new StringBuilder();
        attributeMap.keySet().stream().filter(attribute -> attribute.getName().equals(attributeName)).forEach(desiredAttribute -> mutableReturn.append(getAttributeValue(desiredAttribute)));
        return mutableReturn.toString();
    }

    @Override
    public String toString() {
        StringBuilder returnString = new StringBuilder();
        returnString.append(this.instruction.getText()).append(" ").append(helpText).append(helpText.isEmpty() ? "" : " ");
        this.attributeMap.keySet().stream().sorted((o1, o2) -> o1.getName().compareTo(o2.getName()))
                .forEach(attribute -> returnString.append("[").append(attribute.getName()).append(": ").append(this.attributeMap.get(attribute)).append("] "));
        return returnString.toString();
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
