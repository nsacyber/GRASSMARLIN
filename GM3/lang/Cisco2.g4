/*
* Bro1Log.g4
* Lexer/Parser grammar for BRO2 logs
*/

grammar CiscoSC;

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
    
    public void onData( Consumer<HashMap<String,String>> cb ) {
        logEmitter = Optional.of(cb);
    }    
}

document :
    ( section | comment | emptyLine )*
    EOF;

section : device show command (
    /* ent= unmatched*? */ {
    });

device : DEVICE
        {
        };

show : SHOW
        {
        };

command : (version | config | macAddress | arp | interfaces)
        {
        };

version : VERSION
        {
        };

config : RUNNING_CONFIG
        {
        };

macAddress : MACADDRESS
        {
        };

arp : IP_ARP
        {
        };

interfaces : INTERFACES END (junk END)+
        {
        };


comment : COMMENT END { };

emptyLine : END { };

junk : JUNK { };

INTERFACES : 'in' ('t' ('e' ('r' ('f' ('a' ('c' ('e' ('s')?)?)?)?)?)?)?)?;
IP_ARP : 'ip' WS+ 'ar' 'p'?;
RUNNING_CONFIG : 'ru' ('n' ('n' ('i' ('n' ('g' ('-' ('c' ('o' ('n' ('f' ('i' ('g')?)?)?)?)?)?)?)?)?)?)?)?;
VERSION : 've' ('r' ('s' ('i' ('o' ('n')?)?)?)?)?;

DEVICE    : TextChar+;
SHOW      : '#sh' ('o' ('w')?)?;
MACADDRESS : 'mac' WS_OR_DASH 'address-table';
ARP : (('ip' WS)? 'arp');

QSTRING : '"' ~('"')* '"';
SEMICOLON : ';';

END       : [\r]?[\n];

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

WS        : [ \t\r] -> skip;
WS_OR_DASH : (WS | '-') -> skip;

COMMENT : '!.*';

JUNK : ~('\r' | '\n')+?;