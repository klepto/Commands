package dev.klepto.commands;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.klepto.commands.annotation.Command;
import dev.klepto.commands.annotation.DefaultValue;
import dev.klepto.commands.annotation.Remaining;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static dev.klepto.commands.CommandResult.Type.*;
import static java.util.Arrays.stream;

/**
 * Parses & dispatches text-based commands to methods annotated with {@link Command} annotation.
 *
 * <p>Commands removes the manual-labor involved in parsing text-based commands by automatically parsing strings into
 * java objects, selecting appropriate command keys and applying domain-specific filters.</p>
 *
 * <h2>Commands Contract</h2>
 * <p>In-order for command methods to function, a method annotated with {@link Command} annotation must:
 *   <ul>
 *     <li>Be a non-static, public method.</li>
 *     <li>Have return type of void.</li>
 *     <li>Have it's first parameter match the context type (usually user, author or origin).</li>
 *     <li>Contain only unique command keys (either set by method name or {@link Command} annotation).</li>
 *   </ul>
 * </p>
 *
 * <p>If any rules of the contract are broken, {@link Commands} will throw a {@link IllegalArgumentException} when
 * calling {@link Commands#register(Object)}. Due to annotation-driven configuration not being compile-time safe, it's
 * highly advised to register your command containers during boot-time.</p>
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@RequiredArgsConstructor
public class Commands<T> {

    private final Class<T> contextType;
    private final Splitter delimiter;
    private final Map<Class<?>, Function<String, ?>> parsers;
    private final Map<Class<? extends Annotation>, CommandFilter<?, ?>> filters;
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
     * @throws IllegalArgumentException if command method contract is violated, {@see Commands} documentation
     */
    private void register(Object container, Method method) throws IllegalArgumentException {
        if (method.getReturnType() != void.class) {
            registerError(container, method, "Command method must have return type of 'void'.");
        }

        val command = method.getAnnotation(Command.class);
        val keyStream = (command.keys().length == 0 ? Stream.of(method.getName()) : Stream.of(command.keys()));
        val keys = keyStream.map(String::toLowerCase).collect(toImmutableSet());
        if (keys.stream().anyMatch(commandMethods.keySet()::contains)) {
            registerError(container, method, "Duplicate command key.");
        }

        val invoker = invokerProvider.provideInvoker(container, method);
        val helpMessage = command.help().isEmpty() ? null : command.help();

        val filterStream = Stream.concat(
                filters.keySet().stream().filter(container.getClass()::isAnnotationPresent),
                filters.keySet().stream().filter(method::isAnnotationPresent)
        );

        val filters = filterStream.map(annotationType -> {
            Annotation annotation = method.isAnnotationPresent(annotationType)
                    ? method.getAnnotation(annotationType) : container.getClass().getAnnotation(annotationType);
            return new CommandMethodFilter(annotation, this.filters.get(annotationType));
        }).collect(toImmutableSet());

        val methodParameters = method.getParameters();
        if (methodParameters.length == 0 || methodParameters[0].getType() != contextType) {
            registerError(container, method, "First parameter of command method must match the context type.");
        }

        val parameters = Stream.of(methodParameters).skip(1).map(methodParameter -> {
            Class<?> type = methodParameter.getType();
            if (!parsers.containsKey(type)) {
                registerError(container, method, "Command parameter of type '" + type.getName() + "' does not have a parser.");
            }

            String defaultValue = methodParameter.isAnnotationPresent(DefaultValue.class)
                    ? methodParameter.getAnnotation(DefaultValue.class).value() : null;
            return new CommandParameter(type, defaultValue);
        }).collect(toImmutableList());

        val requiredParameters = (int) parameters.stream().map(CommandParameter::getDefaultValue).filter(Objects::isNull).count();
        val limitedParameters = methodParameters[methodParameters.length - 1].isAnnotationPresent(Remaining.class);
        val commandMethod = new CommandMethod(invoker, keys, helpMessage, filters, parameters, requiredParameters, limitedParameters);
        keys.forEach(key -> commandMethods.put(key, commandMethod));
    }

    private void registerError(Object container, Method method, String message) {
        throw new IllegalArgumentException("Error while registering command method '"
                + container.getClass().getSimpleName() + "." + method.getName() + "': " + message);
    }

    /**
     * Attempts to execute a command for a given text-based message.
     *
     * @param context the message context (usually author/user)
     * @param message the message string
     * @return the command result indicating if command was successfully executed
     */
    public CommandResult execute(T context, String message) {
        if (message.trim().isEmpty()) {
            return new CommandResult(KEY_NOT_FOUND);
        }

        List<String> keyAndArguments = Lists.newArrayList(delimiter.split(message));
        val key = keyAndArguments.get(0).toLowerCase();
        if (!commandMethods.containsKey(key)) {
            return new CommandResult(KEY_NOT_FOUND);
        }

        val commandMethod = commandMethods.get(key);
        if (commandMethod.isLimitedParameters() && !commandMethod.getParameters().isEmpty()) {
            val splitter = delimiter.limit(commandMethod.getParameters().size() + 1);
            keyAndArguments = Lists.newArrayList(splitter.split(message));
        }

        val arguments = ImmutableList.copyOf(keyAndArguments.subList(1,keyAndArguments.size()));
        if (commandMethod.getFilters().stream().anyMatch(filter -> !filter.filter(context, key, arguments))) {
            return new CommandResult(NO_ACCESS);
        }

        if (arguments.size() < commandMethod.getRequiredParameterCount()) {
            return new CommandResult(ARGUMENT_MISMATCH, commandMethod.getHelpMessage());
        }

        val argumentsQueue = Lists.newLinkedList(arguments);
        try {
            val parameters = Lists.newArrayList();
            commandMethod.getParameters().forEach(parameter -> {
                val stringValue = parameter.getDefaultValue() != null ? parameter.getDefaultValue() : argumentsQueue.poll();
                val value = parsers.get(parameter.getType()).apply(stringValue);
                parameters.add(value);
            });

            commandMethod.getInvoker().invoke(context, parameters.toArray());
        } catch (Exception cause) {
            return new CommandResult(ERROR, commandMethod.getHelpMessage(), cause);
        }

        return new CommandResult(SUCCESS);
    }

}
