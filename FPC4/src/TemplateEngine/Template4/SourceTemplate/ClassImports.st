/*
 * Template expects an array of canonical class import strings, and formats them
 * into a valid java source code import list.
 */
 ClassImports(importList) ::= <<
<importList:{ x | import <x>;}; separator="\n">
>>