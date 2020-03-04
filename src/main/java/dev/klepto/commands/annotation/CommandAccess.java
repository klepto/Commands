package dev.klepto.commands.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a default access level to all command methods within the annotated type.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandAccess {

    /**
     * The base-line access level for all commands within this class. Note that any access level provided in
     * {@link Command} annotation will override this option.
     */
    int value();

}
