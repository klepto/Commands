package dev.klepto.commands;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Interface for creating custom command filter annotations. Intended for implementation of domain-specific checks
 * before executing the command, such as checking user privileges, implementing command cool-downs, etc.
 *
 * @param <C> the command context type
 * @param <T> the filter annotation type
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 * @see CommandsBuilder#addFilter(Class, CommandFilter)
 */
public interface CommandFilter<C, T extends Annotation> {

    /**
     * Filters the command execution. Command execution will only proceed if this filter returns {@code true}.
     *
     * @param context    the command context
     * @param annotation the filter annotation
     * @param key        the command key
     * @param arguments  the command arguments
     * @return true if command should execute, false if command execution should be denied
     */
    boolean filter(C context, T annotation, String key, List<String> arguments);

}
