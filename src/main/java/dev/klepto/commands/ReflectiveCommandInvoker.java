package dev.klepto.commands;

import com.google.common.collect.ObjectArrays;
import lombok.Value;
import lombok.val;

import java.lang.reflect.Method;

/**
 * Reflective implementation of {@link CommandInvoker}. Constructor of this class acts as a
 * {@link CommandInvokerProvider} and is used as a default command invoker within the library. This behavior can be
 * changed when building {@link Commands} via {@link CommandsBuilder} by calling
 * {@link CommandsBuilder#setInvokerProvider(CommandInvokerProvider)}.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
@Value
public class ReflectiveCommandInvoker implements CommandInvoker {

    Object listener;
    Method method;

    @Override
    public void invoke(Object context, Object... parameters) throws Exception {
        val contextAndParameters = ObjectArrays.concat(context, parameters);
        method.invoke(listener, contextAndParameters);
    }

}
