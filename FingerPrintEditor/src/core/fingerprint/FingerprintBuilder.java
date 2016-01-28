package core.fingerprint;

import core.fingerprint3.Fingerprint;
import core.fingerprint3.Header;
import core.fingerprint3.ObjectFactory;
import core.ui.graph.ContainerNode;
import core.ui.graph.RoundedTextNode;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import sample.Controller;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 07.13.2015 - CC - New...
 */
public class FingerprintBuilder {
    private Fingerprint fingerprint;
    private Marshaller fpMarshaller;
    private ObjectFactory fpObjectFactory;

    public enum FingerprintContext {
        INSTANCE("core.fingerprint3");

        private JAXBContext context;

        FingerprintContext(String classToCreate) {
            try {
                this.context = JAXBContext.newInstance(classToCreate);
            } catch (JAXBException e) {
                throw new IllegalStateException("unable to create JAXBContext");
            }
        }
        public JAXBContext getContext(){
            return context;
        }
    }

    public FingerprintBuilder(String name, String author, String description, String tags) {
        this.fpObjectFactory = new ObjectFactory();
        this.fingerprint = new ObjectFactory().createFingerprint();
        Header header = new ObjectFactory().createHeader();
        header.setAuthor(author);
        header.setDescription(description);
        header.setName(name);
        header.getTag().addAll(Arrays.asList(tags.split(",")).stream().filter(s1 -> s1 != null).filter(s -> s.length() >= 3).collect(Collectors.toList()));
        this.fingerprint.setHeader(header);
    }

    public void saveFile(File saveFile) throws IOException, JAXBException {
        if(this.fpMarshaller == null) {
            this.fpMarshaller = FingerprintContext.INSTANCE.getContext().createMarshaller();
            this.fpMarshaller.setProperty("jaxb.formatted.output",true);
        }

        try (FileOutputStream out = new FileOutputStream(saveFile)) {
            JAXBElement<Fingerprint> element = new JAXBElement<>(new QName("", "Fingerprint"), Fingerprint.class, this.fingerprint);
            this.fpMarshaller.marshal(element, out);
        }
    }

    public void processFilterPanel(ArrayList<FpPanel> filterPanelList, Controller controller) {
        for(FpPanel panel : filterPanelList) {
            panel.getNodes().stream().filter(node -> node.getName().equals(Controller.FILTER_GROUP_CONTAINER_IDENTIFIER)).forEach(node -> {
                Fingerprint.Filter filter = this.fpObjectFactory.createFingerprintFilter();
                filter.setFor(panel.getUuid());
                ((ContainerNode) node).getElements().stream().forEach(subNode -> {
                    if (subNode instanceof RoundedTextNode) {
                        String filterName = ((RoundedTextNode) subNode).getText();
                        FingerprintFilterType filterType = FingerprintFilterType.valueOf(filterName.trim().toUpperCase());
                        try {
                            filter.getAckAndMSSAndDsize().add(filterType.getJaxBElement((RoundedTextNode) subNode, this.fpObjectFactory));
                        } catch (Exception e) {
                            controller.showException("Error processing filter: [" + filterName + "] in payload group [" + panel.getUuid() + "]",e);
                        }
                    }
                });
                this.fingerprint.getFilter().add(filter);
            });
        }
    }

    public void processPayloadPanel(ArrayList<TreeView<XmlElementTreeItem>> treeViewPanelList, Controller controller) {
        for(TreeView<XmlElementTreeItem> treeView : treeViewPanelList) {
            Fingerprint.Payload payload = this.fpObjectFactory.createFingerprintPayload();
            try {
                traverseTree(treeView.getRoot(),payload);
            } catch (Exception e) {
                controller.showException(e);
            }
            this.fingerprint.getPayload().add(payload);
        }
    }

    private void traverseTree(TreeItem<XmlElementTreeItem> treeNode, Object payload) throws Exception {
        Object parentObject = treeNode.getValue().getInstruction().setValueInXml(payload, treeNode, this.fpObjectFactory);
        for(TreeItem<XmlElementTreeItem> xmlTreeItem : treeNode.getChildren()) {
            traverseTree(xmlTreeItem, parentObject);
        }
    }
}
