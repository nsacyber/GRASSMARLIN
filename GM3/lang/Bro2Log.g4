/*
* Bro1Log.g4
* Lexer/Parser grammar for BRO2 logs
*/

grammar Bro2Log;

@header {
	import java.util.ArrayList;
	import java.util.List;
	import java.util.HashMap;
    import java.util.Optional;
    import java.util.function.Function;
    import java.util.function.Consumer;
}

@members {
    Optional<Consumer<HashMap<String,String>>> logEmitter = Optional.empty();
    Function<String,Boolean> filter = Bro2LogLexer::fieldFilter;
	List<String> column_type = new ArrayList<>();
	List<String> column_id = new ArrayList<>();
    String empty_symbol = "(empty)"; // a typical default
    String unset_symbol = "-"; // a typical default
    int column_count = 0;
    
    public void onData( Consumer<HashMap<String,String>> cb ) {
        logEmitter = Optional.of(cb);
    }
    
    static Boolean fieldFilter( String s ) {
        return true;
    }
    
    public void setFilter(Function<String,Boolean> filter) {
        this.filter = filter;
    }
    
    void checkBadHeaders() {
        if(  column_id.isEmpty() ) {
            throw  new ParseCancellationException("Could not parse #Field or #Types header");
        } else if( column_id.size() != column_type.size() ) {
            throw new ParseCancellationException("Malformed column identifiers");
        }
    }
    
    // l - list to add to
    // i - index of the column 'column_id.get(i); ...'
    // content - token content
    void typeOrSet(HashMap<String,String> map, int i, String content) {
        if( empty_symbol.equals( content ) ) return; 
        if( unset_symbol.equals( content ) ) return;
        String id = column_id.get(i); // should we check bounds or let if fail as "malformed"
        
        if( filter.apply( column_id.get(i) ) )
            map.put( column_id.get(i), content);
    }
    
}

document @after{
    checkBadHeaders();
}:  header
    log*
    footer*
    EOF;

header throws ParseCancellationException
@after {
    checkBadHeaders();
}: (((fields|types)|directive)* END)+;

log :(
    ent=entry {
        logEmitter.ifPresent(cb->cb.accept($ent.ret));
    })* END;

footer : directive* END;

fields    : FIELDS fieldList;
types     : TYPES typeList;
directive : (emptyField | unsetField | normalField);

normalField : DIRECTIVE IDENT;
unsetField  : DIR_UNSET ident=IDENT { unset_symbol=$ident.getText(); };
emptyField  : DIR_EMPTY ident=IDENT { empty_symbol=$ident.getText(); };

entry returns[ HashMap<String,String> ret ]
@init {
    HashMap<String,String> ret = new HashMap<>();
    int index = 0;
}
@after {
    $ret = ret;
} : (
    ident=IDENT {
        typeOrSet( ret, index++, $ident.getText() );
    })+;

typeList :(
    ident=IDENT {
        column_type.add($ident.getText());
    })+;

fieldList :(
    ident=IDENT {
        column_id.add($ident.getText());
    })+;


FIELDS    : '#fields';
TYPES     : '#types';

DIRECTIVE : '#'TextChar+;
DIR_UNSET : '#unset_field';
DIR_EMPTY : '#empty_field';
IDENT     : TextChar+;

END       : [\r]?[\n];
WS        : [ \t\r] -> skip;

fragment
	TextChar:[a-zA-Z0-9]
			| [-/\\,()_?:.\[\]]	
			| '\u00B7'          // bold DOT
			| '\u0300'..'\u036F'// spec chars
			| '\u203F'..'\u2040'
			| '\u2070'..'\u218F'
			| '\u2C00'..'\u2FEF'
			| '\u3001'..'\uD7FF'
			| '\uF900'..'\uFDCF'
			| '\uFDF0'..'\uFFFD'
			;