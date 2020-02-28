package dev.klepto.commands;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import dev.klepto.commands.annotation.Command;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Commands {

    private final Splitter delimiter;
    private final Map<Class<?>, Function<String, ?>> parsers;
    private final CommandInvokerProvider invokerProvider;

    private final Map<String, CommandMethod> listeners = new HashMap<>();

    public void register(Object listener) {
        Arrays.stream(listener.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(Command.class))
                .forEach(method -> register(listener, method));
    }

    private void register(Object listener, Method method) {

    }

    public boolean execute(Object context, String message) {
        return execute(context, 0, message);
    }

    public boolean execute(Object context, int accessLevel, String message) {
        val command = message.toLowerCase().trim();
        if (command.isEmpty()) {
            return false;
        }

        val keyAndArguments = Lists.newArrayList(delimiter.split(message));
        val key = keyAndArguments.get(0);
        if (!listeners.containsKey(key)) {
            return false;
        }

        val listener = listeners.get(key);
        if (listener.getAccessLevel() > accessLevel) {
            return false;
        }

        val arguments = keyAndArguments.stream().skip(1).collect(Collectors.toCollection(LinkedList::new));
        if (arguments.size() < listener.getRequiredParameters().size()) {
            return false;
        }

        val parameters = Lists.newArrayList();
        listener.getParameters().forEach(parameter -> {
            String stringValue = !arguments.isEmpty() ? arguments.poll() : parameter.getDefaultValue();
            Object value = parsers.get(parameter.getType()).apply(stringValue);
            parameters.add(value);
        });

        listener.getInvoker().invoke(context, parameters);
        return true;
    }

}
