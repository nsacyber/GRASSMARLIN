package prefuse.data.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import prefuse.data.Tuple;


/**
 * A trie data structure for fast-lookup of words based on their
 * prefixes. The name "Trie" is a play on the words "tree" and 
 * "retrieval". This class builds a tree structure representing a set of
 * words by their prefixes. It is useful for performing prefix-based
 * searches over large amounts of text in an efficient manner.
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see PrefixSearchTupleSet
 */
public class Trie {

    /**
     * Base class for nodes in the trie structure.
     */
    public class TrieNode {
        boolean isLeaf;
        int leafCount = 0;
    }
    
    /**
     * A TrieNode implementation representing a branch in the tree. The
     * class maintains a list of characters (the next character in the
     * prefix) and associated children TrieNodes for each.
     */
    public class TrieBranch extends TrieNode {
        char[] chars = new char[] {0};
        TrieNode[] children = new TrieNode[1];
    }
    
    /**
     * A TrieNode implementation representing a leaf in the tree. The class
     * stores the word and tuple for the leaf, as well as a reference to the
     * successor leaf node in the trie.
     */
    public class TrieLeaf extends TrieNode {
        public TrieLeaf(String word, Tuple t) {
            this.word = word;
            tuple = t;
            next = null;
            leafCount = 1;
        }
        String word;
        Tuple tuple;
        TrieLeaf next;
    }
    
    /**
     * An iterator for traversing a subtree of the Trie.
     */
    public class TrieIterator implements Iterator {
        private LinkedList queue;
        public TrieIterator(TrieNode node) {
            queue = new LinkedList();
            queue.add(node);
        }
        public boolean hasNext() {
            return !queue.isEmpty();
        }
        public Object next() {
            if ( queue.isEmpty() )
                throw new NoSuchElementException();
            
            TrieNode n = (TrieNode)queue.removeFirst();
            Object o;
            if ( n instanceof TrieLeaf ) {
                TrieLeaf l = (TrieLeaf)n;
                o = l.tuple;
                if ( l.next != null )
                    queue.addFirst(l.next);
                return o;
            } else {
                TrieBranch b = (TrieBranch)n;
                for ( int i = b.chars.length-1; i > 0; i-- ) {
                    queue.addFirst(b.children[i]);
                }
                if ( b.children[0] != null )
                    queue.addFirst(b.children[0]);
                return next();
            }
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    } // end of inner clas TrieIterator
    
    private TrieBranch root = new TrieBranch();
    private boolean caseSensitive = false;
    
    /**
     * Create a new Trie with the specified case-sensitivity.
     * @param caseSensitive true if the index should be case sensitive for
     * indexed words, false otherwise.
     */
    public Trie(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    /**
     * Indicates if this Trie's index takes the case of letters
     * into account.
     * @return true if the index is case-sensitive, false otherwise
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    /**
     * Add a new word to the trie, associated with the given Tuple.
     * @param word the word to add to the Trie
     * @param t the Tuple associated with the word
     */
    public void addString(String word, Tuple t) {
        TrieLeaf leaf = new TrieLeaf(word,t);
        addLeaf(root, leaf, 0);
    }
    
    /**
     * Remove a word/Tuple pair from the trie.
     * @param word the word to remove
     * @param t the associate Tuple to remove
     */
    public void removeString(String word, Tuple t) {
        removeLeaf(root, word, t, 0);
    }
    
    private final int getIndex(char[] chars, char c) {
        for ( int i=0; i<chars.length; i++ )
            if ( chars[i] == c ) return i;
        return -1;
    }
    
    private final char getChar(String s, int i) {
        char c = ( i < 0 || i >= s.length() ? 0 : s.charAt(i) );
        return ( caseSensitive ? c : Character.toLowerCase(c) );
    }
    
    private final TrieNode equalityCheck(String word, TrieLeaf l) {
        if ( caseSensitive ) {
            return l.word.startsWith(word) ? l : null;
        } else {
            // do our own looping to avoid string allocation for case change
            int len = word.length();
            if ( len > l.word.length() ) return null;
            for ( int i=0; i<len; ++i ) {
                char c1 = Character.toLowerCase(word.charAt(i));
                char c2 = Character.toLowerCase(l.word.charAt(i));
                if ( c1 != c2 ) return null;
            }
            return l;
        }
    }
    
    private boolean removeLeaf(TrieBranch b, String word, Tuple t, int depth) {
        char c = getChar(word, depth);
        int i = getIndex(b.chars, c);
        
        if ( i == -1 ) {
            // couldn't find leaf
            return false;
        } else {
            TrieNode n = b.children[i];
            if ( n instanceof TrieBranch ) {
                TrieBranch tb = (TrieBranch)n;
                boolean rem = removeLeaf(tb, word, t, depth+1);
                if ( rem ) {
                    b.leafCount--;
                    if ( tb.leafCount == 1 )
                        b.children[i] = tb.children[tb.children[0]!=null?0:1];
                }
                return rem;
            } else {
                TrieLeaf nl = (TrieLeaf)n;
                if ( nl.tuple == t ) {
                    b.children[i] = nl.next;
                    if ( nl.next == null )
                        repairBranch(b,i);
                    b.leafCount--;
                    return true;
                } else {
                    TrieLeaf nnl = nl.next;
                    while ( nnl != null && nnl.tuple != t ) {
                        nl = nnl; nnl = nnl.next;
                    }
                    if ( nnl == null )
                        return false; // couldn't find leaf
                    
                    // update leaf counts
                    for ( TrieLeaf tl = (TrieLeaf)n; tl.tuple != t; tl = tl.next )
                        tl.leafCount--;
                    
                    nl.next = nnl.next;
                    b.leafCount--;
                    return true;
                }     
            }
        }
    }
    
    private void repairBranch(TrieBranch b, int i) {
        if ( i == 0 ) {
            b.children[0] = null;
        } else {
            int len = b.chars.length;
            char[] nchars = new char[len-1];
            TrieNode[] nkids = new TrieNode[len-1];
            System.arraycopy(b.chars,0,nchars,0,i);
            System.arraycopy(b.children,0,nkids,0,i);
            System.arraycopy(b.chars,i+1,nchars,i,len-i-1);
            System.arraycopy(b.children,i+1,nkids,i,len-i-1);
            b.chars = nchars;
            b.children = nkids;
        }
    }
    
    private void addLeaf(TrieBranch b, TrieLeaf l, int depth) {
        b.leafCount += l.leafCount;
        
        char c = getChar(l.word, depth);
        int i = getIndex(b.chars, c);
        
        if ( i == -1 ) {
            addChild(b,l,c);
        } else {
            TrieNode n = b.children[i];
            if ( n == null ) {
                // we have completely spelled out the word
                b.children[i] = l;
            } else if ( n instanceof TrieBranch ) {
                // recurse down the tree
                addLeaf((TrieBranch)n,l,depth+1);
            } else {
                // node is a leaf, need to do a split?
                TrieLeaf nl = (TrieLeaf)n;
                if ( i==0 || (caseSensitive ? nl.word.equals(l.word) 
                                  : nl.word.equalsIgnoreCase(l.word)) )
                {
                    // same word, so chain the entries
                    for ( ; nl.next != null; nl = nl.next )
                        nl.leafCount++;
                    nl.leafCount++;
                    nl.next = l;
                } else {
                    // different words, need to do a split
                    TrieBranch nb = new TrieBranch();
                    b.children[i] = nb;
                    addLeaf(nb,nl,depth+1);
                    addLeaf(nb,l,depth+1);
                }
            }
        }
    }
    
    private void addChild(TrieBranch b, TrieNode n, char c) {
        int len = b.chars.length;
        char[] nchars = new char[len+1];
        TrieNode[] nkids = new TrieNode[len+1];
        System.arraycopy(b.chars,0,nchars,0,len);
        System.arraycopy(b.children,0,nkids,0,len);
        nchars[len] = c;
        nkids[len] = n;
        b.chars = nchars;
        b.children = nkids;
    }
    
    /**
     * Look up the given word in this Trie. If a match is found, a TrieNode
     * is returned. This node is the root of a subtree containing all the
     * matches to the query.
     * @param word the word to lookup
     * @return the TrieNode root of the subtree containing all matches. A
     * null value is returned if no match is found.
     */
    public TrieNode find(String word) {
        return (word.length() < 1 ? null : find(word, root, 0));
    }
    
    private TrieNode find(String word, TrieBranch b, int depth) {
        char c = getChar(word, depth);
        int i = getIndex(b.chars, c);
        if ( i == -1 ) {
            return null; // not in trie
        } else if ( word.length()-1 == depth ) {
            return b.children[i]; // end of search
        } else if ( b.children[i] instanceof TrieLeaf ) {
            return equalityCheck(word, (TrieLeaf)b.children[i]);
        } else {
            return find(word, (TrieBranch)b.children[i], depth+1); // recurse
        }
    }
    
} // end of class Trie
