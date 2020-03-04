package dev.klepto.commands;

import dev.klepto.commands.annotation.Default;
import lombok.Value;

/**
 * A data container for command method's parameter. Contains the parameter type and default value if annotated with
 * {@link Default} annotation.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Value
public class CommandParameter {

    Class<?> type;
    String defaultValue;

}
