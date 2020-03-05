package dev.klepto.commands.annotation;

import dev.klepto.commands.CommandResult;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a configuration for a command listener method.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    /**
     * Different command keys mapped to this command method. If left empty, method name is used as a command key.
     */
    String[] keys() default {};

    /**
     * Help message to help users understand how to use this command. The value of this is returned in an unsuccessful
     * {@link CommandResult} and it's up to the API consumer to determine how to display this message to the user.
     */
    String help() default "";

}
