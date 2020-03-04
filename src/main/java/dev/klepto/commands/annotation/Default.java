package dev.klepto.commands.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a default value for command method parameter.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {

    /**
     * The default argument value to be parsed as a command parameter.
     */
    String value();

}
