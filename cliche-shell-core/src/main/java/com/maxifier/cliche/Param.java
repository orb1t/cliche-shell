/*
 * This file is part of the Cliche project, licensed under MIT License.
 * See LICENSE.txt file in root folder of Cliche sources.
 */

package com.maxifier.cliche;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for parameters of Command-marked methods.
 * This annotation is of particular usefullness, because Java 5 Reflection doesn't have access
 * to declared parameter names (there's simply no such information stored in classfile).
 * You must at least provide name attribute, others being optional.
 * @author ASG
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
    /**
     * Parameter name.
     * Should (1) reflect the original Java parameter name, (2) be short and descriptive to the user.
     * Recommendations: "number-of-nodes", "user-login", "coefficients".
     * @return The name ascribed to annotated method parameter.
     */
    String name();

    /**
     * One-sentence description of the parameter.
     * It is recommended that you always set it.
     * @return "Short description attribute" of the annotated parameter.
     */
    String description() default "";

    /**
     * Specify the Class which will implement auto-complete logic. The class should return list of possible options by terms.
     *
     * @return command's header or "" if not set.
     */
    Class<? extends CommandCompleter> completer() default Command.DEFAULT_COMPLETER.class;

}
