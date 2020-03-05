package dev.klepto.commands;

import java.lang.reflect.Method;

/**
 * Interface for providing {@link CommandInvoker} instances for given command {@link Method Methods}. Default
 * implementation creates new instances of {@link ReflectiveCommandInvoker} and calls command methods via reflection.
 * If reflection performance is a concern, this interface allows for your own implementation of command call-sites
 * such as method handles, custom bytecode generation or use of libraries like Reflectasm.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
public interface CommandInvokerProvider {

    /**
     * Provides a {@link CommandInvoker} for a given command method.
     *
     * @param container the object containing command method
     * @param method    the command method
     * @return a command invoker that will only invoke given command method
     */
    CommandInvoker provideInvoker(Object container, Method method);

}
