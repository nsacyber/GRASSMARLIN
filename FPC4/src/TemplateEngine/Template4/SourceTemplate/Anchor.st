/*
 * Anchor is a method for users to delcare the location of a cursor.
 *
 * This can be used to set the cursors around a portion or data field.
 * It may also be used to anchor the cursors some negative offset from the back of the payload.
 */

Anchor(Cursor, Position, Relative, Offset, Body) ::= <<
<if(Position || Offset)>

cursor.set<Cursor>(<if(Position)><Position><endif><if(Offset)><if(Position)>+<endif><Offset><endif>);
<elseif(Relative || Offset)>

cursor.set<Cursor>(<if(Relative)>cursor.get()<endif><if(Offset)><if(Relative)>+<endif><Offset><endif>);
<endif>
<Body>
>>
