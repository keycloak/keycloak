package org.keycloak.misc.vulns;

import java.util.Map;
import java.util.Set;

public class CliArgs {

    private static final Set<String> SUPPORTED_ARGS = Set.of(
        "output",
        "repository",
        "stream",
        "list-issues",
        "update-issues"
    );

    private Map<String, String> args;

    public CliArgs(String[] args) {
    }
}
