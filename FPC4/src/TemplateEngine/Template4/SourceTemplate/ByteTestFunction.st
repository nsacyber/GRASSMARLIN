/*
 * A method/code block template for the ByteTestFunction.
 * Tests a bytes inequality.
 *
 * "Endian" attribute is handled in the Template class for this .st file.
 *
 */

ByteTestFunction(PostOffset, Relative, Endian, Offset, Bytes, Operator, Val, Body) ::= <<


offset = <if(Relative || Offset)><if(Relative)>cursor.get()<endif><if(Offset)><if(Relative)>+<endif><Offset><endif><else>0<endif>;
if( payload.getInt(offset<if(Bytes)>, <Bytes><endif>, <if(Endian)>false<else>true<endif>) <Operator> <Val>) {
    <if(PostOffset)>cursor.forward(<PostOffset>);<endif>
    <if(Body)><Body><endif>
}
>>