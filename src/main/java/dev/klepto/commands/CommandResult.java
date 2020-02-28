package dev.klepto.commands;

import lombok.Value;

@Value
public class CommandResult {

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

    public enum Type {
        KEY_NOT_FOUND,
        NO_ACCESS,
        ARGUMENT_MISMATCH,
        ERROR,
        SUCCESS
    }

}
