package dev.klepto.commands;

import dev.klepto.commands.annotation.Command;

/**
 * Represents a call-site for invoking a method annotated with {@link Command} annotation.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
public interface CommandInvoker {

    /**
     * Invokes a command method.
     *
     * @param context the command context (usually origin/author)
     * @param parameters the parsed command parameters
     */
    void invoke(Object context, Object... parameters) throws Exception;

}
