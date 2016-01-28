/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule.parser;

// core
import core.importmodule.Bro2Import;
import core.importmodule.Trait;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lang.Bro2LogLexer;
import lang.Bro2LogParser;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * This reader uses the Bro2LogParser to pull select columns from each row.
 * It could produce a trait vector that can be used to create logical topology.
 */
public final class Bro2Reader extends AbstractReader {

    /**
     * Number of lines to ignore when getting the line count.
     */
    private static final int SLACK_LINES = 8;
    /**
     * timestamp token
     */
    private final String TS_FIELD = "ts";
    /**
     * source ip token
     */
    private final String ORIG_H_FIELD = "id.orig_h";
    /**
     * source port token
     */
    private final String ORIG_P_FIELD = "id.orig_p";
    /**
    /**
     * source bytes token
     */
    private final String ORIG_BYTES_FIELD = "orig_bytes";
    /**
     * remote ip token
     */
    private final String RESP_H_FIELD = "id.resp_h";
    /**
     * remote port token
     */
    private final String RESP_P_FIELD = "id.resp_p";
    /**
     * protocol token
     */
    private final String PROTO_FIELD = "proto";
    /** Value used when a port number is parsed incorrectly. */
    public static final Integer BAD_INT_VALUE = -1;
    
    private boolean hasErrors;
    private long entries;
    
    /**
     * Constructs this reader with the verbosity indicated.
     * @param verbose True to set verbose, else false.
     */
    public Bro2Reader(boolean verbose) {
        super();
        this.setVerbose(verbose);
        entries = 0l;
        hasErrors = false;
        this.setVerbose(false);
    }
    
    /**
     * Constructs with this readers log level as NOT verbose by default;
     */
    public Bro2Reader() {
        this(false);
    }

    public int getEntryCount() {
        return (int) entries;
    }
    
    @Override
    public boolean test(String path) {
        return test(path, true);
    }

    @Override
    public boolean test(String path, Boolean verbose) {
        Bro2Reader inst = new Bro2Reader();
        inst.setVerbose(verbose);
        try {
            List l = new ArrayList<>();
            inst.accept(new Bro2Import(path), l);
            return !inst.hasErrors && !l.isEmpty();
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    protected void handleError(Exception ex) {
        if (isVerbose()) {
            super.handleError(ex);
        }
    }

    public Byte[] asIPv4(String s) {
        Byte[] addr = new Byte[]{0, 0, 0, 0};
        if (s != null && s.length() > "0.0.0.0".length()) {
            String[] s2 = s.split("\\.");
            for (int i = 0; i < 4 && i < s2.length; i++) {
                addr[i] = Integer.valueOf(s2[i]).byteValue();
            }
        }
        return addr;
    }

    public void map( Map<String,String> rawContent, List<Map<Trait,Object>> targetContent ) {
        Map<Trait,Object> content = new HashMap<>();
        rawContent.entrySet().forEach( entry -> 
            map( entry, content )
        );
        if( !content.isEmpty() ) {
            content.put(Trait.FRAME_NO, entries++);
            if( !content.containsKey(Trait.PACKET_SIZE) ) {
                content.put(Trait.PACKET_SIZE, BAD_INT_VALUE);
            }
            targetContent.add(content);
        } else {
            this.handleError(new Exception("Malformed or blank entry in log"));
        }
    }
    
    public void map(Map.Entry<String, String> e1, Map<Trait, Object> ret) {
        switch (e1.getKey()) {
            case TS_FIELD:
                int i = e1.getValue().indexOf(".");
                ret.put(
                        Trait.TIMESTAMP,
                        Long.parseLong(e1.getValue().substring(0, i == -1 ? e1.getValue().length() : i))
                );
                break;
            /* source IP */
            case ORIG_H_FIELD:
                if (e1.getValue().contains(":")) {
                    ret.clear();
                    return;
                }
                ret.put(
                        Trait.IPv4_SRC,
                        asIPv4(e1.getValue())
                );
                break;
            case ORIG_BYTES_FIELD:
                ret.put(
                        Trait.PACKET_SIZE,
                        getIntegerOrDefault(e1.getValue())
                        );
                break;
            case ORIG_P_FIELD:
                ret.put(
                        Trait.PORT_SRC,
                        getIntegerOrDefault(e1.getValue())
                );
                /* destination IP */
                break;
            case RESP_H_FIELD:
                ret.put(
                        Trait.IPv4_DST,
                        asIPv4(e1.getValue())
                );
                break;
            case RESP_P_FIELD:
                ret.put(
                        Trait.PORT_DST,
                        getIntegerOrDefault(e1.getValue())
                );
                break;
            case PROTO_FIELD:
                ret.put(
                        Trait.PROTO,
                        getProtoNumber( e1.getValue() )
                );
            default:
        }
    }
    
    private Integer getIntegerOrDefault(String s) {
        Integer ret;
        try {
            ret = Integer.valueOf(s);
        } catch( java.lang.NumberFormatException ex ) {
            ret = BAD_INT_VALUE;
            if( isVerbose() ) {
                Logger.getLogger(Bro2Reader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return ret;
    }
    
    private Integer getProtoNumber( String s ) {
        if( "udp".equalsIgnoreCase(s) ) {
            return 17;
        }
        if( "tcp".equalsIgnoreCase(s) ) {
            return 6;
        }
        return -1;
    }

    public void accept(Bro2Import t, List<Map<Trait,Object>> lines) throws FileNotFoundException {
        if (t == null) {
            if (isVerbose()) {
                Logger.getLogger(Bro2Reader.class.getName()).log(Level.SEVERE, null, new NullPointerException("IngestItem is null"));
            }
            return;
        }
        if( !t.exists() ) {
            throw new java.io.FileNotFoundException("Import item does not exist.");
        }
        try {
            
            CharStream in = new ANTLRFileStream(t.getSafeCanonicalPath());
            Bro2LogLexer lex = new Bro2LogLexer(in);
            
            CommonTokenStream tokens = new CommonTokenStream(lex);
            Bro2LogParser parser = new Bro2LogParser(tokens);
            parser.setBuildParseTree(false); // cuts down slightly on memory usage.
            /* has no effect if above is false, interesting for tuning. */
            //parser.setTrimParseTree(true); // cuts down slightly on memory usage.
            
            if( !isVerbose() ) {
                lex.removeErrorListeners();
                parser.removeErrorListeners();
            }
            
            if( isVerbose() ) {
                System.out.println("Parser constructed");
            }
            
            // this strategy will bail if a parse error occurs throwing a ParseCancellationException
            parser.setErrorHandler(new BailErrorStrategy() {
                @Override
                public void reportError(Parser recognizer, RecognitionException e) {
                    if (isVerbose()) {
                        super.reportError(recognizer, e);
                    }
                }
            });

            parser.setFilter(this::defaultFilterMethod);
            parser.onData( line -> {
                map(line, lines);
            });
            
            try {
            
                if( isVerbose() ) {
                    System.out.println("Starting parser");
                }
                
                parser.document();

                if( isVerbose() ) {
                    System.out.println("Parser complete");
                }
                
            } catch (ParseCancellationException | IndexOutOfBoundsException ex) {
                if (malformedInputHandler != null) {
                    malformedInputHandler.accept(ex);
                } else {
                    handleError(ex);
                }
                hasErrors = true;
            }

        } catch (IOException ex) {
            handleError(ex);
            hasErrors = true;
        }
    }

    @Override
    public int getLineCount() {
        return super.getLineCount() - Bro2Reader.SLACK_LINES;
    }
    
//    @Override
//    public void setMalformedInputCB(Consumer<Exception> cb) {
//        super.setMalformedInputCB(cb);
//    }
    private Boolean defaultFilterMethod(String s) {
        if (s != null && !s.isEmpty()) {
            switch (s) {
                case TS_FIELD:
                case ORIG_H_FIELD:
                case ORIG_P_FIELD:
                case RESP_H_FIELD:
                case RESP_P_FIELD:
                case PROTO_FIELD:
                case ORIG_BYTES_FIELD:
                    return Boolean.TRUE;
                default:
                    return Boolean.FALSE;
            }
        }
        return Boolean.FALSE;
    }

}
