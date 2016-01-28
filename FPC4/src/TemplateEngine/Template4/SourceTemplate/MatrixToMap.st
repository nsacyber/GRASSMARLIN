/*
 * Contains template to take a matrix and provide formatting for it.
 */

MatrixToMap(matrix,var) ::= <<
<matrix:{ row |<var>.put(<row:{ cell |"<cell>"}; separator=", ">);}; separator="\n">
>>
