package dev.klepto.commands;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * A builder for creating {@link Commands} instances. Ensures runtime type safety & immutability of command parser
 * settings. Builder instances can be re-used; it is safe to call {@code build()} multiple times to create multiple
 * instances of command parsers.
 *
 * @param <T> the command context type (usually command message author/user)
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
public final class CommandsBuilder<T> {

    /**
     * Creates a new builder for given context type.
     *
     * @param contextType the command context type (usually command message author/user)
     * @param <T>         the generic type for command context to ensure type-safety
     * @return a new instance of builder
     */
    public static <T> CommandsBuilder<T> forType(Class<T> contextType) {
        return new CommandsBuilder<>(contextType);
    }

    private final Class<T> contextType;
    private Splitter delimiter;
    private CommandInvokerProvider invokerProvider;
    private Map<Class<?>, Function<String, ?>> parsers = Maps.newHashMap();
    private Map<Class<? extends Annotation>, CommandFilter<?, ?>> filters = Maps.newHashMap();

    /**
     * Creates a new builder with default configuration.
     *
     * @param contextType the context type
     */
    private CommandsBuilder(Class<T> contextType) {
        this.contextType = contextType;
        defaults();
    }

    /**
     * Internal method for setting default builder settings.
     */
    private void defaults() {
        setDelimiter(' ');
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

    /**
     * Sets a given character as the command argument delimiter.
     *
     * @param delimiter the argument delimiter
     * @return this builder instance
     */
    public CommandsBuilder<T> setDelimiter(char delimiter) {
        return setDelimiter(Splitter.on(delimiter));
    }

    /**
     * Sets a given string as the command argument delimiter.
     *
     * @param delimiter the argument delimiter
     * @return this builder instance
     */
    public CommandsBuilder<T> setDelimiter(String delimiter) {
        return setDelimiter(Splitter.on(delimiter));
    }

    /**
     * Sets a given regex pattern as the command argument delimiter.
     *
     * @param delimiter the argument delimiter
     * @return this builder instance
     */
    public CommandsBuilder<T> setDelimiter(Pattern delimiter) {
        return setDelimiter(Splitter.on(delimiter));
    }

    /**
     * Sets the command argument delimiter. Default delimiter is a space character.
     *
     * @param delimiter the argument delimiter
     * @return this builder instance
     */
    private CommandsBuilder<T> setDelimiter(Splitter delimiter) {
        this.delimiter = delimiter;
        return this;
    }

    /**
     * Sets the command invoker provider. The default invoker calls command methods via reflection, but if reflection
     * performance is a concern, this allows for custom implementation of command call-sites such as method handles,
     * custom bytecode generation or use of third-party libraries like Reflectasm.
     *
     * @param invokerProvider the command invoker provider
     * @return this builder instance
     */
    public CommandsBuilder<T> setInvokerProvider(CommandInvokerProvider invokerProvider) {
        this.invokerProvider = invokerProvider;
        return this;
    }

    /**
     * Adds an argument type parser. Main functionality of {@link Commands} is that it automatically parses string
     * arguments into java objects. This method allows you to add your own domain-specific parsers to avoid manual
     * parsing in your command listener methods.
     *
     * @param type   the parser type
     * @param parser the parser implementation
     * @param <P>    generic parameter for parser type
     * @return this builder instance
     */
    public <P> CommandsBuilder<T> addParser(Class<P> type, Function<String, P> parser) {
        parsers.put(type, parser);
        return this;
    }

    /**
     * Adds a custom command filter annotation. Intended for implementation of domain-specific checks before executing
     * the command, such as checking user privileges, implementing command cool-downs, etc. This behavior can be
     * achieved by creating any annotation and implementing {@link CommandFilter} interface with the annotation type.
     *
     * @param type   the annotation type
     * @param filter the command filter for given annotation type
     * @param <A>    generic parameter for annotation type
     * @return this builder instance
     */
    public <A extends Annotation> CommandsBuilder<T> addFilter(Class<A> type, CommandFilter<T, A> filter) {
        filters.put(type, filter);
        return this;
    }

    /**
     * Creates a new instance of {@link Commands} using this builder's settings.
     *
     * @return a newly-created instance of {@link Commands}
     */
    public Commands<T> build() {
        return new Commands<>(
                contextType,
                delimiter,
                ImmutableMap.copyOf(parsers),
                ImmutableMap.copyOf(filters),
                invokerProvider
        );
    }

    /**
     * Character parser for internal use.
     */
    private static final Function<String, Character> CHARACTER_PARSER = string -> {
        if (string.length() > 1) {
            throw new IllegalArgumentException("Expected a character, but received a string.");
        }
        return string.charAt(0);
    };

}
