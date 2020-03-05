package dev.klepto.commands;

import java.lang.annotation.Annotation;
import java.util.List;

public interface CommandFilter<C, T extends Annotation>  {

    boolean filter(C context, T annotation, String key, List<String> arguments);

}
