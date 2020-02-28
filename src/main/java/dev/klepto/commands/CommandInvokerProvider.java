package dev.klepto.commands;

import java.lang.reflect.Method;

public interface CommandInvokerProvider {

    CommandInvoker provideInvoker(Object listener, Method method);

}
