package me.iofdev.leadhunter.cli;

import java.io.BufferedReader;
import java.io.Console;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import me.iofdev.leadhunter.campaign.CampaignRepository;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "menu",
        description = "Open the interactive menu. Also starts when you run lead-hunter with no arguments in a terminal.",
        mixinStandardHelpOptions = true)
class MenuCommand implements Runnable {

    @Spec
    CommandSpec spec;

    private final CampaignRepository campaigns;

    MenuCommand(CampaignRepository campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public void run() {
        CommandLine self = spec.commandLine();
        CommandLine root = self.getParent() != null ? self.getParent() : self;
        Prompter prompter = new Prompter(
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)), self.getOut(),
                "input ended");
        new Menu(prompter, root::execute, campaigns::findAll).run();
    }

    /** True when both stdin and stdout are a terminal. Piped and scheduled runs never get the menu. */
    static boolean interactive() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            // Console.isTerminal() exists from Java 22. Since then System.console() is set even when piped.
            return (boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
