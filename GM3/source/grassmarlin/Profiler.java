package grassmarlin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Profiler {
    public static class Block implements AutoCloseable {
        private final String name;
        private final long nsStart;
        private Block(final String name) {
            this.name = name;
            nsStart = System.nanoTime();
        }

        @Override
        public void close() {
            final long nsDuration = System.nanoTime() - nsStart;
            Profiler.recordBlock(name, nsDuration);
        }
    }

    protected static class Record {
        private long nsTotal;
        private int cntCalls;

        public Record() {
            this.nsTotal = 0L;
            this.cntCalls = 0;
        }

        public void addTime(final long nsDuration) {
            this.nsTotal += nsDuration;
            this.cntCalls++;
        }

        @Override
        public String toString() {
            return String.format("%16dns/%9d (%16dns avg)", this.nsTotal, this.cntCalls, this.nsTotal / this.cntCalls);
        }
    }
    protected static class Counter {
        private final AtomicLong counter;
        private final Set<Thread> threads;

        public Counter() {
            this.counter = new AtomicLong(1);
            this.threads = new ConcurrentSkipListSet<>((thread1, thread2) -> thread1.getName().compareTo(thread2.getName()));
            this.threads.add(Thread.currentThread());
        }

        public void increment() {
            this.counter.incrementAndGet();
            this.threads.add(Thread.currentThread());
        }

        @Override
        public String toString() {
            return String.format("                   %9d %s", this.counter.get(), this.threads.stream().map(thread -> thread.getName()).sorted().collect(Collectors.joining()));
        }
    }
    private static final Map<String, Record> records = new HashMap<>();
    private static final Map<String, Counter> counters = new HashMap<>();

    private static void recordBlock(final String name, final long nsDuration) {
        Record record;
        synchronized(records) {
            record = records.get(name);
            if (record == null) {
                record = new Record();
                records.put(name, record);
            }
        }
        record.addTime(nsDuration);
    }

    public static Block start(final String name) {
        return new Block(name);
    }
    public static void count(final String name) {
        synchronized(counters) {
            final Counter value = counters.get(name);
            if(value == null) {
                counters.put(name, new Counter());
            } else {
                value.increment();
            }
        }
    }
    public static void reset() {
        synchronized(records) {
            synchronized(counters) {
                records.clear();
                counters.clear();
            }
        }
    }
    public static void dumpProfilerData() {
        synchronized(records) {
            synchronized(counters) {
                for (Map.Entry<String, Record> entry : records.entrySet().stream().sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey())).collect(Collectors.toList())) {
                    final String line = String.format("%48s [%s]", entry.getKey(), entry.getValue());
                    System.out.println(line);
                    Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, line);
                }

                for(Map.Entry<String, Counter> entry : counters.entrySet().stream().sorted((o1, o2) -> o1.getKey().compareTo(o2.getKey())).collect(Collectors.toList())) {
                    final String line = String.format("%48s [%s]", entry.getKey(), entry.getValue());
                    System.out.println(line);
                    Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, line);
                }
            }
        }
    }
}
