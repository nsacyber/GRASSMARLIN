package core.exec;

import core.dataexport.DeviceType;
import core.dataexport.FileType;
import core.dataexport.GmDataExportType;
import core.dataexport.NetworkType;
import core.importmodule.Gm3Import;
import core.topology.Mac;
import core.types.ByteTreeItem;
import core.types.ByteTreeRoot;
import org.apache.commons.lang3.ArrayUtils;
import ui.GrassMarlin;
import ui.dialog.DialogManager;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 09.15.2015 - CC - New...
 * 2015.09.25 - CC - added imported items importing
 */
public class Gm3ImportTask extends ImportTask<Gm3Import>  {


    private GmDataExportType gmDataExportType;

    /**
     * Constructs a new ImportTask
     *
     * @param importItem
     */
    public Gm3ImportTask(Gm3Import importItem, GmDataExportType gmDataExportType) {
        super(importItem);
        this.gmDataExportType = gmDataExportType;
    }

    @Override
    public void run() {
        this.gmDataExportType.getData().getNetwork().stream().map(NetworkType::getDevice).flatMap(List::stream).forEach(this::process);

        GrassMarlin.window.manager.reload(
                this.gmDataExportType.getFingerprints().getFingerprint().stream().map(FileType::getPath).map(File::new).collect(Collectors.toList()));
        DialogManager.ImportDialog(null).mergeList(this.gmDataExportType.getImportedItems().getImportedItem().stream().map(FileType::getPath).map(File::new).map(DialogManager.ImportDialog(null)::guessType).collect(Collectors.toList()));
        complete();
    }

    private void process(DeviceType deviceType) {
        final String sourceIpString = deviceType.getPrimaryAddress();
        deviceType.getConnection().stream().forEach(connectionType -> {
            try {
                int destinationPort = connectionType.getDstPort().intValue();
                int sourcePort = connectionType.getSrcPort().intValue();
                int proto = connectionType.getProtocol().intValue();
                int pacSize = connectionType.getSize().intValue();
                long frameNo = connectionType.getFrameNumber();
                long timeStamp = connectionType.getTimestamp();
                Byte[] sourceIp = ArrayUtils.toObject(InetAddress.getByName(sourceIpString).getAddress());
                Byte[] destinationIp = ArrayUtils.toObject(InetAddress.getByName(connectionType.getIp()).getAddress());

                if (destinationIp == null || sourceIp == null) {
                    return;
                }

                ByteTreeRoot byteTreeRoot = super.pipeline.getStorage();

                ByteTreeItem srcNode = byteTreeRoot.add(super.importItem, Arrays.asList(sourceIp).iterator());
                ByteTreeItem dstNode = byteTreeRoot.add(super.importItem, Arrays.asList(destinationIp).iterator());
                deviceType.getDetail().stream().forEach(detailType -> {
                    srcNode.getDetails().put(detailType.getName(), detailType.getValue());
//                    BESTDOG changed to String values instead of Set<String> values.
//                    srcNode.getDetails().getCommon().put(detailType.getName(), unConcat(detailType.getValue()));
                });

                srcNode.setName(deviceType.getName());

                if (!srcNode.isTerminal()) {
                    srcNode.setHash(hash(sourceIp));
                    srcNode.MAC = Mac.toBytes(connectionType.getSrcMac());
                    srcNode.setTerminal(true);
                }

                if (!dstNode.isTerminal()) {
                    dstNode.setHash(hash(destinationIp));
                    dstNode.MAC = Mac.toBytes(connectionType.getDstMac());
                    dstNode.setTerminal(true);
                }

                srcNode.setSource(true);
                dstNode.setSource(false);

                srcNode.putForwardEdge(dstNode, proto, sourcePort, destinationPort, frameNo, pacSize, timeStamp);
                dstNode.backEdges.add(srcNode);
                srcNode.forwardEdges.add(dstNode);

            } catch (UnknownHostException e) {
                Logger.getLogger(Gm3ImportTask.class.getName()).log(Level.SEVERE, "Failed to store entry, " + deviceType.getPrimaryAddress() + ": " + deviceType.getName(), e);
            }

        });
    }

    private Set<String> unConcat(String string) {
        Set<String> returnSet = new HashSet<>();
        Arrays.stream(string.split(",")).forEach(returnSet::add);
        return returnSet;
    }
        
    private int hash( Byte[] b ) {
        return (b[0] << 24) | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3]&0xFF)  ;
    }
    
}
