/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exportmodule;

import core.dataexport.NodeType;
import core.exec.Task;
import core.exportdata.DataExporter;
import core.topology.Entities;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.xml.bind.JAXBException;
import ui.GrassMarlin;
import ui.views.tree.PhysicalTree;
import ui.views.tree.visualnode.PeerVisualNode;
import ui.views.tree.visualnode.VisualNode;

/**
 * 9.24.2015 - BESTDOG - Copied to class. 2015.09.25 - CC - added imported items
 * importing
 */
public class ExportDataTask extends Task {

    /**
     * MAY run after a {@link #saveToFile(java.io.File) } call depending on
     * constructor method of {@link #ExportDataTask(java.util.function.Consumer)
     * }. WILL be used to move saved file to a Zip for export share. MAY be
     * provided with a null value.
     */
    private Consumer<File> afterSave;

    public ExportDataTask() {
    }

    public ExportDataTask(Consumer<File> afterSave) {
        this.afterSave = afterSave;
    }

    @Override
    public void run() {
        GrassMarlin.window.footer.showSpinner(true);
        final JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("GM_DataExport.xml"));
        if (fc.showSaveDialog(GrassMarlin.window) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            saveToFile(file);
        }
        GrassMarlin.window.footer.showSpinner(false);
    }

    public void saveToFile(File saveFile) {
        final DataExporter exporter = new DataExporter();
        VisualNode root = GrassMarlin.window.tree.getRoot();
        root.getChildren().stream()
                .map(VisualNode::getChildren)
                .flatMap(List::stream)
                .forEach(treeViewNode -> exportNode(treeViewNode, exporter, NodeType.LOGICAL));
        exporter.exportFingerprintFiles(GrassMarlin.window.manager.getLoaded());
        exporter.exportImportedFilePaths(GrassMarlin.window.getImports().stream().map(File::getAbsolutePath).collect(Collectors.toList()));
        try {
            exporter.saveFile(saveFile);
            if (afterSave != null) {
                afterSave.accept(saveFile);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JAXBException e1) {
            e1.printStackTrace();
        }
    }

    private void exportNode(PhysicalTree.StyleItem styleItem, final DataExporter exporter, NodeType nodeType) {
        Entities treeViewNodeEntry = ((PhysicalTree.StyleItem) styleItem).getEntity();
        if (treeViewNodeEntry instanceof core.topology.Device) {
            core.topology.Device treeViewNode = ((core.topology.Device) treeViewNodeEntry);
            String ip = treeViewNode.getName();
            String network = treeViewNode.getDisplayText();
            String name = treeViewNode.getName();
            HashMap<String, String> detailMap = new HashMap<>();
            HashMap<String, String> connectionMap = new HashMap<>();
            treeViewNode.getAttributes().keySet().stream().forEach(key -> detailMap.put(key, treeViewNode.getAttributes().get(key).toString()));
        }
    }

    private void exportNode(VisualNode treeViewNode, final DataExporter exporter, NodeType nodeType) {
        String ip = treeViewNode.getAddress();
        String network = treeViewNode.getParent().getAddress();
        String name = treeViewNode.getName();
        HashMap<String, String> detailMap = new HashMap<>();
        HashMap<String, String> connectionMap = new HashMap<>();
//        BESTDOG - start change
//        BESTDOG - modified underlying object behavior, made change accordingly.
//        treeViewNode.getDetails().getCommon().entrySet().stream().forEach(keyValueSet -> {
//            detailMap.put(keyValueSet.getKey(), getConcat(keyValueSet.getValue()));
//        });
//        treeViewNode.getData().getDetails().getCommon().entrySet().stream().forEach(stringSetEntry -> {
//            if(!detailMap.containsKey(stringSetEntry.getKey())) {
//                detailMap.put(stringSetEntry.getKey(),getConcat(stringSetEntry.getValue()));
//            }
//        });
        treeViewNode.getDetails().forEach((k, v) -> {
            detailMap.put(k, v);
        });
        String fingerprintNames = getConcat(treeViewNode.getDetails().getNames());
//        BESTDOG - end change
        if (fingerprintNames != null && !fingerprintNames.isEmpty()) {
            detailMap.put("fingerprint", fingerprintNames);
        }
        detailMap.put("Confidence", treeViewNode.getDetails().getConfidence() + "");
        detailMap.put("Role", treeViewNode.getDetails().getRole().toString());
        //detailMap.put("Vendor", treeViewNode.getDetails().getOUI());
        //detailMap.put("Country", treeViewNode.countryText);

        treeViewNode.getChildren().stream()
                .filter(n -> n instanceof PeerVisualNode)
                .map(n -> (PeerVisualNode) n)
                .forEach(connectionNode -> {
                    final String peerAddress = connectionNode.getAddress();
                    final String total = Integer.toString( connectionNode.total() );
                    connectionMap.put(peerAddress, total);
                });
        exporter.exportDevice(network, name, ip, detailMap, nodeType, connectionMap, treeViewNode.getData());
    }

    private String getConcat(Set<String> stringSet) {
        StringBuilder returnValue = new StringBuilder();
        stringSet.stream().forEach(string -> {
            if (returnValue.length() > 0) {
                returnValue.append(",");
            }
            returnValue.append(string);
        });
        return returnValue.toString();
    }

}
