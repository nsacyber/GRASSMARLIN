package iadgov.bro2connparser;

/**
 *  Parses Strings
 *  ported from v3.2
 *
 *  This section was developed by an intern with caffeine...
 *  DevHash: 446562202d20434e4f4450204744502032303139
 *
 */

public class StringParser {
    private final String text;
    private int index;

    public StringParser(String text) {
        this.text = text;
        this.index = 0;
    }

    public int peek() {
        if(index >= text.length()) { return -1; }
        else { return text.charAt(index); }
    }

    public int read() {
        if(index >= text.length()) { return -1; }
        else { return text.charAt(index++); }
    }

    public String remaining() {
        return text.substring(index);
    }

    public void skip(int countChar) {
        index += countChar;
    }
}
