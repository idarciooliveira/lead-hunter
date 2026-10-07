package me.iofdev.leadhunter.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(
        name = "lead-hunter",
        mixinStandardHelpOptions = true,
        version = "lead-hunter 0.1.0",
        description = "Finds PME leads on Google Maps and ranks them for outreach.",
        subcommands = {CompanyCommand.class, CampaignCommand.class, LeadsCommand.class, UsageCommand.class, LlmCommand.class,
                UsersCommand.class, OrgsCommand.class, MembersCommand.class, MenuCommand.class})
class RootCommand implements Runnable {

    @Option(names = "--org", paramLabel = "SLUG",
            description = "The organization to work in, given before the command. Default: LEADHUNTER_ORG, or the only organization.")
    String org;

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        if (MenuCommand.interactive()) {
            ((Runnable) spec.commandLine().getSubcommands().get("menu").getCommand()).run();
            return;
        }
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
