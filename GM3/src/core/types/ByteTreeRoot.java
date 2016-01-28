/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.types;

import core.importmodule.ImportItem;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * The purpose of the root is to organize byte tree sequences under the tree of their originator
 */
public class ByteTreeRoot extends ByteTreeItem {
    
//    ArrayList<Long> timeStamps;

//    Map<ImportItem,Set<ByteTreeItem>> contentTracker;
    
    public ByteTreeRoot() {
        super();
//        timeStamps = new ArrayList<>();
//        contentTracker = Collections.synchronizedMap( new HashMap<>() );
    }
    
//    public Map<ImportItem, Set<ByteTreeItem>> getContentTracker() {
//        return contentTracker;
//    }
    
//    public void countFrame(long timestampInMillis) {
//        timeStamps.add(timestampInMillis);
//    }
    
//    public ArrayList<Long> getTimeStamps() {
//        return timeStamps;
//    }
    
    @Override
    public String toString() {
        return "";
    }
    
    @Override
    public boolean isRoot() {
        return true;
    }
    
    static void setupLeafNode(ByteTreeItem node) {
        if( !node.isTerminal() ) {
            node.edges = Collections.synchronizedMap( new HashMap<>() );
            node.backEdges = Collections.synchronizedSet(  new HashSet<>() );
            node.forwardEdges = Collections.synchronizedSet(  new HashSet<>() );
            node.details = new VisualDetails();
        } 
    }
    
    /**
     * A storage primer specific to IPv4 traffic.
     * This assumes 4 byte sequences indexed 0 - 3, where on index 3 the setupCallback will trigger
     * @param originator Source of this content
     * @param it Iterator to byte sequence starting at the MSB
     * @return The leaf node where the LSB is located
     */
    public ByteTreeItem add(ImportItem originator, Iterator<Byte> it) {
        ByteTreeItem node = add( it, 0, 3, ByteTreeRoot::setupLeafNode);

        // if the item is new it's not terminal and we do not need to track it twice
        // this didn't work too good.
//        if( !node.isTerminal() )
//            contentTracker.get(originator).add(node);
        node.originator = originator;
        
        return node;
    }
    
    @Override
    public ByteTreeItem add(Iterator<Byte> it, Integer depth, Integer cbDepth, Consumer<ByteTreeItem> cb) {
        if( it.hasNext() ) {
            Byte b = it.next();
            ByteTreeItem ret;
            synchronized( children ) {
                ret = childContaining( b ).orElse( new ByteTreeItem( b, this, 0 ) );
                children.add(ret);
            }
            return ret.add(it, 0, cbDepth, cb);
        }
        return null; 
    }
    
    @Override
    public void clear() {
        // leaf up
        stream().filter(ByteTreeItem::isTerminal).forEach(ByteTreeItem::clear);
//        contentTracker.clear();
        super.clear();
        System.gc();
    }
    
}
