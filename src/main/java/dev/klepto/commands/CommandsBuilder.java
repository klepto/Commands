package dev.klepto.commands;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.function.Function;

public final class CommandsBuilder {

    public static CommandsBuilder forType(Class<?> contextType) {
        return new CommandsBuilder(contextType);
    }

    private final Class<?> contextType;
    private Splitter delimiter;
    private CommandInvokerProvider invokerProvider;
    private Map<Class<?>, Function<String, ?>> parsers = Maps.newHashMap();

    private CommandsBuilder(Class<?> contextType) {
        this.contextType = contextType;
        defaults();
    }

    private void defaults() {
        setDelimiter(Splitter.on(" "));
        setInvokerProvider(ReflectiveCommandInvoker::new);
        addParser(byte.class, Byte::parseByte);
        addParser(Byte.class, Byte::parseByte);
        addParser(short.class, Short::parseShort);
        addParser(Short.class, Short::parseShort);
        addParser(int.class, Integer::parseInt);
        addParser(Integer.class, Integer::parseInt);
        addParser(float.class, Float::parseFloat);
        addParser(Float.class, Float::parseFloat);
        addParser(double.class, Double::parseDouble);
        addParser(Double.class, Double::parseDouble);
        addParser(boolean.class, Boolean::parseBoolean);
        addParser(Boolean.class, Boolean::parseBoolean);
        addParser(char.class, CHARACTER_PARSER);
        addParser(Character.class, CHARACTER_PARSER);
        addParser(String.class, Function.identity());
    }

    public CommandsBuilder setDelimiter(Splitter delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    public CommandsBuilder setInvokerProvider(CommandInvokerProvider invokerProvider) {
        this.invokerProvider = invokerProvider;
        return this;
    }

    public <T> CommandsBuilder addParser(Class<T> type, Function<String, T> parser) {
        parsers.put(type, parser);
        return this;
    }

    public Commands build() {
        return new Commands(contextType, delimiter, ImmutableMap.copyOf(parsers), invokerProvider);
    }

    private static final Function<String, Character> CHARACTER_PARSER = string -> {
        if (string.length() > 1) {
            throw new IllegalArgumentException("Expected a character, but received a string.");
        }
        return string.charAt(0);
    };

}
