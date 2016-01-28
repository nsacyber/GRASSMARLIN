/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

grammar CiscoShow;

command: ((show_interface | show_arp | unknown) ENDL)+ EOF;

/**
 * SHOW ARP COMMAND
 */
show_arp: username SHOW_ARP_COMMAND sa_body;
sa_body: .*?;

/**
 * SHOW INTERFAC ECOMMAND "si"
 * parse tree for show commands
 */
show_interface: username SHOW_INTERFACE_COMMAND si_body;
si_body: si_section+;
si_section: si_header
          (( si_mac
          | si_ip
          | sentence))+
          ;

si_header: interface_name sentence;
si_mac   : SI_Hardware sentence;
si_ip    : SI_Address sentence;

SI_Hardware: 'Hardware';
SI_Address : 'Internet';

sentence : (WORD|SEP)+ ENDL;

/* COMMON RULES */
mac      : 'asdsa';
iface    : WORD SEP NUM;
unknown  : UKNOWN_COMMAND;
username : WORD SEP;
interface_name: IFACE;

text     : WORD+;
/* COMMON TOKENS */
IFUPDOWN: 'is' ('up'|'down');
IFACE: (ALNUM+'-'?)+ ('/' ALNUM)+ ;

             
/* COMMAND TOKENS */
SHOW_INTERFACE_COMMAND: 'show in''t'?'e'?'r'?'f'?'a'?'c'?'e'?'s'?;
SHOW_ARP_COMMAND: 'show a''r'?'p'?;
UKNOWN_COMMAND: .*?;

/* NORMAL TOKENS */
SEP  : [,:.-/#];
WORD : TextChar+;
NUM  : [0-9]+;
ENDL : [\r]?[\n];

fragment
ALNUM : [a-zA-Z0-9];

fragment
TextChar:
    [a-zA-Z0-9]
    | [-/\\,()_?:.\[\]";]	
    | '\u00B7'          // bold DOT
    | '\u0300'..'\u036F'// spec chars
    | '\u203F'..'\u2040'
    | '\u2070'..'\u218F'
    | '\u2C00'..'\u2FEF'
    | '\u3001'..'\uD7FF'
    | '\uF900'..'\uFDCF'
    | '\uFDF0'..'\uFFFD'
    ;