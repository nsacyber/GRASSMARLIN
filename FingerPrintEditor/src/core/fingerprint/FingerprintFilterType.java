package core.fingerprint;

import core.fingerprint3.Fingerprint;
import core.fingerprint3.ObjectFactory;
import core.ui.graph.RoundedTextNode;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.util.Pair;
import sample.Controller;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * The filter buttons are defined here with validation
 *
 * Created by CC on 7/13/2015.
 */
public enum FingerprintFilterType {
    ACK("Ack", "Accepts TCP packets with the ACK flag set. \n" +
            "This will force the TransportProtocol to only check TCP packets.",
            PacketType.TCP) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleLongValidator(valueDialog, newValue, "Must be a [Long] value");
            }));
            return valueDialog.showAndWait().orElse("");
        }

        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            long value = Long.parseLong(node.getTextLine2());
            return fpFactory.createFingerprintFilterAck(value);
        }
    },
    MSS("MSS", "Accepts a MSS value for TCP packets with the MSS optional flag set. \n" +
            "This will force the TransportProtocol to only check TCP packets. ",
            PacketType.TCP) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleIntegerValidator(valueDialog, newValue, "Must be a positive [Integer] value", intValue -> (intValue > 0));
            }));
            return valueDialog.showAndWait().orElse("");
        }

        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            return fpFactory.createFingerprintFilterMSS(new BigInteger(node.getTextLine2()));
        }
    },
    DSIZE("Dsize", "Accepts a packet with a payload size equal to a number of bytess. \n" +
            "This does not include the size of a packet header. ",
            PacketType.ANY) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleIntegerValidator(valueDialog, newValue, "Must be a [Integer]");
            }));
            return valueDialog.showAndWait().orElse("");
        }

        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            return fpFactory.createFingerprintFilterDsize(new BigInteger(node.getTextLine2()));
        }
    },
    DSIZEWITHIN("DsizeWithin","Accepts a packet with a payload size within a range of bytes(s). \n" +
            "This does not include the size of a packet header. Dsize must contain one to two inequalities. ",
            PacketType.ANY) {
        @Override
        public String getValueFromUser() {
            PairTextInputDialog valueDialog = new PairTextInputDialog();
            valueDialog.setTitle("Filter Value");
            valueDialog.setHeaderText("Enter a value for the filter");
            valueDialog.setContentText("Value:");
            //start invalid
            valueDialog.getEditors().getKey().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
            valueDialog.getEditors().getValue().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
            valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            valueDialog.getEditors().getKey().textProperty().addListener(((observable, oldValue, newValue) -> {
                validateMultiple(valueDialog);
            }));
            valueDialog.getEditors().getValue().textProperty().addListener(((observable, oldValue, newValue) -> {
                validateMultiple(valueDialog);
            }));
            StringBuilder returnBuilder = new StringBuilder();
            valueDialog.showAndWait().ifPresent(values -> {
                returnBuilder.append(values.getKey()).append(" - ").append(values.getValue());
            });
            return returnBuilder.toString();
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            String text = node.getTextLine2();
            String[] splitText = text.split(" - ");
            if(splitText.length != 2) {
                throw new Exception("Invalid range format: ["+text+"]");
            }
            Fingerprint.Filter.DsizeWithin withinRange = fpFactory.createFingerprintFilterDsizeWithin();
            withinRange.setMax(new BigInteger(splitText[1]));
            withinRange.setMin(new BigInteger(splitText[0]));
            return fpFactory.createFingerprintFilterDsizeWithin(withinRange);
        }
    },
    DSTPORT("DstPort","Accepts the destination port of a TCP or UDP packet. ", PacketType.ANY) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleIntegerValidator(valueDialog, newValue, "Must be an [Integer] between\n[0] and [" + (int) Math.pow(2, 16) + "]", intValue -> (intValue > 0), intValue1 -> (intValue1 < Math.pow(2, 16)));
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            int value = Integer.parseInt(node.getTextLine2());
            return fpFactory.createFingerprintFilterDstPort(value);
        }
    },
    SRCPORT("SrcPort","Accepts the source port of a TCP or UDP packet. ",PacketType.ANY){
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleIntegerValidator(valueDialog, newValue, "Must be an [Integer] between\n[0] and [" + (int)Math.pow(2,16)+"]",intValue -> (intValue > 0), intValue1 -> (intValue1 < Math.pow(2,16)));
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            int value = Integer.parseInt(node.getTextLine2());
            return fpFactory.createFingerprintFilterSrcPort(value);
        }
    },
    ETHERTYPE("Ethertype","Accepts the Ethertype of an ethernet frame. [2048] = IPv4",PacketType.OTHER) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleIntegerValidator(valueDialog, newValue, "Must be an [Integer] between\n[0] and [" + (int)Math.pow(2,16)+"]", intValue -> (intValue > 0), intValue1 -> (intValue1 < Math.pow(2,16)));
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            int value = Integer.parseInt(node.getTextLine2());
            return fpFactory.createFingerprintFilterEthertype(value);
        }
    },
    FLAGS("Flags","This Filter will check for the presence of TCP flags. \n" +
            "This will force the TransportProtocol to only check TCP packets.",PacketType.TCP) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog("Filter Value","Enter a value for the filter\n(NS CWR ECE URG ACK PSH RST SYN FIN)","Value:");
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                validate(valueDialog, newValue);
            }));
            return valueDialog.showAndWait().orElse("");
        }
        private void validate(TextInputDialog valueDialog, String newValue) {
            if(newValue.replace(" ","").matches("((NS)|(CWR)|(ECE)|(URG)|(ACK)|(PSH)|(RST)|(SYN)|(FIN)){1,9}")) {
                setValid(valueDialog);
            }
            else {
                setInvalid(valueDialog, "Filter values are entered incorrectly\n(NS CWR ECE URG ACK PSH RST SYN FIN)");
            }
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            return fpFactory.createFingerprintFilterFlags(node.getTextLine2().replace(" ", ""));
        }
    },
    SEQ("Seq","Accepts TCP packets which contain a SEQ field equal to the indicated value. \n" +
            "This will force the TransportProtocol to only check TCP packets. ",PacketType.TCP) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleLongValidator(valueDialog, newValue, "Must be a [Long] value");
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            long value = Long.parseLong(node.getTextLine2());
            return fpFactory.createFingerprintFilterSeq(value);
        }
    },
    TRANSPORTPROTOCOL("TransportProtocol","Accepts the protocol number of a packet by the assigned Internet Protocol Numbers. \n" +
            "GM only supports IPv4, UDP and TCP protocols. It is not suggested to use values other than TCP(6) and UDP(17). ",PacketType.OTHER) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog("Filter Value","Enter value TCP:[6] or UDP:[17], other values are unlikely to match","Value:");
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                if(simpleIntegerValidator(valueDialog, newValue, "Must be an [Integer] between\n[0] and [255]", intValue -> (intValue >= 0), intValue1 -> (intValue1 <= 255))){
                    int value = Integer.parseInt(newValue);
                    if(value != 6 && value != 17) {
                        setWarning(valueDialog,"It is suggested that only values TCP:[6] or UDP:[17] are used");
                    }
                    else {
                        setValid(valueDialog);
                    }
                }
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            short value = Short.parseShort(node.getTextLine2());
            return fpFactory.createFingerprintFilterTransportProtocol(value);
        }
    },
    TTL("TTL", "Accepts TCP packets which contain a TTL field equal to a value", PacketType.ANY) {
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleLongValidator(valueDialog, newValue, "Must be a [Integer] value");
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            return fpFactory.createFingerprintFilterTTL(new BigInteger(node.getTextLine2()));
        }
    },
    TTLWITHIN("TTLWithin","Accepts TCP packets which contain a TTL field within a range of value(s)",PacketType.ANY) {
        @Override
        public String getValueFromUser() {
            PairTextInputDialog valueDialog = new PairTextInputDialog();
            valueDialog.setTitle("Filter Value");
            valueDialog.setHeaderText("Enter a value for the filter");
            valueDialog.setContentText("Value:");
            //start invalid
            valueDialog.getEditors().getKey().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
            valueDialog.getEditors().getValue().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
            valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

            valueDialog.getEditors().getKey().textProperty().addListener(((observable, oldValue, newValue) -> {
                validateMultiple(valueDialog);
            }));
            valueDialog.getEditors().getValue().textProperty().addListener(((observable, oldValue, newValue) -> {
                validateMultiple(valueDialog);
            }));
            StringBuilder returnBuilder = new StringBuilder();
            valueDialog.showAndWait().ifPresent(values -> {
                returnBuilder.append(values.getKey()).append(" - ").append(values.getValue());
            });
            return returnBuilder.toString();
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            String text = node.getTextLine2();
            String[] splitText = text.split(" - ");
            if(splitText.length != 2) {
                throw new Exception("Invalid range format: ["+text+"]");
            }
            Fingerprint.Filter.TTLWithin withinRange = fpFactory.createFingerprintFilterTTLWithin();
            withinRange.setMax(new BigInteger(splitText[1]));
            withinRange.setMin(new BigInteger(splitText[0]));
            return fpFactory.createFingerprintFilterTTLWithin(withinRange);
        }
    },
    WINDOW("Window","Accepts a TCP packet which contains a Window Size field equal to the indicated value. \n" +
            "This will force the TransportProtocol to only check TCP packets.",PacketType.TCP){
        @Override
        public String getValueFromUser() {
            TextInputDialog valueDialog = createStandardTextInputDialog();
            valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                simpleIntegerValidator(valueDialog, newValue, "Must be an [Integer] between\n[0] and [255]", intValue -> (intValue >= 0), intValue1 -> (intValue1 <= 255));
            }));
            return valueDialog.showAndWait().orElse("");
        }
        @Override
        public JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception {
            short value = Short.parseShort(node.getTextLine2());
            return fpFactory.createFingerprintFilterWindow(value);
        }
    };

    /** The name of this filter type */
    private final String name;
    /** reference to the button that the user presses to create this filter type in the FpPalen */
    private final Button button = new Button();
    /** The header string, saved incase it was changed due to validation error */
    private String savedHeaderString = "";
    /** the tool tip while mousing over the button */
    private final String toolTip;
    /** the type of packet this filter can apply to */
    private final PacketType type;
    /** the packet types available */
    public enum PacketType {TCP,ANY,OTHER}

    /**
     * Creates a new instance of FinderPrintFilterType
     * @param name
     * @param toolTip
     * @param type
     */
    FingerprintFilterType(String name, String toolTip, PacketType type) {
        this.name = name;
        this.type = type;
        this.toolTip = toolTip;
        this.button.setText(name);
        this.button.setMaxWidth(Double.MAX_VALUE);

    }

    /**
     * Validator for integers
     */
    @FunctionalInterface
    private interface IntegerValidator {
        boolean validateInteger(int intValue);
    }

    /**
     * Validator for longs
     */
    @FunctionalInterface
    private interface LongValidator {
        boolean validateLong(long longValue);
    }

    /**
     * Returns the button that generates this filtertype in the FpPanel
     * Calls getValueFromUser() which is seperately implemented  for each enum const that pops up a dialog to capture
     * values from the user.  The consumer creates a generic graphic element this this information and puts it into
     * the active tabs FP panel.
     * @param consumer
     * @return
     */
    public Button getButton(Controller consumer) {
        this.button.setOnAction(event -> {
            String value = getValueFromUser();
            if(!value.isEmpty()){
                consumer.addFilterToActiveTab(this.name, value);
            }
        });
        return this.button;
    }

    /**
     * Sets the popup dialog to invaluid without changing the header text
     * @param valueDialog
     */
    protected void startInvalid(TextInputDialog valueDialog) {
        valueDialog.getEditor().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
    }

    /**
     * sets the simple dialog to invalid and modifies the header text
     * @param valueDialog
     * @param errorString
     */
    protected void setInvalid(TextInputDialog valueDialog, String errorString) {
        if(this.savedHeaderString.isEmpty()) {
            this.savedHeaderString = valueDialog.getHeaderText();
        }
        valueDialog.setHeaderText(errorString);
        valueDialog.getEditor().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
    }

    /**
     * Sets a warning status that's save-able but likely to not work as expected
     * @param valueDialog
     * @param warningString
     */
    protected void setWarning(TextInputDialog valueDialog, String warningString) {
        if(this.savedHeaderString.isEmpty()) {
            this.savedHeaderString = valueDialog.getHeaderText();
        }
        valueDialog.setHeaderText(warningString);
        valueDialog.getEditor().setStyle("-fx-text-box-border: yellow; -fx-focus-color: yellow;");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
    }

    /**
     * Sets all fields as valid and makes the dialog submit-able
     * @param valueDialog
     */
    protected void setValid(TextInputDialog valueDialog) {
        if(!this.savedHeaderString.isEmpty()) {
            valueDialog.setHeaderText(this.savedHeaderString);
        }
        valueDialog.getEditor().setStyle("");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
    }

    /**
     * validates an integer based on integer validators
     * @param valueDialog
     * @param newValue
     * @param errorString
     * @param integerValidators
     * @return
     */
    protected boolean simpleIntegerValidator(TextInputDialog valueDialog, String newValue, String errorString, IntegerValidator... integerValidators) {
        try {
            int intValue = Integer.parseInt(newValue);
            if (!Arrays.asList(integerValidators).stream().allMatch(validator -> validator.validateInteger(intValue))) {
                throw new NumberFormatException();
            }
            setValid(valueDialog);
            return true;
        } catch (NumberFormatException e) {
            setInvalid(valueDialog, errorString);
            return false;
        }
    }

    /**
     * validates a long based on long validators
     * @param valueDialog
     * @param newValue
     * @param errorString
     * @param longValidators
     * @return
     */
    protected boolean simpleLongValidator(TextInputDialog valueDialog, String newValue, String errorString, LongValidator... longValidators) {
        try {
            long longValue = Long.parseLong(newValue);
            if (!Arrays.asList(longValidators).stream().allMatch(validator -> validator.validateLong(longValue))) {
                throw new NumberFormatException();
            }
            setValid(valueDialog);
            return true;
        } catch (NumberFormatException e) {
            setInvalid(valueDialog, errorString);
            return false;
        }
    }

    /**
     * VaLIDATES A COMPLEX DIALOG WITH MULTIPLE FIELDS
     * @param valueDialog
     */
    protected void validateMultiple(PairTextInputDialog valueDialog) {
        Pair<TextField,TextField> fields = valueDialog.getEditors();
        boolean bothOk = true;
        int first = 0,second = 0;
        try {
            first = Integer.parseInt(fields.getKey().getText());
            fields.getKey().setStyle("");
        } catch (NumberFormatException e) {
            bothOk = false;
            fields.getKey().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        }

        try {
            second = Integer.parseInt(fields.getValue().getText());
            fields.getValue().setStyle("");
        } catch (NumberFormatException e) {
            bothOk = false;
            fields.getValue().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        }

        if(bothOk) {
            if(first <= second){
                valueDialog.setHeaderText("Enter a value for the filter");
                fields.getKey().setStyle("");
                fields.getValue().setStyle("");
                valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);

            }
            else {
                valueDialog.setHeaderText("First value must be less than or equal to second");
                fields.getKey().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
                fields.getValue().setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
                valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
            }
        }
        else {
            valueDialog.setHeaderText("Values must be integers");
            valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
        }
    }

    /**
     * Creates a text input dialog with standard title and header (used most of the time)
     * @return
     */
    protected TextInputDialog createStandardTextInputDialog() {
        return this.createStandardTextInputDialog("Filter Value","Enter a value for the filter","Value:");
    }

    /**
     * creates a standard text dialog with custom title header and content text
     *
     * @param title
     * @param header
     * @param content
     * @return
     */
    protected TextInputDialog createStandardTextInputDialog(String title, String header, String content) {
        TextInputDialog valueDialog = new TextInputDialog();
        valueDialog.setTitle(title);
        valueDialog.setHeaderText(header);
        valueDialog.setContentText(content);
        startInvalid(valueDialog);
        return valueDialog;
    }

    /**
     * implemented by each enuim const, gets the value from the user by popping up a dialog
     * @return
     */
    public abstract String getValueFromUser();

    /**
     * returns the tool tip for this button
     * @return
     */
    public String getToolTip() {
        return toolTip;
    }

    /**
     * returns the packet type
     * @return
     */
    public PacketType getType() {
        return type;
    }

    /**
     * implemented by each enum const, used while generating the xml
     * @param node
     * @param fpFactory
     * @return
     * @throws Exception
     */
    public abstract JAXBElement<?> getJaxBElement(RoundedTextNode node, ObjectFactory fpFactory) throws Exception;

    /**
     * returns the name of the button
     * @return
     */
    public String getName() {
        return name;
    }
}
