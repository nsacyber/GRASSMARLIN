/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exec;

import TemplateEngine.Data.Filter;
import core.fingerprint.Fingerprint;
import core.fingerprint.ProxyBuffer;
import core.importmodule.ImportItem;
import core.types.VisualDetails;
import core.types.DataDetails;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <pre>
 * Task meant to run after a successful StoringTask.
 * This task runs fingerprinting Operations from Fingerprint.Compiler.
 * If the StoringTask that ran before this fingerprinting task fails, so will this task.
 * This task optimally handles TCP filtering from the Filter class by checking Filters specific to TCP.
 * </pre>
 */
public class TCPTask extends LogicalDataImportTask {

    StoringTask previous;
    public ProxyBuffer payload;

    public TCPTask(ImportItem item, StoringTask previous) {
        super(item);
        this.previous = previous;
        if (this.previous != null) {
            this.previous.setNext(this);
        }
    }

    @Override
    public void run() {
        Filter<DataDetails> fil = pipeline.getfilter();

        try {

            HashSet<Integer> possibleOps = new HashSet<>();

            possibleOps.addAll(Arrays.asList(fil.getAck(ack)));
            possibleOps.addAll(Arrays.asList(fil.getSeq(seq)));
            possibleOps.addAll(Arrays.asList(fil.getFlags(flags)));
            possibleOps.addAll(Arrays.asList(fil.getDstPort(dst)));
            possibleOps.addAll(Arrays.asList(fil.getSrcPort(src)));
            possibleOps.addAll(Arrays.asList(fil.getEthertype(eth)));
            possibleOps.addAll(Arrays.asList(fil.getWindow(window)));
            possibleOps.addAll(Arrays.asList(fil.getTransportProtocol(proto)));

            
            if (possibleOps.isEmpty()) {
                complete();
                return;
            }

            /* the previous task created or retrieved the node containing previous fingerprint hits */
            VisualDetails ds = previous.srcNode.details;
            VisualDetails dd = previous.dstNode.details;

            /* if the target object has already recieved this processing, don't do it again */
            possibleOps.removeAll(ds.repeatProtection);
            possibleOps.removeAll(dd.repeatProtection);

            /* create initial cursor and return set */
            Fingerprint.Cursor c = new Fingerprint.Cursor();
            HashSet<DataDetails> ret = new HashSet<>();

            /* run each operation that passed initial filtering */
            possibleOps.forEach(o -> {
                try {
                    /* run the operation */
                    fil.getOperation(o).apply(previous, payload, c, ret::add, DataDetails::new);
                    
                    /* the return set is updated and each is attributed to the target data. (source be default) */
                    ret.forEach(r -> {
                        VisualDetails d = StoringTask.getTarget(previous, r).getDetails();
                        d.repeatProtection.add(o);
                        d.copy(r);
                    });
                    
                    /* clear and reuse these */
                    ret.clear();
                    c.reset();

                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception caught from CG.", ex);
                }
            });
        } catch (Exception ex) {
            Logger.getLogger(TCPTask.class.getName()).log(Level.WARNING, "Exception thrown from fingerprint-runtime", ex);
        }
        complete();
    }
}
