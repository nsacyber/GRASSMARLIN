/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.exec;

import ICSDefines.Direction;
import core.importmodule.ImportItem;
import core.types.ByteTreeItem;
import core.types.ByteTreeRoot;
import core.types.DataDetails;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <pre>
 * Store network data in the ByteTreeRoot.
 * </pre>
 */
public class StoringTask extends LogicalDataImportTask {

    public long time;
    
    public StoringTask(ImportItem importItem) {
        super(importItem);
    }

    @Override
    public void run() {
        try {
            ByteTreeRoot btr = pipeline.getStorage();
            
            srcNode = btr.add(importItem, Arrays.asList(src_ip).iterator());
            dstNode = btr.add(importItem, Arrays.asList(dst_ip).iterator());

            if( !srcNode.isTerminal() ) {
                srcNode.setHash( hash(src_ip) );
                srcNode.MAC = src_mac;
                srcNode.setTerminal(true);
            }
            
            if( !dstNode.isTerminal() ) {
                dstNode.setHash( hash(dst_ip) );
                dstNode.MAC = dst_mac;
                dstNode.setTerminal(true);
            }
            
            srcNode.setSource(true);
            dstNode.setSource(false);
            srcNode.putForwardEdge(dstNode, proto, src, dst, frame, size, time);
            dstNode.backEdges.add(srcNode);
            srcNode.forwardEdges.add(dstNode);
                
        } catch( java.lang.NullPointerException ex ) {
            Logger.getLogger(StoringTask.class.getName()).log(Level.SEVERE,null,ex);
            this.nextTask = null;
        }
        complete(size);
    }
    
    private int hash( Byte[] b ) {
        return (b[0] << 24) | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3]&0xFF)  ;
    }
    
    public static ByteTreeItem getTarget( LogicalDataImportTask previous, DataDetails r ) {
        ByteTreeItem item;
        if( Direction.DESTINATION.equals( r.getDirection() ) ) {
            item = previous.dstNode;
        } else {
            item = previous.srcNode;
        }
        return item;
    }
}
