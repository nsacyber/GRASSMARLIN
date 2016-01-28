package prefuse.util.collections;

import java.util.BitSet;
import java.util.Comparator;
import java.util.NoSuchElementException;

/**
 * Sorted map implementation using bit vectors to map from boolean keys to
 * int values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class BooleanIntBitSetMap implements BooleanIntSortedMap {

    private BitSet m_true = new BitSet();
    private BitSet m_false = new BitSet();
    
    public BooleanIntBitSetMap() {
    }
    
    public boolean firstKey() {
        return false;
    }

    public boolean lastKey() {
        return true;
    }

    public boolean containsKey(boolean key) {
        BitSet set = key ? m_true : m_false;
        return set.cardinality()>0;
    }

    public IntIterator valueRangeIterator(boolean fromKey, boolean fromInc,
                                          boolean toKey, boolean toInc)
    {
        if ( !fromInc && !toInc ) {
            // empty iterator
            return new BitSetIterator(null);
        } else if ( fromKey==toKey || !toInc ) {
            return new BitSetIterator(fromKey ? m_true : m_false);
        } else if ( !fromInc ) {
            return new BitSetIterator(toKey ? m_true : m_false);
        } else {
            return new BitSetIterator(fromKey ? m_true : m_false,
                                      toKey ? m_true : m_false);
        }
    }

    public LiteralIterator keyIterator() {
        return new BitSetIterator(m_false, m_true);
    }

    public LiteralIterator keyRangeIterator(boolean fromKey, boolean fromInc,
                                            boolean toKey, boolean toInc)
    {
        if ( !fromInc && !toInc ) {
            // empty iterator
            return new BitSetIterator(null);
        } else if ( fromKey==toKey || !toInc ) {
            return new BitSetIterator(fromKey ? m_true : m_false);
        } else if ( !fromInc ) {
            return new BitSetIterator(toKey ? m_true : m_false);
        } else {
            return new BitSetIterator(fromKey ? m_true : m_false,
                                      toKey ? m_true : m_false);
        }
    }

    public int get(boolean key) {
        BitSet set = key ? m_true : m_false;
        return set.nextSetBit(0);
    }

    public int remove(boolean key) {
        BitSet set = key ? m_true : m_false;
        int idx = set.length()-1;
        set.clear(idx);
        return idx;
    }

    public int remove(boolean key, int value) {
        BitSet set = key ? m_true : m_false;
        if ( set.get(value) ) {
            set.clear(value);
            return value;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    public int put(boolean key, int value) {
        BitSet set = key ? m_true : m_false;
        boolean ret = set.get(value);
        set.set(value);
        return ret ? value : Integer.MIN_VALUE;
    }

    public int getMinimum() {
        if ( m_false.cardinality() > 0 ) {
            return m_false.nextSetBit(0);
        } else if ( m_true.cardinality() > 0 ) {
            return m_true.nextSetBit(0);
        } else {
            return Integer.MIN_VALUE;
        }
    }

    public int getMaximum() {
        int idx = m_true.length()-1;
        return idx>0 ? idx : m_false.length()-1;
    }

    public int getMedian() {
        int fsize = m_false.cardinality();
        int tsize = m_true.cardinality();
        if ( fsize == 0 && tsize == 0 )
            return Integer.MIN_VALUE;

        int med = (fsize+tsize)/2;
        BitSet set = ( fsize>tsize ? m_false : m_true );
        for( int i=set.nextSetBit(0), j=0; i>=0; 
             i=set.nextSetBit(i+1), ++j )
        {
            if ( j == med ) return i;
        }
        // shouldn't ever happen
        return Integer.MIN_VALUE;
    }

    public int getUniqueCount() {
        int count = 0;
        if ( m_false.cardinality() > 0 ) ++count;
        if ( m_true.cardinality() > 0 )  ++count;
        return count;
    }

    public boolean isAllowDuplicates() {
        return true;
    }

    public int size() {
        return m_true.cardinality() + m_false.cardinality();
    }

    public boolean isEmpty() {
        return m_true.isEmpty() && m_false.isEmpty();
    }

    public Comparator comparator() {
        return DefaultLiteralComparator.getInstance();
    }

    public void clear() {
        m_true.clear();
        m_false.clear();
    }

    public boolean containsValue(int value) {
        return m_false.get(value) || m_true.get(value);
    }


    public IntIterator valueIterator(boolean ascending) {
        if ( !ascending ) {
            return new BitSetIterator(m_true, m_false);
        } else {
            return new BitSetIterator(m_false, m_true);
        }
    }
    
    public class BitSetIterator extends IntIterator {

        private BitSet m_cur, m_next;
        private int m_val = -1;
        
        public BitSetIterator(BitSet set) {
            this(set, null);
        }
        public BitSetIterator(BitSet first, BitSet second) {
            m_cur = first;
            m_next = second;
            if ( first == null ) {
                m_val = -2;
            } else {
                m_val = -1;
                advance();
            }
        }
        private void advance() {
            int idx = m_cur.nextSetBit(m_val+1);
            if ( idx < 0 ) {
                if ( m_next != null ) {
                    m_cur = m_next;
                    m_next = null;
                    m_val = -1;
                    advance();
                } else {
                    m_val = -2;
                }
                return;
            } else {
                m_val = idx;
            }
        }
        public int nextInt() {
            if ( m_val < 0 )
                throw new NoSuchElementException();
            int retval = m_val;
            advance();
            return retval;
        }
        public boolean nextBoolean() {
            if ( m_cur == m_true ) {
                advance();
                return true;
            } else if ( m_cur == m_false ) {
                advance();
                return false;
            } else {
                throw new NoSuchElementException();
            }
        }
        public boolean hasNext() {
            return m_val >= 0;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

} // end of class BooleanIntBitSetMap
