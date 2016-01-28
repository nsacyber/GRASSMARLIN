package TemplateEngine.Template4;

import TemplateEngine.Fingerprint3.AndThen;

/**
 * Created by BESTDOG on 11/13/2015.
 *
 * Interface for interacting with "Function" templates,
 * {@link TemplateEngine.Fingerprint3.MatchFunction}
 * {@link TemplateEngine.Fingerprint3.ByteJumpFunction},
 * {@link TemplateEngine.Fingerprint3.ByteTestFunction}
 * {@link TemplateEngine.Fingerprint3.IsDataAtFunction}
 * {@link TemplateEngine.Fingerprint3.Anchor}
 *
 */
public interface FunctionTemplate extends NestedBlock, ClassAdapter {

    /**
     * An issue with the composition of the Fingerprint3.xsd does not permit strongly typed Objects as members of
     * the un-marshalled JAXB object.
     *
     * All Operations are setup by passing a simple Object and the class handling it should know
     * what to properly cast it to.
     * @param obj Object to cast to either {@link TemplateEngine.Fingerprint3.MatchFunction}, {@link TemplateEngine.fingerprint3.ByteJumpFunction},
     *            {@link TemplateEngine.Fingerprint3.ByteTestFunction}, {@link TemplateEngine.Fingerprint3.IsDataAtFunction}, {@link TemplateEngine.fingerprint3.Anchor}
     */
    AndThen generateFunction(Object obj);

}
