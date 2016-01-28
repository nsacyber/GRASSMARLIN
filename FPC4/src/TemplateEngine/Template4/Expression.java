package TemplateEngine.Template4;


import TemplateEngine.Template4.Structure.VariableDeclaration;

import java.util.List;

/**
 * Created by BESTDOG on 11/12/2015.
 * <p>
 * An expression is a list of variables.
 * <p>
 * Its also a bunch of binary or unary operation to those variables, in abstract, but we handle that in Template4 specific code.
 */
public interface Expression {
    /**
     * Get a list of variables within this Expression.
     * @return List of expression variables.
     */
    List<VariableDeclaration> getVariables();

}
