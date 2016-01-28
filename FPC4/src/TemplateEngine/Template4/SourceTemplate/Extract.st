/*
 * Code block which extracts specified bytes and places then within the return details.
 */

Extract(Name, From, To, MaxLength, Endian, PostProcess, Body) ::= <<
ret.put("<Name>", payload.extract<if(Endian)>Little<endif>(<From>, <To>, <MaxLength>), <PostProcess>);
<if(Body)><Body><endif>
>>
