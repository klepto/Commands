package dev.klepto.commands;

import lombok.Value;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Command method filter annotation container. Commands filter annotations can either annotate the command method method
 * or the method container class itself. Method annotations should always override the container class annotations.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Value
public class CommandMethodFilter {

    Annotation annotation;
    CommandFilter filter;

    @SuppressWarnings("unchecked")
    public boolean filter(Object context, String key, List<String> arguments) {
        return filter.filter(context, annotation, key, arguments);
    }

}
