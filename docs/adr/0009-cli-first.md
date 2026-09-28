# 0009. Build a CLI first with picocli; add a web UI later in the same codebase

- Date: 2026-09-28
- Status: Accepted

## Context

The quality of the leads decides whether the tool is worth anything. A UI doesn't fix bad leads, so building it first would be wasted work.

## Decision

Version 1 is a command line app built on picocli inside a non-web Spring Boot application. We wire picocli to Spring through a small `IFactory` instead of the picocli starter, which targets older Spring Boot versions.

Commands are non-interactive, so Railway can run them as scheduled jobs. The web UI comes later as a Spring profile in the same application, reusing the same services.

## Consequences

Fast to build and easy to script. Reading lead cards in a terminal is less pleasant than in a browser, which we accept until the leads prove themselves.
