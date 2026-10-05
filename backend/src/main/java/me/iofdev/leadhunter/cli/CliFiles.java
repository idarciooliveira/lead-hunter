package me.iofdev.leadhunter.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.ClassPathResource;

final class CliFiles {

    private CliFiles() {
    }

    static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read " + file + ": " + e.getMessage());
        }
    }

    static void printResource(PrintWriter out, String name) {
        try (InputStream in = new ClassPathResource(name).getInputStream()) {
            out.print(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new IllegalStateException("template missing from the jar", e);
        }
    }
}
