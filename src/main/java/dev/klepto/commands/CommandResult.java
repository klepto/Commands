package dev.klepto.commands;

import dev.klepto.commands.annotation.Command;
import lombok.Value;

/**
 * Acts as a return type for {@link Commands#execute(Object, int, String)}. Contains information such as if command
 * executed successfully. May contain other relevant information such as {@link Exception} that occurred during command
 * execution, or help message (if provided by {@link Command} annotation) allowing you to notify your users of a correct
 * usage for the command they tried to access.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Value
public class CommandResult {

    public enum Type {
        /**
         * Indicates that no method was found for given command key.
         */
        KEY_NOT_FOUND,

        /**
         * Indicates that user did not meet access level criteria for accessing this command.
         */
        NO_ACCESS,

        /**
         * Indicates that user did not supply enough arguments for command to be executed.
         */
        ARGUMENT_MISMATCH,

        /**
         * Indicates that exception occurred, either during argument parsing or command method invocation.
         */
        ERROR,

        /**
         * Indicates that command executed successfully.
         */
        SUCCESS
    }

    Type type;
    String helpMessage;
    Exception cause;

    public CommandResult(Type type) {
        this(type, null);
    }

    public CommandResult(Type type, String helpMessage) {
        this(type, helpMessage, null);
    }

    public CommandResult(Type type, String helpMessage, Exception cause) {
        this.type = type;
        this.helpMessage = helpMessage;
        this.cause = cause;
    }

}
