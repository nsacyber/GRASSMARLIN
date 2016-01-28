grammar CiscoShow;

command: END*(username(show_interfaces|unknown))+ EOF;

username: IDENT_B;

show_interfaces  : iface_group*; 

iface_group: IDENT_IFACE END clause* csv*;
    clause : (statement (SEP statement)*) END;
        statement: (name (COLON|IS) value+)*;

    csv: ( name | value ) SEP* END;

        
unknown: 'show'.*?;

/* common rules */
name    : NAMES+ | iface | TEXT+;
unit    : WORD'/'WORD;
ratio   : INT'/'INT;
value   : IDENT* (L_SEP IDENT* value R_SEP) | (UPDOWN | address | date | IDENT);
date    : INT( SLASH INT)+;
iface   : IFACE;
address : ip | mac;
    ip  : IPv4 | IPv6;
    mac : MAC;
    
IPv4 : ADDR ('/' INT)*;
IPv6 : HEX(':'HEX)+;


L_SEP: ["(];
R_SEP: [")];
ADDR  : (INT DOT)+INT;
MAC   : HEX DOT HEX DOT HEX;
IFACE : IDENT( SLASH INT )+;
NAMES : 'MTU'|'Internet'|'Hardware'|'line'|'protocol'|'address'|'Description';
UPDOWN: 'up'|'down';
IS    : ' is ';
COLON : ':';
DOT   : '.';
SLASH : '/';
SEP   : ',';
HEX   : [a-fA-F0-9]+;

IDENT_IFACE: 'show in''t'?'e'?'r'?'f'?'a'?'c'?'e'?'s'?;
IDENT_B: IDENT'#';
IDENT: ALPHA ALNUM+;
TEXT : (WORD|INT)+;
WORD : ALPHA+;
INT  : NUM+;
WS   : [ \t\r-;] -> skip;
END  : [\r]?[\n];

fragment
ALNUM : [a-zA-Z0-9];

fragment
NUM   : [0-9];

fragment
ALPHA : [a-zA-Z];