/*
 * Template for a basic java method
    Typical method for fingerprinting will contain these variables,
        int offset;
        int length;
        int location;
        String string;
        Matcher matcher;
    Make sure to set these to their failure value on success of any expression
 */
ClassMethod(name, body, initVars, argList, returnType, genericType, override=false) ::= <<


<if(override)>@Override<endif>
public <returnType><if(genericType)>\<<genericType>\><endif> <name>(<if(argList)><argList:{ x |<x>}; separator=", "><endif>) {
    <if(initVars)><initVars:{ x |<x>}; separator=";\n">;<endif>
    <body>
}
>>
