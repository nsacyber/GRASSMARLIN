/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.MutablePair;

/**
 * The basic node in this tree structure.
 */
public class ByteTreeItem implements ByteTreeNode<Byte, ByteTreeItem> {
    // allowing these public to simplify construction
    // peer -> src_port -> dst_port -> ( # connections, List of Frames )

    public Map<ByteTreeItem, Map<Integer, // PROTO
            Map<Integer, // SRC PORT
            Map<Integer, // DST PORT
            MutablePair<Integer, // # Connection
            List<FrameInfos>>>>>> // frame infos, frame #, time, Size
            edges; // nullable

    public Set<ByteTreeItem> forwardEdges; // nullable
    public Set<ByteTreeItem> backEdges; // nullable
    public File originator; // nullable
    ByteTreeItem parent; // nullable
    String name = null;
    Boolean terminal;
    boolean visible;
    Integer depth;
    Byte value;
    /** in and out bound frame count. */
    public int in, out;
    public boolean isSource;

    final Set<ByteTreeItem> children;

    public VisualDetails details; // nullable
    public Integer hash; // nullable
    private Integer networkId; // nullable
    private Integer networkMask;  // nullable
    /* this should not be how these are stored */
    public Byte[] MAC; // nullable

    /**
     * Create a LinkItem with the properties of a ROOT NODE
     */
    ByteTreeItem() {
        in = 0;
        out = 0;
        parent = null;
        terminal = false;
        isSource = false;
        depth = -1;
        value = Byte.MIN_VALUE;
        networkId = null;
        children = Collections.synchronizedSet(new HashSet<ByteTreeItem>());
    }

    public ByteTreeItem(Byte value) {
        this();
        this.value = value;
    }

    public ByteTreeItem(Byte value, ByteTreeItem parent) {
        this(value);
        this.parent = parent;
    }

    public ByteTreeItem(Byte value, ByteTreeItem parent, Integer depth) {
        this(value, parent);
        this.depth = depth;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean isSource() {
        return isSource;
    }

    public void setSource(boolean isSource) {
        if( isSource ) {
            out++;
        } else {
            in++;
        }
        this.isSource = isSource;
    }

    public boolean isRoot() {
        return false;
    }

    @Override
    public void setHash(Integer hash) {
        this.hash = hash;
    }

    @Override
    public boolean hasDetail() {
        return details != null;
    }

    @Override
    public VisualDetails getDetails() {
        return details;
    }

    @Override
    public synchronized void ifDetailNotPresent(Supplier<VisualDetails> s) {
        if (details == null) {
            details = s.get();
        }
    }

    @Override
    public Byte getValue() {
        return value;
    }

    public boolean hasForwadEdge(ByteTreeItem n) {
        return this.forwardEdges.contains(n);
    }

    public boolean hasBackEdge(ByteTreeItem n) {
        return this.backEdges.contains(n);
    }

    public Map<Integer, Integer> sampleEdges(Map<Integer, Integer> map) {
        forwardEdges.forEach((s) -> {
            map.put(s.hash, 0);
        });
        return map;
    }

    public void putForwardEdge(ByteTreeItem peer, Integer proto, Integer src, Integer dst, Long frame, Integer size, Long time) {
//        Map<Integer,Map<Integer,Map<Integer,MutablePair<Integer,List<FrameInfos>>>>> protos;
//        Map<Integer,Map<Integer,MutablePair<Integer,List<FrameInfos>>>> srcs;
//        Map<Integer,MutablePair<Integer,List<FrameInfos>>> dsts ;
        MutablePair<Integer, List<FrameInfos>> data;
        data = getData(getDsts(getSrcs(getProtos(peer), proto), src), dst);
        data.left++;
        data.right.add(new FrameInfos(frame, size, time));
    }

    private Map<Integer, Map<Integer, Map<Integer, MutablePair<Integer, List<FrameInfos>>>>> getProtos(ByteTreeItem peer) {
        if (edges.containsKey(peer)) {
            return edges.get(peer);
        }
        Map<Integer, Map<Integer, Map<Integer, MutablePair<Integer, List<FrameInfos>>>>> m0 = Collections.synchronizedMap(new HashMap<>());
        edges.put(peer, m0);
        return m0;
    }

    private Map<Integer, Map<Integer, MutablePair<Integer, List<FrameInfos>>>> getSrcs(Map<Integer, Map<Integer, Map<Integer, MutablePair<Integer, List<FrameInfos>>>>> protos, Integer proto) {
        if (protos.containsKey(proto)) {
            return protos.get(proto);
        }
        Map<Integer, Map<Integer, MutablePair<Integer, List<FrameInfos>>>> m0 = Collections.synchronizedMap(new HashMap<>());
        protos.put(proto, m0);
        return m0;
    }

    private Map<Integer, MutablePair<Integer, List<FrameInfos>>> getDsts(Map<Integer, Map<Integer, MutablePair<Integer, List<FrameInfos>>>> dsts, Integer dst) {
        if (dsts.containsKey(dst)) {
            return dsts.get(dst);
        }
        Map<Integer, MutablePair<Integer, List<FrameInfos>>> m0 = Collections.synchronizedMap(new HashMap<>());
        dsts.put(dst, m0);
        return m0;
    }

    private MutablePair<Integer, List<FrameInfos>> getData(Map<Integer, MutablePair<Integer, List<FrameInfos>>> dsts, Integer dst) {
        if (dsts.containsKey(dst)) {
            return dsts.get(dst);
        }
        MutablePair<Integer, List<FrameInfos>> m0 = new MutablePair(0, new ArrayList<>());
        dsts.put(dst, m0);
        return m0;
    }

    public synchronized void addBackEdge(ByteTreeItem item) {
        backEdges.add(item);
    }

    public boolean hasSiblings() {
        return forwardEdges == null ? false : !forwardEdges.isEmpty();
    }

    public boolean eachSibling(BiConsumer<ByteTreeItem, Integer> cb) {
        if (forwardEdges == null) {
            return false;
        }
        forwardEdges.forEach((n) -> cb.accept(n, n.hash));
        return true;
    }

    @Override
    public Set<ByteTreeItem> getSiblings() {
        return forwardEdges;
    }

    @Override
    public void setTerminal(Boolean terminal) {
        this.terminal = this.visible = terminal;
    }

    @Override
    public Boolean isTerminal() {
        return terminal;
    }

    @Override
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    @Override
    public boolean hasParent() {
        return parent != null;
    }

    @Override
    public Collection<ByteTreeItem> getChildren() {
        return children; // unmodifiable collection necessary?
    }

    @Override
    public Optional<ByteTreeItem> childContaining(Byte t) {
        return children.stream().filter(c -> c.getValue().equals(t)).findFirst();
    }

    // don't care about duplicates? the overhead is minimal?
    @Override
    public ByteTreeItem add(Iterator<Byte> it, Integer depth, Integer cbDepth, Consumer<ByteTreeItem> cb) {
        while (it.hasNext()) {
            Byte b = it.next();
            ByteTreeItem ret;
            synchronized (children) {
                ret = childContaining(b).orElse(
                        new ByteTreeItem(b, this, depth + 1)
                );
                // initialize leaf before its visible
                if (cbDepth.equals(depth + 1)) {
                    cb.accept(ret);
                }
                children.add(ret);
            }
            return ret.add(it, depth + 1, cbDepth, cb); // return the child created
        }
        return this; // returns when this node is the final child
    }

    @Override
    public ByteTreeItem getParent() {
        return parent;
    }

    @Override
    public void setParent(ByteTreeItem parent) {
        this.parent = parent;
    }

    @Override
    public Stream<ByteTreeItem> stream() {
        if (isEmpty()) {
            return Stream.of(this);
        }
        synchronized (children) {
            return children.stream()
                    .map(ByteTreeItem::stream)
                    .reduce(Stream.of(this), Stream::concat);
        }
    }

    @Override
    public Stream<ByteTreeItem> upstream() {
        if (hasParent()) {
            return Stream.concat(parent.upstream(), Stream.of(this));
        }
        return Stream.of(this);
    }

    @Override
    public int depth() {
        return depth;
    }

    @Override
    public Iterator<ByteTreeItem> iterator() {
        return children.iterator();
    }

    @Override
    public int compareTo(Byte o) {
        return value - o;
    }

    @Override
    public int size() {
        return children.size();
    }

    @Override
    public boolean isEmpty() {
        return children.isEmpty();
    }

    @Override
    public boolean add(ByteTreeItem l) {
        return children.add(l);
    }

    @Override
    public boolean remove(Object o) {
        return children.remove(o);
    }

    @Override
    public void clear() {
        try {
            parent = null;
            if (children != null) {
                children.clear();
            }
            if (forwardEdges != null) {
                forwardEdges.clear();
            }
            if (details != null) {
                details.clear();
            }
        } catch (NullPointerException ex) {
            Logger.getLogger(ByteTreeItem.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* This structure really needs better construction */
    @Override
    public int hashCode() {
        return hash == null ? value : hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ByteTreeItem other = (ByteTreeItem) obj;
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return Integer.toString(255 & value);
    }

    /* unnecessary */
    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean addAll(Collection<? extends ByteTreeItem> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNetworkId() {
        return networkId;
    }

    public Integer getNetworkMask() {
        return networkMask;
    }

    public boolean hasNetworkId() {
        return getNetworkId() != null;
    }

    public void updateNetwork(int newNetworkId, int newMask) {
        this.networkId = newNetworkId;
        this.networkMask = newMask;
    }
}
