package dev.klepto.commands;

import dev.klepto.commands.annotation.Command;
import lombok.*;
import org.junit.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link CommandsBuilder}.
 *
 * @author <a href="https://github.com/klepto">Augustinas R.</a>
 */
public class CommandsBuilderTest {

    @Test
    public void setDelimiter_MatchesCommandsDelimiter() {
        val builder = CommandsBuilder.forType(User.class);
        val defaultDelimiterCommands = builder.build();
        val hyphenDelimiterCommands = builder.setDelimiter('-').build();
        val doubleUnderscoreDelimiterCommands = builder.setDelimiter("__").build();
        val caretDelimiterCommands = builder.setDelimiter(Pattern.compile("\\^")).build();

        val user = new User();
        val receivedInput = new AtomicReference<String>();
        val inputConsumer = (Consumer<String>) receivedInput::set;
        defaultDelimiterCommands.register(new CommandListener(inputConsumer));
        hyphenDelimiterCommands.register(new CommandListener(inputConsumer));
        doubleUnderscoreDelimiterCommands.register(new CommandListener(inputConsumer));
        caretDelimiterCommands.register(new CommandListener(inputConsumer));

        defaultDelimiterCommands.execute(user, "test hello");
        assertThat(receivedInput.get()).isEqualTo("hello");
        receivedInput.set(null);

        hyphenDelimiterCommands.execute(user, "test-hello");
        assertThat(receivedInput.get()).isEqualTo("hello");
        receivedInput.set(null);

        doubleUnderscoreDelimiterCommands.execute(user, "test__hello");
        assertThat(receivedInput.get()).isEqualTo("hello");
        receivedInput.set(null);

        caretDelimiterCommands.execute(user, "test^hello");
        assertThat(receivedInput.get()).isEqualTo("hello");
        receivedInput.set(null);
    }

    @Test
    public void setInvokerProvider_MatchesCommandsInvokerProvider() {
        val invokerAccessed = new AtomicBoolean(false);
        val invokerProvider = (CommandInvokerProvider) (container, method) ->
                (CommandInvoker) (context, parameters) -> invokerAccessed.set(true);
        val commands = CommandsBuilder.forType(User.class).setInvokerProvider(invokerProvider).build();
        commands.register(new CommandListener(string -> {}));
        commands.execute(new User(), "test hello");

        assertThat(invokerAccessed.get()).isTrue();
    }

    @Test
    public void addParser_MatchesCommandsParser() {
        val commands = CommandsBuilder.forType(User.class).addParser(String.class, argument -> "bye").build();
        val receivedInput = new AtomicReference<String>();
        commands.register(new CommandListener(receivedInput::set));
        commands.execute(new User(), "test hello");

        assertThat(receivedInput.get()).isEqualTo("bye");
    }

    @Test
    public void addFilter_MatchesCommandsFilter() {
        val commands = CommandsBuilder.forType(User.class).addFilter(AdminAccess.class, new AdminFilter()).build();
        commands.register(new CommandListener(input -> {}));

        val user = new User();
        val noAccessResult = commands.execute(user, "admintest");
        assertThat(noAccessResult.getType()).isEqualTo(CommandResult.Type.NO_ACCESS);

        user.setAdmin(true);
        val successResult = commands.execute(user, "admintest");
        assertThat(successResult.getType()).isEqualTo(CommandResult.Type.SUCCESS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addFilter_WithoutRuntimeRetention_ThrowsException() {
        CommandsBuilder.forType(User.class)
                .addFilter(NoRetentionAdminAccess.class, new NoRetentionAdminFilter()).build();
    }

    @RequiredArgsConstructor
    private static class CommandListener {
        private final Consumer<String> inputConsumer;

        @Command
        public void test(User user, String input) {
            inputConsumer.accept(input);
        }

        @Command
        @AdminAccess
        public void adminTest(User user) {
        }
    }

    private static class AdminFilter implements CommandFilter<User, AdminAccess> {
        @Override
        public boolean filter(User user, AdminAccess annotation, String key, List<String> arguments) {
            return user.isAdmin();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface AdminAccess {

    }

    private static class NoRetentionAdminFilter implements CommandFilter<User, NoRetentionAdminAccess> {
        @Override
        public boolean filter(User user, NoRetentionAdminAccess annotation, String key, List<String> arguments) {
            return user.isAdmin();
        }
    }

    private static @interface NoRetentionAdminAccess {

    }

    @Getter @Setter
    private static class User {
        private boolean admin;
    }

}
