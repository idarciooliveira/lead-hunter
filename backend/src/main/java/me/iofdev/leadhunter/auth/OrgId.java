package me.iofdev.leadhunter.auth;

/**
 * The organization a call works in (ADR 0043). Every repository method that reads or writes tenant data takes one,
 * so a method that forgets it does not compile.
 */
public record OrgId(String value) {

    public OrgId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("an organization id cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
