/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
grammar RuggedConfig;

@header  {}
@members {}

document :
    router_config
    router_con_detail+
;

router_config: (SECTION_SEP config)+ SECTION_END;
config: .*?;

router_con_detail: router_ident ;

router_ident: IDENT_RT IDENT_SHOW IDENT_ARP arp_table;

arp_table : arp_table_header arp_table_row+;
arp_table_header: TABLE_HEADER;
arp_table_row: proto address ENDL;

proto: IDENT;
address: (IPv4|IPv6);

TABLE_HEADER: 'Protocol' 'Address' 'Age (min)' 'Hardware Addr' 'Type' 'Interface';

SECTION_SEP: '!';
SECTION_END: 'end';

IPv4: (Int'.')+Int;
IPv6: (Int':')+Int;

IDENT_ARP : 'ip arp';
IDENT_SHOW: 'show';
IDENT_RT  : IDENT'#';
IDENT     : TextChar+;
ENDL      : [\r]?[\n];
WS        : [ \t\r] -> skip;

fragment
	TextChar: ~([\n]);
fragment
    Int : [0-9];