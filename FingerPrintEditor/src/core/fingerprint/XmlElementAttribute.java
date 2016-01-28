package core.fingerprint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 07.23.2015 - CC - New...
 */
public class XmlElementAttribute {
    private String name;
    /** if this list is empty it is assumed the attribute can accept any string */
    private List<String> options = new ArrayList<>();
    /** text that is displayed to in the UI element */
    private String helpText = "";
    private ArrayList<AttributeValidator> validators = new ArrayList<>();

    public XmlElementAttribute(String name, String helpText, List<String> options, AttributeValidator ... validators) {
        this.name = name;
        this.helpText = helpText;
        this.options.addAll(options);
        if(validators != null) {
            this.validators.addAll(Arrays.asList(validators));
        }
    }
    public XmlElementAttribute(String name, List<String> options, AttributeValidator ... validators) {
        this(name,"",options, validators);
    }
    public XmlElementAttribute(String name, String helpText, AttributeValidator ... validators) {
        this(name,helpText,new ArrayList<>(), validators);
    }
    public XmlElementAttribute(String name, AttributeValidator ... validators) {
        this(name, "", validators);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<String> options) {
        this.options = options;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public ArrayList<AttributeValidator> getValidators() {
        return validators;
    }

    @FunctionalInterface
    public interface AttributeValidator {
        boolean validate(String value);
    }
}

