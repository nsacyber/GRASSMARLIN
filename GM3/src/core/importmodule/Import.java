/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

// java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An enum to for each type of import, note LIVE is special for live PCAP importing
 */
public enum Import {
    CiscoShow("CISCO Show"),
    None("None"),
    Pcap("PCAP"),
    PcapNG(),
    Bro2("Bro2 Log"),
    LIVE(),
    GM3("GrassMarlin Data File");

    /** some are initialized statically to catch an error encountered only during TestCases / use with JUnit */
    static {
        try {
            CiscoShow.constructor = CiscoImport::new;
            Pcap.constructor = PCAPImport::new;
            PcapNG.constructor = PCAPNGImport::new;
            Bro2.constructor = Bro2Import::new;
            GM3.constructor = Gm3Import::new;
            None.constructor = DummyImport::new;
        } catch( Exception | Error ex ) {
            Logger.getLogger(Import.class.getName()).log(Level.FINEST, "Error in initialization.", ex);
        }
    }
    
    public static final List<Import> list;
    
    static {
        ArrayList<Import> l = new ArrayList<>(Arrays.asList(Import.values()));
        l.removeIf(Import::isHidden);
        list = Collections.unmodifiableList(l);
    }
 
    /** The constructor of the ImportItem that is used to auto-construct the item as the type indicated by the user */
    private Function<String,ImportItem> constructor;
    /** Text to display to the user */
    public final String displayText;

    Import(String displayText, Function<String,ImportItem> constructor) {
        this.displayText = displayText;
        this.constructor = constructor;
    }
    
    Import(String text) {
        this(text, null);
    }
    
    Import() {
        this(null, null);
    }
    
    public boolean isHidden() {
        return this.displayText == null;
    }
    
    public ImportItem newItem(String path) {
        if( this.constructor == null ) {
            throw new java.lang.Error(String.format("%s has no default constructor.", name()));
        }
        return this.constructor.apply(path);
    }
    
    @Override
    public String toString() {
        return displayText;
    }

}
