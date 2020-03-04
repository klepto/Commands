package dev.klepto.commands;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import dev.klepto.commands.annotation.Command;
import dev.klepto.commands.annotation.CommandAccess;
import dev.klepto.commands.annotation.Default;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static dev.klepto.commands.CommandResult.Type.*;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;

/**
 * Parses & dispatches text-based commands to methods annotated with {@link Command} annotation.
 *
 * <p>Commands removes the manual-labor involved in parsing text-based commands by automatically parsing strings into
 * java objects, selecting appropriate command keys and checking access level of the user.</p>
 *
 * <h2>Commands Contract</h2>
 * <p>In-order for command methods to function, a method annotated with {@link Command} annotation must:
 *   <ul>
 *     <li>Be a non-static, public method.</li>
 *     <li>Have return type of void.</li>
 *     <li>Have it's first parameter match the context type (usually user, author or origin).</li>
 *     <li>Contain only unique command keys (either set by method name or {@link Command} annotation).</li>
 *     <li>If using {@link Default} annotation, only use it on last method parameters.</li>
 *   </ul>
 * </p>
 *
 * <p>If any rules of the contract are broken, {@link Commands} will throw a {@link IllegalArgumentException} when
 * calling {@link Commands#register(Object)}. Due to annotation-driven configuration not being compile-time safe, it's
 * highly suggested to register your command containers during boot-time.</p>
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@RequiredArgsConstructor
public class Commands {

    private final Class<?> contextType;
    private final Splitter delimiter;
    private final Map<Class<?>, Function<String, ?>> parsers;
    private final CommandInvokerProvider invokerProvider;
    private final Map<String, CommandMethod> commandMethods = new HashMap<>();

    /**
     * Registers all command methods within given container object. A method is considered a command method only-if it's
     * non-static, public method and is annotated with {@link Command} annotation. A container object can be any object
     * containing command methods.
     *
     * @param container the command container object
     */
    public void register(Object container) {
        stream(container.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Command.class))
                .forEach(method -> register(container, method));
    }

    /**
     * Registers a given command method. Parsers all necessary command method information using annotations &
     * reflection. All required method information get stored into {@link CommandMethod} object and gets put into
     * {@code key -> CommandMethod} map for later use.
     *
     * @param container the command method container
     * @param method    the  command method
     * @throws IllegalArgumentException if command method contract is violated, {@see Commands} documentation.
     */
    private void register(Object container, Method method) throws IllegalArgumentException {
        val methodName = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        val contextName = contextType.getName();
        checkArgument(method.getReturnType() == void.class,
                "Command method  '" + methodName + "' must have return type of 'void'.");

        val command = method.getAnnotation(Command.class);
        val invoker = invokerProvider.provideInvoker(container, method);
        val keys = command.keys().length == 0 ? ImmutableSet.of(method.getName()) : ImmutableSet.copyOf(command.keys());
        commandMethods.keySet().stream().filter(keys::contains).findAny().ifPresent(key -> {
            throw new IllegalArgumentException("Command key '" + key + "' is already assigned to another container.");
        });

        val helpMessage = command.help().isEmpty() ? null : command.help();
        val defaultAccessLevel = container.getClass().isAnnotationPresent(CommandAccess.class)
                ? container.getClass().getAnnotation(CommandAccess.class).value() : 0;
        val accessLevel = command.access() < 0 ? defaultAccessLevel : command.access();
        val parameters = Lists.<CommandParameter>newLinkedList();
        boolean contextFound = false;
        for (Parameter parameter : method.getParameters()) {
            if (!contextFound) {
                checkArgument(parameter.getType() == contextType,
                        "First parameter of command method '" + methodName +
                                "' must match the context type of '" + contextName + "'.");
                contextFound = true;
                continue;
            }

            val type = parameter.getType();
            val defaultValue = parameter.isAnnotationPresent(Default.class)
                    ? parameter.getAnnotation(Default.class).value() : null;

            checkArgument(parsers.containsKey(type),
                    "Command parameter of type '" + type.getName() + "' does not have a parser.");
            if (parameters.size() > 0 && defaultValue == null) {
                checkArgument(parameters.getLast().getDefaultValue() == null,
                        "Only last parameters of a command method '" + methodName + "' can have a default value.");
            }

            parameters.add(new CommandParameter(parameter.getType(), defaultValue));
        }

        val requiredParameters = (int) parameters.stream()
                .filter(parameter -> parameter.getDefaultValue() == null).count();

        val commandMethod = new CommandMethod(invoker, keys, helpMessage, accessLevel,
                ImmutableList.copyOf(parameters), requiredParameters);
        keys.forEach(key -> commandMethods.put(key, commandMethod));
    }

    /**
     * Attempts to execute a command for a given text-based message with default access level.
     * @see Commands#execute(Object, int, String) 
     *
     * @param context the message context
     * @param message the message string
     * @return the command result indicating if command was successfully executed
     */
    public CommandResult execute(Object context, String message) {
        return execute(context, 0, message);
    }

    /**
     * Attempts to execute a command for a given text-based message.
     *
     * @param context the message context (usually author/user)
     * @param accessLevel the access level
     * @param message the message string
     * @return the command result indicating if command was successfully executed
     */
    public CommandResult execute(Object context, int accessLevel, String message) {
        val command = message.toLowerCase().trim();
        if (command.isEmpty()) {
            return new CommandResult(KEY_NOT_FOUND);
        }

        val keyAndArguments = Lists.newArrayList(delimiter.split(message));
        val key = keyAndArguments.get(0);
        if (!commandMethods.containsKey(key)) {
            return new CommandResult(KEY_NOT_FOUND);
        }

        val commandMethod = commandMethods.get(key);
        if (commandMethod.getAccessLevel() > accessLevel) {
            return new CommandResult(NO_ACCESS, commandMethod.getHelpMessage());
        }

        val arguments = keyAndArguments.stream().skip(1).collect(toCollection(LinkedList::new));
        if (arguments.size() < commandMethod.getRequiredParameterCount()) {
            return new CommandResult(ARGUMENT_MISMATCH, commandMethod.getHelpMessage());
        }

        try {
            val parameters = Lists.newArrayList();
            commandMethod.getParameters().forEach(parameter -> {
                String stringValue = !arguments.isEmpty() ? arguments.poll() : parameter.getDefaultValue();
                Object value = parsers.get(parameter.getType()).apply(stringValue);
                parameters.add(value);
            });

            commandMethod.getInvoker().invoke(context, parameters.toArray());
        } catch (Exception cause) {
            return new CommandResult(ERROR, commandMethod.getHelpMessage(), cause);
        }

        return new CommandResult(SUCCESS);
    }

}
