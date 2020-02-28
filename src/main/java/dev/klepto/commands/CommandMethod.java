package dev.klepto.commands;

import dev.klepto.commands.annotation.Command;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Configuration container for a method annotated with {@link Command} annotation.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Value
public class CommandMethod {

    CommandInvoker invoker;
    Set<String> keys;
    String helpMessage;
    int accessLevel;

    List<CommandParameter> parameters;
    int requiredParameterCount;

}
