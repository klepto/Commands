package dev.klepto.commands;

public interface CommandInvoker {

    void invoke(Object context, Object... parameters);

}
