package dev.klepto.commands;


import dev.klepto.commands.annotation.Command;
import dev.klepto.commands.annotation.DefaultValue;
import dev.klepto.commands.annotation.Remaining;
import lombok.val;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link Commands}.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
public class CommandsTest {

    @Test
    public void registerAndExecute_ReturnsSuccess() {
        val container = new Object() {
            @Command public void test(User user) { }
        };

        val commands = CommandsBuilder.forType(User.class).build();
        commands.register(container);

        val resultType = commands.execute(new User(), "test").getType();
        assertThat(resultType).isEqualTo(CommandResult.Type.SUCCESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void register_WithNonVoidCommandMethod_ThrowsException() {
        val container = new Object() {
            @Command public int test(User user) { return 0; }
        };

        CommandsBuilder.forType(User.class).build().register(container);
    }

    @Test(expected = IllegalArgumentException.class)
    public void register_WithDuplicateCommandKeys_ThrowsException() {
        val container = new Object() {
            @Command public void test(User user) { }
            @Command(keys = {"test"}) public void anotherTest(User user) { }
        };

        CommandsBuilder.forType(User.class).build().register(container);
    }

    @Test
    public void register_WithContainerAnnotation_InheritsAnnotation() {
        val commands = CommandsBuilder.forType(User.class).addFilter(Disabled.class, new DisabledFilter()).build();
        val container = new DisabledCommandContainer();
        commands.register(container);

        val result = commands.execute(new User(), "test").getType();
        assertThat(result).isEqualTo(CommandResult.Type.NO_ACCESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void register_WithoutContextAsFirstParameter_ThrowsException() {
        val container = new Object() {
            @Command public void test(String string, User user) {}
        };
        CommandsBuilder.forType(User.class).build().register(container);
    }

    @Test
    public void register_WithDefaultValue_ReturnsDefaultValueOnExecute() {
        val result = new AtomicReference<String>();
        val container = new Object() {
            @Command public void test(User user, @DefaultValue("hello") String input) {
                result.set(input);
            }
        };
        val commands = CommandsBuilder.forType(User.class).build();
        commands.register(container);
        commands.execute(new User(), "test");

        assertThat(result.get()).isEqualTo("hello");
    }

    @Test(expected = IllegalArgumentException.class)
    public void register_WithArgumentWithoutParser_ThrowsException() {
        val container = new Object() {
            @Command public void test(User user, User otherUser) { }
        };
        CommandsBuilder.forType(User.class).build().register(container);
    }

    @Test(expected = IllegalArgumentException.class)
    public void register_WithNonDefaultArgumentAfterDefaultArgument_ThrowsException() {
        val container = new Object() {
            @Command public void test(User user, @DefaultValue("hello") String inputA, String inputB) { }
        };
        CommandsBuilder.forType(User.class).build().register(container);
    }

    @Test
    public void execute_WithNonExistingKey_ReturnsKeyNotFound() {
        val commands = CommandsBuilder.forType(User.class).build();
        val resultA = commands.execute(new User(), "").getType();
        val resultB = commands.execute(new User(), "test").getType();
        assertThat(resultA).isEqualTo(CommandResult.Type.KEY_NOT_FOUND);
        assertThat(resultB).isEqualTo(CommandResult.Type.KEY_NOT_FOUND);
    }

    @Test
    public void execute_WithNonEmptyHelpMessage_ReturnsHelpMessageOnError() {
        val container = new Object() {
            @Command(help = "hello") public void test(User user) { throw new RuntimeException();  }
        };
        val commands = CommandsBuilder.forType(User.class).build();
        commands.register(container);

        val help = commands.execute(new User(), "test").getHelpMessage();
        assertThat(help).isEqualTo("hello");
    }

    @Test
    public void execute_WithRemainingAnnotation_UsesLimitedParameters() {
        val result = new AtomicReference<String>();
        val container = new Object() {
            @Command public void test(User user, @Remaining String input) {
                result.set(input);
            }
        };
        val commands = CommandsBuilder.forType(User.class).build();
        commands.register(container);
        commands.execute(new User(), "test a b c");

        assertThat(result.get()).isEqualTo("a b c");
    }

    @Test
    public void execute_WithNotEnoughArguments_ReturnsArgumentMismatch() {
        val container = new Object() {
            @Command public void test(User user, String input) { }
        };
        val commands = CommandsBuilder.forType(User.class).build();
        commands.register(container);

        val result = commands.execute(new User(), "test").getType();
        assertThat(result).isEqualTo(CommandResult.Type.ARGUMENT_MISMATCH);
    }

    private static class User { }

    @Disabled
    private static class DisabledCommandContainer {
        @Command public void test(User user) { }
    }

    private static class DisabledFilter implements CommandFilter<User, Disabled> {
        @Override
        public boolean filter(User user, Disabled annotation, String key, List<String> arguments) {
            return false;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface Disabled {

    }

}
