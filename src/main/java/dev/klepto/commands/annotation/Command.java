package dev.klepto.commands.annotation;

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

    String[] keys() default {};
    String help() default "";
    int access() default -1;

}
