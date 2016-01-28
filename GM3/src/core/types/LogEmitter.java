/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.types;

import core.Core.ALERT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/**
 * <pre>
 * A simple LogEmitter
 * </pre>
 */
public class LogEmitter extends Observable {

    List<Log> backLog;
    Map<String, Runnable> events;

    private static final LogEmitter globalEmitter;

    public static class Log {

        public final Class INVOKER;
        public final ALERT TYPE;
        public final Object OBJ;
        public final Long TIME;
        public final List<Log> condensedMessages;

        public Log(Object INVOKER, ALERT TYPE, Object OBJ) {
            if (INVOKER instanceof Class) {
                this.INVOKER = (Class) INVOKER;
            } else {
                this.INVOKER = INVOKER.getClass();
            }
            this.TYPE = TYPE;
            this.OBJ = OBJ;
            this.TIME = System.currentTimeMillis();
            condensedMessages = new ArrayList<>();
        }

        public List<Log> getCondensedMessages() {
            return condensedMessages;
        }

        public void condenseLog(Log log) {
            condensedMessages.add(log);
        }

        public boolean shouldCondense(Log e) {
            return e.TYPE.equals(TYPE) && e.INVOKER.equals(INVOKER) && e.OBJ.toString().equals(OBJ.toString());
        }

        public String toCSV() {
            return String.format("%s, %s, \"%s\"", TIME, TYPE, OBJ.toString());
        }

        @Override
        public String toString() {
            if (condensedMessages.isEmpty()) {
                return OBJ.toString();
            } else {
                return OBJ.toString() + "\t(" + condensedMessages.size() + " more)";
            }
        }
    }

    static {
        globalEmitter = new LogEmitter();
    }

    public static final Supplier<LogEmitter> factory = () -> {
        return LogEmitter.globalEmitter;
    };

    public LogEmitter() {
        backLog = Collections.synchronizedList(new ArrayList<Log>());
        events = Collections.synchronizedMap(new HashMap<String, Runnable>());
    }

    public void emit(Object i, ALERT a, Object o) {
        Log l = new Log(i, a, o);
        backLog.add(l);
        SwingUtilities.invokeLater(() -> {
            setChanged();
            notifyObservers(l);
            if (events.containsKey(o.toString())) {
                events.get(o.toString()).run();
            }
        });
    }

    public void addEvent(String hook, Runnable cb) {
        events.put(hook, cb);
    }

    public void clearLog() {
        backLog.clear();
    }

    @Override
    public synchronized void addObserver(Observer o) {
        super.addObserver(o);
        backLog.forEach(l -> {
            o.update(this, l);
        });
    }

}
