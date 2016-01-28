/*
 * Template for a java source file.
 */
Class(packageDrtv, importsDrtv, classDclr, globalDclr, interfaceDclr, interfaceGeneric, className, classBody, staticInit, methods) ::= <<
<if(packageDrtv)>package <packageDrtv>;<endif>
<if(importsDrtv)><importsDrtv><endif>
<if(classDclr)><classDclr> <endif>class <className><if(interfaceDclr)> implements <interfaceDclr><if(interfaceGeneric)>\<<interfaceGeneric>\><endif><endif> {
    <globalDclr:{ x | <x> }; separator="\n">
    <if(staticInit)>static {
        <staticInit>
    }
    <endif>

    <if(classBody)><classBody><endif>
    <if(methods)>
    <methods:{ x | <x> }; separator="\n">
    <endif>
}
>>
