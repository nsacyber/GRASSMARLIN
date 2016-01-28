/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exec;

import TemplateEngine.Data.Filter;
import core.fingerprint.Fingerprint;
import core.fingerprint.ProxyBuffer;
import core.importmodule.Bro2Import;
import core.importmodule.Trait;
import core.types.ByteTreeItem;
import core.types.ByteTreeRoot;
import core.types.VisualDetails;
import core.types.DataDetails;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes a bulk-chunk of Bro2logs and reused variables. May create multiple
 * tasks whilst still running.
 * 
 * The size of this chunk is determined by the the preceding task.
 */
public class Bro2ChunkProcessorTask extends ImportTask<Bro2Import> {

    /**
     * value used to set MACS on newly constructed nodes, just to assure they're
     * psuedo-immutable.
     */
    private final static Byte[] BLANK_MAC = new Byte[]{0x0, 0x0, 0x0, 0x0, 0x0, 0x0};

    /**
     * Chunk of entries to process.
     */
    final List<Map<Trait, Object>> items;

    /**
     * A list for follow-up tasks, these will do the light-fingerprinting available for bro.
     */
    List<Bro2Subtask> subTasks;

    public Bro2ChunkProcessorTask(Bro2Import importItem, List<Map<Trait, Object>> items) {
        super(importItem);
        this.items = items;
    }

    @Override
    public void run() {

        if (pipeline.filtersAvailable()) {
            subTasks = new ArrayList<>();
        }

        items.forEach(this::process);

        if (subTasks != null) {
            subTasks.forEach(task -> {
                task.setPipeline(pipeline);
                task.run();
            });
        }

        complete();
    }

    private void process(Map<Trait, Object> item) {
        if (item.isEmpty()) {
            return;
        }

        int destinationPort;
        int sourcePort;
        int proto;
        int pacSize;
        long frameNo;
        long timeStamp;
        Byte[] sourceIp;
        Byte[] destinationIp;

        try {

            frameNo = (long) item.get(Trait.FRAME_NO);
            timeStamp = (long) item.get(Trait.TIMESTAMP);
            destinationPort = (Integer) item.get(Trait.PORT_DST);
            sourcePort = (Integer) item.get(Trait.PORT_SRC);
            proto = (Integer) item.get(Trait.PROTO);
            pacSize = (Integer) item.get(Trait.PACKET_SIZE);
            sourceIp = (Byte[]) item.get(Trait.IPv4_SRC);
            destinationIp = (Byte[]) item.get(Trait.IPv4_DST);

            if (destinationIp == null || sourceIp == null) {
                return;
            }

            ByteTreeRoot btr = pipeline.getStorage();

            ByteTreeItem srcNode = btr.add(importItem, Arrays.asList(sourceIp).iterator());
            ByteTreeItem dstNode = btr.add(importItem, Arrays.asList(destinationIp).iterator());

            if (!srcNode.isTerminal()) {
                srcNode.setHash(hash(sourceIp));
                srcNode.MAC = BLANK_MAC;
                srcNode.setTerminal(true);
            }

            if (!dstNode.isTerminal()) {
                dstNode.setHash(hash(destinationIp));
                dstNode.MAC = BLANK_MAC;
                dstNode.setTerminal(true);
            }

            srcNode.setSource(true);
            dstNode.setSource(false);

            srcNode.putForwardEdge(dstNode, proto, sourcePort, destinationPort, frameNo, pacSize, timeStamp);
            dstNode.backEdges.add(srcNode);
            srcNode.forwardEdges.add(dstNode);

            if (subTasks != null) {
                subTasks.add(new Bro2Subtask(importItem, srcNode, dstNode, sourcePort, destinationPort, pacSize, proto));
            }

        } catch (Exception ex) {
            Logger.getLogger(Bro2ChunkProcessorTask.class.getName()).log(Level.SEVERE, "Failed to store entry, " + item.toString(), ex);
        }

    }

    private int hash(Byte[] b) {
        return (b[0] << 24) | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
    }

    /**
     * Does a fingerprinting routine that quickly processes data available on
     * bro2 data. These occur linearly from the above {@link #run() } method,
     * and it is pre-assumed that the fingerprinting filter is available in the
     * pipeline;
     */
    private class Bro2Subtask extends LogicalDataImportTask<Bro2Import> {

        private final ProxyBuffer EMPTY_JBUFFER = ProxyBuffer.EMPTY_BUFFER;

        public Bro2Subtask(Bro2Import importItem, ByteTreeItem src, ByteTreeItem dst, int srcPort, int dstPort, int packetSize, int proto) {
            super(importItem);
            this.srcNode = src;
            this.dstNode = dst;
            this.dst = dstPort;
            this.src = srcPort;
            this.dsize = packetSize;
            this.proto = proto;
        }

        @Override
        public void run() {

            Filter<DataDetails> fil = pipeline.getfilter();

            try {
                HashSet<Integer> possibleOps = new HashSet<>();

                Integer[] ary = fil.getDsize(dsize);
                if (ary.length != 0) {
                    possibleOps.addAll(Arrays.asList(ary));
                } else {
                    possibleOps.addAll(Arrays.asList(fil.DsizeInRange(dsize)));
                }

                possibleOps.addAll(Arrays.asList(fil.getDstPort(dst)));
                possibleOps.addAll(Arrays.asList(fil.getSrcPort(src)));
                possibleOps.addAll(Arrays.asList(fil.getTransportProtocol(proto)));

                if (possibleOps.isEmpty()) {
                    complete();
                    return;
                }

                /* the previous task created or retrieved the node containing previous fingerprint hits */
                VisualDetails ds = srcNode.details;
                VisualDetails dd = dstNode.details;

                possibleOps.removeAll(ds.repeatProtection);
                possibleOps.removeAll(dd.repeatProtection);

                Fingerprint.Cursor c = new Fingerprint.Cursor();
                HashSet<DataDetails> ret = new HashSet<>();
                
                possibleOps.forEach(o -> {
                    try {

                        fil.getOperation(o).apply(this, EMPTY_JBUFFER, c, ret::add, DataDetails::new);

                        ret.forEach(r -> {
                            VisualDetails d = StoringTask.getTarget(this, r).getDetails();
                            d.repeatProtection.add(o);
                            d.copy(r);
                        });

                        ret.clear();
                        c.reset();

                    } catch (Exception ex) {
                        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error in reflected OP", ex);
                    }
                });

            } catch (Exception ex) {
                Logger.getLogger(Bro2Subtask.class.getName()).log(Level.SEVERE, "Failure in generated code.", ex);
            }

            complete();
        }
    }

}
