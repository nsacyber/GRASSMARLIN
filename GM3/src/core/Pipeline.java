/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core;

// app
import TemplateEngine.Data.Filter;
import core.exec.TaskDispatcher;
import core.exec.TopologyDiscoveryTask;
import core.fingerprint.FManager;
import core.importmodule.ImportItem;
import core.topology.PhysicalNode;
import core.types.ByteTreeItem;
import core.types.ByteTreeRoot;
import core.types.DataDetails;
import core.types.VisualDetails;
import core.types.InvokeObservable;
import core.types.LogEmitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * The pipeline contains the "StorageRoot" and provides downward write access
 * as well as upward read access.
 * 
 * Pipeline will generate logs about the availability of Fingerprints.
 * 
 * Pipeline will observe pluggable components and be the access point to the rest of the application to those components.
 */
public class Pipeline implements Observer {
    final LogEmitter logEmitter;
    final TaskDispatcher dispatcher;
    
    boolean firstRun;
    ByteTreeRoot root;
    Filter<DataDetails>   activeFilter; 
    long     elapseTimeMS;
    boolean  filtersAvailable;
    long     dataPollingTimeMS;
    AtomicBoolean dataChangeFlag;
    /** If set, accepts a existing empty folder to write a .xml and .png of the logical graph to */
    private final List<PhysicalNode> rawPhysicalData;
    public Supplier<Collection<String>> fingerprintNameSupplier;
    
    public Pipeline() {
        firstRun = true;
        elapseTimeMS = 0;
        filtersAvailable = false;
        root = new ByteTreeRoot();
        rawPhysicalData = new ArrayList<>();
        dataChangeFlag = new AtomicBoolean();
        dispatcher = new TaskDispatcher(this);
        logEmitter = LogEmitter.factory.get();
    }
    
    public TaskDispatcher taskDispatcher() {
        return dispatcher;
    }
    
    public boolean filtersAvailable() {
        return filtersAvailable;
    }
    
    public ByteTreeRoot getStorage() {
        return root;
    }
	
    public Filter<DataDetails> getfilter() {
        return activeFilter;
    }
    
    public void setDetect( Filter detect ) {
        this.activeFilter = detect;
    }

    public void clear() {
        root.clear();
        rawPhysicalData.stream().map(PhysicalNode::getSourceFile).filter(Objects::nonNull)
                .filter(f-> f instanceof ImportItem)
                .forEach(f -> {
                    System.out.println("reset " + f.getName());
                    ((ImportItem)f).reset();
                });
        rawPhysicalData.clear();
        System.gc();
    }
    
    public Stream<ByteTreeItem> streamTerminals() {
        return root.stream().filter(ByteTreeItem::isTerminal);
    }

    public void runTopologyDiscover(List<Observer> observers) {
        this.taskDispatcher().accept(new TopologyDiscoveryTask(observers));
    }
    
    @Override
    public void update(Observable o, Object arg) {
        if( !(o instanceof InvokeObservable) ) return;
        Object obj = ((InvokeObservable)o).getInvoker();
        if( obj instanceof FManager ) {
            handleFManager( (FManager)obj, (arg instanceof Boolean) && (Boolean)arg  );
        } 
    }

    private void handleFManager(FManager manager, Boolean filterGood ) {
        if( filterGood ) {
            activeFilter = manager.getActiveFilter();
            String msg = String.format("%d Fingerprints have loaded successfully.", manager.getFingerprintNames().size()-1);
            logEmitter.emit(this, Core.ALERT.MESSAGE, msg );
//            Logger.getLogger(Pipeline.class.getName()).log(Level.INFO, msg );
        } else {
            activeFilter = null;
            if( firstRun ) {
                firstRun = false;
            } else {
                if( !manager.isLoading() ) {
                    String msg = "Fingerprints are unavailable.";
                    logEmitter.emit(this, Core.ALERT.DANGER, msg);
                    Logger.getLogger(Pipeline.class.getName()).log(Level.SEVERE, msg);
                }
            }
        }
        filtersAvailable = filterGood;
    }

    public void migrateNodes(int networkHash, int networkMask, List<ByteTreeItem> allNodes) {
        allNodes.forEach(n -> {
            n.getDetails().setNetworkHash( networkHash );
            n.getDetails().setNetworkMask( networkMask );
            n.getDetails().setCidr(ViewUtils.getCIDR( networkMask ));
        });
    }

    public void resetVisualRows() {
        root.stream()
            .filter(ByteTreeItem::isTerminal)
            .peek(item-> item.setVisible(true)) // removes effects of filters
            .filter(ByteTreeItem::hasDetail)
            .map(ByteTreeItem::getDetails)
            .forEach(VisualDetails::resetVisualRows);
    }

    public List<PhysicalNode> getRawPhysicalData() {
        return rawPhysicalData;
    }

    public Collection<String> getFingerprintNames() {
        Collection<String> names;
        if( fingerprintNameSupplier == null ) {
            names = Collections.EMPTY_SET;
        } else {
            names = fingerprintNameSupplier.get();
        }
        return names;
    }

}
