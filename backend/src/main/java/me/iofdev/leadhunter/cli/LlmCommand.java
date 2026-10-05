package me.iofdev.leadhunter.cli;

import java.io.PrintWriter;

import me.iofdev.leadhunter.llm.LlmClient;
import me.iofdev.leadhunter.llm.LlmRequest;
import me.iofdev.leadhunter.llm.LlmResponse;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "llm",
        description = "Check the LLM gateway setup.",
        mixinStandardHelpOptions = true,
        subcommands = LlmCommand.Test.class)
class LlmCommand implements Runnable {

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }

    @Command(name = "test", description = "Send one prompt to the configured model and print the answer and token usage.")
    static class Test implements Runnable {

        static final String DEFAULT_PROMPT = "Escreve uma mensagem de WhatsApp com no máximo 3 frases, em português de Angola, "
                + "a apresentar uma software house de Luanda ao dono de uma clínica que ainda não tem site.";

        @Spec
        CommandSpec spec;

        @Parameters(arity = "0..1", description = "Prompt. Defaults to a short WhatsApp pitch in Portuguese.")
        String prompt;

        private final LlmClient llm;

        Test(LlmClient llm) {
            this.llm = llm;
        }

        @Override
        public void run() {
            PrintWriter out = spec.commandLine().getOut();
            out.printf("Model: %s%n", llm.model());
            out.flush();
            long started = System.nanoTime();
            LlmResponse response = llm.complete(LlmRequest.text(null, prompt == null ? DEFAULT_PROMPT : prompt).forCampaign(null, "test"));
            long millis = (System.nanoTime() - started) / 1_000_000;
            out.println();
            out.println(response.text());
            out.println();
            out.printf("Served by %s in %d ms, %d prompt tokens, %d completion tokens, cost %s%n",
                    response.model(), millis, response.promptTokens(), response.completionTokens(),
                    response.costUsd() == null ? "not reported" : "$" + response.costUsd().toPlainString());
        }
    }
}
