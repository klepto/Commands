package dev.klepto.commands;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import dev.klepto.commands.annotation.Command;
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

@RequiredArgsConstructor
public class Commands {

    private final Class<?> contextType;
    private final Splitter delimiter;
    private final Map<Class<?>, Function<String, ?>> parsers;
    private final CommandInvokerProvider invokerProvider;
    private final Map<String, CommandMethod> listeners = new HashMap<>();

    public void register(Object listener) {
        stream(listener.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Command.class))
                .forEach(method -> register(listener, method));
    }

    private void register(Object listener, Method method) {
        val methodName = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
        val contextName = contextType.getName();
        checkArgument(method.getReturnType() == void.class,
                "Command method  '" + methodName + "' must have return type of 'void'.");

        val command = method.getAnnotation(Command.class);
        val invoker = invokerProvider.provideInvoker(listener, method);
        val keys = command.keys().length == 0 ? ImmutableSet.of(method.getName()) : ImmutableSet.copyOf(command.keys());
        listeners.keySet().stream().filter(keys::contains).findAny().ifPresent(key -> {
            throw new IllegalArgumentException("Command key '" + key + "' is already assigned to another listener.");
        });

        val helpMessage = command.help().isEmpty() ? null : command.help();
        val accessLevel = command.access() < 0 ? 0 : command.access();
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
                .filter(parameter -> parameter.getDefaultValue() == null) .count();

        val listenerMethod = new CommandMethod(invoker, keys, helpMessage, accessLevel,
                ImmutableList.copyOf(parameters), requiredParameters);
        keys.forEach(key -> listeners.put(key, listenerMethod));
    }

    public CommandResult execute(Object context, String message) {
        return execute(context, 0, message);
    }

    public CommandResult execute(Object context, int accessLevel, String message) {
        val command = message.toLowerCase().trim();
        if (command.isEmpty()) {
            return new CommandResult(KEY_NOT_FOUND);
        }

        val keyAndArguments = Lists.newArrayList(delimiter.split(message));
        val key = keyAndArguments.get(0);
        if (!listeners.containsKey(key)) {
            return new CommandResult(KEY_NOT_FOUND);
        }

        val listener = listeners.get(key);
        if (listener.getAccessLevel() > accessLevel) {
            return new CommandResult(NO_ACCESS, listener.getHelpMessage());
        }

        val arguments = keyAndArguments.stream().skip(1).collect(toCollection(LinkedList::new));
        if (arguments.size() < listener.getRequiredParameterCount()) {
            return new CommandResult(ARGUMENT_MISMATCH, listener.getHelpMessage());
        }

        try {
            val parameters = Lists.newArrayList();
            listener.getParameters().forEach(parameter -> {
                String stringValue = !arguments.isEmpty() ? arguments.poll() : parameter.getDefaultValue();
                Object value = parsers.get(parameter.getType()).apply(stringValue);
                parameters.add(value);
            });

            listener.getInvoker().invoke(context, parameters);
        } catch (Exception cause) {
            return new CommandResult(ERROR, listener.getHelpMessage(), cause);
        }

        return new CommandResult(SUCCESS);
    }

}
