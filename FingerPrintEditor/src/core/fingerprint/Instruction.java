package core.fingerprint;

import core.fingerprint3.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Each instruction allowed in the payload of the fingerprint
 *
 * 07.21.2015 - CC - New...
 */
public enum Instruction {
    ANDTHEN("AndThen"){
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception{
            //this is a special case because its the root node
            AndThen andThen = new AndThen();
            if(payloadElement instanceof ByteTestFunction) {
                ((ByteTestFunction) payloadElement).setAndThen(andThen);
            }
            else if(payloadElement instanceof MatchFunction) {
                ((MatchFunction) payloadElement).setAndThen(andThen);
            }
            else if(payloadElement instanceof IsDataAtFunction) {
                ((IsDataAtFunction) payloadElement).setAndThen(andThen);
            }
            else if(payloadElement instanceof ByteJumpFunction) {
                ((ByteJumpFunction) payloadElement).setAndThen(andThen);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (ByteTestFunction or MatchFunction, or IsDataAtFunction or ByteJumpFunction)"};
                throw new Exception(error[0]);
            }
            return andThen;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof AndThen) {
                TreeItem<XmlElementTreeItem> andThenTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(andThenTreeItem);

                ((AndThen)xmlElement).getMatchOrByteTestOrIsDataAt().stream()
                        .forEach(operation -> Instruction.getOperationsList().stream()
                                .forEach(instruction -> instruction.loadFromXml(operation, andThenTreeItem)));
            }
        }
    },
    PAYLOAD("Payload") {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception{
            //this is a special case because its the root node
            if(payloadElement instanceof Fingerprint.Payload) {
                String id = treeItem.getValue().getAttributeValue(treeItem.getValue().getAttributes().get(0));
                ((Fingerprint.Payload)payloadElement).setFor(id);
                return payloadElement;
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload)"};
                throw new Exception(error[0]);
            }
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof Fingerprint.Payload) {
                Instruction.ALWAYS.loadFromXml(((Fingerprint.Payload) xmlElement).getAlways(), parentTreeItem);

                ((Fingerprint.Payload)xmlElement).getOperation().stream()
                        .forEach(operation -> Instruction.getOperationsList().stream()
                                .forEach(instruction -> instruction.loadFromXml(operation, parentTreeItem)));
            }
        }
    },
    DESCRIPTION("Description","Enter a Description for this Payload", new XmlElementAttribute("Description",Instruction::isNotEmpty)) {
        @Override
        public EventHandler<ActionEvent> getAction(TreeCell<XmlElementTreeItem> consumer) {
            return event -> {
                //description is unique, check for other Descriptions
                if(instructionExists(consumer)) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error adding element");
                    alert.setHeaderText("Cannot add more than one Description");
                    alert.showAndWait();
                }
                else {
                    XmlElementTreeItem value = collectInformationFromUser();
                    if(value!=null) {
                        TreeItem<XmlElementTreeItem> newItem = new TreeItem<>(value);
                        consumer.getTreeItem().getChildren().add(0, newItem);
                    }
                }

            };
        }
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            if(payloadElement instanceof Fingerprint.Payload) {
                String description = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Description"));
                ((Fingerprint.Payload)payloadElement).setDescription(description);
                return payloadElement;
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload)"};
                throw new Exception(error[0]);
            }
        }
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {

        }
    },
    ALWAYS("Always") {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            if(payloadElement instanceof Fingerprint.Payload) {
                Fingerprint.Payload.Always always = fpObjectFactory.createFingerprintPayloadAlways();
                ((Fingerprint.Payload)payloadElement).setAlways(always);
                return always;
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload)"};
                throw new Exception(error[0]);
            }
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof Fingerprint.Payload.Always) {
                TreeItem<XmlElementTreeItem> alwaysTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(alwaysTreeItem);
                ((Fingerprint.Payload.Always) xmlElement).getReturn().stream()
                        .forEach(returnElement -> Instruction.RETURN.loadFromXml(returnElement, alwaysTreeItem));
            }
        }
    },
    RETURN("Return", "Select Confidence level for this Return instruction",
            new XmlElementAttribute("Confidence",Arrays.asList("5", "4", "3", "2", "1")),
            new XmlElementAttribute("Direction",Arrays.asList("SOURCE", "DESTINATION"))) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            Return returnElement = fpObjectFactory.createReturn();
            returnElement.setConfidence(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Confidence"))));
            returnElement.setDirection(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Direction")));
            if(payloadElement instanceof Fingerprint.Payload) {
                ((Fingerprint.Payload)payloadElement).getOperation().add(returnElement);
            }
            else if(payloadElement instanceof Fingerprint.Payload.Always) {
                ((Fingerprint.Payload.Always)payloadElement).getReturn().add(returnElement);
            }
            else if(payloadElement instanceof AndThen) {
                ((AndThen)payloadElement).getMatchOrByteTestOrIsDataAt().add(returnElement);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload or Fingerprint.Payload.Always or AndThen)"};
                throw new Exception(error[0]);
            }
            return returnElement;

        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof Return) {
                TreeItem<XmlElementTreeItem> returnTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(returnTreeItem);
                returnTreeItem.getValue().addAttribute(getNameAttributeMap().get("Direction"), ((Return) xmlElement).getDirection());
                returnTreeItem.getValue().addAttribute(getNameAttributeMap().get("Confidence"), ((Return) xmlElement).getConfidence() + "");
                ((Return)xmlElement).getExtract().stream().forEach(extract -> Instruction.EXTRACT.loadFromXml(extract, returnTreeItem));
                Instruction.DETAILS.loadFromXml(((Return) xmlElement).getDetails(), returnTreeItem);
            }
        }
    },
    DETAILS("Details") {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            if(payloadElement instanceof Return) {
                DetailGroup detailGroup = fpObjectFactory.createDetailGroup();
                ((Return)payloadElement).setDetails(detailGroup);
                return detailGroup;
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Return)"};
                throw new Exception(error[0]);
            }
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof DetailGroup) {
                TreeItem<XmlElementTreeItem> detailsTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(detailsTreeItem);
                ((DetailGroup)xmlElement).getDetail().stream().forEach(detail -> Instruction.DETAIL.loadFromXml(detail, detailsTreeItem));
            }
        }
    },
    CATEGORY("Category","Select a category to apply",
            new XmlElementAttribute("Category",XmlEnumeration.CATEGORIES)) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            String returnElement = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Category"));
            if(payloadElement instanceof DetailGroup) {
                ((DetailGroup)payloadElement).setCategory(returnElement);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (DetailGroup)"};
                throw new Exception(error[0]);
            }
            return returnElement;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            TreeItem<XmlElementTreeItem> newLeaf = new TreeItem<>(new XmlElementTreeItem(this));
            parentTreeItem.getChildren().add(newLeaf);
            newLeaf.getValue().addAttribute(getNameAttributeMap().get("Category"), xmlElement.toString());
        }
    },
    ROLE("Role", "Select a Role to apply",
            new XmlElementAttribute("Role",XmlEnumeration.ROLES)) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            String returnElement = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Role"));
            if(payloadElement instanceof DetailGroup) {
                ((DetailGroup)payloadElement).setRole(returnElement);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (DetailGroup)"};
                throw new Exception(error[0]);
            }
            return returnElement;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            TreeItem<XmlElementTreeItem> newLeaf = new TreeItem<>(new XmlElementTreeItem(this));
            parentTreeItem.getChildren().add(newLeaf);
            newLeaf.getValue().addAttribute(getNameAttributeMap().get("Role"), xmlElement.toString());
        }
    },
    DETAIL("Detail", "Enter the Detail Name and value you want to add for this payload",
            new XmlElementAttribute("Detail", Instruction::isNotEmpty),
            new XmlElementAttribute("Value", Instruction::isNotEmpty) ) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            DetailGroup.Detail detail = new DetailGroup.Detail();
            detail.setName(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Detail")));
            detail.setValue(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Value")));
            if(payloadElement instanceof DetailGroup) {
                ((DetailGroup)payloadElement).getDetail().add(detail);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (DetailGroup)"};
                throw new Exception(error[0]);
            }
            return detail;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof DetailGroup.Detail) {
                TreeItem<XmlElementTreeItem> detailTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(detailTreeItem);
                detailTreeItem.getValue().addAttribute(getNameAttributeMap().get("Detail"), ((DetailGroup.Detail) xmlElement).getName());
                detailTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((DetailGroup.Detail) xmlElement).getValue());
            }
        }
    },//intValue -> (intValue > 0), intValue1 -> (intValue1 < Math.pow(2, 16)
    EXTRACT("Extract", "Enter information for Extraction Instruction",
            new XmlElementAttribute("Name","Name", Instruction::isNotEmpty),
            new XmlElementAttribute("Endian",Arrays.asList("LITTLE", "BIG")),
            new XmlElementAttribute("From","From",XmlEnumeration.POSITIONS, Instruction::isNotEmpty, Instruction::isPositionOrInt),
            new XmlElementAttribute("To","To",XmlEnumeration.POSITIONS, Instruction::isNotEmpty, Instruction::isPositionOrInt),
            new XmlElementAttribute("Max Length", "Max Length", Instruction::isNotEmpty, Instruction::isUnsignedShort)) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            Extract extract = new Extract();
            extract.setName(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Name")));
            extract.setFrom(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("From")));
            extract.setTo(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("To")));
            extract.setMaxLength(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Max Length"))));
            extract.setEndian(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Endian")));
            if(payloadElement instanceof Return) {
                ((Return)payloadElement).getExtract().add(extract);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Return)"};
                throw new Exception(error[0]);
            }
            return extract;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof Extract) {
                TreeItem<XmlElementTreeItem> extractTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(extractTreeItem);
                extractTreeItem.getValue().addAttribute(getNameAttributeMap().get("Name"), ((Extract) xmlElement).getName());
                extractTreeItem.getValue().addAttribute(getNameAttributeMap().get("Endian"), ((Extract) xmlElement).getEndian());
                extractTreeItem.getValue().addAttribute(getNameAttributeMap().get("From"), ((Extract) xmlElement).getFrom());
                extractTreeItem.getValue().addAttribute(getNameAttributeMap().get("To"), ((Extract) xmlElement).getTo());
                extractTreeItem.getValue().addAttribute(getNameAttributeMap().get("Max Length"), ((Extract) xmlElement).getMaxLength()+"");
                Instruction.POST.loadFromXml(((Extract) xmlElement).getPost(), extractTreeItem);
            }
        }
    },
    POST("Post", "Select how you want to post process the data",
            new XmlElementAttribute("Convert",Arrays.asList("NONE", "HEX", "INTEGER", "RAW_BYTES", "STRING")),
            new XmlElementAttribute("Lookup",Arrays.asList("NONE", "BACNET", "ENIPVENDOR", "ENIPDEVICE"))) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            Post post = new Post();
            String convert = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Convert"));
            String lookup = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Lookup"));
            if(!convert.equals("NONE")) {
                post.setConvert(ContentType.valueOf(convert));
            }
            if(!lookup.equals("NONE")){
                post.setLookup(lookup);
            }
            if( payloadElement instanceof Extract) {
                ((Extract)payloadElement).setPost(post);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Extract)"};
                throw new Exception(error[0]);
            }
            return post;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof Post) {
                TreeItem<XmlElementTreeItem> postTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(postTreeItem);
                if(((Post) xmlElement).getConvert() != null) {
                    postTreeItem.getValue().addAttribute(getNameAttributeMap().get("Convert"), ((Post) xmlElement).getConvert().name());
                }
                if(((Post) xmlElement).getLookup() != null) {
                    postTreeItem.getValue().addAttribute(getNameAttributeMap().get("Lookup"), ((Post) xmlElement).getLookup());
                }
            }
        }
    },
    MATCH("Match", "Select parameters for matching",
            new XmlElementAttribute("Offset","Integer from -65535 to 65535", Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
           // new XmlElementAttribute("Reverse",Arrays.asList("TRUE", "FALSE")),
            new XmlElementAttribute("NoCase",Arrays.asList("FALSE", "TRUE")),
            new XmlElementAttribute("Depth","Values 0 to 255", Instruction::isNotEmpty, Instruction::isUnsignedShort),
            new XmlElementAttribute("Relative",Arrays.asList("TRUE", "FALSE")),
            new XmlElementAttribute("Within","Values 0 to 255", Instruction::isNotEmpty, Instruction::isUnsignedShort),
            new XmlElementAttribute("MoveCursors",Arrays.asList("TRUE", "FALSE"))) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            MatchFunction match = new MatchFunction();
            match.setOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Offset"))));
           // match.setReverse(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Reverse"))));
            match.setNoCase(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("NoCase"))));
            match.setDepth(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Depth"))));
            match.setRelative(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Relative"))));
            match.setWithin(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Within"))));
            match.setMoveCursors(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("MoveCursors"))));
            if(payloadElement instanceof Fingerprint.Payload) {
                ((Fingerprint.Payload)payloadElement).getOperation().add(match);
            }
            else if(payloadElement instanceof AndThen) {
                ((AndThen)payloadElement).getMatchOrByteTestOrIsDataAt().add(match);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload OR AndThen)"};
                throw new Exception(error[0]);
            }
            return match;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof MatchFunction) {
                TreeItem<XmlElementTreeItem> matchTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(matchTreeItem);
                matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("Offset"), ((MatchFunction) xmlElement).getOffset() + "");
                //matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("Reverse"), ((MatchFunction) xmlElement).isReverse() + "");
                matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("NoCase"), ((MatchFunction) xmlElement).isNoCase()+"");
                matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("Depth"), ((MatchFunction) xmlElement).getDepth() + "");
                matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("Relative"), ((MatchFunction) xmlElement).isRelative() + "");
                matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("Within"), ((MatchFunction) xmlElement).getWithin() + "");
                matchTreeItem.getValue().addAttribute(getNameAttributeMap().get("MoveCursors"), ((MatchFunction) xmlElement).isMoveCursors() + "");
                Instruction.ANDTHEN.loadFromXml(((MatchFunction) xmlElement).getAndThen(), matchTreeItem);
                Instruction.CONTENT.loadFromXml(((MatchFunction) xmlElement).getContent(), matchTreeItem);
                if(((MatchFunction) xmlElement).getPattern() != null) {
                    Instruction.PATTERN.loadFromXml(((MatchFunction) xmlElement).getPattern(), matchTreeItem);
                }
            }
        }
    },
    PATTERN("Pattern","Enter Regex Pattern to match", new XmlElementAttribute("Pattern",Instruction::isNotEmpty)) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            String pattern = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Pattern"));
            if(payloadElement instanceof MatchFunction ) {
                ((MatchFunction)payloadElement).setPattern(pattern);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (MatchFunction)"};
                throw new Exception(error[0]);
            }
            return pattern;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            TreeItem<XmlElementTreeItem> newLeaf = new TreeItem<>(new XmlElementTreeItem(this));
            parentTreeItem.getChildren().add(newLeaf);
            newLeaf.getValue().addAttribute(getNameAttributeMap().get("Pattern"), ((String)xmlElement).toString());
        }
    },
    CONTENT("Content","Select the content that will match",
            new XmlElementAttribute("Type",Arrays.asList("HEX", "INTEGER", "RAW_BYTES", "STRING")),
            new XmlElementAttribute("Value","Value",Instruction::isNotEmpty)){
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            MatchFunction.Content content = new MatchFunction.Content();
            content.setType(ContentType.valueOf(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Type"))));
            content.setValue(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Value")));
            if(payloadElement instanceof MatchFunction ) {
                ((MatchFunction)payloadElement).setContent(content);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (MatchFunction)"};
                throw new Exception(error[0]);
            }
            return content;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof MatchFunction.Content) {
                TreeItem<XmlElementTreeItem> contentTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(contentTreeItem);
                contentTreeItem.getValue().addAttribute(getNameAttributeMap().get("Type"), ((MatchFunction.Content) xmlElement).getType().name());
                contentTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((MatchFunction.Content) xmlElement).getValue());
            }
        }
    },
    BYTETEST("ByteTest","Select parameters for testing",
            new XmlElementAttribute("Offset","Integer from -65535 to 65535",Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
            new XmlElementAttribute("PostOffset","Integer from -65535 to 65535",Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
            new XmlElementAttribute("Relative",Arrays.asList("TRUE", "FALSE")),
            new XmlElementAttribute("Bytes","1 through 10",Instruction::isNotEmpty, value -> Instruction.isInRange(value,1, 10)),
            new XmlElementAttribute("Endian",Arrays.asList("BIG", "LITTLE"))){
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            ByteTestFunction byteTest = new ByteTestFunction();
            byteTest.setOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Offset"))));
            byteTest.setPostOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("PostOffset"))));
            byteTest.setRelative(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Relative"))));
            byteTest.setBytes(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Bytes"))));
            byteTest.setEndian(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Endian")));
            if(payloadElement instanceof Fingerprint.Payload) {
                ((Fingerprint.Payload)payloadElement).getOperation().add(byteTest);
            }
            else if(payloadElement instanceof AndThen) {
                ((AndThen)payloadElement).getMatchOrByteTestOrIsDataAt().add(byteTest);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload OR AndThen)"};
                throw new Exception(error[0]);
            }
            return byteTest;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof ByteTestFunction) {
                TreeItem<XmlElementTreeItem> byteTestTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(byteTestTreeItem);
                byteTestTreeItem.getValue().addAttribute(getNameAttributeMap().get("Offset"), ((ByteTestFunction) xmlElement).getOffset() + "");
                byteTestTreeItem.getValue().addAttribute(getNameAttributeMap().get("PostOffset"), ((ByteTestFunction) xmlElement).getPostOffset() + "");
                byteTestTreeItem.getValue().addAttribute(getNameAttributeMap().get("Relative"), ((ByteTestFunction) xmlElement).isRelative() + "");
                byteTestTreeItem.getValue().addAttribute(getNameAttributeMap().get("Bytes"), ((ByteTestFunction) xmlElement).getBytes() + "");
                byteTestTreeItem.getValue().addAttribute(getNameAttributeMap().get("Endian"), ((ByteTestFunction) xmlElement).getEndian() + "");

                Instruction.ANDTHEN.loadFromXml(((ByteTestFunction) xmlElement).getAndThen(), byteTestTreeItem);
                Instruction.BYTECOMPARATOR.loadFromXml(xmlElement,byteTestTreeItem);
            }
        }
    },
    BYTECOMPARATOR("ByteComparator","Select Comparator and value to compare",
            new XmlElementAttribute("Comparator",Arrays.asList("GT","LT","GTE", "LTE", "EQ", "AND", "OR")),
            new XmlElementAttribute("Value","Value to compare",Instruction::isNotEmpty)) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            String comparator = treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Comparator"));
            if(payloadElement instanceof ByteTestFunction) {
                Arrays.asList(((ByteTestFunction)payloadElement).getClass().getMethods()).stream()
                        .filter(methodToFilter -> (methodToFilter.getName().startsWith("set") && methodToFilter.getName().endsWith(comparator))).forEach(method -> {
                    try {
                        method.invoke(payloadElement, treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Value")));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
            }
            return payloadElement;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof ByteTestFunction) {
                TreeItem<XmlElementTreeItem> byteTestComparatorTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(byteTestComparatorTreeItem);
                if(((ByteTestFunction) xmlElement).getGT() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "GT");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getGT().toString());
                }
                else if(((ByteTestFunction) xmlElement).getLT() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "LT");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getLT().toString());
                }
                else if(((ByteTestFunction) xmlElement).getGTE() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "GTE");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getGTE().toString());
                }
                else if(((ByteTestFunction) xmlElement).getLTE() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "LTE");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getLTE().toString());
                }
                else if(((ByteTestFunction) xmlElement).getEQ() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "EQ");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getEQ().toString());
                }
                else if(((ByteTestFunction) xmlElement).getAND() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "AND");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getAND().toString());
                }
                else if(((ByteTestFunction) xmlElement).getOR() != null) {
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Comparator"), "OR");
                    byteTestComparatorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Value"), ((ByteTestFunction) xmlElement).getOR().toString());
                }

            }
        }
    },
    ISDATAAT("IsDataAt","Select parameters for testing",
            new XmlElementAttribute("Offset","Integer from -65535 to 65535",Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
            new XmlElementAttribute("Relative",Arrays.asList("FALSE", "TRUE"))) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            IsDataAtFunction isDataAt = new IsDataAtFunction();
            isDataAt.setOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Offset"))));
            isDataAt.setRelative(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Relative"))));
            if(payloadElement instanceof Fingerprint.Payload) {
                ((Fingerprint.Payload)payloadElement).getOperation().add(isDataAt);
            }
            else if(payloadElement instanceof AndThen) {
                ((AndThen)payloadElement).getMatchOrByteTestOrIsDataAt().add(isDataAt);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload OR AndThen)"};
                throw new Exception(error[0]);
            }
            return isDataAt;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof IsDataAtFunction) {
                TreeItem<XmlElementTreeItem> isDataAtTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(isDataAtTreeItem);
                isDataAtTreeItem.getValue().addAttribute(getNameAttributeMap().get("Offset"), ((MatchFunction) xmlElement).getOffset() + "");
                isDataAtTreeItem.getValue().addAttribute(getNameAttributeMap().get("Relative"), ((MatchFunction) xmlElement).isRelative() + "");

                Instruction.ANDTHEN.loadFromXml(((IsDataAtFunction) xmlElement).getAndThen(), isDataAtTreeItem);
            }
        }
    },
    BYTEJUMP("ByteJump","Select parameters for testing",
            new XmlElementAttribute("Offset","Integer from -65535 to 65535",Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
            new XmlElementAttribute("PostOffset","Integer from -65535 to 65535",Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
            new XmlElementAttribute("Relative",Arrays.asList("TRUE", "FALSE")),
            new XmlElementAttribute("Bytes","1 through 10",Instruction::isNotEmpty, value -> Instruction.isInRange(value, 1, 10)),
            new XmlElementAttribute("Endian",Arrays.asList("BIG", "LITTLE"))){
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            ByteJumpFunction byteJump = new ByteJumpFunction();
            byteJump.setOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Offset"))));
            byteJump.setPostOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("PostOffset"))));
            byteJump.setRelative(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Relative"))));
            byteJump.setBytes(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Bytes"))));
            byteJump.setEndian(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Endian")));
            if(payloadElement instanceof Fingerprint.Payload) {
                ((Fingerprint.Payload)payloadElement).getOperation().add(byteJump);
            }
            else if(payloadElement instanceof AndThen) {
                ((AndThen)payloadElement).getMatchOrByteTestOrIsDataAt().add(byteJump);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload OR AndThen)"};
                throw new Exception(error[0]);
            }
            return byteJump;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof ByteJumpFunction) {
                TreeItem<XmlElementTreeItem> byteJumpTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(byteJumpTreeItem);
                byteJumpTreeItem.getValue().addAttribute(getNameAttributeMap().get("Offset"), ((ByteJumpFunction) xmlElement).getOffset() + "");
                byteJumpTreeItem.getValue().addAttribute(getNameAttributeMap().get("PostOffset"), ((ByteJumpFunction) xmlElement).getPostOffset() + "");
                byteJumpTreeItem.getValue().addAttribute(getNameAttributeMap().get("Relative"), ((ByteJumpFunction) xmlElement).isRelative() + "");
                byteJumpTreeItem.getValue().addAttribute(getNameAttributeMap().get("Bytes"), ((ByteJumpFunction) xmlElement).getBytes() + "");
                byteJumpTreeItem.getValue().addAttribute(getNameAttributeMap().get("Endian"), ((ByteJumpFunction) xmlElement).getEndian());

                Instruction.ANDTHEN.loadFromXml(((ByteJumpFunction) xmlElement).getAndThen(), byteJumpTreeItem);
            }
        }
    },
    CALC("Calc","Enter calculation to do, use x to represent the byte\nEXAMPLE: 3*x+x/13", new XmlElementAttribute("Calculation",value -> value.replaceAll(" ", "").matches("((\\d+)|x)([-+/*]((\\d+)|x))*"))) {
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            if(payloadElement instanceof ByteJumpFunction) {
                ((ByteJumpFunction) payloadElement).setCalc(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Calculation")));
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (ByteJumpFunction)"};
                throw new Exception(error[0]);
            }
            return payloadElement;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            TreeItem<XmlElementTreeItem> newLeaf = new TreeItem<>(new XmlElementTreeItem(this));
            parentTreeItem.getChildren().add(newLeaf);
            newLeaf.getValue().addAttribute(getNameAttributeMap().get("Calculation"), ((String)xmlElement).toString());
        }
    },
    ANCHOR("Anchor","Select parameters for testing",
            new XmlElementAttribute("Offset","Integer from -65535 to 65535",Instruction::isNotEmpty, value -> Instruction.isInRange(value, -65535, 65535)),
            new XmlElementAttribute("Relative",Arrays.asList("FALSE", "TRUE")),
            new XmlElementAttribute("Cursor",Arrays.asList("START", "MAIN", "END")),
            new XmlElementAttribute("Position",XmlEnumeration.POSITIONS)){
        @Override
        public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
            Anchor anchor = new Anchor();
            anchor.setOffset(Integer.parseInt(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Offset"))));
            anchor.setRelative(Boolean.parseBoolean(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Relative"))));
            anchor.setCursor(Cursor.valueOf(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Cursor"))));
            anchor.setPosition(Position.valueOf(treeItem.getValue().getAttributeValue(getNameAttributeMap().get("Position"))));
            if(payloadElement instanceof Fingerprint.Payload) {
                ((Fingerprint.Payload)payloadElement).getOperation().add(anchor);
            }
            else if(payloadElement instanceof AndThen) {
                ((AndThen)payloadElement).getMatchOrByteTestOrIsDataAt().add(anchor);
            }
            else {
                String[] error = {"Object passed to setValueInXml not what was expected (Fingerprint.Payload OR AndThen)"};
                throw new Exception(error[0]);
            }
            return anchor;
        }
        @Override
        public void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem) {
            if(xmlElement instanceof Anchor) {
                TreeItem<XmlElementTreeItem> anchorTreeItem = new TreeItem<>(new XmlElementTreeItem(this));
                parentTreeItem.getChildren().add(anchorTreeItem);
                anchorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Offset"), ((Anchor) xmlElement).getOffset() + "");
                anchorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Relative"), ((Anchor) xmlElement).isRelative() + "");
                anchorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Cursor"), ((Anchor) xmlElement).getCursor().name());
                anchorTreeItem.getValue().addAttribute(getNameAttributeMap().get("Position"), ((Anchor) xmlElement).getPosition().name());
            }
        }
    };

    /** Text of the instruction */
    private String text;
    /** header text displayed int he dialog for creation */
    private String headerText;
    /** list of allowed attributes */
    private ArrayList<XmlElementAttribute> attributes = new ArrayList<>();
    /** list of parent elements for this node */
    private Collection<Instruction> possibleParents = new LinkedHashSet<>();
    /** Map af the attribute names to the XmlElementAttribute type */
    private HashMap<String, XmlElementAttribute> nameAttributeMap = new HashMap<>();
    /** a special static grouping of instructions */
    private static ArrayList<Instruction> operationsList = new ArrayList<>();
    /** for use in validation */
    private String savedHeaderString = "";

    //must do this to get around forward reference issue
    //Static blocks in ENUMS are called AFTER the enum has declared its const members
    static {
        ANDTHEN.setPossibleParents(BYTETEST,MATCH,ISDATAAT,BYTEJUMP);
        ANCHOR.setPossibleParents(PAYLOAD, ANDTHEN);
        CALC.setPossibleParents(BYTEJUMP);
        BYTEJUMP.setPossibleParents(PAYLOAD, ANDTHEN);
        ISDATAAT.setPossibleParents(PAYLOAD, ANDTHEN);
        BYTECOMPARATOR.setPossibleParents(BYTETEST);
        BYTETEST.setPossibleParents(PAYLOAD, ANDTHEN);
        CONTENT.setPossibleParents(MATCH);
        PATTERN.setPossibleParents(MATCH);
        MATCH.setPossibleParents(PAYLOAD, ANDTHEN);
        POST.setPossibleParents(EXTRACT);
        EXTRACT.setPossibleParents(RETURN);
        DETAIL.setPossibleParents(DETAILS);
        ROLE.setPossibleParents(DETAILS);
        CATEGORY.setPossibleParents(DETAILS);
        DETAILS.setPossibleParents(RETURN);
        RETURN.setPossibleParents(ALWAYS, PAYLOAD, ANDTHEN);
        ALWAYS.setPossibleParents(PAYLOAD);
        DESCRIPTION.setPossibleParents(PAYLOAD);

        Instruction.operationsList.add(Instruction.MATCH);
        Instruction.operationsList.add(Instruction.BYTETEST);
        Instruction.operationsList.add(Instruction.ISDATAAT);
        Instruction.operationsList.add(Instruction.BYTEJUMP);
        Instruction.operationsList.add(Instruction.ANCHOR);
        Instruction.operationsList.add(Instruction.RETURN);
    }

    /**
     * Creates a new instance of the instruction enum const
     *
     * @param text
     * @param headerText
     * @param attributes
     */
    Instruction(String text, String headerText, XmlElementAttribute ... attributes) {
        this.text = text;
        this.headerText = headerText;
        if(attributes.length > 0 ) {
            this.attributes.addAll(Arrays.asList(attributes));
            this.attributes.stream().forEach(attribute -> nameAttributeMap.put(attribute.getName(), attribute));
        }
    }

    /**
     * convenience contructor
     * @param text
     * @param attributes
     */
    Instruction(String text, XmlElementAttribute ... attributes) {
        this(text, null, attributes);
    }

    /**
     * returns teh text
     * @return
     */
    public String getText() {
        return this.text;
    }

    /**
     * Test to determine if the this instruction has the passed in element as a parent
     * @param element
     * @return
     */
    public boolean isInstructionForParent(XmlElementTreeItem element) {
        return possibleParents.stream().anyMatch(instruction -> instruction == element.getInstruction());
    }

    /**
     * determines if this instruction already exists
     * @param consumer
     * @return
     */
    protected boolean instructionExists(TreeCell<XmlElementTreeItem> consumer) {
        return consumer.getTreeItem().getChildren().stream().anyMatch(item -> item.getValue().getInstruction() == this);
    }

    /**
     * Sets the possible parent instructions
     * @param parentItems
     */
    public void setPossibleParents(Instruction ... parentItems) {
        possibleParents.addAll(Arrays.asList(parentItems));
    }

    /**
     * Creates a new choice box in the grid pane of the popup dialog
     * Conveniently sets the label and the choice box next to it
     * @param grid
     * @param label
     * @param startingCol
     * @param startingRow
     * @param selectFirst
     * @param choices
     * @return
     */
    protected ChoiceBox<String> createChoiceInGrid(GridPane grid, String label, int startingCol, int startingRow, boolean selectFirst, List<String> choices) {
        grid.add(new Label(label), startingCol, startingRow);
        ChoiceBox<String> choiceBox = new ChoiceBox<>();
        choices.stream().forEach(s -> choiceBox.getItems().add(s));
        if(selectFirst) {
            choiceBox.getSelectionModel().selectFirst();
        }
        grid.add(choiceBox, startingCol + 1, startingRow);
        return choiceBox;
    }

    /**
     * Creates a new ComboBox in the grid pane of the popup dialog
     * Conveniently sets the label and the ComboBox next to it
     * @param grid
     * @param label
     * @param prompt
     * @param startingCol
     * @param startingRow
     * @param choices
     * @return
     */
    protected ComboBox<String> creatComboBoxInGrid(GridPane grid, String label, String prompt, int startingCol, int startingRow, List<String> choices) {
        grid.add(new Label(label), startingCol, startingRow);
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPromptText(prompt);
        comboBox.setEditable(true);
        choices.stream().forEach(s -> comboBox.getItems().add(s));
        grid.add(comboBox, startingCol + 1, startingRow);
        return comboBox;
    }

    /**
     * Creates a new TextField in the grid pane of the popup dialog
     * Conveniently sets the label and the TextField next to it
     * @param grid
     * @param label
     * @param prompt
     * @param startingCol
     * @param startingRow
     * @return
     */
    protected  TextField createTextFieldInGrid(GridPane grid, String label, String prompt, int startingCol, int startingRow) {
        grid.add(new Label(label), startingCol, startingRow);
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        grid.add(textField, (startingCol + 1), startingRow);
        return textField;
    }

    /**
     * creates the standard dialog grid pane
     * @return
     */
    protected GridPane createStandardDialogGridPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        return grid;
    }

    /**
     * Process the results from a dialog when the return value is a hashmap
     * @param valueDialog
     * @return
     */
    protected XmlElementTreeItem processHashMapDialogResults(Dialog<HashMap<XmlElementAttribute, String>> valueDialog) {
        XmlElementTreeItem returnItem = new XmlElementTreeItem(this);
        boolean[] valueSet = {false};
        valueDialog.showAndWait().ifPresent(values -> {
            valueSet[0] = true;
            this.getAttributes().stream()
                    .forEach(attribute -> returnItem.addAttribute(attribute, values.get(attribute)));
        });
        if(valueSet[0]) {
            return returnItem;
        }
        else {
            return null;
        }
    }

    /**
     * processes a simple dialogs results
     * @param valueDialog
     * @return
     */
    protected XmlElementTreeItem processSimpleDialogResult(Dialog<String> valueDialog) {
        XmlElementTreeItem returnItem = new XmlElementTreeItem(this);
        boolean[] valueSet = {false};
        valueDialog.showAndWait().ifPresent(value -> {
            valueSet[0] = true;
            this.getAttributes().stream()
                    .forEach(attribute -> returnItem.addAttribute(attribute, value));
        });
        if(valueSet[0]) {
            return returnItem;
        }
        else {
            return null;
        }
    }

    /**
     * overridden by some of the enum consts,
     * gets the action if the create or edit actions are invoked
     * @param consumer
     * @return
     */
    public EventHandler<ActionEvent> getAction(TreeCell<XmlElementTreeItem> consumer) {
        return event -> {
            XmlElementTreeItem value = collectInformationFromUser();
            if(value!=null) {
                TreeItem<XmlElementTreeItem> newItem = new TreeItem<>(value);
                consumer.getTreeItem().getChildren().add(newItem);
            }
        };
    }

    /**
     * an incrementer for positioning dynamic number of elements ina  grid pane
     *
     */
    private class TwoColumnGridIncrementer {
        private int col = 2;
        private int row = -1;
        public Pair<Integer, Integer> getRowColPair() {
            if(col == 0) {
                col = 2;
            }
            else {
                col = 0;
                row++;
            }
            return new Pair<>(col,row);
        }
    }

    /**
     * sets the dialog invalid
     * @param valueDialog
     * @param errorString
     * @param invalidField
     */
    protected void setInvalid(Dialog valueDialog, String errorString, Control invalidField) {
        if(errorString != null && !errorString.isEmpty()) {
            if (this.savedHeaderString.isEmpty()) {
                this.savedHeaderString = valueDialog.getHeaderText();
            }
            valueDialog.setHeaderText(errorString);
        }
        //invalidField.setStyle("-fx-text-box-border: red; -fx-focus-color: red;");
        invalidField.setStyle("-fx-border-color: red; -fx-focus-color: red;");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
    }

    /**
     * Sets the dialog to warning level
     * @param valueDialog
     * @param warningString
     * @param invalidField
     */
    protected void setWarning(Dialog valueDialog, String warningString, Control invalidField) {
        if(warningString != null && !warningString.isEmpty()) {
            if (this.savedHeaderString.isEmpty()) {
                this.savedHeaderString = valueDialog.getHeaderText();
            }
            valueDialog.setHeaderText(warningString);
        }
        invalidField.setStyle("-fx-text-box-border: yellow; -fx-focus-color: yellow;");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
    }

    /**
     * Sets dialog as valid and eligible for submission
     * @param valueDialog
     * @param validField
     */
    protected void setValid(Dialog valueDialog, Control validField) {
        if(!this.savedHeaderString.isEmpty()) {
            valueDialog.setHeaderText(this.savedHeaderString);
        }
        validField.setStyle("");
        valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(false);
    }

    /**
     * Collects information from the user for a new node of this isntruction type
     * @return
     */
    public XmlElementTreeItem collectInformationFromUser() {
        return collectInformationFromUser(null);
    }

    /**
     * collects information from the user but this is an edit action so prepopulates the dialog with attribute values already set
     * @param treeItem
     * @return
     */
    public XmlElementTreeItem collectInformationFromUser(TreeItem<XmlElementTreeItem> treeItem) {
        if(this.attributes.isEmpty()) {
            //no attributes means no need to prompt the user for anything
            return new XmlElementTreeItem(this);
        }
        else if (this.attributes.size() == 1 && this.attributes.get(0).getOptions().isEmpty()) {
            //only 1 attriburte with no options means simple text dialog
            TextInputDialog valueDialog = new TextInputDialog();
            valueDialog.setTitle(this.getText());
            valueDialog.setHeaderText(this.headerText);
            valueDialog.setContentText(this.attributes.get(0).getName());
            if(treeItem!=null) {
                valueDialog.getEditor().setText(treeItem.getValue().getAttributeValue(this.attributes.get(0)));
            }
            if(this.attributes.get(0).getValidators().size() > 0) {
                valueDialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
                valueDialog.getEditor().textProperty().addListener(((observable, oldValue, newValue) -> {
                    if (!this.attributes.get(0).getValidators().stream().allMatch(attributeValidator -> attributeValidator.validate(newValue))) {
                        setInvalid(valueDialog, this.attributes.get(0).getName()+"is invalid",valueDialog.getEditor());
                    }
                    else {
                        setValid(valueDialog,valueDialog.getEditor());
                    }
                }));
            }
            return processSimpleDialogResult(valueDialog);
        }
        else {
            //create a fully dynamic dialog
            Dialog<HashMap<XmlElementAttribute, String>> valueDialog = new Dialog<>();
            valueDialog.setTitle(this.getText());
            valueDialog.setHeaderText(this.headerText);

            valueDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            TwoColumnGridIncrementer colRowIncrementer = new TwoColumnGridIncrementer();
            GridPane grid = createStandardDialogGridPane();
            HashMap<XmlElementAttribute, TextField> textFieldAttributeMap = new HashMap<>();
            HashMap<XmlElementAttribute, ChoiceBox<String>> choiceBoxAttributeMap = new HashMap<>();
            HashMap<XmlElementAttribute, ComboBox<String>> comboBoxAttributeMap = new HashMap<>();

            this.getAttributes().stream().forEach(attribute -> {
                Pair<Integer, Integer> colRowPair = colRowIncrementer.getRowColPair();
                if (attribute.getOptions().isEmpty()) {
                    //text field with no options
                    if(treeItem!=null) {
                        TextField textField = createTextFieldInGrid(grid, attribute.getName() + ":", attribute.getHelpText(), colRowPair.getKey(), colRowPair.getValue());
                        textField.setText(treeItem.getValue().getAttributeValue(attribute));
                        textFieldAttributeMap.put(attribute, textField);
                    } else {
                        textFieldAttributeMap.put(attribute, createTextFieldInGrid(grid, attribute.getName() + ":", attribute.getHelpText(), colRowPair.getKey(), colRowPair.getValue()));
                    }
                } else {
                    //choice box
                    if(attribute.getHelpText().isEmpty()){
                        if(treeItem!=null) {
                            ChoiceBox<String> choiceBox =  createChoiceInGrid(grid, attribute.getName() + ":", colRowPair.getKey(), colRowPair.getValue(), true, attribute.getOptions());
                            choiceBox.setValue(treeItem.getValue().getAttributeValue(attribute));
                            choiceBoxAttributeMap.put(attribute, choiceBox);
                        }
                        else {
                            choiceBoxAttributeMap.put(attribute, createChoiceInGrid(grid, attribute.getName() + ":", colRowPair.getKey(), colRowPair.getValue(), true, attribute.getOptions()));
                        }
                    }
                    else {
                        //new special case where we have a combo box
                        if(treeItem!=null) {
                            ComboBox<String> comboBox = creatComboBoxInGrid(grid, attribute.getName() + ":", attribute.getHelpText(), colRowPair.getKey(), colRowPair.getValue(), attribute.getOptions());
                            comboBox.setValue(treeItem.getValue().getAttributeValue(attribute));
                            comboBoxAttributeMap.put(attribute, comboBox);
                        }
                        else {
                            comboBoxAttributeMap.put(attribute, creatComboBoxInGrid(grid, attribute.getName() + ":", attribute.getHelpText(), colRowPair.getKey(), colRowPair.getValue(), attribute.getOptions()));
                        }
                    }
                }
            });
            //Add validators
            final boolean[] runValidatorsOnCreattion = {false};
            textFieldAttributeMap.keySet().stream().forEach(textAttribute -> {
                if(!textAttribute.getValidators().isEmpty()) {
                    runValidatorsOnCreattion[0] = true;
                    textFieldAttributeMap.get(textAttribute).textProperty().addListener(((observable, oldValue, newValue) -> {
                        if (validateAllFields(valueDialog, textFieldAttributeMap, comboBoxAttributeMap)) {
                            setValid(valueDialog,textFieldAttributeMap.get(textAttribute));
                        }
                    }));
                }
            });
            comboBoxAttributeMap.keySet().stream().forEach(comboAttribute -> {
                if(!comboAttribute.getValidators().isEmpty()) {
                    runValidatorsOnCreattion[0] = true;
                    comboBoxAttributeMap.get(comboAttribute).valueProperty().addListener(((observable, oldValue, newValue) -> {
                        if (validateAllFields(valueDialog, textFieldAttributeMap, comboBoxAttributeMap)) {
                            setValid(valueDialog, comboBoxAttributeMap.get(comboAttribute));
                        }
                    }));
                }
            });
            if(runValidatorsOnCreattion[0]) {
                validateAllFields(valueDialog, textFieldAttributeMap, comboBoxAttributeMap);
            }
            valueDialog.getDialogPane().setContent(grid);

            valueDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    HashMap<XmlElementAttribute, String> resultsMap = new HashMap<>();
                    textFieldAttributeMap.keySet().stream().forEach(attribute -> resultsMap.put(attribute, textFieldAttributeMap.get(attribute).getText()));
                    choiceBoxAttributeMap.keySet().stream().forEach(attribute -> resultsMap.put(attribute, choiceBoxAttributeMap.get(attribute).getValue()));
                    comboBoxAttributeMap.keySet().stream().forEach(attribute -> resultsMap.put(attribute, comboBoxAttributeMap.get(attribute).getValue()));
                    return resultsMap;
                }
                return null;
            });
            return processHashMapDialogResults(valueDialog);
        }

    }

    /**
     * validates all the fields int he dialog
     * @param valueDialog
     * @param textFieldAttributeMap
     * @param comboBoxAttributeMap
     * @return
     */
    private boolean validateAllFields(Dialog valueDialog, HashMap<XmlElementAttribute, TextField> textFieldAttributeMap, HashMap<XmlElementAttribute, ComboBox<String>> comboBoxAttributeMap) {
        final boolean[] isAllValid = {true};
        textFieldAttributeMap.keySet().stream().filter(textAttribute -> !textAttribute.getValidators().isEmpty()).forEach(textAttribute -> {
            if (!textAttribute.getValidators().stream().allMatch(attributeValidator -> attributeValidator.validate(textFieldAttributeMap.get(textAttribute).getText()))) {
                setInvalid(valueDialog, null, textFieldAttributeMap.get(textAttribute));
                isAllValid[0] = false;
            }
            else {
                textFieldAttributeMap.get(textAttribute).setStyle("");
            }
        });
        comboBoxAttributeMap.keySet().stream().filter(comboAttribute -> !comboAttribute.getValidators().isEmpty()).forEach(comboAttribute -> {
            if (!comboAttribute.getValidators().stream().allMatch(attributeValidator -> attributeValidator.validate(comboBoxAttributeMap.get(comboAttribute).getValue()))) {
                setInvalid(valueDialog, null, comboBoxAttributeMap.get(comboAttribute));
                isAllValid[0] = false;
            } else {
                comboBoxAttributeMap.get(comboAttribute).setStyle("");
            }
        });
        return isAllValid[0];
    }

    /**
     * checks for null or empty
     * @param value
     * @return
     */
    protected static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /**
     * Test to see if value is an int
     * @param value
     * @return
     */
    protected static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * tests to see if value is an int in a range
     * @param value
     * @param lowEnd
     * @param highEnd
     * @return
     */
    protected static boolean isInRange(String value, int lowEnd, int highEnd) {
        if(isInt(value)) {
            int intValue = Integer.parseInt(value);
            if(intValue >= lowEnd && intValue <= highEnd) {
                return true;
            }
        }
        return false;
    }

    /**
     * special validator for positions
     * @param value
     * @return
     */
    protected static boolean isPositionOrInt(String value) {
        if(isInt(value)) {
            return true;
        }
        else if(XmlEnumeration.POSITIONS.stream().anyMatch(s -> s.equals(value))) {
            return true;
        }
        return false;
    }

    /**
     * Validates unsigned short
     * @param value
     * @return
     */
    protected static boolean isUnsignedShort(String value) {
        return isInRange(value, 0, 255);
    }

    /**
     * returns the attributes
     * @return
     */
    public ArrayList<XmlElementAttribute> getAttributes() {
        return attributes;
    }

    /**
     * returns the attribute map
     * @return
     */
    public HashMap<String, XmlElementAttribute> getNameAttributeMap() {
        return nameAttributeMap;
    }

    /**
     * This is overridden by most of the enum const,
     * sets the values for this instruction in the xml (payloadElement)
     * @param payloadElement
     * @param treeItem
     * @param fpObjectFactory
     * @return
     * @throws Exception
     */
    public Object setValueInXml(Object payloadElement, TreeItem<XmlElementTreeItem> treeItem, ObjectFactory fpObjectFactory) throws Exception {
        return null;
    }

    /**
     * this is ovverridden by the enum consts,
     * creates a gui element for the loaded in xml
     * @param xmlElement
     * @param parentTreeItem
     */
    public abstract void loadFromXml(Object xmlElement, TreeItem<XmlElementTreeItem> parentTreeItem);

    /**
     * gets the operation list
     * @return
     */
    public static ArrayList<Instruction> getOperationsList() {
        return operationsList;
    }
}
