package prefuse.util.display;

import java.util.Arrays;

import prefuse.util.ArrayLib;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.ItemSorter;

/**
 * A helper class representing rendering and picking queues. This functionality
 * is listed separately to keep the Display implementation a bit cleaner.
 * Fields are public and used directly by a single Display instance.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RenderingQueue {

    private static final int DEFAULT_SIZE = 256;
    
    public ItemSorter   sort   = new ItemSorter();
    
    // rendering queue
    public VisualItem[] ritems  = new VisualItem[DEFAULT_SIZE];
    public int[]        rscores = new int[DEFAULT_SIZE];
    public int          rsize   = 0;
    
    // picking queue
    public VisualItem[] pitems  = new VisualItem[DEFAULT_SIZE];
    public int[]        pscores = new int[DEFAULT_SIZE];
    public int          psize   = 0;
    public boolean      psorted = false;
    
    // buffer queues for use in sorting, these prevent continual re-allocation
    transient static VisualItem[] items_buf;
    transient static int[]        scores_buf;
    
    /**
     * Clear both rendering and picking queues.
     */
    public void clear() {
        Arrays.fill(ritems, 0, rsize, null);
        Arrays.fill(pitems, 0, psize, null);
        rsize = 0;
        psize = 0;
        psorted = false;
    }
    
    /**
     * Clears the rendering queue and resizes internal arrays to a small size.
     * This should help reclaim used memory.
     */
    public void clean() {
    	clear();
        sort = new ItemSorter();
    	ritems = new VisualItem[DEFAULT_SIZE];
    	rscores = new int[DEFAULT_SIZE];
    	pitems = new VisualItem[DEFAULT_SIZE];
    	pscores = new int[DEFAULT_SIZE];
    	items_buf = null;
    	scores_buf = null;
    }
    
    /**
     * Add an item to the rendering queue.
     * @param item the item to add
     */
    public void addToRenderQueue(VisualItem item) {
        if ( ritems.length == rsize ) {
            int capacity = (3*ritems.length)/2 + 1;
            VisualItem[] q = new VisualItem[capacity];
            int[] s = new int[capacity];
            System.arraycopy(ritems, 0, q, 0, rsize);
            System.arraycopy(rscores, 0, s, 0, rsize);
            ritems = q;
            rscores = s;
        }
        ritems[rsize] = item;
        rscores[rsize++] = (sort != null ? sort.score(item) : 0);
    }
    
    /**
     * Add an item to the picking queue.
     * @param item the item to add
     */
    public void addToPickingQueue(VisualItem item) {
        if ( pitems.length == psize ) {
            int capacity = (3*pitems.length)/2 + 1;
            VisualItem[] q = new VisualItem[capacity];
            int[] s = new int[capacity];
            System.arraycopy(pitems, 0, q, 0, psize);
            System.arraycopy(pscores, 0, s, 0, psize);
            pitems = q;
            pscores = s;
        }
        pitems[psize] = item;
        pscores[psize++] = (sort != null ? sort.score(item) : 0);
        psorted = false;
    }
    
    /**
     * Sort the rendering queue.
     */
    public void sortRenderQueue() {
        sort(ritems, rscores, rsize);
    }
    
    /**
     * Sort the picking queue. 
     */
    public void sortPickingQueue() {
        sort(pitems, pscores, psize);
        psorted = true;
    }
    
    /**
     * Sort a queue of items based upon an array of ordering scores. 
     */
    private void sort(VisualItem[] items, int[] scores, int size) {
        if ( sort == null ) return;
        // first check buffer queues
        if ( items_buf == null || items_buf.length < size ) {
            items_buf = new VisualItem[items.length];
            scores_buf = new int[scores.length];
        }
        // now sort
        ArrayLib.sort(scores, items, scores_buf, items_buf, 0, size);
    }
    
} // end of class RenderingQueue
