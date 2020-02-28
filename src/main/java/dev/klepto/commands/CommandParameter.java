package dev.klepto.commands;

import lombok.Value;

@Value
public class CommandParameter {

    Class<?> type;
    String defaultValue;

}
