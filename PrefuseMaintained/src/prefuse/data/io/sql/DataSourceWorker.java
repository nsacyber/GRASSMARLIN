package prefuse.data.io.sql;

import java.util.logging.Logger;

import prefuse.data.Table;
import prefuse.data.io.DataIOException;
import prefuse.util.PrefuseConfig;
import prefuse.util.StringLib;
import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Worker thread that asynchronously handles a queue of jobs, with each job
 * responsible for issuing a query and processing the results. Currently
 * involves just a single thread, in the future this may be expanded to
 * thread pool for greater concurrency.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see DatabaseDataSource
 */
public class DataSourceWorker extends Thread {

    private static Logger s_logger
        = Logger.getLogger(DataSourceWorker.class.getName());
    
    // TODO: in future, may want to expand this to a thread pool
    private static DataSourceWorker s_instance;
    
    private static CopyOnWriteArrayList s_queue;
    
    /**
     * Submit a job to the worker thread.
     * @param e an {@link DataSourceWorker.Entry} instance that contains
     * the parameters of the job.
     */
    public synchronized static void submit(Entry e)
    {
        // perform lazily initialization as needed
        if ( s_queue == null )
            s_queue = new CopyOnWriteArrayList();
        if ( s_instance == null )
            s_instance = new DataSourceWorker();
        
        // queue it up
        s_queue.add(e);
        
        // wake up a sleepy thread
        synchronized ( s_instance ) {
            s_instance.notify();
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new DataSourceWorker.
     */
    private DataSourceWorker() {
        super("prefuse_DatabaseWorker");
        
        int priority = PrefuseConfig.getInt("data.io.worker.threadPriority");
        if ( priority >= Thread.MIN_PRIORITY && 
             priority <= Thread.MAX_PRIORITY )
        {
            this.setPriority(priority);
        }
        this.setDaemon(true);
        this.start();
    }
    
    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {
        while ( true ) {
            Entry e = null;
            synchronized ( s_queue ) {
                if ( s_queue.size() > 0 )
                    e = (Entry)s_queue.remove(0);    
            }
            
            if ( e != null ) {
                try {
                    if ( e.listener != null ) e.listener.preQuery(e);
                    e.ds.getData(e.table, e.query, e.keyField, e.lock);
                    if ( e.listener != null ) e.listener.postQuery(e);
                } catch ( DataIOException dre ) {
                    s_logger.warning(dre.getMessage() + "\n" 
                        + StringLib.getStackTrace(dre));
                }
            } else {
                // nothing to do, chill out until notified
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ex) { }
            }
        }
    }
    
    /**
     * Stores the parameters of a data query and processing job.
     * @author <a href="http://jheer.org">jeffrey heer</a>
     */
    public static class Entry {
        /**
         * Create a new Entry.
         * @param ds the DatabaseDataSource to query
         * @param table the Table for storing the results
         * @param query the query to issue
         * @param keyField the key field that should be used to identify
         * when duplicate results occur
         * @param lock an optional lock to synchronize on when processing
         * data and adding it to the Table
         * @param listener an optional callback listener that allows
         * notifications to be issued before and after query processing
         */
        public Entry(DatabaseDataSource ds, Table table, String query,
                     String keyField, Object lock, Listener listener)
        {
            this.ds = ds;
            this.table = table;
            this.query = query;
            this.keyField = keyField;
            this.lock = lock;
            this.listener = listener;
        }
        
        /** The DatabaseDataSource to query. */
        DatabaseDataSource ds;
        /** An optional callback listener that allows
         * notifications to be issued before and after query processing. */
        Listener listener;
        /** The Table for storing the results. */
        Table  table;
        /** The query to issue. */
        String query;
        /** The key field that should be used to identify
         * when duplicate results occur. */
        String keyField;
        /** An optional lock to synchronize on when processing
         * data and adding it to the Table. */
        Object lock;
    }
    
    /**
     * Listener interface for receiving notifications about the status of
     * a submitted data query and processing job.
     * @author <a href="http://jheer.org">jeffrey heer</a>
     */
    public static interface Listener {
        /**
         * Notification that the query is about to be issued.
         * @param job the current job being processed
         */
        public void preQuery(DataSourceWorker.Entry job);
        /**
         * Notification that the query processing has just completed.
         * @param job the current job being processed
         */
        public void postQuery(DataSourceWorker.Entry job);
    }
    
} // end of class DataSourceWorker
