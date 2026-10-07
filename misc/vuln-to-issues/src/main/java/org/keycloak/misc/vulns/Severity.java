package org.keycloak.misc.vulns;

public enum Severity {

    CRITICAL, HIGH, MEDIUM, LOW;

    public static Severity of(String severity) {
        return Severity.valueOf(severity.toUpperCase());
    }

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
