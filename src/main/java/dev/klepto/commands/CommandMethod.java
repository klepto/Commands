package dev.klepto.commands;

import dev.klepto.commands.annotation.Command;
import lombok.Value;

import java.util.List;
import java.util.Set;

/**
 * Data container for a method annotated with {@link Command} annotation. Command configurations are extracted with
 * help of annotations & reflection, this class acts as a container for extracted configuration data for later use.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Value
public class CommandMethod {

    CommandInvoker invoker;
    Set<String> keys;
    String helpMessage;
    Set<CommandMethodFilter> filters;

    List<CommandParameter> parameters;
    int requiredParameterCount;
    boolean limitedParameters;

}
