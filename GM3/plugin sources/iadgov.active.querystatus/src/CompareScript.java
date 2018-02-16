package iadgov.active.querystatus;

import grassmarlin.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.atomic.AtomicInteger;

public class CompareScript {
    public enum Result {
        Identical,
        UpdateUnchanged,
        UpdateChanged,
        Error
    }

    private final static AtomicInteger NextIndex = new AtomicInteger(0);

    private final static String SCRIPT_TEMPLATE = "" +
            "function %s(lhs, rhs) {" +
            "%s" +
            "}";

    private final static ScriptEngine engine;

    static {
        engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    private final String name;
    private final String text;

    public CompareScript(final String text) {
        this.name = "compare" + Integer.toString(NextIndex.getAndIncrement());
        this.text = String.format(SCRIPT_TEMPLATE, name, text);
        try {
            CompareScript.engine.eval(this.text);
        } catch(ScriptException ex) {
            //Log the error and fail hard.
            Logger.log(Logger.Severity.ERROR, "Invalid Script: %s", ex.getMessage());
            throw new RuntimeException(ex);
        }
    }


    public Result compare(final byte[] lhs, final byte[] rhs) {
        try {
            final Object result = ((Invocable) CompareScript.engine).invokeFunction(name, lhs, rhs);
            if(result instanceof Result) {
                return (Result)result;
            } else if(result instanceof Integer) {
                final Integer i = (Integer)result;
                switch(i) {
                    case 0: return Result.Identical;
                    case -1: return Result.UpdateUnchanged;
                    case 1: return Result.UpdateChanged;
                }
            } else if(result instanceof Boolean) {
                return (Boolean)result ? Result.Identical : Result.UpdateChanged;
            }
            return Result.Error;
        } catch(ScriptException | NoSuchMethodException ex) {
            Logger.log(Logger.Severity.ERROR, "Unable to evaluate script: %s", ex.getMessage());
            return Result.Error;
        }
    }
}
