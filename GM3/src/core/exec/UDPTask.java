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
 * Task meant to run after a successful storing task.
 * This task runs fingerprinting operations from the Fingerprint.Compiler.
 * If the storing task that ran before this fingerprinting task fails, so will this task.
 * This class optimally handles UDP filters fro mthe Filter class by not checking filters that are specific to other protocols.
 * </pre>
 */
public class UDPTask extends LogicalDataImportTask {

    private final static int ZEP_PORT = 17754;

    StoringTask previous;
    public ProxyBuffer payload;
    
    public UDPTask(ImportItem item, StoringTask previous) {
        super(item);
        this.previous = previous;
        if( this.previous != null )
            this.previous.setNext(this);
    }

    @Override
    public void run() {
        Filter<DataDetails> fil = pipeline.getfilter();
        try {
            
            HashSet<Integer> possibleOps = new HashSet<>();
            
            possibleOps.addAll( Arrays.asList( fil.getDstPort(dst) ) );
            possibleOps.addAll( Arrays.asList( fil.getSrcPort(src) ) );
            possibleOps.addAll( Arrays.asList( fil.getEthertype(eth) ) );
            possibleOps.addAll( Arrays.asList( fil.getTransportProtocol(proto) ) );

            if( possibleOps.isEmpty() ) {
                complete();
                return;
            }
            
            /* the previous task created or retrieved the node containing previous fingerprint hits */
            VisualDetails ds = previous.srcNode.details;
            VisualDetails dd = previous.dstNode.details;
            
            possibleOps.removeAll(ds.repeatProtection);
            possibleOps.removeAll(dd.repeatProtection);
            
            Fingerprint.Cursor c = new Fingerprint.Cursor();
            HashSet<DataDetails> ret = new HashSet<>();

            possibleOps.forEach(opId ->{
                try {
                    
                    fil.getOperation(opId).apply(previous, payload, c, ret::add, DataDetails::new);

                    ret.forEach(dataDetails->{
                        VisualDetails d = StoringTask.getTarget(previous, dataDetails).getDetails();
                        d.repeatProtection.add(opId);
                        d.copy(dataDetails);
                    });
                    
                    ret.clear();
                    c.reset();
                    
                } catch( Exception ex ) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error in reflected OP", ex);
                }
            });
            
        } catch( Exception ex ) {
            Logger.getLogger(UDPTask.class.getName()).log(Level.WARNING,"Exception thrown from fingerprint-runtime", ex);
        }
        complete(); 
    }
    
}