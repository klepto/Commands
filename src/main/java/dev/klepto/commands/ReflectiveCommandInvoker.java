package dev.klepto.commands;

import com.google.common.collect.ObjectArrays;
import lombok.Value;
import lombok.val;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Value
public class ReflectiveCommandInvoker implements CommandInvoker {

    Object listener;
    Method method;

    @Override
    public void invoke(Object context, Object... parameters) {
        val contextAndParameters = ObjectArrays.concat(context, parameters);

        try {
            method.invoke(listener, contextAndParameters);
        } catch (IllegalAccessException | InvocationTargetException cause) {
            throw new RuntimeException(cause);
        }
    }

}
