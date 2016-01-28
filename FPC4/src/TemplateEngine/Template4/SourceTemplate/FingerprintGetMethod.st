/*
 * Creates the {@link core.fingerprint.Operation} getMethod(String name) method
 * used in {@link core.fingerprint.Fingerprint} classes to expose their entry methods.
 */
FingerprintGetMethod(methodNames, returnType) ::= <<
FunctionalOperation\<<returnType>\> op;
switch(name) {
    <methodNames:{ x| case "<x>": op = this::<x>; break; }; separator="\n">
    default: op = null;
}
return op;
>>