/*
 * Method stub for testing the length of a packets payload and its length from the cursor.
 */

IsDataAtFunction(Offset, Relative, Body) ::= <<


if(payload.size() > <if(Relative || Offset)><if(Relative)>cursor.get()<endif><if(Offset)><if(Relative)>+<endif><Offset><endif><else>0<endif>) {
    <if(Body)><Body><endif>
}
>>
