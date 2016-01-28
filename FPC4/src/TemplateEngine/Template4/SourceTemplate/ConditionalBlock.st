/*
 * A conditional code block. If 'x' then do 'y'
 */
ConditionalBlock(expression, body) ::= <<
if(<expression>) {
    <body>
}
>>