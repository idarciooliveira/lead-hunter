package me.iofdev.leadhunter.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@ConditionalOnProperty(name = "leadhunter.cli.enabled", havingValue = "true", matchIfMissing = true)
class CliRunner implements CommandLineRunner, ExitCodeGenerator {

    private final SpringCommandFactory factory;
    private int exitCode;

    CliRunner(SpringCommandFactory factory) {
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        exitCode = newCommandLine(factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    static CommandLine newCommandLine(CommandLine.IFactory factory) {
        CommandLine commandLine = new CommandLine(RootCommand.class, factory);
        commandLine.setExecutionExceptionHandler((ex, cmd, parseResult) -> {
            cmd.getErr().println("error: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
            return 1;
        });
        return commandLine;
    }
}
