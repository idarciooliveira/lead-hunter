package me.iofdev.leadhunter.cli;

import java.io.Console;

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
    private final CliOrg orgs;

    MenuCommand(CampaignRepository campaigns, CliOrg orgs) {
        this.campaigns = campaigns;
        this.orgs = orgs;
    }

    @Override
    public void run() {
        CommandLine self = spec.commandLine();
        CommandLine root = self.getParent() != null ? self.getParent() : self;
        Prompter prompter = Prompter.stdin(self.getOut(), "input ended");
        String org = ((RootCommand) spec.root().userObject()).org;
        new Menu(prompter, args -> root.execute(withOrg(org, args)), () -> campaigns.findAll(orgs.require(org))).run();
    }

    /** Picocli resets options on every execute, so each menu action repeats the {@code --org} the menu started with. */
    private static String[] withOrg(String org, String[] args) {
        if (org == null || org.isBlank()) {
            return args;
        }
        String[] full = new String[args.length + 2];
        full[0] = "--org";
        full[1] = org;
        System.arraycopy(args, 0, full, 2, args.length);
        return full;
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
