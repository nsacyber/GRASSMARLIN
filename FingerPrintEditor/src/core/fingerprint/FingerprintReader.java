package core.fingerprint;

import core.fingerprint3.ByteTestFunction;
import core.fingerprint3.Fingerprint;
import core.fingerprint3.MatchFunction;
import core.fingerprint3.Return;
import core.ui.graph.ContainerNode;
import core.ui.graph.RoundedTextNode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import sample.Controller;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * 07.24.2015 - CC - New...
 */
public class FingerprintReader {
    private Unmarshaller unmarshaller;
    private Controller controller;

    public void loadFile(File fingerPrintFile, Controller controller) throws JAXBException {
        this.controller = controller;
        if(this.unmarshaller ==null) {
                this.unmarshaller = FingerprintBuilder.FingerprintContext.INSTANCE.getContext().createUnmarshaller();
        }
        Fingerprint fingerprint = (Fingerprint)unmarshaller.unmarshal(fingerPrintFile);
        this.controller.getTextFieldAuthorId().setText(fingerprint.getHeader().getAuthor());
        this.controller.getTextFieldDescriptionId().setText(fingerprint.getHeader().getDescription());
        this.controller.getTextFieldNameId().setText(fingerprint.getHeader().getName());
        StringBuilder tagBuilder = new StringBuilder();
        fingerprint.getHeader().getTag().stream().forEach(s -> tagBuilder.append(tagBuilder.length() != 0 ? "," : "").append(s));
        this.controller.getTextFieldTagsId().setText(tagBuilder.toString());
        populateFilterPanelList(fingerprint);
        populateTreeViewPanelList(fingerprint);
    }

    private void populateTreeViewPanelList(Fingerprint fingerprint) {
        HashMap<String, TreeView<XmlElementTreeItem>> treeMap = new HashMap<>();
        fingerprint.getPayload().stream().forEach(payload -> {
            String uuid = payload.getFor();
            if (treeMap.containsKey(uuid)) {
                System.err.println("some reason there is more than one payload with the same UUID: " + uuid);
            } else {
                treeMap.put(uuid, this.controller.getTreeViewForUuid(uuid));
                populateTreeView(treeMap.get(uuid), payload);
            }
        });
    }

    private void populateTreeView(TreeView<XmlElementTreeItem> treeView, Fingerprint.Payload payload) {
        String description = payload.getDescription();
        if(description != null && !description.isEmpty()) {
            XmlElementTreeItem value = new XmlElementTreeItem(Instruction.DESCRIPTION);
            Instruction.DESCRIPTION.getAttributes().stream().forEach(attribute -> value.addAttribute(attribute, description));

            TreeItem<XmlElementTreeItem> newItem = new TreeItem<>(value);
            treeView.getRoot().getChildren().add(0, newItem);
        }
        Instruction.PAYLOAD.loadFromXml(payload, treeView.getRoot());
    }

    private void processOperation(List<Object> operation, TreeItem<XmlElementTreeItem> parentTreeItem) {
    }

    private void processAlways(Fingerprint.Payload.Always always, TreeItem<XmlElementTreeItem> parentTreeItem) {
        TreeItem<XmlElementTreeItem> alwaysTreeItem = new TreeItem<>(new XmlElementTreeItem(Instruction.ALWAYS));
        parentTreeItem.getChildren().add(alwaysTreeItem);
        processReturn(always.getReturn(), alwaysTreeItem);
    }

    private void processReturn(List<Return> returnElements, TreeItem<XmlElementTreeItem> parentTreeItem) {
        returnElements.stream().forEach(returnElement -> {
            TreeItem<XmlElementTreeItem> returnTreeItem = new TreeItem<>(new XmlElementTreeItem(Instruction.RETURN));
            //returnTreeItem.getValue().addAttribute(returnTreeItem.getValue(), );
            returnElement.getDirection();
            returnElement.getConfidence();

            returnElement.getExtract();
            returnElement.getDetails();

        });


    }

    private void populateFilterPanelList(Fingerprint fingerprint) {
        HashMap<String, FpPanel> panelMap = new HashMap<>();
        fingerprint.getFilter().stream().forEach(filter -> {
            String uuid = filter.getFor();
            if(panelMap.containsKey(uuid)) {
                populateFilterPanel(panelMap.get(uuid), filter);
            }
            else {
                this.controller.loadNewPayLoadTab(uuid);
                this.controller.getFilterPanelList().stream().filter(fpPanel -> fpPanel.getUuid().equals(uuid)).forEach(thisPanel -> {
                    panelMap.put(uuid,thisPanel);
                    populateFilterPanel(thisPanel, filter);
                });
            }
        });
    }

    private void populateFilterPanel(FpPanel panel, Fingerprint.Filter filter) {
        ContainerNode container = this.controller.addNewReturnGroup(Controller.FILTER_GROUP_CONTAINER_LABEL_TEXT, Controller.FILTER_GROUP_CONTAINER_IDENTIFIER,panel, false );
        filter.getAckAndMSSAndDsize().stream().forEach(element -> {
            String valueString = "";
            if(element.getValue() instanceof Fingerprint.Filter.DsizeWithin) {
                valueString = ((Fingerprint.Filter.DsizeWithin)element.getValue()).getMin() + " - " + ((Fingerprint.Filter.DsizeWithin)element.getValue()).getMax();
            }
            else if (element.getValue() instanceof Fingerprint.Filter.TTLWithin) {
                valueString = ((Fingerprint.Filter.TTLWithin)element.getValue()).getMin() + " - " + ((Fingerprint.Filter.TTLWithin)element.getValue()).getMax();
            } else {
                valueString = element.getValue().toString();
            }
            addFilterToFilterGroup(element.getName().toString(),valueString,container,panel);
        });

    }

    public void addFilterToFilterGroup(String name, String value, ContainerNode filterGroup, FpPanel panel) {
        RoundedTextNode newNode = this.controller.createFilterNode(name, value);
        filterGroup.addElement(newNode);
        filterGroup.refresh();
        panel.resize();
    }

}
