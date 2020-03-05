package dev.klepto.commands;

import lombok.Value;

import java.lang.annotation.Annotation;
import java.util.List;

@Value
public class CommandMethodFilter {

    Annotation annotation;
    CommandFilter filter;

    @SuppressWarnings("unchecked")
    public boolean filter(Object context, String key, List<String> arguments) {
        return filter.filter(context, annotation, key, arguments);
    }

}
