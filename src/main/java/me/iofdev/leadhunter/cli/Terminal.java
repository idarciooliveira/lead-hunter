package me.iofdev.leadhunter.cli;

import java.io.Console;

/**
 * Terminal detection and the soft screen clear for the interactive mode. Everything is a no-op when
 * stdout is not a terminal, so piped runs and tests never see an escape sequence. See ADR 0026.
 */
final class Terminal {

    /** Cursor home plus clear screen. Older lines stay in the scrollback. */
    private static final String SOFT_CLEAR = "\u001B[H\u001B[2J";

    private final boolean terminal;
    private final Runnable clearAction;

    private Terminal(boolean terminal, Runnable clearAction) {
        this.terminal = terminal;
        this.clearAction = clearAction;
    }

    /**
     * Backward-compatible constructor for tests: escapes go to the given writer-like string builder,
     * or nowhere when {@code null}. Production code uses {@link #live()}.
     */
    static Terminal testing(boolean terminal, StringBuilder sink) {
        return new Terminal(terminal, sink == null ? null : () -> sink.append(SOFT_CLEAR));
    }

    /** The real terminal: detection comes from {@code Console.isTerminal()} (Java 22+). */
    static Terminal live() {
        boolean terminal = detect();
        return new Terminal(terminal, () -> System.out.print(SOFT_CLEAR));
    }

    boolean isTerminal() {
        return terminal;
    }

    /** Moves the terminal cursor home and clears the screen; appends to the sink in tests. */
    void clear() {
        if (terminal && clearAction != null) {
            clearAction.run();
        }
    }

    static boolean detect() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            return (boolean) Console.class.getMethod("isTerminal").invoke(console);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
