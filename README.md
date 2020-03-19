# Commands
[![Maven Central](https://img.shields.io/maven-central/v/dev.klepto.commands/commands.svg)](https://search.maven.org/search?q=g:dev.klepto.commands) [![Gradle CI](https://github.com/klepto/Commands/workflows/ci/badge.svg)](https://github.com/klepto/Commands/actions?query=workflow%3Aci) [![License: MIT](https://img.shields.io/badge/License-MIT-orange.svg)](https://github.com/klepto/Commands/blob/master/LICENSE)

Commands removes the manual-labor involved in parsing text-based commands by automatically parsing strings into java objects,
selecting appropriate command listener methods, handling default values and applying domain-specific filters.

## Installation
To use Commands with gradle, please use the following configuration:
```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation "dev.klepto.commands:commands:1.0.1"
}
```

## Getting Started
Configuring a command method.
```java
public class CommandContainer {

    @Command(keys = {"!helloworld"})
    public void helloWorld(User user) {
        user.reply("Hello World!");
    }

}
```
Creating Commands instance and registering a command method.
```java
Commands commands = CommandsBuilder.forType(User.class).build();
commands.register(new CommandContainer());
```
Parsing user message & executing the command.
```java
commands.execute(user, message);
```

## Argument Parsers
By default, Commands are only capable of parsing strings into primitive types. 
You can add your domain specific parsers by using `addParser` within `CommandsBuilder`.
```java
Commands commands = CommandsBuilder.forType(User.class)
            .addParser(User.class, users::getByName).build();
```
Now we can accept `User` as our command method parameter.
```java
@Command(keys = {"!kick", "!k"})
public void kick(User user, User victim) {
    victim.kick(10);
    user.reply("Successfully kicked user " + victim.getName() + " for 10 minutes!");
}
```

## Default Values
You can specify default values for the rightmost parameters using `@DefaultValue` annotation.
```java
@Command(keys = {"!kick", "!k"})
public void kick(User user, User victim, @DefaultValue("10") int duration) {
    victim.kick(duration);
    user.reply("Successfully kicked user " + victim.getName() + " for " + duration + " minutes!");
}
```

Now user can trigger command by both `!kick <name>` and `!kick <name> [duration]`.

## Command Filters
Commands allow you to filter command access by creating your own annotations. 
This is useful when you want to apply domain-specific filters such as checking the priviliges of the user.

Defining custom annotation.
```java
@Retention(RetentionPolicy.RUNTIME)
public @interface AdministratorAccess { }
```

Defining CommandFilter for our annotation.
```java
public class AdministratorAccessFilter implements CommandFilter<User, AdministratorAccess> {

    @Override
    public boolean filter(User user, AdministratorAccess annotation, String key, List<String> arguments) {
        return user.isAdministrator();
    }

}
```

Adding our new `CommandFilter` to the `Commands` instance.
```java
Commands commands = CommandsBuilder.forType(User.class)
            .addParser(User.class, users::getByName)
            .addFilter(AdministratorAccess.class, new AdministratorAccessFilter()).build();
```
Annotating our command. Filter annotations can be used both on individual methods and the class that contains the command methods.
```java
@AdministratorAccess
@Command(keys = {"!kick", "!k"})
public void kick(User user, User victim, @DefaultValue("10") int duration) {
    victim.kick(duration);
    user.reply("Successfully kicked user " + victim.getName() + " for " + duration + " minutes!");
}
```

## Text Input
You can accept remaining arguments as one method parameter by annotating your last parameter with `@Remaining` annotation.
```java
@AdministratorAccess
@Command(keys = {"!kick", "!k"})
public void kick(User user,
                 User victim,
                 @DefaultValue("10") int duration,
                 @DefaultValue("No reason given") @Remaining String reason) {
    victim.kick(duration, reason);
    user.reply("Successfully kicked user " + victim.getName() + " for " + duration + " minutes!");
}
```

# License
This project is available under the terms of the MIT license. See the [LICENSE](https://github.com/klepto/Commands/blob/master/LICENSE) file for the copyright information and licensing terms.
