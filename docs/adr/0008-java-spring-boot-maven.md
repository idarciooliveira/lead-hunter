# 0008. Use Java 21, Spring Boot 4, and Maven

- Date: 2026-09-28
- Status: Accepted

## Context

The first plan was NestJS. The owner uses Spring Boot at work and prefers to build with it here. Spring Boot 4.1 is the current stable line; the 3.x line has reached the end of its open source support.

## Decision

- Java 21, using virtual threads for I/O-heavy steps such as crawling websites.
- Spring Boot 4.1 with Maven and the Maven wrapper.
- Base package `me.iofdev.leadhunter`.
- Spring's `RestClient` for HTTP, Jsoup for HTML parsing, Jackson for JSON and YAML.

## Consequences

Same tools as the owner's day job, so less context switching. Spring Boot 4 moved to Jackson 3 and split auto-configuration into more modules, so some older examples online won't apply as written.
